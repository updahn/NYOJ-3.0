package top.hcode.hoj.crawler.cookie;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import top.hcode.hoj.utils.CookiesUtils;

@Slf4j(topic = "hoj")
public class VjudgeCookieStrategy extends CookieStrategy {
    public static final String Oj = "vjudge";

    private static final String HOST = "https://vjudge.net";
    private static final String CHECK_LOGIN_STATUS_API = "/user/checkLogInStatus";

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

    @Override
    public List<HttpCookie> getCookiesByLogin(List<HttpCookie> cookies, String username, String password)
            throws Exception {

        try {
            // 检查cookies是否过期
            if (checkLogin(cookies, username)) {
                return cookies;
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

                return cookies;
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
                    return cookies;
                } catch (TimeoutError e2) {
                    throw new RuntimeException("[VJ] Username: " + username + " Failed to login!");
                }
            }
        }
    }

    @Override
    public Boolean checkLogin(List<HttpCookie> cookies, String username) throws Exception {
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

}
