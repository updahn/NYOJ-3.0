package top.hcode.hoj.crawler.language;

import top.hcode.hoj.pojo.bo.Pair_;
import top.hcode.hoj.pojo.entity.problem.Language;
import top.hcode.hoj.utils.Constants;

import java.util.List;

/**
 * @Author: Himit_ZH
 * @Date: 2022/1/27 21:15
 * @Description:
 */
public class LanguageContext {

    private LanguageStrategy languageStrategy;

    public LanguageContext(LanguageStrategy languageStrategy) {
        this.languageStrategy = languageStrategy;
    }

    public LanguageContext(Constants.RemoteOJ remoteOJ) {
        switch (remoteOJ) {
            case SPOJ:
                languageStrategy = new SPOJLanguageStrategy();
                break;
            case ATCODER:
                languageStrategy = new AtCoderLanguageStrategy();
                break;
            case VJ:
                languageStrategy = new VJLanguageStrategy();
                break;
            default:
                throw new RuntimeException("未知的OJ的名字，暂时不支持！");
        }
    }

    public List<Language> buildLanguageList() {
        return languageStrategy.buildLanguageList();
    }

    public String getLanguageNameById(String id, String oj) {
        return languageStrategy.getLanguageNameById(id, oj);
    }

    public List<Language> buildLanguageListByIds(List<Language> allLanguageList, List<String> langIdList) {
        return languageStrategy.buildLanguageListByIds(allLanguageList, langIdList);
    }

    public List<Language> buildAddLanguageList(List<Language> allLanguageList, List<Pair_<String, String>> langList,
            String oj) {
        return languageStrategy.buildAddLanguageList(allLanguageList, langList, oj);
    }
}