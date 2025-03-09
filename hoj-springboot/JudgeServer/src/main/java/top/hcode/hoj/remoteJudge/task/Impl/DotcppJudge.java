package top.hcode.hoj.remoteJudge.task.Impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

import top.hcode.hoj.remoteJudge.entity.RemoteJudgeDTO;
import top.hcode.hoj.remoteJudge.entity.RemoteJudgeRes;
import top.hcode.hoj.remoteJudge.task.RemoteJudgeStrategy;
import top.hcode.hoj.util.Constants;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import top.hcode.hoj.util.OCREngineUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;

import java.awt.image.BufferedImage;
import java.net.SocketTimeoutException;
import org.springframework.util.CollectionUtils;

@Slf4j(topic = "hoj")
public class DotcppJudge extends RemoteJudgeStrategy {
    public static final String HOST = "https://www.dotcpp.com";
    public static final String CAPTCHA_API = "/oj/vcode.php";
    public static final String LOGIN_API = "/oj/login.php";
    private static final String SUBMIT_API = "/oj/submit.php";
    private static final String SUBMISSION_RESULT_API = "/oj/submit_status.php?sid=%s";

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
        statusTypeMap.put("正确", Constants.Judge.STATUS_ACCEPTED);
        statusTypeMap.put("格式错误", Constants.Judge.STATUS_PRESENTATION_ERROR);
        statusTypeMap.put("答案错误", Constants.Judge.STATUS_WRONG_ANSWER);
        statusTypeMap.put("时间超限", Constants.Judge.STATUS_TIME_LIMIT_EXCEEDED);
        statusTypeMap.put("内存超限", Constants.Judge.STATUS_MEMORY_LIMIT_EXCEEDED);
        statusTypeMap.put("输出超限", Constants.Judge.STATUS_WRONG_ANSWER);
        statusTypeMap.put("运行错误", Constants.Judge.STATUS_RUNTIME_ERROR);
        statusTypeMap.put("编译错误", Constants.Judge.STATUS_COMPILE_ERROR);
        statusTypeMap.put("等待", Constants.Judge.STATUS_JUDGING);
        statusTypeMap.put("等待重判", Constants.Judge.STATUS_JUDGING);
        statusTypeMap.put("编译中", Constants.Judge.STATUS_COMPILING);
        statusTypeMap.put("运行并评判", Constants.Judge.STATUS_JUDGING);
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
                    throw new RuntimeException("[DOTCPP] Failed to submit!");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("[DOTCPP] Failed to submit! The msg is " + e);
        }
    }

    public String submitCode() throws IOException {
        RemoteJudgeDTO remoteJudgeDTO = getRemoteJudgeDTO();

        String cookie_ = remoteJudgeDTO.getCookies().stream()
                .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                .collect(Collectors.joining("; ", "", ";"));

        String submit_url = String.format(HOST + SUBMIT_API);

        String O2 = remoteJudgeDTO.getLanguage().contains("O2") ? "1" : "0";

        Connection.Response response = Jsoup.connect(submit_url)
                .header("Cookie", cookie_)
                .header("Connection", "keep-alive")
                .data("id", remoteJudgeDTO.getProblemNum())
                .data("language", remoteJudgeDTO.getKey())
                .data("ifO2", O2)
                .data("source", remoteJudgeDTO.getUserCode() + getRandomBlankString())
                .method(Connection.Method.POST)
                .ignoreContentType(true)
                .execute();

        String html = response.body();

        if (html.contains("内只能提交一次代码")) {
            // 再试一次
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException ignore) {
            }
            submitCode();
        }

        String submissionId = null;
        Pattern pattern = Pattern.compile("sid=(\\d+)");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            submissionId = matcher.group(1);
        } else {
            log.info("[DOTCPP] Failed Processing Html: " + html);
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

        Document document = Jsoup.parse(html);

        String status = document.select(".head_box_0").text().trim().split(":")[0];

        Integer time = 0;
        Integer memory = 0;

        // 使用单一正则表达式匹配
        Matcher matcher = Pattern.compile("运行时间: (\\d+)ms.*?消耗内存: (\\d+)KB").matcher(html);
        if (matcher.find()) {
            time = Integer.parseInt(matcher.group(1));
            memory = Integer.parseInt(matcher.group(2));
        }

        // 获取状态码
        Constants.Judge judgeStatus = statusTypeMap.get(status);

        return RemoteJudgeRes.builder()
                .status(judgeStatus.getStatus())
                .memory(memory)
                .time(time)
                .build();
    }

    @Override
    public void login() {
        // 清除当前线程的 cookies 缓存
        HttpRequest.getCookieManager().getCookieStore().removeAll();
        RemoteJudgeDTO remoteJudgeDTO = getRemoteJudgeDTO();

        String username = remoteJudgeDTO.getUsername();
        String password = remoteJudgeDTO.getPassword();

        List<HttpCookie> cookies = remoteJudgeDTO.getCookies();

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
                if (loginResponse.isOk()
                        && !loginResponse.body().contains("验证码错误")
                        && !loginResponse.body().contains("用户名或者密码错误")) {

                    log.info("[DOTCPP] Username: {} Login successful!", username);
                    remoteJudgeDTO.setLoginStatus(200);
                    remoteJudgeDTO.setCookies(cookies);
                    remoteJudgeDTO.setMethod("0");
                    return;
                }

                TimeUnit.SECONDS.sleep(2);
            } catch (SocketTimeoutException e) {
                log.warn("[DOTCPP] Username: {} Login attempt {} timed out. Retrying...", username, attempt);
                refreshCookies = true; // 超时重试
            } catch (Exception e) {
                log.error("[DOTCPP] Username: {} Login failed: {}", username, e.getMessage());
                refreshCookies = true; // IO错误或其他异常，继续重试
            }

            // 达到最大重试次数
            if (attempt == MAX_ATTEMPTS) {
                log.warn("[DOTCPP] Username: {} Reached max retry limit. Exiting.", username);
                remoteJudgeDTO.setLoginStatus(404);
                remoteJudgeDTO.setCookies(null);
                throw new RuntimeException("[DOTCPP] Username: " + username + " Failed to login!");
            }

        }
    }

    @Override
    public String getLanguage(String language, String languageKey) {
        return null;
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
                .form("user_id", username) // 添加用户名
                .form("password", password); // 添加密码

        // 如果验证码不为空，加入验证码字段
        if (captcha != null && !captcha.isEmpty()) {
            request.form("vcode", captcha);
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

                // 判断验证码是否为全字母和数字并且长度为4
                if (predict != null && predict.length() == 4 && Pattern.matches("^[a-zA-Z0-9]+$", predict)) {
                    return predict.toUpperCase(); // 转换为大写后返回
                }
            }

            // 如果验证码识别失败次数达到上限，抛出异常
            if (++totalAttempts >= MAX_TOTAL_ATTEMPTS) {
                throw new Exception("[DOTCPP] Captcha recognition failed more than " + MAX_TOTAL_ATTEMPTS
                        + " times. Stopping attempts.");
            }

            TimeUnit.SECONDS.sleep(2);
            return handleLoginCaptcha(cookies); // 递归调用直到成功

        } catch (SocketTimeoutException e) {
            // 捕获超时异常并重试
            if (++timeoutAttempts >= MAX_TIMEOUTS) {
                throw new Exception(
                        "[DOTCPP] Captcha request timeout exceeded " + MAX_TIMEOUTS + " times. Stopping attempts.");
            }

            TimeUnit.SECONDS.sleep(2);
            return handleLoginCaptcha(cookies); // 超时后递归重试
        }
    }

    protected String getRandomBlankString() {
        StringBuilder string = new StringBuilder("\n");
        int random = new Random().nextInt(Integer.MAX_VALUE);
        while (random > 0) {
            string.append(random % 2 == 0 ? ' ' : '\t');
            random /= 2;
        }
        return string.toString();
    }
}
