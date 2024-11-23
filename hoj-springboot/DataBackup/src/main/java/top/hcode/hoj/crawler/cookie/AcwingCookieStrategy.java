package top.hcode.hoj.crawler.cookie;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import top.hcode.hoj.utils.CookiesUtils;

import java.net.HttpCookie;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class AcwingCookieStrategy extends CookieStrategy {
    public static final String Oj = "acwing";

    private static final String BASE_URL = "https://www.acwing.com/";
    private static final String LOGIN_API = "/user/account/signin/";

    public static String extractCsrfToken(String html) {
        Document doc = Jsoup.parse(html);
        Element csrfInput = doc.select("input[name=csrfmiddlewaretoken]").first();
        if (csrfInput != null) {
            return csrfInput.attr("value");
        } else {
            return null;
        }
    }

    @Override
    public List<HttpCookie> getCookiesByLogin(List<HttpCookie> cookies, String username, String password)
            throws Exception {
        // 清除当前线程的cookies缓存
        HttpRequest.getCookieManager().getCookieStore().removeAll();

        // 检查cookies是否过期
        if (checkLogin(cookies, username)) {
            return cookies;
        }

        HttpResponse execute = HttpRequest.get(BASE_URL).execute();
        String body = execute.body();
        HttpCookie csrftoken = execute.getCookie("csrftoken");

        String postData = "csrfmiddlewaretoken=" + extractCsrfToken(body) +
                "&username=" + username +
                "&password=" + password +
                "&remember_me=on";

        HttpResponse loginExecute = HttpRequest.post(BASE_URL + LOGIN_API)
                .header(Header.REFERER, BASE_URL)
                .header(Header.CONTENT_TYPE, "application/x-www-form-urlencoded; charset=UTF-8")
                .header(Header.COOKIE, csrftoken.toString())
                .header("X-CSRFToken", csrftoken.getValue())
                .header("X-Requested-With", "XMLHttpRequest")
                .body(postData)
                .execute();

        return loginExecute.getCookies();
    }

    @Override
    public Boolean checkLogin(List<HttpCookie> cookies, String username) throws Exception {
        String html = getHtml(cookies, BASE_URL);
        // 定义匹配 ac_chat 的正则表达式
        String regex = "ac_chat\\s*:\\s*(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(html);

        // 如果找到匹配项则返回 true
        return matcher.find();
    }

}
