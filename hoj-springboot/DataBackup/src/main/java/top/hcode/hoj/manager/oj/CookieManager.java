package top.hcode.hoj.manager.oj;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import top.hcode.hoj.config.NacosSwitchConfig;
import top.hcode.hoj.config.SwitchConfig;
import top.hcode.hoj.crawler.cookie.*;
import top.hcode.hoj.mapper.RemoteJudgeAccountMapper;
import top.hcode.hoj.pojo.bo.Pair_;
import top.hcode.hoj.pojo.entity.judge.JudgeCookie;
import top.hcode.hoj.pojo.entity.judge.RemoteJudgeAccount;
import top.hcode.hoj.pojo.vo.AliveVO;
import top.hcode.hoj.utils.CookiesUtils;
import top.hcode.hoj.utils.RedisUtils;

@Component
public class CookieManager {

    @Autowired
    private NacosSwitchConfig nacosSwitchConfig;

    @Autowired
    private RemoteJudgeAccountMapper remoteJudgeAccountMapper;

    @Autowired
    private RedisUtils redisUtils;

    public IPage<AliveVO> getCoursePage(Integer limit, Integer currentPage, String scraper) throws Exception {
        String finalScraper = scraper.equals("GYM") ? "CF" : scraper;

        // 页数，每页题数若为空，设置默认值
        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        if (limit == null || limit < 1)
            limit = 2;

        List<AliveVO> aliveVoList = getAliveList(finalScraper);

        List<AliveVO> pageList = new ArrayList<>();

        int count = aliveVoList.size();

        // 计算当前页第一条数据的下标
        int currId = currentPage > 1 ? (currentPage - 1) * limit : 0;
        for (int i = 0; i < limit && i < count - currId; i++) {
            AliveVO aliveVO = aliveVoList.get(currId + i);
            pageList.add(aliveVO);
        }

        Page<AliveVO> page = new Page<>(currentPage, limit);
        page.setSize(limit);
        page.setCurrent(currentPage);
        page.setTotal(count);
        page.setRecords(pageList);
        return page;
    }

    public Map<String, String> getCookieMap(String scraper, String user) throws Exception {
        List<HttpCookie> cookies = getCookieList(scraper, user, true);

        return CookiesUtils.convertHttpCookieListToMap(cookies);
    }

    public List<HttpCookie> getCookieList(String scraper, String user, Boolean isCheckLogin) throws Exception {
        String finalScraper = scraper.equals("GYM") ? "CF" : scraper;

        List<Pair_<String, String>> userList = getCourseInfoList(finalScraper);

        int index = 0; // 默认使用第一个账号
        if (user != null) {
            // 查找用户名在列表中的位置
            index = IntStream.range(0, userList.size())
                    .filter(i -> userList.get(i).getKey().equals(user))
                    .findFirst()
                    .orElseThrow(() -> new Exception("Noknown Account: [" + finalScraper + ": " + user + "]"));
        }

        // 获取用户名和密码
        String username = userList.get(index).getKey();
        String password = userList.get(index).getValue();

        String key = finalScraper + ":" + user;

        List<HttpCookie> oldCookies = null; // 初始化为空

        // 从 Redis 获取 Cookie 列表
        try {

            String jsonCookies = (String) redisUtils.get(key);
            if (jsonCookies != null) {
                oldCookies = convertStringToHttpCookie(jsonCookies);
            }

        } catch (Exception e) {
            // 如果转化失败，删除 Redis 中的键并将 oldCookies 设为 null
            redisUtils.del(key);
            oldCookies = null;
        }

        // 如果不检查登录状态，且有对应的cookies直接返回
        if (!isCheckLogin && (oldCookies != null)) {
            return oldCookies;
        }

        // 检查缓存中的 Cookie 是否过期，如果过期则更新
        CookieStrategy scraperStrategy = getScraperStrategy(finalScraper);
        CookieContext remoteOjContext = new CookieContext(scraperStrategy);
        List<HttpCookie> cookies = remoteOjContext.getCookiesByLogin(oldCookies, username, password);

        redisUtils.set(key, convertHttpCookieToString(cookies), 60 * 60); // 保留60分钟

        return cookies;
    }

    public List<AliveVO> getAliveList(String scraper) {
        List<AliveVO> aliveVoList = new ArrayList<>();
        QueryWrapper<RemoteJudgeAccount> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_alive", true);

        for (RemoteJudgeAccount account : remoteJudgeAccountMapper.selectList(queryWrapper)) {
            String oj = account.getOj();

            if (!StringUtils.isEmpty(scraper) && !scraper.equals(oj))
                continue;

            aliveVoList.add(new AliveVO()
                    .setOj(oj)
                    .setUser(account.getUsername())
                    .setLink(account.getLink())
                    .setTitle(account.getTitle()));
        }

        return aliveVoList;
    }

    public List<Pair_<String, String>> getCourseInfoList(String scraper) throws Exception {
        String finalScraper = scraper.equals("GYM") ? "CF" : scraper;

        SwitchConfig switchConfig = nacosSwitchConfig.getSwitchConfig();
        List<String> usernameList;
        List<String> passwordList;

        // 根据 scraper 获取对应的用户名和密码或课程用户名和链接列表
        switch (finalScraper) {
            case "NOWCODER":
                usernameList = switchConfig.getNowcoderUsernameList();
                passwordList = switchConfig.getNowcoderPasswordList();
                break;
            case "ACWING":
                usernameList = switchConfig.getAcwingUsernameList();
                passwordList = switchConfig.getAcwingPasswordList();
                break;
            case "VJ":
                usernameList = switchConfig.getVjUsernameList();
                passwordList = switchConfig.getVjPasswordList();
                break;
            case "CF":
                usernameList = switchConfig.getCfUsernameList();
                passwordList = switchConfig.getCfPasswordList();
                break;
            default:
                throw new Exception("未知的存活cookie名字，暂时不支持！");
        }

        // 将用户名和密码或课程用户名和链接配对返回
        List<Pair_<String, String>> result = new ArrayList<>();
        for (int i = 0; i < usernameList.size(); i++) {
            result.add(new Pair_<>(usernameList.get(i), passwordList.get(i)));
        }

        return result;
    }

    public List<HttpCookie> getCookiesByLogin(String scraper, String loginUsername, String loginPassword,
            List<HttpCookie> cookies) throws Exception {

        CookieStrategy scraperStrategy = getScraperStrategy(scraper);
        CookieContext remoteOjContext = new CookieContext(scraperStrategy);

        return remoteOjContext.getCookiesByLogin(cookies, loginUsername, loginPassword);
    }

    // 使用传统的 switch 语句替代 switch 表达式
    private CookieStrategy getScraperStrategy(String scraper) throws Exception {
        String finalScraper = scraper.equals("GYM") ? "CF" : scraper;

        CookieStrategy cookieStrategy;
        switch (finalScraper) {
            case "NOWCODER":
                cookieStrategy = new NowcoderCookieStrategy();
                break;
            case "ACWING":
                cookieStrategy = new AcwingCookieStrategy();
                break;
            case "VJ":
                cookieStrategy = new VjudgeCookieStrategy();
                break;
            case "CF":
                cookieStrategy = new CFCookieStrategy();
                break;
            default:
                throw new Exception("未知的存活cookie名字，暂时不支持！");
        }
        return cookieStrategy;
    }

    public String convertHttpCookieToString(List<HttpCookie> cookies) throws Exception {
        List<JudgeCookie> customCookies = cookies.stream().map(JudgeCookie::fromHttpCookie)
                .collect(Collectors.toList());
        return new ObjectMapper().writeValueAsString(customCookies);
    }

    public List<HttpCookie> convertStringToHttpCookie(String cookie) throws Exception {
        // 使用 CustomHttpCookie 进行反序列化
        List<JudgeCookie> customCookies = new ObjectMapper().readValue(cookie,
                new TypeReference<List<JudgeCookie>>() {
                });
        return customCookies.stream().map(JudgeCookie::toHttpCookie).collect(Collectors.toList());
    }
}