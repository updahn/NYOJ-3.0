package top.hcode.hoj.util;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;
import com.alibaba.druid.util.StringUtils;
import com.microsoft.playwright.options.Cookie;

/**
 * CookiesUtils 工具类
 */
public class CookiesUtils {

    /**
     * 将 List<HttpCookie> 转换为 Map<String, String>。
     *
     * @param cookies List<HttpCookie> 类型的 cookie 列表
     * @return Map<String, String> 类型的 cookie 键值对映射
     */
    public static Map<String, String> convertHttpCookieListToMap(List<HttpCookie> cookies) {
        if (cookies != null && !cookies.isEmpty()) {
            return cookies.stream()
                    .filter(cookie -> cookie != null && cookie.getName() != null && cookie.getValue() != null)
                    .collect(Collectors.toMap(
                            HttpCookie::getName,
                            HttpCookie::getValue,
                            (oldValue, newValue) -> newValue // 处理键冲突
                    ));
        }
        return new HashMap<>();
    }

    /**
     * 将 Map<String, String> 转换为 List<HttpCookie>。
     *
     * @param cookies Map<String, String> 类型的 cookie 键值对映射
     * @return List<HttpCookie> 类型的 cookie 列表
     */
    public static List<HttpCookie> convertMapToHttpCookieList(Map<String, String> cookies) {
        if (CollectionUtils.isEmpty(cookies)) {
            return null;
        }
        return cookies.entrySet().stream()
                .map(entry -> new HttpCookie(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 将 Playwright 的 List<Cookie> 转换为 Map<String, String>。
     *
     * @param playwrightCookies Playwright 中的 Cookie 列表
     * @return Map<String, String> 类型的 cookie 键值对映射
     */
    public static Map<String, String> convertCookiesToMap(List<Cookie> playwrightCookies) {
        Map<String, String> cookieMap = new HashMap<>();
        for (Cookie cookie : playwrightCookies) {
            cookieMap.put(cookie.name, cookie.value);
        }
        return cookieMap;
    }

    /**
     * 将 Playwright 的 List<Cookie> 转换为 List<HttpCookie>。
     *
     * @param playwrightCookies Playwright 中的 Cookie 列表
     * @return List<HttpCookie> 类型的 cookie 列表
     */
    public static List<HttpCookie> convertCookiesToHttpCookieList(List<Cookie> playwrightCookies) {
        List<HttpCookie> httpCookies = new ArrayList<>();
        for (Cookie cookie : playwrightCookies) {
            HttpCookie httpCookie = new HttpCookie(cookie.name, cookie.value);
            httpCookie.setDomain(cookie.domain);
            httpCookie.setPath(cookie.path);

            // 如果 expires 不为空，将其转换为 long 值并设置 max-age
            if (cookie.expires != null) {
                long maxAge = cookie.expires.longValue();
                httpCookie.setMaxAge(maxAge);
            }

            httpCookie.setSecure(cookie.secure);
            httpCookie.setHttpOnly(cookie.httpOnly);
            httpCookies.add(httpCookie);
        }
        return httpCookies;
    }

    /**
     * 将字符串形式的 cookies 转换为 List<HttpCookie>。
     *
     * @param cookieString 字符串形式的 cookies，如 "name=value; name2=value2"
     * @return List<HttpCookie> 类型的 cookie 列表
     */
    public static List<HttpCookie> parseCookies(String cookieString) {
        List<HttpCookie> cookies = new ArrayList<>();

        // 去掉外部的中括号并分隔 cookie 条目
        String[] cookieArray = cookieString.replaceAll("^\\[|\\]$", "").split(", (?=[^;]+)");

        for (String cookie : cookieArray) {
            // 提取 name=value 部分
            String[] parts = cookie.split(";");
            String nameValue = parts[0].trim();

            // 解析 name 和 value
            String[] nameValuePair = nameValue.split("=", 2);
            String name = nameValuePair[0].trim();
            String value = nameValuePair.length > 1 ? nameValuePair[1].trim().replace("\"", "") : "";

            // 创建 HttpCookie 对象
            HttpCookie httpCookie = new HttpCookie(name, value);

            // 遍历属性（如 Path 和 Domain）
            for (int i = 1; i < parts.length; i++) {
                String attribute = parts[i].trim();
                if (attribute.startsWith("$Path")) {
                    String path = attribute.split("=", 2)[1].replace("\"", "");
                    httpCookie.setPath(path);
                } else if (attribute.startsWith("$Domain")) {
                    String domain = attribute.split("=", 2)[1].replace("\"", "");
                    httpCookie.setDomain(domain);
                }
            }

            cookies.add(httpCookie);
        }

        return cookies;
    }

    /**
     * 将字符串形式的 cookie 转换为 Map<String, String>。
     *
     * @param cookieString 字符串形式的 cookie，例如 "{name=value, name2=value2}"
     * @return Map<String, String> 类型的 cookie 键值对映射
     */
    public Map<String, String> dealCookiesStringToMap(String cookieString) {
        Map<String, String> cookieMap = new HashMap<>();

        if (StringUtils.isEmpty(cookieString)) {
            return null;
        }

        // 去除花括号
        if (cookieString.startsWith("{") && cookieString.endsWith("}")) {
            cookieString = cookieString.substring(1, cookieString.length() - 1);
        }

        // 定义正则表达式来匹配 key=value 的结构
        Pattern pattern = Pattern.compile("([^=,\\s]+)=([^,]*)");
        Matcher matcher = pattern.matcher(cookieString);

        // 遍历所有匹配的 key=value 对
        while (matcher.find()) {
            String key = matcher.group(1); // 获取键
            String value = matcher.group(2); // 获取值
            cookieMap.put(key, value); // 将键值对放入Map
        }

        return cookieMap;
    }

    /**
     * 将 List<HttpCookie> 转换为 HTTP 请求的 Cookie 头部值格式。
     * 每个 HttpCookie 被转换为 "name=value" 格式，并通过 "; " 连接。
     *
     * @param cookies 包含 HTTP Cookie 的列表
     * @return 一个格式化的字符串，可用于 HTTP 请求中的 Cookie 头部
     */
    public static String convertCookiesToHeaderValue(List<HttpCookie> cookies) {
        return cookies.stream()
                .map(cookie -> cookie.getName() + "=" + cookie.getValue())
                .collect(Collectors.joining("; "));
    }

    /**
     * 将 Map<String, String> 转换为 HTTP 请求的 Cookie 头部值格式。
     * Map 中的每个条目被转换为 "name=value" 格式，并通过 "; " 连接。
     *
     * @param cookieMap 包含 Cookie 名称和对应值的 Map
     * @return 一个格式化的字符串，可用于 HTTP 请求中的 Cookie 头部
     */
    public static String convertMapToCookieHeader(Map<String, String> cookieMap) {
        return cookieMap.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("; "));
    }

    /**
     * 将 List<HttpCookie> 转换为 Playwright 的 List<Cookie>。
     *
     * @param httpCookies Java 中的 HttpCookie 列表
     * @return List<Cookie> 类型的 Playwright cookie 列表
     */
    public static List<Cookie> convertHttpCookieListToPlaywrightCookies(List<HttpCookie> httpCookies) {
        List<Cookie> playwrightCookies = new ArrayList<>();

        for (HttpCookie httpCookie : httpCookies) {
            if (httpCookie.getPath() == null || httpCookie.getDomain() == null) {
                continue;
            }

            Cookie cookie = new Cookie(httpCookie.getName(), httpCookie.getValue());
            cookie.domain = httpCookie.getDomain();
            cookie.path = httpCookie.getPath();

            playwrightCookies.add(cookie);
        }

        return playwrightCookies;
    }

    public static void addCookiesToList(Map<String, String> cookieMap, List<HttpCookie> cookiesList) {
        for (Map.Entry<String, String> entry : cookieMap.entrySet()) {
            // 创建 HttpCookie 对象
            HttpCookie httpCookie = new HttpCookie(entry.getKey(), entry.getValue());
            // 添加到 List 中
            cookiesList.add(httpCookie);
        }
    }
}
