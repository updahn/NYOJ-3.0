package top.hcode.hoj.crawler.scraper;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Date;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import lombok.extern.slf4j.Slf4j;

import top.hcode.hoj.pojo.vo.ACMContestRankVO;
import top.hcode.hoj.utils.CookiesUtils;

@Slf4j(topic = "hoj")
@Component
public class VJScraperStrategy extends ScraperStrategy {

    private static final String HOST = "https://vjudge.net";
    private static final String CONTEST_RANK_API = "/contest/rank/single/%s";
    private static final String RANK_API = "/contest/%s#rank";

    private static final String USERNAME_INPUT_SELECTOR = "input[placeholder='Username or Email']";
    private static final String PASSWORD_INPUT_SELECTOR = "input[placeholder='Password']";
    private static final String PASSWORD_LOGIN_BUTTON_SELECTOR = "a.nav-link.login";
    private static final String PASSWORD_LOGOUT_BUTTON_SELECTOR = "a.nav-link.logout";

    @Override
    public List<ACMContestRankVO> getScraperInfoByLogin(String cid, Map<String, String> cookies, String username,
            String password, Map<String, String> usernameToUidMap) throws Exception {

        List<ACMContestRankVO> rankDatas = new ArrayList<>();

        // 获取响应信息
        String html = getRankInfo(cid, cookies);

        // 获取并处理排名数据
        rankDatas = dealRank(html, usernameToUidMap);

        return rankDatas;
    }

    @Override
    public List<ACMContestRankVO> getScraperInfo(String cid, Map<String, String> usernameToUidMap) throws Exception {
        return null;
    }

    @Override
    public Map<String, String> getLoginCookies(String loginUsername, String loginPassword) throws Exception {
        return login(loginUsername, loginPassword);
    }

    public static String getRankInfo(String cid, Map<String, String> cookies) throws IOException {
        // 构造请求的 URL
        URL url = new URL(String.format(HOST + CONTEST_RANK_API, cid));

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        // 设置请求方法和头信息
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "text/html, application/xhtml+xml, */*");
        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        // 设置 cookies
        StringJoiner cookieHeader = new StringJoiner("; ");
        for (Map.Entry<String, String> cookie : cookies.entrySet()) {
            cookieHeader.add(cookie.getKey() + "=" + cookie.getValue());
        }
        connection.setRequestProperty("Cookie", cookieHeader.toString());

        // 读取响应，指定编码为 UTF-8
        BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }

        // 关闭连接和缓冲读取器
        in.close();
        connection.disconnect();

        return content.toString();
    }

    public static List<ACMContestRankVO> dealRank(String html, Map<String, String> usernameToUidMap) throws Exception {

        if (StringUtils.isEmpty(html)) {
            throw new Exception("Vj 爬取的html为空！");
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rankJson = mapper.readTree(html);

        // 比赛ID
        String cid = rankJson.get("id").asText();
        String title = rankJson.get("title").asText();
        String link = String.format(HOST + RANK_API, cid);
        Date startTime = getContestDate(rankJson.get("begin").asLong());

        // 比赛时长（秒）
        double duration = rankJson.get("length").asDouble() / 1000;
        // 参赛选手信息
        JsonNode participants = rankJson.get("participants");

        // 初始化用户统计数据
        Map<String, Map<String, Object>> userStats = new HashMap<>();

        // 对提交记录按时间进行排序
        List<JsonNode> submissions = new ArrayList<>();
        rankJson.get("submissions").forEach(submissions::add);
        submissions.sort(Comparator.comparingLong(submission -> submission.get(3).asLong()));

        for (JsonNode submission : submissions) {
            String uid = submission.get(0).asText();
            String pid = submission.get(1).asText();
            int status = submission.get(2).asInt();
            long costTime = submission.get(3).asLong();

            userStats.putIfAbsent(uid, new HashMap<>());
            Map<String, Object> userStat = userStats.get(uid);
            userStat.putIfAbsent("ac_count", 0);
            userStat.putIfAbsent("penalty_time", 0L);
            userStat.putIfAbsent("attempts", new HashMap<String, Long>());
            userStat.putIfAbsent("accepted", new HashMap<String, Boolean>());

            Map<String, Long> attempts = (Map<String, Long>) userStat.get("attempts");
            attempts.putIfAbsent(pid, 0L);
            Map<String, Boolean> accepted = (Map<String, Boolean>) userStat.get("accepted");
            accepted.putIfAbsent(pid, false);

            // 不包含赛后提交
            if (costTime <= duration) {
                if (accepted.get(pid)) { // 被标记过的已ac的题目
                    continue;
                }
                if (status == 1) { // 如果提交状态是正确的
                    userStat.put("ac_count", (int) userStat.get("ac_count") + 1);
                    long penaltyTime = costTime + attempts.getOrDefault(pid, 0L);
                    userStat.put("penalty_time", (long) userStat.get("penalty_time") + penaltyTime);
                    accepted.put(pid, true);// 标记该题目已经正确提交
                } else { // 如果提交状态是错误的
                    attempts.put(pid, attempts.getOrDefault(pid, 0L) + 1200L);
                }
            }
        }

        // 生成排名数据
        List<ACMContestRankVO> rankDataList = new ArrayList<>();
        participants.fields().forEachRemaining(entry -> {
            String uid = entry.getKey();

            JsonNode participant = entry.getValue();
            String username = participant.get(0).asText();
            String realname = participant.get(1).asText();

            // 检查是否已有对应的 UID
            String uid_ = usernameToUidMap.computeIfAbsent(username, k -> IdUtil.fastSimpleUUID());

            if (userStats.containsKey(uid)) {
                Map<String, Object> stats = userStats.get(uid);

                int acCount = (int) stats.getOrDefault("ac_count", 0);
                long penaltyTime = (long) stats.getOrDefault("penalty_time", 0);

                ACMContestRankVO vo = new ACMContestRankVO()
                        .setCid(cid)
                        .setTitle(title)
                        .setStartTime(startTime)
                        .setLink(link)
                        .setSynchronous(true)
                        .setAc((double) acCount)
                        .setTotalTime((double) penaltyTime)
                        .setUsername(username)
                        .setRealname(realname.split("（")[0].split("\\(")[0].trim())
                        .setUid(uid_);

                rankDataList.add(vo);
            }
        });

        // 按照规定的排序方式对rankDatas排序
        rankDataList.sort(Comparator.comparing(ACMContestRankVO::getAc).reversed()
                .thenComparing(ACMContestRankVO::getTotalTime));

        // 填充ranking
        for (int i = 0; i < rankDataList.size(); i++) {
            rankDataList.get(i).setRank(i + 1);
        }

        return rankDataList;
    }

    public static Map<String, String> login(String username, String password) throws IOException {
        // 清除当前线程的cookies缓存
        HttpRequest.getCookieManager().getCookieStore().removeAll();

        Map<String, String> cookies = null;

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

            Page page = context.newPage();

            page.navigate(HOST, new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT));

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
                cookies = CookiesUtils.convertCookiesToMap(cookiesSet);

                log.info("[VJ] Username: {} Login successful!", username);
                return cookies;
            } catch (TimeoutError e) {
                throw new RuntimeException("[VJ] Username: " + username + " Failed to login!");
            }
        }
    }

}
