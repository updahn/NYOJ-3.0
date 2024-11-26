package top.hcode.hoj.crawler.cookie;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import top.hcode.hoj.utils.CookiesUtils;

import java.net.HttpCookie;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.WaitUntilState;

public class CFCookieStrategy extends CookieStrategy {
    public static final String Oj = "codeforcecs";

    private static final String BASE_URL = "https://codeforces.com";
    private static final String LOGIN_API = "/enter";

    // 模拟浏览器操作
    private static final String USERNAME_INPUT_SELECTOR = "#handleOrEmail";
    private static final String PASSWORD_INPUT_SELECTOR = "#password";
    private static final String PASSWORD_LOGOUT_BUTTON_SELECTOR = "a[href*='/logout']";

    @Override
    public Boolean checkLogin(List<HttpCookie> cookies, String username) throws Exception {
        // 清除当前线程的cookies缓存
        HttpRequest.getCookieManager().getCookieStore().removeAll();

        if (!CollectionUtils.isEmpty(cookies)) {
            HttpRequest request = HttpRequest.get(BASE_URL)
                    .header("referer", BASE_URL)
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

            // 设置 Cookies
            if (cookies != null) {
                context.addCookies(CookiesUtils.convertHttpCookieListToPlaywrightCookies(cookies));
            }

            Page page = context.newPage();

            // page.navigate("https://bot.sannysoft.com/");

            page.navigate(BASE_URL + LOGIN_API, new Page.NavigateOptions().setWaitUntil(WaitUntilState.COMMIT));

            try {
                // 等待出现退出按钮, 已经登录
                page.waitForSelector(PASSWORD_LOGOUT_BUTTON_SELECTOR,
                        new Page.WaitForSelectorOptions().setTimeout(3000));

                return cookies;
            } catch (TimeoutError e) {
                // 输入用户名
                page.waitForSelector(USERNAME_INPUT_SELECTOR,
                        new Page.WaitForSelectorOptions().setTimeout(3000));
                page.fill(USERNAME_INPUT_SELECTOR, username);

                // 输入密码
                page.waitForSelector(PASSWORD_INPUT_SELECTOR,
                        new Page.WaitForSelectorOptions().setTimeout(3000));
                page.fill(PASSWORD_INPUT_SELECTOR, password);

                page.keyboard().press("Enter");

                try {
                    // 等待出现退出按钮
                    page.waitForSelector(PASSWORD_LOGOUT_BUTTON_SELECTOR,
                            new Page.WaitForSelectorOptions().setTimeout(10000));

                    String body = page.content();

                    // 等待 `window._bfaa` 变量被设置
                    page.waitForFunction("() => window._bfaa !== undefined");
                    String bfaa = page.evaluate("() => window._bfaa").toString();

                    String csrfToken = ReUtil.get("data-csrf=\"([a-f0-9]+)\"", body, 1);

                    // 获取并输出 cookies
                    List<Cookie> cookiesSet = page.context().cookies();

                    cookies = CookiesUtils.convertCookiesToHttpCookieList(cookiesSet);

                    Map<String, String> cookie = CookiesUtils.convertCookiesToMap(cookiesSet);
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
                    return cookies;
                } catch (TimeoutError e2) {
                    return null;
                }
            }
        }
    }

}
