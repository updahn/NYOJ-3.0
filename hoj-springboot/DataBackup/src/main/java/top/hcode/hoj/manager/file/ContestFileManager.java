package top.hcode.hoj.manager.file;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.result.ResultStatus;
import top.hcode.hoj.dao.common.FileEntityService;
import top.hcode.hoj.dao.contest.ContestEntityService;
import top.hcode.hoj.dao.contest.ContestPrintEntityService;
import top.hcode.hoj.dao.contest.ContestProblemEntityService;
import top.hcode.hoj.dao.judge.JudgeEntityService;
import top.hcode.hoj.dao.tools.StatisticContestEntityService;
import top.hcode.hoj.dao.tools.StatisticRankEntityService;
import top.hcode.hoj.dao.user.UserInfoEntityService;
import top.hcode.hoj.dao.user.UserRoleEntityService;
import top.hcode.hoj.manager.oj.ContestCalculateRankManager;
import top.hcode.hoj.pojo.bo.File_;
import top.hcode.hoj.manager.oj.ContestRankManager;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.entity.contest.ContestPrint;
import top.hcode.hoj.pojo.entity.contest.ContestProblem;
import top.hcode.hoj.pojo.entity.contest.StatisticContest;
import top.hcode.hoj.pojo.entity.contest.StatisticRank;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.pojo.vo.ACMContestRankVO;
import top.hcode.hoj.pojo.vo.OIContestRankVO;
import top.hcode.hoj.pojo.vo.StatisticVO;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.validator.ContestValidator;
import top.hcode.hoj.validator.GroupValidator;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/10 14:27
 * @Description:
 */
@Component
@Slf4j(topic = "hoj")
public class ContestFileManager {

    @Autowired
    private ContestEntityService contestEntityService;

    @Autowired
    private ContestProblemEntityService contestProblemEntityService;

    @Autowired
    private ContestPrintEntityService contestPrintEntityService;

    @Autowired
    private FileEntityService fileEntityService;

    @Autowired
    private JudgeEntityService judgeEntityService;

    @Autowired
    private UserInfoEntityService userInfoEntityService;

    @Autowired
    private ContestCalculateRankManager contestCalculateRankManager;

    @Autowired
    private ContestRankManager contestRankManager;

    @Autowired
    private ContestValidator contestValidator;

    @Autowired
    private GroupValidator groupValidator;

    @Autowired
    private UserRoleEntityService userRoleEntityService;

    @Resource
    private StatisticRankEntityService statisticRankEntityService;

    @Resource
    private StatisticContestEntityService statisticContestEntityService;

    public void downloadContestRank(Long cid, Boolean forceRefresh, Boolean removeStar,
            Boolean isContainsAfterContestJudge,
            HttpServletResponse response) throws IOException, StatusFailException, StatusForbiddenException {
        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        // 获取本场比赛的状态
        Contest contest = contestEntityService.getById(cid);

        if (contest == null) {
            throw new StatusFailException("错误：该比赛不存在！");
        }

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");

        Long gid = contest.getGid();

        if (!isRoot && !contest.getUid().equals(userRolesVo.getUid())
                && !(contest.getIsGroup() && groupValidator.isGroupRoot(userRolesVo.getUid(), gid))) {
            throw new StatusForbiddenException("错误：您并非该比赛的管理员，无权下载榜单！");
        }

        // 检查是否需要开启封榜模式
        boolean isOpenSealRank = contestValidator.isSealRank(userRolesVo.getUid(), contest, forceRefresh, isRoot);
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        // 这里URLEncoder.encode可以防止中文乱码
        String fileName = URLEncoder.encode("contest_" + contest.getId() + "_rank", "UTF-8");
        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        response.setHeader("Content-Type", "application/xlsx");

        // 获取题目displayID列表
        QueryWrapper<ContestProblem> contestProblemQueryWrapper = new QueryWrapper<>();
        contestProblemQueryWrapper.eq("cid", contest.getId()).select("display_id").orderByAsc("display_id");
        List<String> contestProblemDisplayIDList = contestProblemEntityService.list(contestProblemQueryWrapper)
                .stream().map(ContestProblem::getDisplayId).collect(Collectors.toList());

        if (contest.getType().intValue() == Constants.Contest.TYPE_ACM.getCode()
                || contest.getType().intValue() == Constants.Contest.TYPE_EXAM.getCode()) {
            // ACM比赛或者考试
            List<ACMContestRankVO> acmContestRankVOList = contestCalculateRankManager.calcACMRank(
                    isOpenSealRank,
                    removeStar,
                    contest,
                    null,
                    null,
                    null,
                    isContainsAfterContestJudge,
                    null);
            EasyExcel.write(response.getOutputStream())
                    .head(fileEntityService.getContestRankExcelHead(contestProblemDisplayIDList, contest.getType()))
                    .sheet("rank")
                    .doWrite(fileEntityService.changeACMContestRankToExcelRowList(acmContestRankVOList,
                            contestProblemDisplayIDList, contest.getRankShowName(),
                            contest.getType() == Constants.Contest.TYPE_ACM.getCode()));
        } else {
            List<OIContestRankVO> oiContestRankVOList = contestCalculateRankManager.calcOIRank(
                    isOpenSealRank,
                    removeStar,
                    contest,
                    null,
                    null,
                    null,
                    isContainsAfterContestJudge,
                    null);
            EasyExcel.write(response.getOutputStream())
                    .head(fileEntityService.getContestRankExcelHead(contestProblemDisplayIDList, contest.getType()))
                    .sheet("rank")
                    .doWrite(fileEntityService.changOIContestRankToExcelRowList(oiContestRankVOList,
                            contestProblemDisplayIDList, contest.getRankShowName()));
        }
    }

    public void downloadStatisticRank(String cids, String scid, String keyword, HttpServletResponse response)
            throws IOException, StatusFailException, StatusForbiddenException, Exception {
        cids = cids.replace(" ", "+");

        List<ACMContestRankVO> result = new ArrayList<>();

        // 如果传入的是scid
        StatisticContest statisticContest = statisticContestEntityService.getOne(
                new QueryWrapper<StatisticContest>().eq("scid", scid), false);

        if (statisticContest != null) {
            List<StatisticRank> statisticRankList = statisticRankEntityService.list(
                    new QueryWrapper<StatisticRank>().in("scid", statisticContest.getScid()));

            // 转换 StatisticRank 为 ACMContestRankVO
            result = (List<ACMContestRankVO>) statisticRankList.stream()
                    .map(statisticRank -> {
                        ACMContestRankVO acmContestRankVo = new ACMContestRankVO();
                        // 复制属性
                        BeanUtil.copyProperties(statisticContest, acmContestRankVo, "account", "data");
                        BeanUtil.copyProperties(statisticRank, acmContestRankVo, "submissionInfo");

                        // 添加username和realname
                        acmContestRankVo.setRealname(userRoleEntityService.getRealNameByUid(statisticRank.getUid()));
                        acmContestRankVo.setUsername(userRoleEntityService.getUsernameByUid(statisticRank.getUid()));

                        // 解析 JSON 数据并设置到相应的字段
                        acmContestRankVo.setSubmissionInfo(parseJsonWithType(statisticRank.getJson(),
                                new TypeReference<HashMap<String, HashMap<String, Object>>>() {
                                }));
                        acmContestRankVo.setAccount(parseJsonWithType(statisticContest.getAccount(),
                                new TypeReference<HashMap<String, String>>() {
                                }));
                        acmContestRankVo.setData(parseJsonWithType(statisticContest.getData(),
                                new TypeReference<HashMap<String, String>>() {
                                }));

                        acmContestRankVo.setPercents(statisticContest.getPercents());
                        return acmContestRankVo; // 返回ACMContestRankVO对象
                    })
                    .collect(Collectors.toList());

        } else {
            List<Contest> contestList = contestValidator.validateContestList(cids);

            StatisticVO statisticVo = new StatisticVO().setContestList(contestList);

            result = contestRankManager.getStatisticRankList(statisticVo);
        }

        List<String> contest_cids = contestValidator.getSplitedCid(cids);

        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");

        // 这里URLEncoder.encode可以防止中文乱码
        String fileName = encodeFileName("contest_" + cids + "_rank");

        response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
        response.setHeader("Content-Type", "application/xlsx");

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");

        EasyExcel.write(response.getOutputStream())
                .head(fileEntityService.getStatisticRankExcelHead(contest_cids, isRoot))
                .sheet("rank")
                .doWrite(fileEntityService.changeStatisticContestRankToExcelRowList(result,
                        contest_cids, isRoot));
    }

    private String encodeFileName(String fileName) throws UnsupportedEncodingException {
        return URLEncoder.encode(fileName, "UTF-8").replace("+", "%20").replace("%2B", "+");
    }

    public void downloadContestACSubmission(Long cid, Boolean excludeAdmin, String splitType,
            HttpServletResponse response) throws StatusForbiddenException, StatusFailException {

        Contest contest = contestEntityService.getById(cid);

        if (contest == null) {
            throw new StatusFailException("错误：该比赛不存在！");
        }

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");

        Long gid = contest.getGid();
        if (!isRoot
                && !contest.getUid().equals(userRolesVo.getUid())
                && !(contest.getIsGroup() && groupValidator.isGroupRoot(userRolesVo.getUid(), gid))) {
            throw new StatusForbiddenException("错误：您并非该比赛的管理员，无权下载AC记录！");
        }

        boolean isACM = contest.getType().intValue() == Constants.Contest.TYPE_ACM.getCode();

        List<Judge> judgeList = getJudgeList(isACM, cid, excludeAdmin);

        List<ContestProblem> contestProblemList = getContestProblemList(cid);

        // 打包文件的临时路径 -> username为文件夹名字
        String tmpFilesDir = Constants.File.CONTEST_AC_SUBMISSION_TMP_FOLDER.getPath() + File.separator
                + IdUtil.fastSimpleUUID();
        FileUtil.mkdir(new File(tmpFilesDir));

        HashMap<String, Boolean> recordMap = new HashMap<>();
        if ("user".equals(splitType)) {
            /**
             * 以用户来分割提交的代码
             */
            List<String> usernameList = judgeList.stream()
                    .filter(distinctByKey(Judge::getUsername)) // 根据用户名过滤唯一
                    .map(Judge::getUsername).collect(Collectors.toList()); // 映射出用户名列表

            HashMap<Long, String> cpIdMap = new HashMap<>();
            for (ContestProblem contestProblem : contestProblemList) {
                cpIdMap.put(contestProblem.getId(), contestProblem.getDisplayId());
            }

            HashMap<Long, String> displayTitleMap = new HashMap<>();
            for (ContestProblem contestProblem : contestProblemList) {
                displayTitleMap.put(contestProblem.getId(), contestProblem.getDisplayTitle());
            }

            for (String username : usernameList) {
                // 对于每个用户生成对应的文件夹
                String userDir = tmpFilesDir + File.separator + username;
                FileUtil.mkdir(new File(userDir));
                // 如果是ACM模式，则所有提交代码都要生成，如果同一题多次提交AC，加上提交时间秒后缀 ---> A_(666666).c
                // 如果是OI模式就生成最近一次提交即可，且带上分数 ---> A_(666666)_100.c
                List<Judge> userSubmissionList = judgeList.stream()
                        .filter(judge -> judge.getUsername().equals(username)) // 过滤出对应用户的提交
                        .sorted(Comparator.comparing(Judge::getSubmitTime).reversed()) // 根据提交时间进行降序
                        .collect(Collectors.toList());

                for (Judge judge : userSubmissionList) {
                    String filePath = userDir + File.separator + cpIdMap.getOrDefault(judge.getCpid(), "null") + "_"
                            + displayTitleMap.getOrDefault(judge.getCpid(), "null");

                    // OI模式只取最后一次提交
                    if (!isACM) {
                        String key = judge.getUsername() + "_" + judge.getPid();
                        if (!recordMap.containsKey(key)) {
                            filePath += "_(" + judge.getSubmitId().toString() + ")"
                                    + judge.getScore() + "."
                                    + languageToFileSuffix(judge.getLanguage().toLowerCase());
                            FileWriter fileWriter = new FileWriter(new File(filePath));
                            fileWriter.write(judge.getCode());
                            recordMap.put(key, true);
                        }
                    } else {
                        filePath += "_(" + judge.getSubmitId().toString() + ")."
                                + languageToFileSuffix(judge.getLanguage().toLowerCase());
                        FileWriter fileWriter = new FileWriter(new File(filePath));
                        fileWriter.write(judge.getCode());
                    }

                }
            }
        } else if ("problem".equals(splitType)) {
            /**
             * 以比赛题目编号来分割提交的代码
             */

            for (ContestProblem contestProblem : contestProblemList) {
                // 对于每题目生成对应的文件夹
                String problemDir = tmpFilesDir + File.separator + contestProblem.getDisplayId();
                FileUtil.mkdir(new File(problemDir));
                // 如果是ACM模式，则所有提交代码都要生成，如果同一题多次提交AC，加上提交时间秒后缀 ---> username_(666666).c
                // 如果是OI模式就生成最近一次提交即可，且带上分数 ---> username_(666666)_100.c
                List<Judge> problemSubmissionList = judgeList.stream()
                        .filter(judge -> judge.getPid().equals(contestProblem.getPid())) // 过滤出对应题目的提交
                        .sorted(Comparator.comparing(Judge::getSubmitTime).reversed()) // 根据提交时间进行降序
                        .collect(Collectors.toList());

                for (Judge judge : problemSubmissionList) {
                    String filePath = problemDir + File.separator + judge.getUsername();
                    if (!isACM) {
                        String key = judge.getUsername() + "_" + contestProblem.getDisplayId();
                        // OI模式只取最后一次提交
                        if (!recordMap.containsKey(key)) {
                            filePath += "_" + judge.getScore() + "_("
                                    + threadLocalTime.get().format(judge.getSubmitTime()) + ")."
                                    + languageToFileSuffix(judge.getLanguage().toLowerCase());
                            FileWriter fileWriter = new FileWriter(new File(filePath));
                            fileWriter.write(judge.getCode());
                            recordMap.put(key, true);
                        }
                    } else {
                        filePath += "_(" + threadLocalTime.get().format(judge.getSubmitTime()) + ")."
                                + languageToFileSuffix(judge.getLanguage().toLowerCase());
                        FileWriter fileWriter = new FileWriter(new File(filePath));
                        fileWriter.write(judge.getCode());
                    }
                }
            }
        }

        String zipFileName = "contest_" + contest.getId() + ".zip";
        String zipPath = Constants.File.CONTEST_AC_SUBMISSION_TMP_FOLDER.getPath() + File.separator + zipFileName;
        File_.zip(new File(tmpFilesDir), new File(zipPath));
        // 将zip变成io流返回给前端
        FileReader zipFileReader = new FileReader(new File(zipPath));
        BufferedInputStream bins = new BufferedInputStream(zipFileReader.getInputStream());// 放到缓冲流里面
        OutputStream outs = null;// 获取文件输出IO流
        BufferedOutputStream bouts = null;
        try {
            outs = response.getOutputStream();
            bouts = new BufferedOutputStream(outs);
            response.setContentType("application/x-download");
            response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(zipFileName, "UTF-8"));
            int bytesRead = 0;
            byte[] buffer = new byte[1024 * 10];
            // 开始向网络传输文件流
            while ((bytesRead = bins.read(buffer, 0, 1024 * 10)) != -1) {
                bouts.write(buffer, 0, bytesRead);
            }
            // 刷新缓存
            bouts.flush();
        } catch (IOException e) {
            log.error("下载比赛AC提交代码的压缩文件异常------------>", e);
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            Map<String, Object> map = new HashMap<>();
            map.put("status", ResultStatus.SYSTEM_ERROR);
            map.put("msg", "下载文件失败，请重新尝试！");
            map.put("data", null);
            try {
                response.getWriter().println(JSONUtil.toJsonStr(map));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } finally {
            try {
                bins.close();
                if (outs != null) {
                    outs.close();
                }
                if (bouts != null) {
                    bouts.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        FileUtil.del(new File(tmpFilesDir));
        FileUtil.del(new File(zipPath));

    }

    public List<Judge> getJudgeList(Boolean isACM, Long cid, Boolean excludeAdmin) {
        Contest contest = contestEntityService.getById(cid);
        List<String> AdminUidList = userInfoEntityService.getNowContestAdmin(contest.getId());
        QueryWrapper<Judge> judgeQueryWrapper = new QueryWrapper<>();
        judgeQueryWrapper.eq("cid", cid)
                .eq(isACM, "status", Constants.Judge.STATUS_ACCEPTED.getStatus())
                .isNotNull(!isACM, "score") // OI模式取得分不为null的
                .between("submit_time", contest.getStartTime(), contest.getEndTime())
                .ne(excludeAdmin, "uid", contest.getUid()) // 排除比赛创建者和root
                .notIn(excludeAdmin && AdminUidList.size() > 0, "uid", AdminUidList)
                .orderByDesc("submit_time");

        List<Judge> judgeList = judgeEntityService.list(judgeQueryWrapper);

        return judgeList;
    }

    public List<ContestProblem> getContestProblemList(Long cid) {
        QueryWrapper<ContestProblem> contestProblemQueryWrapper = new QueryWrapper<>();
        contestProblemQueryWrapper.eq("cid", cid);
        List<ContestProblem> contestProblemList = contestProblemEntityService.list(contestProblemQueryWrapper);
        return contestProblemList;
    }

    public void downloadContestPrintText(Long id, HttpServletResponse response) throws StatusForbiddenException {
        ContestPrint contestPrint = contestPrintEntityService.getById(id);
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");

        Long cid = contestPrint.getCid();

        Contest contest = contestEntityService.getById(cid);

        Long gid = contest.getGid();

        if (!isRoot && !contest.getUid().equals(userRolesVo.getUid())
                && !(contest.getIsGroup() && groupValidator.isGroupRoot(userRolesVo.getUid(), gid))) {
            throw new StatusForbiddenException("错误：您并非该比赛的管理员，无权下载打印代码！");
        }

        String username = userRoleEntityService.getUsernameByUid(contestPrint.getUid());
        String filename = username + "_Contest_Print.txt";
        String filePath = Constants.File.CONTEST_TEXT_PRINT_FOLDER.getPath() + File.separator + id + File.separator
                + filename;
        if (!FileUtil.exist(new File(filePath))) {

            FileWriter fileWriter = new FileWriter(new File(filePath));
            fileWriter.write(contestPrint.getContent());
        }

        FileReader zipFileReader = new FileReader(new File(filePath));
        BufferedInputStream bins = new BufferedInputStream(zipFileReader.getInputStream());// 放到缓冲流里面
        OutputStream outs = null;// 获取文件输出IO流
        BufferedOutputStream bouts = null;
        try {
            outs = response.getOutputStream();
            bouts = new BufferedOutputStream(outs);
            response.setContentType("application/x-download");
            response.setHeader("Content-disposition", "attachment;filename=" + URLEncoder.encode(filename, "UTF-8"));
            int bytesRead = 0;
            byte[] buffer = new byte[1024 * 10];
            // 开始向网络传输文件流
            while ((bytesRead = bins.read(buffer, 0, 1024 * 10)) != -1) {
                bouts.write(buffer, 0, bytesRead);
            }
            // 刷新缓存
            bouts.flush();
        } catch (IOException e) {
            log.error("下载比赛打印文本文件异常------------>", e);
            response.reset();
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            Map<String, Object> map = new HashMap<>();
            map.put("status", ResultStatus.SYSTEM_ERROR);
            map.put("msg", "下载文件失败，请重新尝试！");
            map.put("data", null);
            try {
                response.getWriter().println(JSONUtil.toJsonStr(map));
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } finally {
            try {
                bins.close();
                if (outs != null) {
                    outs.close();
                }
                if (bouts != null) {
                    bouts.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public final ThreadLocal<SimpleDateFormat> threadLocalTime = new ThreadLocal<SimpleDateFormat>() {
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat("yyyyMMddHHmmss");
        }
    };

    public <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    public String languageToFileSuffix(String language) {
        // 统一转化为小写
        language = language.toLowerCase();

        List<String> CPPLang = Arrays.asList("c++", "c", "g++", "gcc", "clang++", "clang");
        List<String> PythonLang = Arrays.asList("python", "pypy");
        List<String> CsLang = Arrays.asList("c#", "csharp");

        for (String lang : CPPLang) {
            if (language.contains(lang)) {
                return "cpp";
            }
        }

        for (String lang : PythonLang) {
            if (language.contains(lang)) {
                return "py";
            }
        }

        for (String lang : CsLang) {
            if (language.contains(lang)) {
                return "cs";
            }
        }

        if (language.contains("javascript")) {
            return "js";
        }

        if (language.contains("pascal")) {
            return "pas";
        }

        if (language.contains("haskell")) {
            return "hs";
        }

        if (language.contains("fortran")) {
            return "f";
        }

        if (language.contains("perl")) {
            return "pl";
        }

        if (language.contains("matlab")) {
            return "m";
        }

        if (language.contains("prolog")) {
            return "pl";
        }

        if (language.contains("modula2")) {
            return "mod";
        }

        if (language.contains("a8086")) {
            return "asm";
        }

        // 语言和文件后缀相同
        if (language.contains("java")) {
            return "java";
        }

        if (language.contains("ada")) {
            return "ada";
        }

        if (language.contains("ml")) {
            return "ml";
        }

        if (language.contains("lisp")) {
            return "lisp";
        }

        if (language.contains("schema")) {
            return "schema";
        }

        if (language.contains("vhdl")) {
            return "vhdl";
        }

        if (language.contains("mips")) {
            return "mips";
        }

        if (language.contains("spice")) {
            return "spice";
        }

        if (language.contains("vb")) {
            return "vb";
        }

        if (language.contains("plsql")) {
            return "plsql";
        }

        if (language.contains("go")) {
            return "go";
        }

        if (language.contains("php")) {
            return "php";
        }

        return "txt";
    }

    private static <T> T parseJsonWithType(String json, TypeReference<T> typeReference) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

}