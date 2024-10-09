package top.hcode.hoj.manager.oj;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import top.hcode.hoj.crawler.scraper.*;
import top.hcode.hoj.pojo.vo.ACMContestRankVO;

@Component
public class ScraperManager {

    public List<ACMContestRankVO> getScraperInfo(String cid, String keyword, String scraper,
            String loginUsername, String loginPassword, Map<String, String> usernameToUidMap)
            throws Exception {

        ScraperStrategy scraperStrategy;
        switch (scraper) {
            case "cf":
                scraperStrategy = new CFScraperStrategy(scraper);
                break;
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
            default:
                throw new Exception("未知的OJ的名字，暂时不支持！");
        }
        List<ACMContestRankVO> rankDatas = new ArrayList<>();

        ScraperContext remoteOjContext = new ScraperContext(scraperStrategy);

        if (Objects.equals("gym", scraper) || Objects.equals("hdu", scraper)) {
            rankDatas = remoteOjContext.getScraperInfoByLogin(cid, keyword, loginUsername, loginPassword,
                    usernameToUidMap);
        } else {
            rankDatas = remoteOjContext.getScraperInfo(cid, keyword, usernameToUidMap);
        }

        return rankDatas;
    }

}