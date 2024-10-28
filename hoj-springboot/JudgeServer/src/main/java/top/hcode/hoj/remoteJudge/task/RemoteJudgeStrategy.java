package top.hcode.hoj.remoteJudge.task;

import java.net.HttpCookie;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.util.CollectionUtils;

import lombok.Getter;
import lombok.Setter;
import top.hcode.hoj.remoteJudge.entity.RemoteJudgeDTO;
import top.hcode.hoj.remoteJudge.entity.RemoteJudgeRes;

/**
 * 远程评测抽象类
 */
public abstract class RemoteJudgeStrategy {

    @Setter
    @Getter
    private RemoteJudgeDTO remoteJudgeDTO;

    public abstract void submit();

    public abstract RemoteJudgeRes result();

    public abstract void login();

    public abstract String getLanguage(String language, String languageKey); // 传入数据库中的语言提交码

    // 将 List<HttpCookie> 转换为 Map<String, String>
    public static Map<String, String> convertHttpCookieListToMap(List<HttpCookie> cookies) {
        if (CollectionUtils.isEmpty(cookies)) {
            return null;
        }
        return cookies.stream()
                .collect(Collectors.toMap(HttpCookie::getName, HttpCookie::getValue));
    }

    // 将 Map<String, String> 转换为 List<HttpCookie>
    public static List<HttpCookie> convertMapToHttpCookieList(Map<String, String> cookies) {
        if (CollectionUtils.isEmpty(cookies)) {
            return null;
        }
        return cookies.entrySet().stream()
                .map(entry -> new HttpCookie(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }
}
