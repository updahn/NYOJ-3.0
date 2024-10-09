package top.hcode.hoj.crawler.scraper;

import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import top.hcode.hoj.pojo.vo.ACMContestRankVO;

@Slf4j(topic = "hoj")
public class ScraperContext {

    private ScraperStrategy scraperStrategy;

    public ScraperContext(ScraperStrategy scraperStrategy) {
        this.scraperStrategy = scraperStrategy;
    }

    // 上下文接口
    public List<ACMContestRankVO> getScraperInfo(String cid, String keyword, Map<String, String> usernameToUidMap)
            throws Exception {
        return scraperStrategy.getScraperInfo(cid, keyword, usernameToUidMap);
    }

    // 上下文接口
    public List<ACMContestRankVO> getScraperInfoByLogin(String cid, String keyword, String loginUsername,
            String loginPassword, Map<String, String> usernameToUidMap)
            throws Exception {
        return scraperStrategy.getScraperInfoByLogin(cid, loginUsername, loginPassword, keyword, usernameToUidMap);
    }

}