package top.hcode.hoj.crawler.cookie;

import java.net.HttpCookie;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "hoj")
public class CookieContext {

    private CookieStrategy cookieStrategy;

    public CookieContext(CookieStrategy cookieStrategy) {
        this.cookieStrategy = cookieStrategy;
    }

    // 上下文接口
    public List<HttpCookie> getCookiesByLogin(List<HttpCookie> cookies, String username, String password)
            throws Exception {
        return cookieStrategy.getCookiesByLogin(cookies, username, password);
    }

    public Boolean checkLogin(List<HttpCookie> cookies, String username) throws Exception {
        return cookieStrategy.checkLogin(cookies, username);
    }

}