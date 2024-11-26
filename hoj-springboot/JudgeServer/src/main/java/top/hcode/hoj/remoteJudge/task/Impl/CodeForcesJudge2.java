package top.hcode.hoj.remoteJudge.task.Impl;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import top.hcode.hoj.pojo.entity.judge.JudgeCase;
import top.hcode.hoj.remoteJudge.entity.RemoteJudgeDTO;
import top.hcode.hoj.remoteJudge.entity.RemoteJudgeRes;
import top.hcode.hoj.remoteJudge.task.RemoteJudgeStrategy;
import top.hcode.hoj.util.Constants;
import top.hcode.hoj.util.CookiesUtils;

import java.net.HttpCookie;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j(topic = "hoj")
public class CodeForcesJudge2 extends RemoteJudgeStrategy {

    public static final String HOST = "https://codeforces.com";
    public static final String LOGIN_URL = "/enter";
    public static final String SUBMIT_URL = "/contest/%s/submit";
    public static final String SUBMISSION_RESULT_URL = "/api/user.status?handle=%s&from=1&count=%s";
    public static final String SUBMIT_SOURCE_URL = "/data/submitSource";
    public static final String SUBMISSION_URL = "/contest/%s/status";
    public static final String SUBMISSION_RUN_ID_URL = "/problemset/status?my=on";

    // 模拟浏览器操作
    private static final String USERNAME_INPUT_SELECTOR = "#handleOrEmail";
    private static final String PASSWORD_INPUT_SELECTOR = "#password";
    private static final String PASSWORD_LOGOUT_BUTTON_SELECTOR = "a[href*='/logout']";

    protected static final Map<String, Constants.Judge> statusMap = new HashMap<String, Constants.Judge>() {
        {
            put("FAILED", Constants.Judge.STATUS_SUBMITTED_FAILED);
            put("OK", Constants.Judge.STATUS_ACCEPTED);
            put("PARTIAL", Constants.Judge.STATUS_PARTIAL_ACCEPTED);
            put("COMPILATION_ERROR", Constants.Judge.STATUS_COMPILE_ERROR);
            put("RUNTIME_ERROR", Constants.Judge.STATUS_RUNTIME_ERROR);
            put("WRONG_ANSWER", Constants.Judge.STATUS_WRONG_ANSWER);
            put("PRESENTATION_ERROR", Constants.Judge.STATUS_PRESENTATION_ERROR);
            put("TIME_LIMIT_EXCEEDED", Constants.Judge.STATUS_TIME_LIMIT_EXCEEDED);
            put("MEMORY_LIMIT_EXCEEDED", Constants.Judge.STATUS_MEMORY_LIMIT_EXCEEDED);
            put("IDLENESS_LIMIT_EXCEEDED", Constants.Judge.STATUS_RUNTIME_ERROR);
            put("SECURITY_VIOLATED", Constants.Judge.STATUS_RUNTIME_ERROR);
            put("CRASHED", Constants.Judge.STATUS_SYSTEM_ERROR);
            put("INPUT_PREPARATION_CRASHED", Constants.Judge.STATUS_SYSTEM_ERROR);
            put("CHALLENGED", Constants.Judge.STATUS_SYSTEM_ERROR);
            put("SKIPPED", Constants.Judge.STATUS_SYSTEM_ERROR);
            put("TESTING", Constants.Judge.STATUS_JUDGING);
            put("REJECTED", Constants.Judge.STATUS_SYSTEM_ERROR);
            put("RUNNING & JUDGING", Constants.Judge.STATUS_JUDGING);
        }
    };

    @Override
    public void submit() {
        RemoteJudgeDTO remoteJudgeDTO = getRemoteJudgeDTO();

        if (remoteJudgeDTO.getCompleteProblemId() == null || remoteJudgeDTO.getUserCode() == null) {
            return;
        }

        login();

        if (remoteJudgeDTO.getLoginStatus() != HttpStatus.SC_MOVED_TEMPORARILY) {
            log.error("[Codeforces] Error Username:[{}], Password:[{}]", remoteJudgeDTO.getUsername(),
                    remoteJudgeDTO.getPassword());
            String msg = "[Codeforces] Failed to Login, possibly due to incorrect remote judge account or password of codeforces!";
            throw new RuntimeException(msg);
        }

        submitCode(remoteJudgeDTO);

        if (remoteJudgeDTO.getSubmitStatus() == 403) {
            // 如果提交出现403可能是cookie失效了，再执行登录，重新提交
            remoteJudgeDTO.setCookies(null);
            login();

            if (remoteJudgeDTO.getLoginStatus() != HttpStatus.SC_MOVED_TEMPORARILY) {
                log.error("[Codeforces] Error Username:[{}], Password:[{}]", remoteJudgeDTO.getUsername(),
                        remoteJudgeDTO.getPassword());
                String msg = "[Codeforces] Failed to Login, possibly due to incorrect remote judge account or password of codeforces!";
                throw new RuntimeException(msg);
            }
            submitCode(remoteJudgeDTO);
            if (remoteJudgeDTO.getSubmitStatus() == 403) {
                String log = String.format("[Codeforces] [%s] [%s]:Failed to submit code, caused by `403 Forbidden`",
                        remoteJudgeDTO.getContestId(), remoteJudgeDTO.getCompleteProblemId());
                throw new RuntimeException(log);
            }
        }
        // 获取提交的题目id
        // Long maxRunId = getMaxRunId(nowTime);
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Long maxRunId = getMaxIdByParseHtmlWithRetry();
        remoteJudgeDTO.setSubmitId(String.valueOf(maxRunId));
    }

    @SuppressWarnings("unchecked")
    private Long getMaxRunId(long nowTime) {

        RemoteJudgeDTO remoteJudgeDTO = getRemoteJudgeDTO();

        int retryNum = 0;
        // 防止cf的nginx限制访问频率，重试5次
        while (retryNum != 10) {
            HttpResponse httpResponse = getMaxIdForSubmissionResult(remoteJudgeDTO.getUsername(), 30);
            if (httpResponse.getStatus() == 200) {
                try {
                    Map<String, Object> json;
                    try {
                        json = JSONUtil.parseObj(httpResponse.body());
                    } catch (JSONException e) {
                        // 接口限制，导致返回数据非json，此处替换成页面解析
                        return getMaxIdByParseHtml();
                    }
                    List<Map<String, Object>> results = (List<Map<String, Object>>) json.get("result");
                    for (Map<String, Object> result : results) {
                        Long runId = Long.valueOf(result.get("id").toString());
                        long creationTimeSeconds = Long.parseLong(result.get("creationTimeSeconds").toString());
                        if (creationTimeSeconds < nowTime && retryNum < 8) {
                            continue;
                        }
                        Map<String, Object> problem = (Map<String, Object>) result.get("problem");
                        if (remoteJudgeDTO.getContestId().equals(problem.get("contestId").toString()) &&
                                remoteJudgeDTO.getProblemNum().equals(problem.get("index").toString())) {
                            return runId;
                        }
                    }
                } catch (Exception e) {
                    String log = String.format("[Codeforces] Failed to get run id for problem: [%s], error:%s",
                            remoteJudgeDTO.getCompleteProblemId(), e.toString());
                    throw new RuntimeException(log);
                }
            }
            retryNum++;
        }
        return -1L;
    }

    // CF的这个接口有每两秒的访问限制，所以需要加锁，保证只有一次查询
    public static synchronized HttpResponse getMaxIdForSubmissionResult(String username, Integer count) {
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String url = HOST + String.format(SUBMISSION_RESULT_URL, username, count);
        return HttpUtil.createGet(url)
                .timeout(30000)
                .execute();
    }

    private Long getMaxIdByParseHtmlWithRetry() {
        int count = 0;
        while (count < 3) {
            try {
                return getMaxIdByParseHtml();
            } catch (Exception e) {
                count++;
                if (count == 3) {
                    throw e;
                }
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException ignored) {
                }
            }
        }
        return null;
    }

    protected String getRunIdUrl() {
        return HOST + SUBMISSION_RUN_ID_URL;
    }

    private Long getMaxIdByParseHtml() {
        // 清除当前线程的cookies缓存
        HttpRequest.getCookieManager().getCookieStore().removeAll();
        RemoteJudgeDTO remoteJudgeDTO = getRemoteJudgeDTO();
        HttpRequest request = HttpUtil.createGet(getRunIdUrl());
        request.cookie(remoteJudgeDTO.getCookies());
        HttpResponse response = request.execute();
        String maxRunIdStr = ReUtil.get("data-submission-id=\"(\\d+)\"", response.body(), 1);
        if (StringUtils.isEmpty(maxRunIdStr)) {
            log.error("[Codeforces] Failed to parse submission html:{}", response.body());
            String log = String.format("[Codeforces] Failed to parse html to get run id for problem: [%s]",
                    remoteJudgeDTO.getCompleteProblemId());
            throw new RuntimeException(log);
        } else {
            return Long.valueOf(maxRunIdStr);
        }
    }

    protected String getResultReferer(String contestId) {
        return String.format(HOST + SUBMISSION_URL, contestId);
    }

    @Override
    public RemoteJudgeRes result() {
        // 清除当前线程的cookies缓存
        HttpRequest.getCookieManager().getCookieStore().removeAll();

        RemoteJudgeDTO remoteJudgeDTO = getRemoteJudgeDTO();
        login();

        List<HttpCookie> cookies = remoteJudgeDTO.getCookies();

        HttpResponse httpResponse = HttpRequest.post(HOST + SUBMIT_SOURCE_URL)
                .header("origin", HOST)
                .header("referer", getResultReferer(remoteJudgeDTO.getContestId()))
                .header("x-csrf-token", remoteJudgeDTO.getCsrfToken())
                .header("cookie", CookiesUtils.convertCookiesToHeaderValue(cookies))
                .header("user-agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0")
                .header("x-requested-with", "XMLHttpRequest")
                .form("csrf_token", remoteJudgeDTO.getCsrfToken())
                .form("submissionId", remoteJudgeDTO.getSubmitId())
                .timeout(30000)
                .execute();

        RemoteJudgeRes remoteJudgeRes = RemoteJudgeRes.builder()
                .status(Constants.Judge.STATUS_JUDGING.getStatus())
                .build();

        if (httpResponse.getStatus() == 200) {

            JSONObject submissionInfoJson = JSONUtil.parseObj(httpResponse.body());
            String compilationError = submissionInfoJson.getStr("compilationError");
            if ("true".equals(compilationError)) {
                remoteJudgeRes
                        .setMemory(0)
                        .setTime(0)
                        .setStatus(Constants.Judge.STATUS_COMPILE_ERROR.getStatus());
                String CEMsg = submissionInfoJson.getStr("checkerStdoutAndStderr#1");
                if (StringUtils.isEmpty(CEMsg)) {
                    remoteJudgeRes.setErrorInfo(
                            "Oops! Because Codeforces does not provide compilation details, it is unable to provide the reason for compilation failure!");
                } else {
                    remoteJudgeRes.setErrorInfo(CEMsg);
                }
                return remoteJudgeRes;
            }
            Integer testcaseNum = remoteJudgeDTO.getTestcaseNum();
            Integer maxTime = remoteJudgeDTO.getMaxTime();
            Integer maxMemory = remoteJudgeDTO.getMaxMemory();
            if (testcaseNum == null) {
                testcaseNum = 1;
                maxTime = 0;
                maxMemory = 0;
            }
            List<JudgeCase> judgeCaseList = new ArrayList<>();
            String testCountStr = submissionInfoJson.getStr("testCount");
            int testCount = Integer.parseInt(testCountStr);
            for (; testcaseNum <= testCount; testcaseNum++) {
                String verdict = submissionInfoJson.getStr("verdict#" + testcaseNum);
                if (StringUtils.isEmpty(verdict)) {
                    continue;
                }
                Constants.Judge judgeRes = statusMap.get(verdict);
                Integer time = Integer.parseInt(submissionInfoJson.getStr("timeConsumed#" + testcaseNum));
                Integer memory = Integer.parseInt(submissionInfoJson.getStr("memoryConsumed#" + testcaseNum)) / 1024;
                String msg = submissionInfoJson.getStr("checkerStdoutAndStderr#" + testcaseNum);
                judgeCaseList.add(new JudgeCase()
                        .setSubmitId(remoteJudgeDTO.getJudgeId())
                        .setPid(remoteJudgeDTO.getPid())
                        .setUid(remoteJudgeDTO.getUid())
                        .setTime(time)
                        .setMemory(memory)
                        .setStatus(judgeRes.getStatus())
                        .setUserOutput(msg));
                if (time > maxTime) {
                    maxTime = time;
                }
                if (memory > maxMemory) {
                    maxMemory = memory;
                }
            }

            remoteJudgeDTO.setTestcaseNum(testcaseNum);
            remoteJudgeDTO.setMaxMemory(maxMemory);
            remoteJudgeDTO.setMaxTime(maxTime);
            remoteJudgeRes.setJudgeCaseList(judgeCaseList);
            if ("true".equals(submissionInfoJson.getStr("waiting"))) {
                return remoteJudgeRes;
            }
            Constants.Judge finalJudgeRes = statusMap.get(submissionInfoJson.getStr("verdict#" + testCount));
            remoteJudgeRes.setStatus(finalJudgeRes.getStatus())
                    .setTime(maxTime)
                    .setMemory(maxMemory);

            return remoteJudgeRes;
        } else if (httpResponse.getStatus() == 403) {
            // cookies 失效重新登录
            remoteJudgeDTO.setCookies(null);
            login();

            return remoteJudgeRes;
        } else if (httpResponse.getStatus() == 503) {
            // 网页暂时未响应
            return remoteJudgeRes;
        } else {
            remoteJudgeRes.setStatus(Constants.Judge.STATUS_SYSTEM_ERROR.getStatus())
                    .setMemory(0)
                    .setTime(0)
                    .setErrorInfo(
                            "Oops! Error in obtaining the judging result. The status code returned by the interface is "
                                    + httpResponse.getStatus() + ".");

            return remoteJudgeRes;
        }
    }

    @Override
    public void login() {
        RemoteJudgeDTO remoteJudgeDTO = getRemoteJudgeDTO();

        Map<String, String> cookie = new HashMap<>();
        List<HttpCookie> cookies = remoteJudgeDTO.getCookies();

        if (cookies != null && !cookies.isEmpty()) {
            cookie = CookiesUtils.convertHttpCookieListToMap(cookies);

            try {
                // 检查cookies是否过期
                if (checkLogin(cookies)) {
                    remoteJudgeDTO.setCookies(cookies);
                    remoteJudgeDTO.setLoginStatus(HttpStatus.SC_MOVED_TEMPORARILY);
                    remoteJudgeDTO.setCsrfToken(cookie.get("csrf_token"));
                    return;
                }
            } catch (Exception e) {
            }
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

            // 设置 Cookies
            if (cookies != null && !cookies.isEmpty()) {
                context.addCookies(CookiesUtils.convertHttpCookieListToPlaywrightCookies(cookies));
            }

            Page page = context.newPage();

            page.navigate(HOST + LOGIN_URL, new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT));

            try {
                // 等待出现退出按钮, 已经登录
                page.waitForSelector(PASSWORD_LOGOUT_BUTTON_SELECTOR,
                        new Page.WaitForSelectorOptions().setTimeout(5000));

                remoteJudgeDTO.setCookies(cookies);
                remoteJudgeDTO.setLoginStatus(HttpStatus.SC_MOVED_TEMPORARILY);
                remoteJudgeDTO.setCsrfToken(cookie.get("csrf_token"));
                return;
            } catch (TimeoutError e) {

                // 输入用户名
                page.waitForSelector(USERNAME_INPUT_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(3000));
                page.fill(USERNAME_INPUT_SELECTOR, remoteJudgeDTO.getUsername());

                // 输入密码
                page.waitForSelector(PASSWORD_INPUT_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(3000));
                page.fill(PASSWORD_INPUT_SELECTOR, remoteJudgeDTO.getPassword());

                page.keyboard().press("Enter");

                try {
                    // 等待出现退出按钮
                    page.waitForSelector(PASSWORD_LOGOUT_BUTTON_SELECTOR,
                            new Page.WaitForSelectorOptions().setTimeout(5000));

                    String body = page.content();

                    // 等待 `window._bfaa` 变量被设置
                    page.waitForFunction("() => window._bfaa !== undefined");
                    String bfaa = page.evaluate("() => window._bfaa").toString();

                    String csrfToken = ReUtil.get("data-csrf=\"([a-f0-9]+)\"", body, 1);
                    remoteJudgeDTO.setCsrfToken(csrfToken);

                    // 获取并输出 cookies
                    List<Cookie> cookiesSet = page.context().cookies();

                    cookies = CookiesUtils.convertCookiesToHttpCookieList(cookiesSet);

                    cookie = CookiesUtils.convertCookiesToMap(cookiesSet);
                    HashMap<String, String> res = new HashMap<>();

                    String ftaa = cookie.get("70a7c28f3de");
                    res.put("ftaa", ftaa);

                    // String bfaa = ReUtil.get("_bfaa = \"(.{32})\"", body, 1);
                    if (StringUtils.isEmpty(bfaa)) {
                        bfaa = cookie.get("raa");
                        if (StringUtils.isEmpty(bfaa)) {
                            bfaa = cookie.get("bfaa");
                        }
                    }
                    res.put("bfaa", bfaa);

                    res.put("csrf_token", csrfToken);

                    String _39ce7 = cookie.get("39ce7");
                    int _tta = 0;
                    for (int c = 0; c < _39ce7.length(); c++) {
                        _tta = (_tta + (c + 1) * (c + 2) * _39ce7.charAt(c)) % 1009;
                        if (c % 3 == 0)
                            _tta++;
                        if (c % 2 == 0)
                            _tta *= 2;
                        if (c > 0)
                            _tta -= (_39ce7.charAt(c / 2) / 2) * (_tta % 5);
                        while (_tta < 0)
                            _tta += 1009;
                        while (_tta >= 1009)
                            _tta -= 1009;
                    }

                    res.put("_tta", String.valueOf(_tta));

                    CookiesUtils.addCookiesToList(res, cookies);

                    remoteJudgeDTO.setCookies(cookies);
                    remoteJudgeDTO.setLoginStatus(HttpStatus.SC_MOVED_TEMPORARILY);
                } catch (TimeoutError e2) {
                    remoteJudgeDTO.setLoginStatus(403);
                }
            }
        }
    }

    public Boolean checkLogin(List<HttpCookie> cookies) throws Exception {
        // 清除当前线程的cookies缓存
        HttpRequest.getCookieManager().getCookieStore().removeAll();

        if (!CollectionUtils.isEmpty(cookies)) {
            HttpRequest request = HttpRequest.get(HOST)
                    .header("referer", HOST)
                    .header("Upgrade-Insecure-Requests", "1")
                    .header("user-agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("cookie", CookiesUtils.convertCookiesToHeaderValue(cookies))
                    .timeout(10000);

            // 发送请求并获取响应
            HttpResponse response = request.execute();

            String html = response.body();

            Document document = Jsoup.parse(html);

            Element logoutLink = document.selectFirst(PASSWORD_LOGOUT_BUTTON_SELECTOR);

            return logoutLink != null;
        }
        return false;
    }

    protected String getSubmitUrl(String contestNum) {
        return HOST + String.format(SUBMIT_URL, contestNum);
    }

    public void submitCode(RemoteJudgeDTO remoteJudgeDTO) {
        Map<String, String> cookie = CookiesUtils.convertHttpCookieListToMap(remoteJudgeDTO.getCookies());

        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("csrf_token", cookie.get("csrf_token"));
        paramMap.put("_tta", cookie.get("_tta"));
        paramMap.put("bfaa", cookie.get("bfaa"));
        paramMap.put("ftaa", cookie.get("ftaa"));
        paramMap.put("action", "submitSolutionFormSubmitted");
        paramMap.put("submittedProblemIndex", remoteJudgeDTO.getProblemNum());
        paramMap.put("programTypeId", getLanguage(remoteJudgeDTO.getLanguage(), remoteJudgeDTO.getKey()));
        paramMap.put("tabsize", 4);
        paramMap.put("sourceFile", "");
        paramMap.put("source", remoteJudgeDTO.getUserCode() + getRandomBlankString());

        String submit_url = getSubmitUrl(remoteJudgeDTO.getContestId()) + "?csrf_token=" + cookie.get("csrf_token");

        HttpResponse response = HttpRequest.post(submit_url)
                .header("user-agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0")
                .header("referer", getSubmitUrl(remoteJudgeDTO.getContestId()))
                .header("origin", HOST)
                .header("cookie", CookiesUtils.convertMapToCookieHeader(cookie))
                .header("accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("content-type", "application/x-www-form-urlencoded")
                .timeout(30000)
                .form(paramMap)
                .execute();

        remoteJudgeDTO.setSubmitStatus(response.getStatus());

        if (response.getStatus() != HttpStatus.SC_MOVED_TEMPORARILY) {
            if (response.body().contains("error for__programTypeId")) {
                String log = String.format("Codeforces[%s] [%s]:Failed to submit code, caused by `Language Rejected`",
                        remoteJudgeDTO.getContestId(), remoteJudgeDTO.getProblemNum());
                throw new RuntimeException(log);
            }
            if (response.body().contains("error for__source")) {
                String log = String.format("Codeforces[%s] [%s]:Failed to submit code, caused by `Source Code Error`",
                        remoteJudgeDTO.getContestId(), remoteJudgeDTO.getProblemNum());
                throw new RuntimeException(log);
            }
        }
    }

    @Override
    public String getLanguage(String language, String languageKey) {
        if (!StringUtils.isEmpty(languageKey)) {
            return languageKey;
        }
        if (language.startsWith("GNU GCC C11")) {
            return "43";
        } else if (language.startsWith("Clang++17 Diagnostics")) {
            return "52";
        } else if (language.startsWith("GNU G++11")) {
            return "50";
        } else if (language.startsWith("GNU G++14")) {
            return "50";
        } else if (language.startsWith("GNU G++17")) {
            return "54";
        } else if (language.startsWith("GNU G++20")) {
            return "89";
        } else if (language.startsWith("Microsoft Visual C++ 2017")) {
            return "59";
        } else if (language.startsWith("C# 8, .NET Core")) {
            return "65";
        } else if (language.startsWith("C# Mono")) {
            return "9";
        } else if (language.startsWith("D DMD32")) {
            return "28";
        } else if (language.startsWith("Go")) {
            return "32";
        } else if (language.startsWith("Haskell GHC")) {
            return "12";
        } else if (language.startsWith("Java 21")) {
            return "87";
        } else if (language.startsWith("Java 1.8")) {
            return "36";
        } else if (language.startsWith("Kotlin")) {
            return "48";
        } else if (language.startsWith("OCaml")) {
            return "19";
        } else if (language.startsWith("Delphi")) {
            return "3";
        } else if (language.startsWith("Free Pascal")) {
            return "4";
        } else if (language.startsWith("PascalABC.NET")) {
            return "51";
        } else if (language.startsWith("Perl")) {
            return "13";
        } else if (language.startsWith("PHP")) {
            return "6";
        } else if (language.startsWith("Python 2")) {
            return "7";
        } else if (language.startsWith("Python 3")) {
            return "31";
        } else if (language.startsWith("PyPy 2")) {
            return "40";
        } else if (language.startsWith("PyPy 3")) {
            return "41";
        } else if (language.startsWith("Ruby")) {
            return "67";
        } else if (language.startsWith("Rust")) {
            return "49";
        } else if (language.startsWith("Scala")) {
            return "20";
        } else if (language.startsWith("JavaScript")) {
            return "34";
        } else if (language.startsWith("Node.js")) {
            return "55";
        } else {
            return null;
        }
    }

    protected static String getRandomBlankString() {
        StringBuilder string = new StringBuilder("\n");
        int random = new Random().nextInt(Integer.MAX_VALUE);
        while (random > 0) {
            string.append(random % 2 == 0 ? ' ' : '\t');
            random /= 2;
        }
        return string.toString();
    }

}
