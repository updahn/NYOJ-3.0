package top.hcode.hoj.manager.admin.problem;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ReUtil;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import top.hcode.hoj.config.NacosSwitchConfig;
import top.hcode.hoj.config.SwitchConfig;
import top.hcode.hoj.crawler.language.LanguageContext;
import top.hcode.hoj.crawler.problem.*;
import top.hcode.hoj.dao.judge.RemoteJudgeAccountEntityService;
import top.hcode.hoj.pojo.entity.problem.*;
import top.hcode.hoj.dao.problem.*;
import top.hcode.hoj.manager.oj.CookieManager;
import top.hcode.hoj.utils.Constants;

import java.net.HttpCookie;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/9 17:33
 * @Description:
 */
@Component
@Slf4j(topic = "hoj")
public class RemoteProblemManager {

    @Autowired
    private ProblemEntityService problemEntityService;

    @Autowired
    private ProblemDescriptionEntityService problemDescriptionEntityService;

    @Autowired
    private ProblemTagEntityService problemTagEntityService;

    @Autowired
    private TagEntityService tagEntityService;

    @Autowired
    private LanguageEntityService languageEntityService;

    @Autowired
    private ProblemLanguageEntityService problemLanguageEntityService;

    @Autowired
    private RemoteJudgeAccountEntityService remoteJudgeAccountEntityService;

    @Autowired
    private NacosSwitchConfig nacosSwitchConfig;

    @Autowired
    private CookieManager cookieManager;

    public ProblemStrategy.RemoteProblemInfo getOtherOJProblemInfo(String OJName, String problemId, String author)
            throws Exception {

        ProblemStrategy problemStrategy;
        switch (OJName) {
            case "HDU":
                problemStrategy = new HDUProblemStrategy();
                break;
            case "CF":
                problemStrategy = new CFProblemStrategy();
                break;
            case "POJ":
                problemStrategy = new POJProblemStrategy();
                break;
            case "GYM":
                problemStrategy = new GYMProblemStrategy();
                break;
            case "SPOJ":
                problemStrategy = new SPOJProblemStrategy();
                break;
            case "AC":
                problemStrategy = new AtCoderProblemStrategy();
                break;
            case "LIBRE":
                problemStrategy = new LibreProblemStrategy();
                break;
            case "SCPC":
                problemStrategy = new SCPCProblemStrategy();
                break;
            case "QOJ":
                problemStrategy = new QOJProblemStrategy();
                break;
            case "NSWOJ":
                problemStrategy = new NSWOJProblemStrategy();
                break;
            case "NEWOJ":
                problemStrategy = new NEWOJProblemStrategy();
                break;
            case "VJ":
                problemStrategy = new VJProblemStrategy();
                break;
            case "DOTCPP":
                problemStrategy = new DotcppProblemStrategy();
                break;
            default:
                throw new Exception("未知的OJ的名字，暂时不支持！");
        }

        SwitchConfig switchConfig = nacosSwitchConfig.getSwitchConfig();

        ProblemContext problemContext = new ProblemContext(problemStrategy);
        try {
            if (Objects.equals("SCPC", OJName)) {
                if (StringUtils.isEmpty(switchConfig.getScpcSuperAdminAccount())) {
                    throw new Exception("未配备对应oj远程评测账号");
                }
                String username = switchConfig.getScpcSuperAdminAccount();
                String password = switchConfig.getScpcSuperAdminPassword();
                return problemContext.getProblemInfoByLogin(problemId, author, username, password);
            } else if (Objects.equals("QOJ", OJName)) {
                if (CollectionUtils.isEmpty(switchConfig.getQojUsernameList())) {
                    throw new Exception("未配备对应oj远程评测账号");
                }
                String username = switchConfig.getQojUsernameList().get(0);
                String password = switchConfig.getQojPasswordList().get(0);
                return problemContext.getProblemInfoByLogin(problemId, author, username, password);
            } else if (Objects.equals("VJ", OJName)) {
                if (CollectionUtils.isEmpty(switchConfig.getVjUsernameList())) {
                    throw new Exception("未配备对应oj远程评测账号");
                }
                String username = switchConfig.getVjUsernameList().get(0);
                List<HttpCookie> cookies = cookieManager.getCookieList(OJName, username, false);

                return problemContext.getProblemInfoByCookie(problemId, author, cookies);
            } else {
                return problemContext.getProblemInfo(problemId, author);
            }
        } catch (IllegalStateException e) {
            if (Objects.equals("GYM", OJName)) {
                if (CollectionUtils.isEmpty(switchConfig.getCfUsernameList())) {
                    throw new Exception("未配备对应oj远程评测账号");
                }
                String username = switchConfig.getCfUsernameList().get(0);
                List<HttpCookie> cookies = cookieManager.getCookieList(OJName, username, false);

                return problemContext.getProblemInfoByCookie(problemId, author, cookies);
            }
            return null;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Problem adminAddOtherOJProblem(ProblemStrategy.RemoteProblemInfo remoteProblemInfo, String OJName,
            Long gid) {

        Problem problem = remoteProblemInfo.getProblem();
        problem.setGid(gid);

        if (gid != null) {
            problem.setIsGroup(true);
        }

        boolean addProblemResult = problemEntityService.save(problem);

        List<ProblemDescription> problemDescriptionList = remoteProblemInfo.getProblemDescriptionList();

        boolean addProblemDescriptionResult = problemDescriptionList.stream()
                .peek(problemDescription -> {
                    problemDescription.setPid(problem.getId());
                    if (problemDescription.getAuthor() == null) {
                        problemDescription.setAuthor(problem.getAuthor());
                    }
                })
                .allMatch(problemDescriptionEntityService::save);

        boolean addProblemLanguageResult = addProblemLanguage(remoteProblemInfo, OJName, problem);

        boolean addProblemTagResult = true;
        List<Tag> addTagList = remoteProblemInfo.getTagList();

        List<Tag> needAddTagList = new LinkedList<>();

        HashMap<String, Tag> tagFlag = new HashMap<>();

        if (addTagList != null && addTagList.size() > 0) {
            QueryWrapper<Tag> tagQueryWrapper = new QueryWrapper<>();
            tagQueryWrapper.eq("oj", OJName);
            List<Tag> tagList = tagEntityService.list(tagQueryWrapper);
            // 已存在的tag不进行添加
            for (Tag hasTag : tagList) {
                tagFlag.put(hasTag.getName().toUpperCase(), hasTag);
            }
            for (Tag tmp : addTagList) {
                Tag tag = tagFlag.get(tmp.getName().toUpperCase());
                if (tag == null) {
                    tmp.setOj(OJName);
                    needAddTagList.add(tmp);
                } else {
                    needAddTagList.add(tag);
                }
            }
            tagEntityService.saveOrUpdateBatch(needAddTagList);

            List<ProblemTag> problemTagList = new LinkedList<>();
            for (Tag tmp : needAddTagList) {
                problemTagList.add(new ProblemTag().setTid(tmp.getId()).setPid(problem.getId()));
            }
            addProblemTagResult = problemTagEntityService.saveOrUpdateBatch(problemTagList);
        } else {
            QueryWrapper<Tag> tagQueryWrapper = new QueryWrapper<>();
            tagQueryWrapper.eq("name", OJName);
            Tag OJNameTag = tagEntityService.getOne(tagQueryWrapper, false);
            if (OJNameTag == null) {
                OJNameTag = new Tag();
                OJNameTag.setOj(OJName);
                OJNameTag.setName(OJName);
                tagEntityService.saveOrUpdate(OJNameTag);
            }
            addProblemTagResult = problemTagEntityService.saveOrUpdate(new ProblemTag().setTid(OJNameTag.getId())
                    .setPid(problem.getId()));
        }

        if (addProblemResult && addProblemDescriptionResult && addProblemTagResult && addProblemLanguageResult) {
            return problem;
        } else {
            return null;
        }
    }

    public Boolean addProblemLanguage(ProblemStrategy.RemoteProblemInfo remoteProblemInfo, String OJName,
            Problem problem) {
        String oj = OJName.equals("VJ")
                ? "VJ_" + ReUtil.get("VJ-(\\d+)\\(([^-]+)-", problem.getProblemId(), 2)
                : OJName;

        QueryWrapper<Language> languageQueryWrapper = new QueryWrapper<>();
        if (OJName.equals("GYM")) {
            languageQueryWrapper.eq("oj", "CF");
        } else {
            languageQueryWrapper.eq("oj", oj);
        }

        List<Language> OJLanguageList = languageEntityService.list(languageQueryWrapper);

        List<ProblemLanguage> problemLanguageList = new LinkedList<>();
        // 构建语言列表并批量保存
        List<Language> languageList = buildProblemLanguageList(remoteProblemInfo, oj, OJLanguageList);

        if (languageList != null) {
            for (Language language : languageList) {
                problemLanguageList.add(new ProblemLanguage().setPid(problem.getId()).setLid(language.getId()));
            }
        } else {
            for (Language language : OJLanguageList) {
                problemLanguageList.add(new ProblemLanguage().setPid(problem.getId()).setLid(language.getId()));
            }
        }

        return problemLanguageEntityService.saveOrUpdateBatch(problemLanguageList);
    }

    private List<Language> buildProblemLanguageList(ProblemStrategy.RemoteProblemInfo remoteProblemInfo, String oj,
            List<Language> OJLanguageList) {
        if (CollectionUtil.isEmpty(remoteProblemInfo.getLangIdList())) {
            return null;
        }

        LanguageContext languageContext = new LanguageContext(remoteProblemInfo.getRemoteOJ());
        if (remoteProblemInfo.getRemoteOJ().equals(Constants.RemoteOJ.VJ)) {
            List<Language> addLanguageList = languageContext.buildAddLanguageList(OJLanguageList,
                    remoteProblemInfo.getLangList(), oj);
            if (!CollectionUtils.isEmpty(addLanguageList)) {
                languageEntityService.saveOrUpdateBatch(addLanguageList);

                OJLanguageList.addAll(addLanguageList);

                return languageContext.buildLanguageListByIds(OJLanguageList, remoteProblemInfo.getLangIdList());
            }
        }

        return languageContext.buildLanguageListByIds(OJLanguageList, remoteProblemInfo.getLangIdList());
    }

}