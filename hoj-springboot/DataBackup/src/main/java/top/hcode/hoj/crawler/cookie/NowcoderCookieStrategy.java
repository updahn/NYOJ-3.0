package top.hcode.hoj.crawler.cookie;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;

import org.jsoup.Jsoup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;

import lombok.extern.slf4j.Slf4j;
import top.hcode.hoj.utils.SlideMatcherUtils;
import top.hcode.hoj.utils.CookiesUtils;

@Slf4j(topic = "hoj")
public class NowcoderCookieStrategy extends CookieStrategy {
    public static final String Oj = "nowcoder";

    private static final String BASE_URL = "https://ac.nowcoder.com";
    private static final String LOGIN_API = "/login";
    private static final String COURSE_API = "/acm/course";
    private static final String CHECK_LOGIN_URL = "https://gw-c.nowcoder.com/api/live/list/top";

    private static final String USERNAME_INPUT_SELECTOR = "input[placeholder='请输入邮箱/手机号码']";
    private static final String PASSWORD_LOGIN_BUTTON_SELECTOR = "li:has-text('密码登录')";
    private static final String AGREEMENT_CHECKBOX_SELECTOR = ".el-checkbox.login-page-agreement-checkbox";
    private static final String PASSWORD_INPUT_SELECTOR = "input[placeholder='请输入密码']";

    private static final String YIDUN_CLASS_SELECTOR = ".yidun.yidun-custom";
    private static final String BG_IMG_SELECTOR = "img.yidun_bg-img";
    private static final String JIGSAW_IMG_SELECTOR = "img.yidun_jigsaw";
    private static final String REFRESH_BUTTON_SELECTOR = ".yidun_refresh";

    private static final int MAX_TOTAL_ATTEMPTS = 30;
    private static int totalAttempts = 0;

    @Override
    public List<HttpCookie> getCookiesByLogin(List<HttpCookie> cookies, String username, String password)
            throws Exception {

        Boolean isLogin = checkLogin(cookies, username);
        if (isLogin) {
            return cookies;
        }

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            page.navigate(BASE_URL + LOGIN_API);
            page.waitForSelector(PASSWORD_LOGIN_BUTTON_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(3000))
                    .click();

            ElementHandle checkbox = page.waitForSelector(AGREEMENT_CHECKBOX_SELECTOR);
            if (checkbox != null && checkbox.evaluate("element => element.checked") == null) {
                checkbox.click();
            }

            page.waitForSelector(USERNAME_INPUT_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(3000))
                    .fill(username);
            page.waitForSelector(PASSWORD_INPUT_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(3000))
                    .fill(password);
            page.keyboard().press("Enter");

            List<Cookie> cookiesSet = handleSliderCaptcha(username, page);
            cookies = CookiesUtils.convertCookiesToHttpCookieList(cookiesSet);
            return cookies;
        }
    }

    @Override
    public Boolean checkLogin(List<HttpCookie> cookies, String username) throws Exception {
        String jsonString = Jsoup.parse(getHtml(cookies, CHECK_LOGIN_URL)).body().text();
        JsonNode rootNode = new ObjectMapper().readTree(jsonString);

        if (!rootNode.path("success").asBoolean()) {
            throw new Exception("Login check failed: " + rootNode.path("msg").asText());
        }
        return rootNode.path("data").path("isLogin").asBoolean();
    }

    private static List<Cookie> handleSliderCaptcha(String username, Page page) throws InterruptedException, Exception {

        try {
            page.waitForSelector(YIDUN_CLASS_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(3000));
            ElementHandle bgImg = page.waitForSelector(BG_IMG_SELECTOR,
                    new Page.WaitForSelectorOptions().setTimeout(3000));

            ElementHandle yidunClass = page.waitForSelector(YIDUN_CLASS_SELECTOR,
                    new Page.WaitForSelectorOptions().setTimeout(3000));
            if (yidunClass.getAttribute("class").contains("yidun--maxerror")) {
                page.click(".yidun_tips");
            } else if (yidunClass.getAttribute("class").contains("yidun--jigsaw")) {

                ElementHandle jigsawImg = page.waitForSelector(JIGSAW_IMG_SELECTOR,
                        new Page.WaitForSelectorOptions().setTimeout(3000));

                byte[] jigsaw = SlideMatcherUtils.imgFromUrl(jigsawImg.getAttribute("src"));
                byte[] bgimg = SlideMatcherUtils.imgFromUrl(bgImg.getAttribute("src"));

                Double space = SlideMatcherUtils.recognize(jigsaw, bgimg, totalAttempts);
                int targetDistance = space.intValue();

                if (targetDistance <= 0) {
                    page.waitForSelector(REFRESH_BUTTON_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(3000))
                            .click();
                    return handleSliderCaptcha(username, page);
                }

                ElementHandle slider = page.waitForSelector(".yidun_slider",
                        new Page.WaitForSelectorOptions().setTimeout(3000));
                move(page, slider, targetDistance);

                try {
                    page.waitForURL(url -> !url.equals(BASE_URL + LOGIN_API),
                            new Page.WaitForURLOptions().setTimeout(5000));
                    log.info("[Nowcoder] Username: {} Login successful!", username);
                    page.navigate(BASE_URL + COURSE_API);
                    return page.context().cookies();
                } catch (com.microsoft.playwright.TimeoutError e) {
                }
            } else if (yidunClass.getAttribute("class").contains("yidun--point")) {
                Random random = new Random();
                int imgWidth = (int) bgImg.boundingBox().width;
                int imgHeight = (int) bgImg.boundingBox().height;
                int centerX = imgWidth / 2;
                int centerY = imgHeight / 2;

                for (int i = 0; i < 3; i++) {
                    int randomX = random.nextInt(imgWidth) - centerX;
                    int randomY = 30 + random.nextInt(imgHeight - 30) - centerY;
                    page.mouse().click(bgImg.boundingBox().x + randomX, bgImg.boundingBox().y + randomY);
                }
                Thread.sleep(3000);
            } else {
                page.waitForSelector(REFRESH_BUTTON_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(3000))
                        .click();
            }

            if (++totalAttempts >= MAX_TOTAL_ATTEMPTS) {
                throw new Exception("[Nowcoder] Slider Captcha recognition failed more than " + MAX_TOTAL_ATTEMPTS
                        + " times. Stopping attempts.");
            }
            return handleSliderCaptcha(username, page);
        } catch (TimeoutException e) {
            throw new Exception("[Nowcoder] Slider Captcha TimeOut");
        }
    }

    public static void move(Page page, ElementHandle slider, int distance) throws InterruptedException {
        List<Integer> track = getMoveTrack(distance - 2);
        page.mouse().move(slider.boundingBox().x, slider.boundingBox().y);
        page.mouse().down();
        for (int move : track) {
            page.mouse().move(slider.boundingBox().x + move, slider.boundingBox().y);
            Thread.sleep(new Random().nextInt(50));
        }
        page.mouse().up();
    }

    public static List<Integer> getMoveTrack(int distance) {
        List<Integer> track = new ArrayList<>();
        int current = 0;
        int mid = (int) distance * 4 / 5;
        int a = 0;
        int move = 0;
        while (true) {
            a = new Random().nextInt(10);
            if (current <= mid) {
                move += a;
            } else {
                move -= a;
            }
            if ((current + move) < distance) {
                track.add(move);
            } else {
                track.add(distance - current);
                break;
            }
            current += move;
        }
        return track;
    }
}
