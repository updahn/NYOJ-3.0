package top.hcode.hoj.remoteJudge.task.Impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import top.hcode.hoj.remoteJudge.entity.RemoteJudgeDTO;
import top.hcode.hoj.remoteJudge.entity.RemoteJudgeRes;
import top.hcode.hoj.remoteJudge.task.RemoteJudgeStrategy;
import top.hcode.hoj.util.Constants;
import top.hcode.hoj.util.OCREngineUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.util.CollectionUtils;

@Slf4j(topic = "hoj")
public class VJJudge extends RemoteJudgeStrategy {
    private static final String HOST = "https://vjudge.net";
    private static final String LOGIN_API = "/user/login";
    private static final String CHECK_LOGIN_STATUS_API = "/user/checkLogInStatus";
    private static final String CAPTCHA_API = "/util/captcha";
    private static final String SUBMIT_API = "/problem/submit";
    private static final String SUBMISSION_RESULT_API = "/solution/data/%s";

    // 熔断机制，保证尝试登录死循环不会卡死进程
    private static final int MAX_ATTEMPTS = 5; // 最大登录尝试次数
    private static final int MAX_TIMEOUTS = 5; // 最大超时尝试次数
    private static final int MAX_TOTAL_ATTEMPTS = 50; // 验证码识别最大尝试次数

    private static int totalAttempts = 0; // 总验证码识别尝试次数
    private static int timeoutAttempts = 0; // 超时次数

    private static Map<String, String> headers = MapUtil.builder(new HashMap<String, String>())
            .put("Accept", "*/*")
            .put("Content-Type", "application/x-www-form-urlencoded; application/json; application/xml; charset=UTF-8")
            .put("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36")
            .put("X-Requested-With", "XMLHttpRequest")
            .map();

    private static final Map<String, Constants.Judge> statusTypeMap = new HashMap<>();

    static {
        // result返回结果初始化
        statusTypeMap.put("Pending", Constants.Judge.STATUS_PENDING);
        statusTypeMap.put("Submitted", Constants.Judge.STATUS_JUDGING);
        statusTypeMap.put("Accepted", Constants.Judge.STATUS_ACCEPTED);
        statusTypeMap.put("Wrong Answer", Constants.Judge.STATUS_WRONG_ANSWER);
        statusTypeMap.put("Presentation Error", Constants.Judge.STATUS_PRESENTATION_ERROR);
        statusTypeMap.put("Compile Error", Constants.Judge.STATUS_COMPILE_ERROR);
        statusTypeMap.put("Runtime Error", Constants.Judge.STATUS_RUNTIME_ERROR);
        statusTypeMap.put("Time Limit Exceed", Constants.Judge.STATUS_TIME_LIMIT_EXCEEDED);
        statusTypeMap.put("Memory Limit Exceed", Constants.Judge.STATUS_MEMORY_LIMIT_EXCEEDED);
        statusTypeMap.put("Output Limit Exceed", Constants.Judge.STATUS_WRONG_ANSWER);
        statusTypeMap.put("Judge Failed", Constants.Judge.STATUS_NOT_SUBMITTED);
        statusTypeMap.put("Unknown Error", Constants.Judge.STATUS_WRONG_ANSWER);
        statusTypeMap.put("Submit Failed", Constants.Judge.STATUS_NOT_SUBMITTED);
        statusTypeMap.put("Queuing && Judging", Constants.Judge.STATUS_JUDGING);
        statusTypeMap.put("Remote OJ Unavailable", Constants.Judge.STATUS_NOT_SUBMITTED);
        statusTypeMap.put("Daily quota exceeded", Constants.Judge.STATUS_NOT_SUBMITTED);
        statusTypeMap.put("Running", Constants.Judge.STATUS_JUDGING);
        statusTypeMap.put("Duplicate Code", Constants.Judge.STATUS_DUPLICATE_CODE);
    }

    @Override
    public void submit() {
        RemoteJudgeDTO remoteJudgeDTO = getRemoteJudgeDTO();
        if (remoteJudgeDTO.getProblemNum() == null || remoteJudgeDTO.getUserCode() == null) {
            return;
        }
        try {
            login();
        } catch (cn.hutool.core.io.IORuntimeException e) {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException ignored) {
            }
            // 超时重试
            login();
        }

        String submissionId = null;

        try {
            submissionId = submitCode();

            // 提交成功
            if (StringUtils.isNotBlank(submissionId)) {
                remoteJudgeDTO.setSubmitId(submissionId);
            } else {
                // 再试一次
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException ignore) {
                }

                submissionId = submitCode();

                if (StringUtils.isNotBlank(submissionId)) {
                    remoteJudgeDTO.setSubmitId(submissionId);
                } else {
                    throw new RuntimeException("[VJ] Failed to submit!");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("[VJ] Failed to submit! The msg is " + e);
        }
    }

    public String submitCode() throws IOException {
        RemoteJudgeDTO remoteJudgeDTO = getRemoteJudgeDTO();

        String cookie_ = remoteJudgeDTO.getCookies().stream()
                .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                .collect(Collectors.joining("; ", "", ";"));

        String submit_url = String.format(HOST + SUBMIT_API);

        String oj = remoteJudgeDTO.getCompleteProblemId().split("-")[0];
        String problemNum = remoteJudgeDTO.getCompleteProblemId().split("-")[1];
        Connection.Response response = Jsoup.connect(submit_url)
                .header("Cookie", cookie_)
                .header("Connection", "keep-alive")
                .data("open", "1")
                .data("method", remoteJudgeDTO.getMethod())
                .data("language", remoteJudgeDTO.getKey())
                .data("source", remoteJudgeDTO.getUserCode())
                .data("oj", oj)
                .data("probNum", problemNum)
                .data("captcha", "")
                .method(Connection.Method.POST)
                .ignoreContentType(true)
                .execute();

        String html = response.body();

        if (html.equals("{\"error\":\"This problem doesn\\u0027t support submitting by bot account\"}")) {
            // 使用账号重新提交
            remoteJudgeDTO.setMethod("1");
            submitCode();
        } else if (html.startsWith("No verified remote account of")) {
            throw new RuntimeException(
                    "[VJ] Submit Falied: No verified remote account for Username: " + remoteJudgeDTO.getUsername()
                            + "With: " + oj);
        }

        String submissionId = null;
        Pattern pattern = Pattern.compile("\"runId\":(\\d+)");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            submissionId = matcher.group(1);
        } else {
            log.info("[VJ] Failed Processing Html: " + html);
        }

        return submissionId;
    }

    @Override
    public RemoteJudgeRes result() {
        login();

        RemoteJudgeDTO remoteJudgeDTO = getRemoteJudgeDTO();

        List<HttpCookie> cookies_list = remoteJudgeDTO.getCookies();
        String cookie_ = cookies_list.stream()
                .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                .collect(Collectors.joining("; ", "", ";"));

        String submisson_url = String.format(HOST + SUBMISSION_RESULT_API, remoteJudgeDTO.getSubmitId());

        HttpResponse response = HttpRequest.get(submisson_url)
                .cookie(cookie_)
                .execute();

        String html = response.body();

        // 将字符串转换为 JSONObject 对象
        JSONObject jsonObject = JSONUtil.parseObj(html);

        String status = jsonObject.getStr("status").trim();
        Integer time = jsonObject.getInt("runtime");
        Integer memory = jsonObject.getInt("memory");
        String additionalInfo = jsonObject.getStr("additionalInfo");

        // 获取状态码
        Map<String, Constants.Judge> resultValues = statusTypeMap.entrySet().stream()
                .filter(entry -> status.toLowerCase().startsWith(entry.getKey().toLowerCase()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (resultValues.isEmpty()) {
            log.info("[VJ] Failed Processing status: " + status);
            return RemoteJudgeRes.builder()
                    .status(Constants.Judge.STATUS_SUBMITTED_FAILED.getStatus())
                    .errorInfo(status) // 检测对应的状态码
                    .build();
        } else {
            List<Constants.Judge> resultList = new ArrayList<>(resultValues.values());

            return RemoteJudgeRes.builder()
                    .status(resultList.get(0).getStatus())
                    .memory(memory)
                    .time(time)
                    .errorInfo(additionalInfo)
                    .build();

        }

    }

    @Override
    public void login() {
        // 清除当前线程的 cookies 缓存
        HttpRequest.getCookieManager().getCookieStore().removeAll();
        RemoteJudgeDTO remoteJudgeDTO = getRemoteJudgeDTO();

        String username = remoteJudgeDTO.getUsername();
        String password = remoteJudgeDTO.getPassword();

        List<HttpCookie> cookies = remoteJudgeDTO.getCookies();

        // 检查登录状态
        try {
            Boolean isOnline = checkLogInStatus(cookies);

            if (isOnline) {
                remoteJudgeDTO.setLoginStatus(200);
                remoteJudgeDTO.setCookies(cookies);
                remoteJudgeDTO.setMethod("0");
                return;
            }
        } catch (Exception e) {
        }

        boolean refreshCookies = true;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

            try {

                if (refreshCookies) {
                    totalAttempts = 0;
                    timeoutAttempts = 0;
                }

                // 判断是否需要验证码
                HttpResponse loginResponse = login(
                        username,
                        password,
                        refreshCookies ? null : handleLoginCaptcha(cookies),
                        refreshCookies ? null : cookies);

                // 更新 Cookies
                if (refreshCookies) {
                    cookies = loginResponse.getCookies();
                    refreshCookies = false;
                }

                // 登录成功判断
                if (loginResponse.body().contains("success")) {
                    log.info("[VJ] Username: {} Login successful!", username);
                    remoteJudgeDTO.setLoginStatus(200);
                    remoteJudgeDTO.setCookies(cookies);
                    remoteJudgeDTO.setMethod("0");
                    return;
                }

                TimeUnit.SECONDS.sleep(2);
            } catch (SocketTimeoutException e) {
                log.warn("[VJ] Username: {} Login attempt {} timed out. Retrying...", username, attempt);
                refreshCookies = true; // 超时重试
            } catch (Exception e) {
                log.error("[VJ] Username: {} Login failed: {}", username, e.getMessage());
                refreshCookies = true; // IO错误或其他异常，继续重试
            }

            // 达到最大重试次数
            if (attempt == MAX_ATTEMPTS) {
                log.warn("[VJ] Username: {} Reached max retry limit. Exiting.", username);
                remoteJudgeDTO.setLoginStatus(404);
                remoteJudgeDTO.setCookies(null);
                throw new RuntimeException("[VJ] Username: " + username + " Failed to login!");
            }
        }
    }

    @Override
    public String getLanguage(String language, String languageKey) {
        return null;
    }

    /**
     * 检查登录状态的方法
     *
     * @param cookies 用户的Cookies信息
     * @return 返回用户是否在线的状态
     */
    public Boolean checkLogInStatus(List<HttpCookie> cookies) throws Exception {
        // 清除当前线程的cookies缓存
        HttpRequest.getCookieManager().getCookieStore().removeAll();

        boolean isOnline = false;

        for (int attempt = 0; attempt < MAX_TIMEOUTS; attempt++) {
            try {
                // 执行请求
                HttpRequest request = HttpRequest.post(HOST + CHECK_LOGIN_STATUS_API)
                        .headerMap(headers, false)
                        .timeout(3000);

                if (!CollectionUtils.isEmpty(cookies)) {
                    request.cookie(cookies);
                }

                HttpResponse response = request.execute();

                if (response.getStatus() == 200 && "true".equals(response.body())) {
                    isOnline = true;
                    break; // 成功获取状态后跳出循环
                }

                TimeUnit.SECONDS.sleep(2);
            } catch (cn.hutool.core.io.IORuntimeException e) {
                log.warn("[VJ] CheckLoginStatus attempt {} timed out. Retrying...", attempt + 1);
            }
        }
        return isOnline;
    }

    /**
     * 登录方法，通过POST请求登录
     *
     * @param username 用户名
     * @param password 密码
     * @param captcha  验证码（可为空）
     * @param cookies  Cookies信息
     * @return 返回登录后的响应
     */
    public static HttpResponse login(String username, String password, String captcha, List<HttpCookie> cookies) {
        // 清除当前线程的cookies缓存
        HttpRequest.getCookieManager().getCookieStore().removeAll();

        // 构建POST请求
        HttpRequest request = HttpRequest.post(HOST + LOGIN_API)
                .headerMap(headers, false)
                .timeout(5000)
                .form("username", username) // 添加用户名
                .form("password", password); // 添加密码

        // 如果验证码不为空，加入验证码字段
        if (captcha != null && !captcha.isEmpty()) {
            request.form("captcha", captcha);
        }

        // 如果 Cookies 不为空，加入 Cookies
        if (!CollectionUtils.isEmpty(cookies)) {
            request.cookie(cookies);
        }

        // 执行请求并返回响应
        return request.execute();
    }

    /**
     * 处理验证码逻辑，递归调用直到识别成功或超过最大次数
     *
     * @param cookies Cookies信息
     * @return 返回识别到的验证码字符串
     * @throws Exception 当超过最大尝试次数时抛出异常
     */
    public static String handleLoginCaptcha(List<HttpCookie> cookies) throws Exception {

        try {
            // 从URL获取验证码图像
            BufferedImage image = OCREngineUtils.imgFromUrl(HOST + CAPTCHA_API, cookies);

            if (image != null) {
                // 调用OCR引擎识别验证码
                String predict = OCREngineUtils.recognize(image);

                // 判断验证码是否为全字母并且长度为7
                if (predict != null && predict.length() == 7 && Pattern.matches("[a-zA-Z]+", predict)) {
                    return predict.toUpperCase(); // 转换为大写后返回
                }
            }

            // 如果验证码识别失败次数达到上限，抛出异常
            if (++totalAttempts >= MAX_TOTAL_ATTEMPTS) {
                throw new Exception("[VJ] Captcha recognition failed more than " + MAX_TOTAL_ATTEMPTS
                        + " times. Stopping attempts.");
            }

            TimeUnit.SECONDS.sleep(2);
            return handleLoginCaptcha(cookies); // 递归调用直到成功

        } catch (SocketTimeoutException e) {
            // 捕获超时异常并重试
            if (++timeoutAttempts >= MAX_TIMEOUTS) {
                throw new Exception(
                        "[VJ] Captcha request timeout exceeded " + MAX_TIMEOUTS + " times. Stopping attempts.");
            }

            TimeUnit.SECONDS.sleep(2);
            return handleLoginCaptcha(cookies); // 超时后递归重试
        }
    }

    public static List<HttpCookie> parseCookies(String cookieString) {
        List<HttpCookie> cookies = new ArrayList<>();

        // 去掉字符串两端的方括号并分割为单个cookie
        String[] cookieArray = cookieString.replaceAll("[\\[\\]]", "").split(", ");

        // 遍历每个cookie并将其添加到List<HttpCookie>
        for (String cookie : cookieArray) {
            String[] nameValuePair = cookie.split("=", 2); // 按等号分割
            if (nameValuePair.length == 2) {
                // 创建 HttpCookie 对象并加入列表
                cookies.add(new HttpCookie(nameValuePair[0], nameValuePair[1]));
            }
        }

        return cookies;
    }

}
