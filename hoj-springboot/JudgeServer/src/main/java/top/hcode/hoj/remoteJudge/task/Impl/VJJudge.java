package top.hcode.hoj.remoteJudge.task.Impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import top.hcode.hoj.remoteJudge.entity.RemoteJudgeDTO;
import top.hcode.hoj.remoteJudge.entity.RemoteJudgeRes;
import top.hcode.hoj.remoteJudge.task.RemoteJudgeStrategy;
import top.hcode.hoj.util.Constants;
import top.hcode.hoj.util.CookiesUtils;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

@Slf4j(topic = "hoj")
public class VJJudge extends RemoteJudgeStrategy {
    private static final String HOST = "https://vjudge.net";
    private static final String CHECK_LOGIN_STATUS_API = "/user/checkLogInStatus";
    private static final String SUBMIT_API = "/problem/submit";
    private static final String STATUS_API = "/status/data?draw=1&start=0&length=20&un=%s&OJId=%s&probNum=%s";

    private static final String USERNAME_INPUT_SELECTOR = "input[placeholder='Username or Email']";
    private static final String PASSWORD_INPUT_SELECTOR = "input[placeholder='Password']";
    private static final String PASSWORD_LOGIN_BUTTON_SELECTOR = "a.nav-link.login";
    private static final String PASSWORD_LOGOUT_BUTTON_SELECTOR = "a.nav-link.logout";

    private static Map<String, String> headers = MapUtil.builder(new HashMap<String, String>())
            .put("Accept", "*/*")
            .put("Content-Type", "application/x-www-form-urlencoded; application/json; application/xml; charset=UTF-8")
            .put("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0")
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
        statusTypeMap.put("Compilation error", Constants.Judge.STATUS_COMPILE_ERROR);
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
                .data("source", remoteJudgeDTO.getUserCode() + getRandomBlankString())
                .data("oj", oj)
                .data("probNum", problemNum)
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

        String oj = remoteJudgeDTO.getCompleteProblemId().split("-")[0];
        String problemNum = remoteJudgeDTO.getCompleteProblemId().split("-")[1];

        String submisson_url = String.format(HOST + STATUS_API, remoteJudgeDTO.getUsername(), oj, problemNum);

        HttpResponse response = HttpRequest.get(submisson_url)
                .cookie(cookie_)
                .execute();

        String html = response.body();

        JSONObject jsonObject = JSONUtil.parseObj(html);
        JSONArray dataArray = jsonObject.getJSONArray("data");

        for (Object obj : dataArray) {
            JSONObject record = (JSONObject) obj;
            long runId = record.getLong("runId");

            // 比较runId是否与submitId相等
            if (runId == Long.parseLong(remoteJudgeDTO.getSubmitId())) {

                String status = record.getStr("status").trim();
                Integer time = record.getInt("runtime");
                Integer memory = record.getInt("memory");
                String additionalInfo = record.getStr("additionalInfo");

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
        }

        // 提交详情页最多20，提交不成功可能是一个账号同时提交过多
        return RemoteJudgeRes.builder()
                .status(Constants.Judge.STATUS_SUBMITTED_FAILED.getStatus())
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

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(Arrays.asList("--disable-blink-features=AutomationControlled")));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0")
                    .setDeviceScaleFactor(1)
                    .setViewportSize(1366, 768));

            // 隐藏自动化痕迹，避免被检测
            String fileContent = ResourceUtil.readUtf8Str("stealth.min.js");
            context.addInitScript(fileContent);

            if (cookies != null) {
                context.addCookies(CookiesUtils.convertHttpCookieListToPlaywrightCookies(cookies));
            }

            Page page = context.newPage();

            page.navigate(HOST, new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT));

            try {
                // 等待出现退出按钮
                page.waitForSelector(PASSWORD_LOGOUT_BUTTON_SELECTOR,
                        new Page.WaitForSelectorOptions().setTimeout(3000));

            } catch (TimeoutError e) {
                // 点击登录
                page.waitForSelector(PASSWORD_LOGIN_BUTTON_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(3000))
                        .click();

                page.waitForSelector(USERNAME_INPUT_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(3000))
                        .fill(username);
                page.waitForSelector(PASSWORD_INPUT_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(3000))
                        .fill(password);

                page.keyboard().press("Enter");

                try {
                    // 等待出现退出按钮
                    page.waitForSelector(PASSWORD_LOGOUT_BUTTON_SELECTOR,
                            new Page.WaitForSelectorOptions().setTimeout(3000));

                    // 获取并输出 cookies
                    List<Cookie> cookiesSet = page.context().cookies();
                    cookies = CookiesUtils.convertCookiesToHttpCookieList(cookiesSet);

                    log.info("[VJ] Username: {} Login successful!", username);
                    remoteJudgeDTO.setLoginStatus(200);
                    remoteJudgeDTO.setCookies(cookies);
                    remoteJudgeDTO.setMethod("0");
                    return;
                } catch (TimeoutError e2) {
                    remoteJudgeDTO.setLoginStatus(404);
                    remoteJudgeDTO.setCookies(null);
                    throw new RuntimeException("[VJ] Username: " + username + " Failed to login!");
                }
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
        if (!CollectionUtils.isEmpty(cookies)) {
            // 执行请求
            HttpRequest request = HttpRequest.post(HOST + CHECK_LOGIN_STATUS_API)
                    .headerMap(headers, false)
                    .timeout(3000);
            request.cookie(cookies);
            HttpResponse response = request.execute();

            if (response.getStatus() == 200 && "true".equals(response.body())) {
                isOnline = true;
            }

        }
        return isOnline;
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
