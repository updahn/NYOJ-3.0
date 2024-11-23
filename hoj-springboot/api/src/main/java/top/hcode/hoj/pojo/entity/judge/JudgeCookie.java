package top.hcode.hoj.pojo.entity.judge;

import java.net.HttpCookie;

import lombok.experimental.Accessors;

@Accessors(chain = true)
public class JudgeCookie {
    private String name;
    private String value;
    private String domain;
    private String path;
    private boolean secure;
    private boolean httpOnly;

    // 默认构造器（必要）
    public JudgeCookie() {
    }

    // 全参数构造器（可选）
    public JudgeCookie(String name, String value, String domain, String path, boolean secure, boolean httpOnly) {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.path = path;
        this.secure = secure;
        this.httpOnly = httpOnly;
    }

    // Getter 和 Setter 方法
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public boolean isHttpOnly() {
        return httpOnly;
    }

    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    // 转换为 HttpCookie
    public HttpCookie toHttpCookie() {
        HttpCookie cookie = new HttpCookie(this.name, this.value);
        cookie.setDomain(this.domain);
        cookie.setPath(this.path);
        cookie.setSecure(this.secure);
        cookie.setHttpOnly(this.httpOnly);
        return cookie;
    }

    // 从 HttpCookie 转换
    public static JudgeCookie fromHttpCookie(HttpCookie cookie) {
        JudgeCookie vo = new JudgeCookie();
        vo.setName(cookie.getName());
        vo.setValue(cookie.getValue());
        vo.setDomain(cookie.getDomain());
        vo.setPath(cookie.getPath());
        vo.setSecure(cookie.getSecure());
        vo.setHttpOnly(cookie.isHttpOnly());
        return vo;
    }

}
