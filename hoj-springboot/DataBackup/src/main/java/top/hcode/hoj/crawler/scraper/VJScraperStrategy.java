package top.hcode.hoj.crawler.scraper;

import java.io.*;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.awt.image.BufferedImage;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;

import top.hcode.hoj.pojo.vo.ACMContestRankVO;
import top.hcode.hoj.utils.OCREngineUtils;

@Slf4j(topic = "hoj")
@Component
public class VJScraperStrategy extends ScraperStrategy {

    private static final String HOST = "https://vjudge.net";
    private static final String LOGIN_API = "/user/login";
    private static final String CAPTCHA_API = "/util/captcha";
    private static final String CONTEST_RANK_API = "/contest/rank/single/%s";
    private static final String RANK_API = "/contest/%s#rank";

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

        List<HttpCookie> cookies = null;
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
                    return convertHttpCookieListToMap(cookies);
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
                throw new RuntimeException("[VJ] Username: " + username + " Failed to login!");
            }
        }
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

    // 将 List<HttpCookie> 转换为 Map<String, String>
    public static Map<String, String> convertHttpCookieListToMap(List<HttpCookie> cookies) {
        if (CollectionUtils.isEmpty(cookies)) {
            return null;
        }
        return cookies.stream()
                .collect(Collectors.toMap(HttpCookie::getName, HttpCookie::getValue));
    }

}
