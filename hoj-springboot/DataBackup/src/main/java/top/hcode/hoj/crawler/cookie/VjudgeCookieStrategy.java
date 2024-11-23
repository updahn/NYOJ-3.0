package top.hcode.hoj.crawler.cookie;

import java.net.HttpCookie;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.awt.image.BufferedImage;

import org.springframework.util.CollectionUtils;

import cn.hutool.core.map.MapUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import top.hcode.hoj.utils.OCREngineUtils;

@Slf4j(topic = "hoj")
public class VjudgeCookieStrategy extends CookieStrategy {
    public static final String Oj = "vjudge";

    private static final String HOST = "https://vjudge.net";
    private static final String LOGIN_API = "/user/login";
    private static final String CHECK_LOGIN_STATUS_API = "/user/checkLogInStatus";
    private static final String CAPTCHA_API = "/util/captcha";

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
    public List<HttpCookie> getCookiesByLogin(List<HttpCookie> cookies, String username, String password)
            throws Exception {

        Boolean isLogin = checkLogin(cookies, username);
        if (isLogin) {
            return cookies;
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
                    return cookies;
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
        return new ArrayList<>();
    }

    @Override
    public Boolean checkLogin(List<HttpCookie> cookies, String username) throws Exception {
        // 清除当前线程的cookies缓存
        HttpRequest.getCookieManager().getCookieStore().removeAll();

        boolean isOnline = false;
        if (!CollectionUtils.isEmpty(cookies)) {
            for (int attempt = 0; attempt < MAX_TIMEOUTS; attempt++) {
                try {
                    // 执行请求
                    HttpRequest request = HttpRequest.post(HOST + CHECK_LOGIN_STATUS_API)
                            .headerMap(headers, false)
                            .timeout(3000);
                    request.cookie(cookies);
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
