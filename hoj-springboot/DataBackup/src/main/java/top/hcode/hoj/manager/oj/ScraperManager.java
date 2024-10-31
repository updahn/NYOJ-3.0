package top.hcode.hoj.manager.oj;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

import org.springframework.stereotype.Component;

import top.hcode.hoj.crawler.scraper.*;
import top.hcode.hoj.pojo.vo.ACMContestRankVO;

@Component
public class ScraperManager {

    public List<ACMContestRankVO> getScraperInfo(String scraper, String cid, Map<String, String> cookies,
            String username, String password, Map<String, String> usernameToUidMap) throws Exception {

        ScraperStrategy scraperStrategy = getScraperStrategy(scraper);
        ScraperContext remoteOjContext = new ScraperContext(scraperStrategy);
        List<ACMContestRankVO> rankDatas;

        // 登录所需的 scraper 集合
        Set<String> loginRequiredScrapers = new HashSet<>();
        Collections.addAll(loginRequiredScrapers, "gym", "hdu", "vj");

        if (loginRequiredScrapers.contains(scraper)) {
            rankDatas = remoteOjContext.getScraperInfoByLogin(cid, cookies, username, password, usernameToUidMap);
        } else {
            rankDatas = remoteOjContext.getScraperInfo(cid, usernameToUidMap);
        }

        return rankDatas;
    }

    public Map<String, String> getLoginCookies(String scraper, String loginUsername, String loginPassword)
            throws Exception {

        ScraperStrategy scraperStrategy = getScraperStrategy(scraper);
        ScraperContext remoteOjContext = new ScraperContext(scraperStrategy);

        return remoteOjContext.getLoginCookies(loginUsername, loginPassword);
    }

    // 使用传统的 switch 语句替代 switch 表达式
    private ScraperStrategy getScraperStrategy(String scraper) throws Exception {
        ScraperStrategy scraperStrategy;
        switch (scraper) {
            case "cf":
            case "gym":
                scraperStrategy = new CFScraperStrategy(scraper);
                break;
            case "hdu":
                scraperStrategy = new HDUScraperStrategy();
                break;
            case "nowcoder":
                scraperStrategy = new NCScraperStrategy();
                break;
            case "pta":
                scraperStrategy = new PTAcraperStrategy();
                break;
            case "xcpc":
                scraperStrategy = new XCPCcraperStrategy();
                break;
            case "vj":
                scraperStrategy = new VJScraperStrategy();
                break;
            default:
                throw new Exception("未知的OJ的名字，暂时不支持！");
        }
        return scraperStrategy;
    }

}