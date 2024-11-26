package top.hcode.hoj.crawler.scraper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.alibaba.excel.util.CollectionUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;
import top.hcode.hoj.pojo.vo.ACMContestRankVO;
import top.hcode.hoj.utils.CookiesUtils;

@Slf4j(topic = "hoj")
public class CFScraperStrategy extends ScraperStrategy {

    private static final String IMAGE_HOST = "https://codeforces.com";
    private static final String LOGIN_API = "/enter";
    private static String RANK_API = "/api/contest.standings?contestId=%s&showUnofficial=true";
    private static final String CONTEST_RANK_URL = "/contest/%s/standings";
    private static final String GYM_RANK_URL = "/gym/%s/standings";
    private String RANK_URL;

    private Map<String, String> cookies;

    // 模拟浏览器操作
    private static final String USERNAME_INPUT_SELECTOR = "#handleOrEmail";
    private static final String PASSWORD_INPUT_SELECTOR = "#password";
    private static final String PASSWORD_LOGOUT_BUTTON_SELECTOR = "a[href*='/logout']";

    public CFScraperStrategy(String scraperType) {
        if ("cf".equals(scraperType)) {
            this.RANK_URL = IMAGE_HOST + CONTEST_RANK_URL;
        } else {
            this.RANK_URL = IMAGE_HOST + GYM_RANK_URL;
        }
    }

    @Override
    public List<ACMContestRankVO> getScraperInfoByLogin(String cid, Map<String, String> cookies, String username,
            String password, Map<String, String> usernameToUidMap) throws Exception {
        this.cookies = cookies;

        List<ACMContestRankVO> rankDatas = new ArrayList<>();

        // 获取排名信息
        String rankHtml = getRankInfo(cid);

        // 处理排名数据
        rankDatas = dealRank(rankHtml, cid, usernameToUidMap);

        return rankDatas;
    }

    @Override
    public List<ACMContestRankVO> getScraperInfo(String cid, Map<String, String> usernameToUidMap)
            throws Exception {
        return null;
    }

    @Override
    public Map<String, String> getLoginCookies(String loginUsername, String loginPassword) throws Exception {
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

            page.navigate(IMAGE_HOST + LOGIN_API, new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT));

            try {
                // 等待出现退出按钮, 已经登录
                page.waitForSelector(PASSWORD_LOGOUT_BUTTON_SELECTOR,
                        new Page.WaitForSelectorOptions().setTimeout(3000));
            } catch (TimeoutError e) {
                // 输入用户名
                page.waitForSelector(USERNAME_INPUT_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(3000));
                page.fill(USERNAME_INPUT_SELECTOR, loginUsername);

                // 输入密码
                page.waitForSelector(PASSWORD_INPUT_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(3000));
                page.fill(PASSWORD_INPUT_SELECTOR, loginPassword);

                page.keyboard().press("Enter");

                try {
                    // 等待出现退出按钮
                    page.waitForSelector(PASSWORD_LOGOUT_BUTTON_SELECTOR,
                            new Page.WaitForSelectorOptions().setTimeout(5000));

                    List<Cookie> cookiesSet = page.context().cookies();
                    return CookiesUtils.convertCookiesToMap(cookiesSet);
                } catch (TimeoutError e2) {
                }
            }
        }
        return null;
    }

    public String getRankInfo(String cid) throws IOException {
        String url = IMAGE_HOST + RANK_API;

        HttpURLConnection connection = (HttpURLConnection) new URL(String.format(url, cid)).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            return response.toString();
        } catch (IOException e) {
            // 处理 400 状态码
            if (connection.getResponseCode() == 400) {
                StringBuilder errorResponse = new StringBuilder();
                try (BufferedReader errorReader = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream()))) {
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        errorResponse.append(errorLine);
                    }
                }

                // 检查返回的 JSON 数据
                if (isContestNotFound(errorResponse.toString())) {
                    return getRankInfoWithCookies(cid);
                }
            }
            throw e;
        } finally {
            connection.disconnect();
        }
    }

    // 检查 JSON 是否表明 Contest ID 无效
    private boolean isContestNotFound(String jsonResponse) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(jsonResponse);
            return "FAILED".equals(root.path("status").asText())
                    && root.path("comment").asText().contains("Contest with id");
        } catch (IOException e) {
            // 如果解析失败，返回 false
            return false;
        }
    }

    private String getRankInfoWithCookies(String cid) throws IOException {
        Connection.Response response = Jsoup.connect(String.format(RANK_URL, cid))
                .cookies(cookies) // 使用登录时获得的 cookies
                .method(Connection.Method.GET)
                .execute();
        return response.body();
    }

    public List<ACMContestRankVO> dealRank(String html, String cid, Map<String, String> usernameToUidMap) {
        List<ACMContestRankVO> rankDataList = new ArrayList<>();

        if (CollectionUtils.isEmpty(cookies)) {
            // 解析 JSON 数据
            JSONObject jsonObject = new JSONObject(html);
            JSONObject resultObject = jsonObject.getJSONObject("result");

            JSONArray rankDataArray = resultObject.getJSONArray("rows");
            JSONObject contestData = resultObject.getJSONObject("contest");
            String title = contestData.optString("name");
            Date startTime = getContestDate(contestData.optLong("startTimeSeconds"));
            String link = String.format(RANK_URL, cid);

            for (int i = 0; i < rankDataArray.length(); i++) {
                try {
                    JSONObject data = rankDataArray.getJSONObject(i);
                    JSONObject party = data.getJSONObject("party");

                    int ranking = data.optInt("rank");
                    int acceptedCount = data.optInt("points");
                    long penaltyTimeInMinutes = data.optLong("penalty") * 60;

                    JSONArray memberNames = party.getJSONArray("members");
                    List<String> handles = new ArrayList<>();

                    memberNames.forEach(item -> handles.add(((JSONObject) item).optString("handle")));
                    String username = getOrderNumber(handles);

                    // 检查是否已有对应的 UID
                    String uid = usernameToUidMap.computeIfAbsent(username, k -> IdUtil.fastSimpleUUID());

                    rankDataList.add(new ACMContestRankVO()
                            .setTitle(title)
                            .setCid(cid)
                            .setStartTime(startTime)
                            .setLink(link)
                            .setSynchronous(true)
                            .setAc((double) acceptedCount)
                            .setTotalTime((double) penaltyTimeInMinutes)
                            .setRank(ranking)
                            .setUsername(username)
                            .setUid(uid));

                } catch (Exception e) {
                    // 将异常封装为 RuntimeException 以便抛出
                    throw new RuntimeException(e);
                }
            }

        } else {
            Document doc = Jsoup.parse(html);

            String title = null;
            // 获取标题元素
            Element titleElement = doc.selectFirst("title");
            if (titleElement != null) {
                title = titleElement.text();
                // 使用正则表达式提取中间内容
                Pattern pattern = Pattern.compile("Dashboard - (.*?) - Codeforces");
                Matcher matcher = pattern.matcher(title);
                if (matcher.find()) {
                    title = matcher.group(1);
                }
            }

            // 找到表格
            Element table = doc.selectFirst("table");

            if (table == null) {
                return rankDataList;
            }

            // 从每一行提取数据
            Elements rows = table.select("tr");

            String link = String.format(RANK_URL, cid);

            for (Element row : rows.subList(1, rows.size() - 1)) { // 跳过表头和表尾
                Elements columns = row.select("th, td");
                String ranking = columns.get(0).text().trim();
                String username = columns.get(1).text().trim();
                String ac = columns.get(2).text().trim();
                String penaltyTime = columns.get(3).text().trim();

                // 检查是否已有对应的 UID
                String uid = usernameToUidMap.computeIfAbsent(username, k -> IdUtil.fastSimpleUUID());

                rankDataList.add(new ACMContestRankVO()
                        .setTitle(title)
                        .setCid(cid)
                        .setLink(link)
                        .setSynchronous(true)
                        .setAc(Double.valueOf(ac))
                        .setTotalTime(Double.valueOf(penaltyTime) * 60)
                        .setRank(Integer.parseInt(ranking))
                        .setUsername(username)
                        .setUid(uid));
            }

        }

        // for (int i = 0; i < rankDataList.size() && i < 10; i++) {
        // System.out.println(rankDataList.get(i));
        // }

        return rankDataList;
    }

}
