package top.hcode.hoj.manager.oj;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import top.hcode.hoj.annotation.HOJAccessEnum;
import top.hcode.hoj.common.exception.StatusAccessDeniedException;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.exception.StatusNotFoundException;
import top.hcode.hoj.dao.contest.ContestEntityService;
import top.hcode.hoj.dao.judge.JudgeEntityService;
import top.hcode.hoj.dao.judge.RemoteJudgeEntityService;
import top.hcode.hoj.dao.problem.*;
import top.hcode.hoj.dao.training.TrainingProblemEntityService;
import top.hcode.hoj.exception.AccessException;
import top.hcode.hoj.pojo.dto.LastAcceptedCodeVO;
import top.hcode.hoj.pojo.dto.PidListDTO;
import top.hcode.hoj.pojo.dto.ProblemRes;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.pojo.entity.judge.RemoteJudge;
import top.hcode.hoj.pojo.entity.problem.*;
import top.hcode.hoj.pojo.entity.training.TrainingProblem;
import top.hcode.hoj.pojo.vo.*;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.utils.HtmlToPdfUtils;
import top.hcode.hoj.validator.AccessValidator;
import top.hcode.hoj.validator.ContestValidator;
import top.hcode.hoj.validator.GroupValidator;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/11 10:37
 * @Description:
 */
@Component
public class ProblemManager {
    @Autowired
    private ProblemEntityService problemEntityService;

    @Autowired
    private ProblemTagEntityService problemTagEntityService;

    @Autowired
    private JudgeEntityService judgeEntityService;

    @Autowired
    private TagEntityService tagEntityService;

    @Autowired
    private LanguageEntityService languageEntityService;

    @Autowired
    private ContestEntityService contestEntityService;

    @Autowired
    private ProblemLanguageEntityService problemLanguageEntityService;

    @Autowired
    private CodeTemplateEntityService codeTemplateEntityService;

    @Autowired
    private ContestValidator contestValidator;

    @Autowired
    private GroupValidator groupValidator;

    @Autowired
    private AccessValidator accessValidator;

    @Autowired
    private TrainingManager trainingManager;

    @Autowired
    private ContestManager contestManager;

    @Autowired
    private HtmlToPdfUtils htmlToPdfUtils;

    @Autowired
    private TrainingProblemEntityService trainingProblemEntityService;

    @Autowired
    private RemoteJudgeEntityService remoteJudgeEntityService;

    /**
     * @MethodName getProblemList
     * @Params * @param null
     * @Description 获取题目列表分页
     * @Since 2020/10/27
     */
    public Page<ProblemVO> getProblemList(Integer limit, Integer currentPage,
            String keyword, List<Long> tagId, Integer difficulty, Integer type, String oj) {
        // 页数，每页题数若为空，设置默认值
        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        if (limit == null || limit < 1)
            limit = 10;

        // 关键词查询不为空
        if (!StringUtils.isEmpty(keyword)) {
            keyword = keyword.trim();
        }
        if (oj != null && !Constants.RemoteOJ.isRemoteOJ(oj)) {
            oj = "Mine";
        }
        return problemEntityService.getProblemList(limit, currentPage, null, keyword,
                difficulty, type, tagId, oj);
    }

    /**
     * @MethodName getRandomProblem
     * @Description 随机选取一道题目
     * @Since 2020/10/27
     */
    public RandomProblemVO getRandomProblem(String oj) throws StatusFailException {
        QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();

        // 必须是公开题目
        if (!Constants.RemoteOJ.isRemoteOJ(oj)) {
            queryWrapper.select("problem_id").eq("auth", 1).eq("is_remote", false).eq("is_group", false);
        } else {
            queryWrapper.select("problem_id").eq("auth", 1).eq("is_remote", true).eq("is_group", false)
                    .likeRight("problem_id", oj);
        }

        List<Problem> list = problemEntityService.list(queryWrapper);
        if (list.size() == 0) {
            throw new StatusFailException("获取随机题目失败，题库暂无公开题目！");
        }
        Random random = new Random();
        int index = random.nextInt(list.size());
        RandomProblemVO randomProblemVo = new RandomProblemVO();
        randomProblemVo.setProblemId(list.get(index).getProblemId());
        return randomProblemVo;
    }

    /**
     * @MethodName getUserProblemStatus
     * @Description 获取用户对应该题目列表中各个题目的做题情况
     * @Since 2020/12/29
     */
    public HashMap<Long, Object> getUserProblemStatus(PidListDTO pidListDto) throws StatusNotFoundException {

        // 需要获取一下该token对应用户的数据
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        HashMap<Long, Object> result = new HashMap<>();
        // 先查询判断该用户对于这些题是否已经通过，若已通过，则无论后续再提交结果如何，该题都标记为通过
        QueryWrapper<Judge> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("distinct pid,status,submit_time,score")
                .in("pid", pidListDto.getPidList())
                .eq("uid", userRolesVo.getUid())
                .orderByDesc("submit_time");

        if (pidListDto.getIsContestProblemList()) {
            // 如果是比赛的提交记录需要判断cid
            queryWrapper.eq("cid", pidListDto.getCid());
        } else {
            queryWrapper.eq("cid", 0);
            if (pidListDto.getGid() != null) {
                queryWrapper.eq("gid", pidListDto.getGid());
            } else {
                queryWrapper.isNull("gid");
            }
        }
        List<Judge> judges = judgeEntityService.list(queryWrapper);

        boolean isACMContest = true;
        boolean isContainsContestEndJudge = false;
        long contestEndTime = 0L;
        Contest contest = null;
        if (pidListDto.getIsContestProblemList()) {
            contest = contestEntityService.getById(pidListDto.getCid());
            if (contest == null) {
                throw new StatusNotFoundException("错误：该比赛不存在！");
            }
            isACMContest = contest.getType().intValue() == Constants.Contest.TYPE_ACM.getCode();
            isContainsContestEndJudge = Objects.equals(contest.getAllowEndSubmit(), true)
                    && Objects.equals(pidListDto.getContainsEnd(), true);
            contestEndTime = contest.getEndTime().getTime();
        }
        boolean isSealRank = false;
        if (!isACMContest && CollectionUtil.isNotEmpty(judges)) {
            isSealRank = contestValidator.isSealRank(userRolesVo.getUid(), contest, false,
                    SecurityUtils.getSubject().hasRole("root"));
        }
        for (Judge judge : judges) {

            HashMap<String, Object> temp = new HashMap<>();
            if (pidListDto.getIsContestProblemList()) { // 如果是比赛的题目列表状态

                // 如果是隐藏比赛后的提交，需要判断提交时间进行过滤
                if (!isContainsContestEndJudge && judge.getSubmitTime().getTime() >= contestEndTime) {
                    continue;
                }

                if (!isACMContest) {
                    if (!result.containsKey(judge.getPid())) {
                        // IO比赛的，如果还未写入，则使用最新一次提交的结果
                        // 判断该提交是否为封榜之后的提交,OI赛制封榜后的提交看不到提交结果，
                        // 只有比赛结束可以看到,比赛管理员与超级管理员的提交除外
                        if (isSealRank) {
                            temp.put("status", Constants.Judge.STATUS_SUBMITTED_UNKNOWN_RESULT.getStatus());
                            temp.put("score", null);
                        } else {
                            temp.put("status", judge.getStatus());
                            temp.put("score", judge.getScore());
                        }
                        result.put(judge.getPid(), temp);
                    }
                } else {
                    if (judge.getStatus().intValue() == Constants.Judge.STATUS_ACCEPTED.getStatus()) {
                        // 如果该题目已通过，且同时是为不封榜前提交的，则强制写为通过（0）
                        temp.put("status", Constants.Judge.STATUS_ACCEPTED.getStatus());
                        temp.put("score", null);
                        result.put(judge.getPid(), temp);
                    } else if (!result.containsKey(judge.getPid())) {
                        // 还未写入，则使用最新一次提交的结果
                        temp.put("status", judge.getStatus());
                        temp.put("score", null);
                        result.put(judge.getPid(), temp);
                    }
                }

            } else { // 不是比赛题目
                if (judge.getStatus().intValue() == Constants.Judge.STATUS_ACCEPTED.getStatus()) {
                    // 如果该题目已通过，则强制写为通过（0）
                    temp.put("status", Constants.Judge.STATUS_ACCEPTED.getStatus());
                    result.put(judge.getPid(), temp);
                } else if (!result.containsKey(judge.getPid())) {
                    // 还未写入，则使用最新一次提交的结果
                    temp.put("status", judge.getStatus());
                    result.put(judge.getPid(), temp);
                }
            }
        }

        // 再次检查，应该可能从未提交过该题，则状态写为-10
        for (Long pid : pidListDto.getPidList()) {
            // 如果是比赛的题目列表状态
            if (pidListDto.getIsContestProblemList()) {
                if (!result.containsKey(pid)) {
                    HashMap<String, Object> temp = new HashMap<>();
                    temp.put("score", null);
                    temp.put("status", Constants.Judge.STATUS_NOT_SUBMITTED.getStatus());
                    result.put(pid, temp);
                }
            } else {
                if (!result.containsKey(pid)) {
                    HashMap<String, Object> temp = new HashMap<>();
                    temp.put("status", Constants.Judge.STATUS_NOT_SUBMITTED.getStatus());
                    result.put(pid, temp);
                }
            }
        }
        return result;

    }

    /**
     * @MethodName getProblemInfo
     * @Description 获取指定题目的详情信息，标签，所支持语言，做题情况（只能查询公开题目 也就是auth为1）
     * @Since 2020/10/27
     */
    public ProblemInfoVO getProblemInfo(String problemId, Long gid, Long tid, Long peid)
            throws StatusNotFoundException, StatusForbiddenException {
        ProblemRes problem = new ProblemRes();
        List<ProblemDescription> problemDescriptionList = new ArrayList<>();

        if (tid != null) {
            // 根据cid和displayId获取pid
            QueryWrapper<TrainingProblem> trainingProblemQueryWrapper = new QueryWrapper<>();
            trainingProblemQueryWrapper.eq("tid", tid).eq("display_id", problemId);
            TrainingProblem trainingProblem = trainingProblemEntityService.getOne(trainingProblemQueryWrapper);

            if (trainingProblem == null) {
                throw new StatusNotFoundException("该训练题目不存在");
            }

            // 查询题目详情，题目标签，题目语言，题目做题情况
            problem = problemEntityService.getProblemRes(trainingProblem.getPid(), trainingProblem.getPeid(),
                    null, null, null);
            problemDescriptionList = problemEntityService.getProblemDescriptionList(
                    trainingProblem.getPid(), trainingProblem.getPeid(), null, null);
        } else {
            problem = problemEntityService.getProblemRes(null, null, problemId, gid, null);
            problemDescriptionList = problemEntityService.getProblemDescriptionList(null, null, problemId, gid);
        }

        if (problem == null) {
            throw new StatusNotFoundException("该题号对应的题目不存在");
        }
        if (problem.getAuth() != 1) {
            throw new StatusForbiddenException("该题号对应题目并非公开题目，不支持访问！");
        }

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        if (problem.getIsGroup() && !isRoot) {
            if (gid == null) {
                throw new StatusForbiddenException("题目为团队所属，此处不支持访问，请前往团队查看！");
            }
            if (!groupValidator.isGroupMember(userRolesVo.getUid(), problem.getGid())) {
                throw new StatusForbiddenException("对不起，您并非该题目所属的团队内成员，无权查看题目！");
            }
        }

        QueryWrapper<ProblemTag> problemTagQueryWrapper = new QueryWrapper<>();
        problemTagQueryWrapper.eq("pid", problem.getId());

        // 获取该题号对应的标签id
        List<Long> tidList = new LinkedList<>();
        problemTagEntityService.list(problemTagQueryWrapper).forEach(problemTag -> {
            tidList.add(problemTag.getTid());
        });
        List<Tag> tags = new ArrayList<>();
        if (tidList.size() > 0) {
            tags = (List<Tag>) tagEntityService.listByIds(tidList);
        }

        // 记录 languageId对应的name
        HashMap<Long, String> tmpMap = new HashMap<>();

        // 题目编程语言对应的判题key
        HashMap<String, String> languageKey = new HashMap<>();
        // 获取题目提交的代码支持的语言
        List<String> languagesStr = new LinkedList<>();
        QueryWrapper<ProblemLanguage> problemLanguageQueryWrapper = new QueryWrapper<>();
        problemLanguageQueryWrapper.eq("pid", problem.getId()).select("lid");
        List<Long> lidList = problemLanguageEntityService.list(problemLanguageQueryWrapper)
                .stream().map(ProblemLanguage::getLid).collect(Collectors.toList());
        if (CollectionUtil.isNotEmpty(lidList)) {
            Collection<Language> languages = languageEntityService.listByIds(lidList);
            languages = languages.stream().sorted(Comparator.comparing(Language::getSeq, Comparator.reverseOrder())
                    .thenComparing(Language::getId))
                    .collect(Collectors.toList());
            languages.forEach(language -> {
                languagesStr.add(language.getName());
                tmpMap.put(language.getId(), language.getName());
                languageKey.put(language.getName(), language.getKey());
            });
        }
        // 获取题目的提交记录
        ProblemCountVO problemCount = judgeEntityService.getProblemCount(problem.getId(), gid);

        // 获取题目的代码模板
        QueryWrapper<CodeTemplate> codeTemplateQueryWrapper = new QueryWrapper<>();
        codeTemplateQueryWrapper.eq("pid", problem.getId()).eq("status", true);
        List<CodeTemplate> codeTemplates = codeTemplateEntityService.list(codeTemplateQueryWrapper);
        HashMap<String, String> LangNameAndCode = new HashMap<>();
        if (CollectionUtil.isNotEmpty(codeTemplates)) {
            for (CodeTemplate codeTemplate : codeTemplates) {
                LangNameAndCode.put(tmpMap.get(codeTemplate.getLid()), codeTemplate.getCode());
            }
        }
        // 屏蔽一些题目参数
        problem.setJudgeExtraFile(null)
                .setSpjCode(null)
                .setSpjLanguage(null);

        // 将数据统一写入到一个Vo返回数据实体类中
        return new ProblemInfoVO(problem, problemDescriptionList, tags, languagesStr, problemCount, LangNameAndCode,
                languageKey);
    }

    public String getProblemPdf(Long pid, Long peid, Long cid)
            throws StatusForbiddenException, StatusNotFoundException, IOException, StatusFailException {

        // 查询题目详情
        ProblemRes problem = problemEntityService.getProblemRes(pid, peid, null, null, cid);
        if (problem == null) {
            throw new StatusNotFoundException("该题号对应的题目不存在");
        }

        // 屏蔽一些题目参数
        problem.setJudgeExtraFile(null)
                .setSpjCode(null)
                .setSpjLanguage(null);

        String filePath = problem.getPdfDescription();

        if (StringUtils.isEmpty(filePath)) {
            // 更新题面对应的pdf信息
            htmlToPdfUtils.updateProblemPDF(problem);
        }

        return filePath;
    }

    public LastAcceptedCodeVO getUserLastAcceptedCode(Long pid, Long cid) {
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        if (cid == null) {
            cid = 0L;
        }
        QueryWrapper<Judge> judgeQueryWrapper = new QueryWrapper<>();
        judgeQueryWrapper.select("submit_id", "cid", "code", "username", "submit_time", "language")
                .eq("uid", userRolesVo.getUid())
                .eq("pid", pid)
                .eq("cid", cid)
                .eq("status", 0)
                .orderByDesc("submit_id")
                .last("limit 1");
        List<Judge> judgeList = judgeEntityService.list(judgeQueryWrapper);
        LastAcceptedCodeVO lastAcceptedCodeVO = new LastAcceptedCodeVO();
        if (CollectionUtil.isNotEmpty(judgeList)) {
            Judge judge = judgeList.get(0);
            lastAcceptedCodeVO.setSubmitId(judge.getSubmitId());
            lastAcceptedCodeVO.setLanguage(judge.getLanguage());
            lastAcceptedCodeVO.setCode(buildCode(judge));
        } else {
            lastAcceptedCodeVO.setCode("");
        }
        return lastAcceptedCodeVO;
    }

    private String buildCode(Judge judge) {
        if (judge.getCid() == 0) {
            // 比赛外的提交代码 如果不是超管或题目管理员，需要检查网站是否开启隐藏代码功能
            boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                    || SecurityUtils.getSubject().hasRole("problem_admin")
                    || SecurityUtils.getSubject().hasRole("admin");
            if (!isRoot) {
                try {
                    accessValidator.validateAccess(HOJAccessEnum.HIDE_NON_CONTEST_SUBMISSION_CODE);
                } catch (AccessException e) {
                    return "Because the super administrator has enabled " +
                            "the function of not viewing the submitted code outside the contest of master station, \n" +
                            "the code of this submission details has been hidden.";
                }
            }
        }
        if (judge.getLanguage().toLowerCase().contains("py")) {
            return judge.getCode() + "\n\n" +
                    "'''\n" +
                    "    @runId: " + judge.getSubmitId() + "\n" +
                    "    @language: " + judge.getLanguage() + "\n" +
                    "    @author: " + judge.getUsername() + "\n" +
                    "    @submitTime: " + DateUtil.format(judge.getSubmitTime(), "yyyy-MM-dd HH:mm:ss") + "\n" +
                    "'''";
        } else if (judge.getLanguage().toLowerCase().contains("ruby")) {
            return judge.getCode() + "\n\n" +
                    "=begin\n" +
                    "* @runId: " + judge.getSubmitId() + "\n" +
                    "* @language: " + judge.getLanguage() + "\n" +
                    "* @author: " + judge.getUsername() + "\n" +
                    "* @submitTime: " + DateUtil.format(judge.getSubmitTime(), "yyyy-MM-dd HH:mm:ss") + "\n" +
                    "=end";
        } else {
            return judge.getCode() + "\n\n" +
                    "/**\n" +
                    "* @runId: " + judge.getSubmitId() + "\n" +
                    "* @language: " + judge.getLanguage() + "\n" +
                    "* @author: " + judge.getUsername() + "\n" +
                    "* @submitTime: " + DateUtil.format(judge.getSubmitTime(), "yyyy-MM-dd HH:mm:ss") + "\n" +
                    "*/";
        }
    }

    public List<ProblemFullScreenListVO> getFullScreenProblemList(Long tid, Long cid)
            throws StatusFailException, StatusForbiddenException, StatusAccessDeniedException {
        if (tid != null) {
            return trainingManager.getProblemFullScreenList(tid);
        } else if (cid != null && cid != 0) {
            return contestManager.getContestFullScreenProblemList(cid);
        } else {
            throw new StatusFailException("请求参数错误：tid或cid不能为空");
        }
    }

    public List<RemotejudgeVO> getRemoteJudgeStatusList(String remoteOj) {
        // 查询远程评测记录
        QueryWrapper<RemoteJudge> queryWrapper = new QueryWrapper<RemoteJudge>()
                .orderByDesc("gmt_create");
        if (!StringUtils.isEmpty(remoteOj)) {
            queryWrapper.eq("oj", remoteOj);
        }

        List<RemoteJudge> remoteJudgeList = remoteJudgeEntityService.list(queryWrapper);

        // 按OJ分组处理
        Map<String, RemotejudgeVO> resultMap = new HashMap<>();

        // 处理查询结果
        for (RemoteJudge judge : remoteJudgeList) {
            String oj = judge.getOj();
            RemotejudgeVO vo = resultMap.computeIfAbsent(oj,
                    k -> new RemotejudgeVO(k, new ArrayList<>(), new ArrayList<>()));

            // 每个OJ只保留最新的25条记录
            if (vo.getPercentList().size() < 25) {
                vo.getPercentList().add(judge.getPercent());
                vo.getCreateTimeList().add(judge.getGmtCreate());
            }
        }

        List<Constants.RemoteOJ> remoteOjList = Constants.RemoteOJ.getRemoteOJList();

        // 如果未指定远程OJ，则补充所有支持的OJ
        if (StringUtils.isEmpty(remoteOj)) {
            remoteOjList.forEach(oj -> resultMap.putIfAbsent(oj.getName(),
                    new RemotejudgeVO(oj.getName(), new ArrayList<>(), new ArrayList<>())));
        }

        // 设置每个OJ的可见性状态（使用最新一条记录的状态）
        resultMap.values().forEach(vo -> {
            if (!vo.getPercentList().isEmpty()) {
                vo.setPercent(vo.getPercentList().get(0));
            }
        });

        // 处理并返回结果
        return resultMap.values().stream()
                .peek(vo -> {
                    Collections.reverse(vo.getPercentList());
                    Collections.reverse(vo.getCreateTimeList());
                })
                .sorted(Comparator.comparingInt(vo -> {
                    for (int i = 0; i < remoteOjList.size(); i++) {
                        if (remoteOjList.get(i).getName().equals(vo.getOj())) {
                            return i;
                        }
                    }
                    return -1;
                }))
                .collect(Collectors.toList());
    }

}