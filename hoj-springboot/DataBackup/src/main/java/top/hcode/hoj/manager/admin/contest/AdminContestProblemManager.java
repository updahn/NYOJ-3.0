package top.hcode.hoj.manager.admin.contest;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.exception.StatusNotFoundException;
import top.hcode.hoj.crawler.problem.ProblemStrategy;
import top.hcode.hoj.dao.contest.ContestEntityService;
import top.hcode.hoj.dao.contest.ContestProblemEntityService;
import top.hcode.hoj.dao.judge.JudgeEntityService;
import top.hcode.hoj.dao.problem.ProblemEntityService;
import top.hcode.hoj.manager.admin.problem.RemoteProblemManager;
import top.hcode.hoj.manager.group.GroupManager;
import top.hcode.hoj.manager.msg.AdminNoticeManager;
import top.hcode.hoj.pojo.dto.ContestProblemDTO;
import top.hcode.hoj.pojo.dto.ProblemDTO;
import top.hcode.hoj.pojo.dto.ProblemRes;
import top.hcode.hoj.pojo.dto.ProblemResDTO;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.entity.contest.ContestProblem;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.problem.ProblemDescription;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.utils.HtmlToPdfUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/9 11:20
 * @Description:
 */
@Component
@Slf4j(topic = "hoj")
public class AdminContestProblemManager {

    @Autowired
    private ContestProblemEntityService contestProblemEntityService;

    @Autowired
    private ProblemEntityService problemEntityService;

    @Autowired
    private JudgeEntityService judgeEntityService;

    @Autowired
    private RemoteProblemManager remoteProblemManager;

    @Autowired
    private ContestEntityService contestEntityService;

    @Autowired
    private GroupManager groupManager;

    @Autowired
    private HtmlToPdfUtils htmlToPdfUtils;

    @Autowired
    private AdminNoticeManager adminNoticeManager;

    public HashMap<String, Object> getProblemList(Integer limit, Integer currentPage, String keyword,
            Long cid, Integer problemType, String oj, Integer difficulty, Integer type, Long gid)
            throws StatusForbiddenException {

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = groupManager.getGroupAuthAdmin(gid);

        // 获取本场比赛的信息
        Contest contest = contestEntityService.getById(cid);
        // 只有超级管理员和题目管理员、题目创建者才能操作
        if (!isRoot && !userRolesVo.getUsername().equals(contest.getAuthor())) {
            throw new StatusForbiddenException("对不起，你无权限查看题目！");
        }

        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        if (limit == null || limit < 1)
            limit = 10;
        IPage<ProblemResDTO> iPage = new Page<>(currentPage, limit);
        // 根据cid在ContestProblem表中查询到对应pid集合
        QueryWrapper<ContestProblem> contestProblemQueryWrapper = new QueryWrapper<>();
        contestProblemQueryWrapper.eq("cid", cid);
        List<Long> pidList = new LinkedList<>();

        List<ContestProblem> contestProblemList = contestProblemEntityService.list(contestProblemQueryWrapper);
        HashMap<Long, ContestProblem> contestProblemMap = new HashMap<>();
        contestProblemList.forEach(contestProblem -> {
            contestProblemMap.put(contestProblem.getPid(), contestProblem);
            pidList.add(contestProblem.getPid());
        });

        HashMap<String, Object> contestProblemHashMap = new HashMap<>();

        Boolean isRemote = Constants.RemoteOJ.isRemoteOJ(oj);

        Long contestGid = contest.getGid();
        IPage<ProblemResDTO> problemListPage = problemEntityService.getAdminContestProblemList(iPage, keyword, cid,
                problemType, oj, difficulty, type, gid, isRemote, contestGid, pidList);

        if (pidList.size() > 0 && problemType == null) {
            List<ProblemResDTO> problemList = problemListPage.getRecords();

            problemList.forEach(problemResDto -> {
                ContestProblem contestProblem = contestProblemMap.get(problemResDto.getId());
                List<ProblemDescription> problemDescriptionList = problemResDto.getProblemDescriptionList();

                // 获取对应的 peid 或第一个 problemDescription 的 title 和 id
                problemDescriptionList.stream()
                        .filter(pd -> contestProblem.getPeid() == null || pd.getId().equals(contestProblem.getPeid()))
                        .findFirst()
                        .ifPresent(problemDescription -> {
                            problemResDto.setTitle(problemDescription.getTitle())
                                    .setPeid(problemDescription.getId());
                        });
            });

            List<ProblemResDTO> sortedProblemList = problemList.stream()
                    .sorted(Comparator.comparing(ProblemResDTO::getId, (a, b) -> {
                        ContestProblem x = contestProblemMap.get(a);
                        ContestProblem y = contestProblemMap.get(b);
                        if (x == null && y != null) {
                            return 1;
                        } else if (x != null && y == null) {
                            return -1;
                        } else if (x == null) {
                            return -1;
                        } else {
                            return x.getDisplayId().compareTo(y.getDisplayId());
                        }
                    })).collect(Collectors.toList());
            problemListPage.setRecords(sortedProblemList);
        }

        contestProblemHashMap.put("problemList", problemListPage);
        contestProblemHashMap.put("contestProblemMap", contestProblemMap);

        return contestProblemHashMap;
    }

    public ProblemResDTO getProblem(Long pid, Long peid) throws StatusFailException, StatusForbiddenException {

        ProblemResDTO problem = problemEntityService.getProblemResDTO(pid, peid, null, null);

        if (problem != null) { // 查询成功
            // 获取当前登录的用户
            AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

            boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                    || SecurityUtils.getSubject().hasRole("admin");
            // 只有超级管理员和题目管理员、题目创建者才能操作
            if (!isRoot && !userRolesVo.getUsername().equals(problem.getAuthor())) {
                throw new StatusForbiddenException("对不起，你无权限查看题目！");
            }

            return problem;
        } else {
            throw new StatusFailException("查询失败！");
        }
    }

    public void deleteProblem(Long pid, Long cid) throws StatusForbiddenException {
        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");

        // 获取本场比赛的信息
        Contest contest = contestEntityService.getById(cid);
        // 只有超级管理员和题目管理员、题目创建者才能操作
        if (!isRoot && !userRolesVo.getUsername().equals(contest.getAuthor())) {
            throw new StatusForbiddenException("对不起，你无权限删除题目！");
        }

        // 比赛id不为null，表示就是从比赛列表移除而已
        if (cid != null) {
            QueryWrapper<ContestProblem> contestProblemQueryWrapper = new QueryWrapper<>();
            contestProblemQueryWrapper.eq("cid", cid).eq("pid", pid);
            contestProblemEntityService.remove(contestProblemQueryWrapper);
            // 把该题目在比赛的提交全部删掉
            UpdateWrapper<Judge> judgeUpdateWrapper = new UpdateWrapper<>();
            judgeUpdateWrapper.eq("cid", cid).eq("pid", pid);
            judgeEntityService.remove(judgeUpdateWrapper);

            log.info("[{}],[{}],cid:[{}],pid:[{}],operatorUid:[{}],operatorUsername:[{}]",
                    "Admin_Contest", "Remove_Problem", cid, pid, userRolesVo.getUid(), userRolesVo.getUsername());
        } else {
            /*
             * problem的id为其他表的外键的表中的对应数据都会被一起删除！
             */
            problemEntityService.removeById(pid);
            FileUtil.del(new File(Constants.File.TESTCASE_BASE_FOLDER.getPath() + File.separator + "problem_" + pid));

            log.info("[{}],[{}],cid:[{}],pid:[{}],operatorUid:[{}],operatorUsername:[{}]",
                    "Admin_Contest", "Delete_Problem", cid, pid, userRolesVo.getUid(), userRolesVo.getUsername());
        }
    }

    public Map<Object, Object> addProblem(ProblemDTO problemDto) throws StatusFailException {

        if (StringUtils.isEmpty(problemDto.getProblem().getProblemId())) {
            String lastProblemId = problemEntityService.getProblemLastId(problemDto.getProblem().getGid());

            // 设置题目自增的problemId
            problemDto.getProblem().setProblemId(lastProblemId);
        }

        QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("problem_id", problemDto.getProblem().getProblemId().toUpperCase());

        Long gid = problemDto.getProblem().getGid();
        if (gid == null) {
            queryWrapper.isNull("gid");
        } else {
            queryWrapper.eq("gid", gid);
        }
        Problem problem = problemEntityService.getOne(queryWrapper);
        if (problem != null) {
            throw new StatusFailException("该题目的Problem ID已存在，请更换！");
        }
        // 设置为比赛题目
        problemDto.getProblem().setAuth(3);
        boolean isOk = problemEntityService.adminAddProblem(problemDto);
        if (isOk) { // 添加成功
            // 顺便返回新的题目id，好下一步添加外键操作
            return MapUtil.builder().put("pid", problemDto.getProblem().getId()).map();
        } else {
            throw new StatusFailException("添加失败");
        }
    }

    public void updateProblem(ProblemDTO problemDto) throws StatusForbiddenException, StatusFailException {
        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");
        // 只有超级管理员和题目管理员、题目创建者才能操作
        if (!isRoot && !userRolesVo.getUsername().equals(problemDto.getProblem().getAuthor())) {
            throw new StatusForbiddenException("对不起，你无权限修改题目！");
        }

        QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("problem_id", problemDto.getProblem().getProblemId().toUpperCase());
        Long gid = problemDto.getProblem().getGid();
        if (gid == null) {
            queryWrapper.isNull("gid");
        } else {
            queryWrapper.eq("gid", gid);
        }
        Problem problem = problemEntityService.getOne(queryWrapper);

        // 如果problem_id不是原来的且已存在该problem_id，则修改失败！
        if (problem != null && problem.getId().longValue() != problemDto.getProblem().getId()) {
            throw new StatusFailException("当前的Problem ID 已被使用，请重新更换新的！");
        }

        // 记录修改题目的用户
        problemDto.getProblem().setModifiedUser(userRolesVo.getUsername());

        boolean isOk = problemEntityService.adminUpdateProblem(problemDto);
        if (!isOk) {
            throw new StatusFailException("修改失败");
        }
    }

    public ContestProblem getContestProblem(Long cid, Long pid) throws StatusFailException {
        QueryWrapper<ContestProblem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("cid", cid).eq("pid", pid);
        ContestProblem contestProblem = contestProblemEntityService.getOne(queryWrapper);
        if (contestProblem == null) {
            throw new StatusFailException("查询失败");
        }
        return contestProblem;
    }

    public ContestProblem setContestProblem(ContestProblem contestProblem) throws StatusFailException, Exception {
        boolean isOk = contestProblemEntityService.saveOrUpdate(contestProblem);
        if (isOk) {
            contestProblemEntityService.syncContestRecord(contestProblem.getPid(), contestProblem.getCid(),
                    contestProblem.getDisplayId());
            // 获取当前登录的用户
            AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
            log.info("[{}],[{}],cid:[{}],ContestProblem:[{}],operatorUid:[{}],operatorUsername:[{}]",
                    "Admin_Contest", "Update_Problem", contestProblem.getCid(), contestProblem, userRolesVo.getUid(),
                    userRolesVo.getUsername());

            // 获取题面
            ProblemRes problem = problemEntityService.getProblemRes(contestProblem.getPid(), contestProblem.getPeid(),
                    null, null, contestProblem.getCid());

            // 更新题面对应的pdf信息
            htmlToPdfUtils.updateProblemPDF(problem);

            return contestProblem;
        } else {
            throw new StatusFailException("更新失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void addProblemFromPublic(ContestProblemDTO contestProblemDto) throws StatusFailException {

        Long pid = contestProblemDto.getPid();
        Long peid = contestProblemDto.getPeid();
        Long cid = contestProblemDto.getCid();
        String displayId = contestProblemDto.getDisplayId();

        QueryWrapper<ContestProblem> contestProblemQueryWrapper = new QueryWrapper<>();
        contestProblemQueryWrapper.eq("cid", cid)
                .and(wrapper -> wrapper.eq("pid", pid)
                        .or()
                        .eq("display_id", displayId));
        ContestProblem contestProblem = contestProblemEntityService.getOne(contestProblemQueryWrapper, false);
        if (contestProblem != null) {
            throw new StatusFailException("添加失败，该题目已添加或者题目的比赛展示ID已存在！");
        }

        // 比赛中题目显示默认为原标题
        Problem problem = problemEntityService.getById(pid);

        String displayName = problemEntityService.getDefaultProblemTitle(problem);

        problem.setAuth(3); // 设置为比赛题目

        // 修改成比赛题目
        boolean updateProblem = problemEntityService.saveOrUpdate(problem);

        boolean isOk = contestProblemEntityService.saveOrUpdate(new ContestProblem()
                .setCid(cid).setPid(pid).setPeid(peid).setDisplayTitle(displayName).setDisplayId(displayId));
        if (!isOk || !updateProblem) {
            throw new StatusFailException("添加失败");
        }

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        log.info("[{}],[{}],cid:[{}],pid:[{}],operatorUid:[{}],operatorUsername:[{}]",
                "Admin_Contest", "Add_Public_Problem", cid, pid, userRolesVo.getUid(), userRolesVo.getUsername());
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeProblemDescription(ContestProblemDTO contestProblemDto) throws StatusFailException {
        Long pid = contestProblemDto.getPid();
        Long peid = contestProblemDto.getPeid();
        Long cid = contestProblemDto.getCid();

        ProblemResDTO problem = problemEntityService.getProblemResDTO(pid, peid, null, null);
        List<ProblemDescription> problemDescriptionList = problem.getProblemDescriptionList();

        boolean isOk = problemDescriptionList.stream()
                .anyMatch(desc -> {
                    if (peid.equals(desc.getId())) {
                        UpdateWrapper<ContestProblem> contestProblemUpdateWrapper = new UpdateWrapper<>();
                        contestProblemUpdateWrapper.eq("cid", cid).eq("pid", pid).set("peid", peid);
                        return contestProblemEntityService.update(contestProblemUpdateWrapper);
                    }
                    return false;
                });

        if (!isOk) {
            throw new StatusFailException("更新失败");
        }
    }

    @Async
    public void importContestRemoteOJProblem(String name, String problemIds, Long cid, String displayIds, Long gid)
            throws StatusFailException, StatusForbiddenException {
        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        // 获取本场比赛的信息
        Contest contest = contestEntityService.getById(cid);
        if (contest == null) { // 查询不存在
            throw new StatusFailException("查询失败：该比赛不存在,请检查参数cid是否准确！");
        }

        boolean isRoot = groupManager.getGroupAuthAdmin(gid);

        // 只有超级管理员和题目管理和比赛拥有者才能操作
        if (!isRoot && !userRolesVo.getUsername().equals(contest.getAuthor())) {
            throw new StatusForbiddenException("对不起，你无权限操作！");
        }

        final Long finalGid = gid;
        final String ojName = name.toUpperCase();
        final String finalUsername = userRolesVo.getUsername();

        // 记录导入结果
        Set<String> failedProblemIds = new HashSet<>();
        Set<String> existingProblemIds = new HashSet<>();
        Set<String> successProblemIds = new HashSet<>();

        List<String> problemIdList, displayIdList;

        if (problemIds.contains("-")) {
            String[] pr = problemIds.trim().split("-");
            String[] dr = displayIds.trim().split("-");
            if (pr.length != 2 || dr.length != 2)
                throw new StatusFailException("范围格式错误！");

            String psStr = pr[0].trim(), peStr = pr[1].trim();
            String dsStr = dr[0].trim(), deStr = dr[1].trim();

            if (!psStr.matches("\\d+") || !peStr.matches("\\d+"))
                throw new StatusFailException("题目ID范围应为纯数字！");
            if (!dsStr.matches("\\d+") || !deStr.matches("\\d+"))
                throw new StatusFailException("展示ID范围应为纯数字！");

            int ps = Integer.parseInt(psStr), pe = Integer.parseInt(peStr);
            int ds = Integer.parseInt(dsStr), de = Integer.parseInt(deStr);

            if (ps > pe)
                throw new StatusFailException("题目ID范围错误！");
            if (ds > de)
                throw new StatusFailException("展示ID范围错误！");
            if ((pe - ps) != (de - ds))
                throw new StatusFailException("题目ID和展示ID数量不一致！");

            problemIdList = IntStream.rangeClosed(ps, pe).mapToObj(String::valueOf).collect(Collectors.toList());
            displayIdList = IntStream.rangeClosed(ds, de).mapToObj(String::valueOf).collect(Collectors.toList());
        } else if (problemIds.contains(",")) {
            String[] pr = problemIds.split(","), dr = displayIds.split(",");
            if (pr.length != dr.length)
                throw new StatusFailException("题目ID和展示ID数量不一致！");

            problemIdList = Arrays.stream(pr).map(String::trim).collect(Collectors.toList());
            displayIdList = Arrays.stream(dr).map(String::trim).collect(Collectors.toList());
        } else {
            problemIdList = Collections.singletonList(problemIds.trim());
            displayIdList = Collections.singletonList(displayIds.trim());
        }

        if (problemIdList.size() != displayIdList.size()) {

            String errMsg = String.format("题目ID和比赛展示ID数量不一致！题目IDs：%s，长度：%s；比赛展示IDs：%s，长度：%s", problemIdList,
                    problemIdList.size(), displayIdList, displayIdList.size());

            // 异步同步系统通知
            adminNoticeManager.syncNoticeToNewRemoteProblemBatchUser(errMsg, userRolesVo.getUid());

            throw new StatusFailException("题目ID和比赛展示ID数量不一致！");
        }

        // 分割并处理每个题目ID
        IntStream.range(0, problemIdList.size()).parallel().forEach(i -> {
            String problemId = problemIdList.get(i);
            String displayId = displayIdList.get(i);
            // 检查题目是否已存在
            QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();

            queryWrapper.like("problem_id",
                    ojName.equals("VJ") ? problemId.toUpperCase() : ojName + "-" + problemId.toUpperCase());

            if (finalGid == null) {
                queryWrapper.isNull("gid");
            } else {
                queryWrapper.eq("gid", finalGid);
            }
            Problem problem = problemEntityService.getOne(queryWrapper, false);

            // 如果该题目不存在，需要先导入
            if (problem == null) {
                try {
                    ProblemStrategy.RemoteProblemInfo otherOJProblemInfo = remoteProblemManager
                            .getOtherOJProblemInfo(ojName, problemId, finalUsername);
                    if (otherOJProblemInfo != null) {
                        problem = remoteProblemManager.adminAddOtherOJProblem(otherOJProblemInfo, ojName, finalGid);
                        if (problem == null) {
                            throw new StatusFailException("导入新题目失败！请重新尝试！");
                        }
                    } else {
                        throw new StatusFailException("导入新题目失败！原因：可能是与该OJ链接超时或题号格式错误！");
                    }
                } catch (Exception e) {
                    log.error("导入题目 [" + ojName + "]" + " [" + problemId + "] 失败，原因: " + e.getMessage(), e);
                    failedProblemIds.add(problemId);
                    return;
                }
            }

            QueryWrapper<ContestProblem> contestProblemQueryWrapper = new QueryWrapper<>();
            Problem finalProblem = problem;
            contestProblemQueryWrapper.eq("cid", cid)
                    .and(wrapper -> wrapper.eq("pid", finalProblem.getId())
                            .or()
                            .eq("display_id", displayId));
            ContestProblem contestProblem = contestProblemEntityService.getOne(contestProblemQueryWrapper, false);

            if (contestProblem != null) {
                existingProblemIds.add(problemId);
                return;
            }

            // 比赛中题目显示默认为原标题
            String displayName = problemEntityService.getDefaultProblemTitle(finalProblem);

            // 修改成比赛题目
            boolean updateProblem = problemEntityService.saveOrUpdate(problem.setAuth(3));

            boolean isOk = contestProblemEntityService.saveOrUpdate(new ContestProblem()
                    .setCid(cid).setPid(problem.getId()).setDisplayTitle(displayName).setDisplayId(displayId));

            if (!isOk || !updateProblem) {
                failedProblemIds.add(problemId);
                return;
            }

            successProblemIds.add(problemId);
        });

        if (!failedProblemIds.isEmpty() || !existingProblemIds.isEmpty()) {
            int failedCount = failedProblemIds.size();
            int existCount = existingProblemIds.size();
            int successCount = problemIdList.size() - failedCount - existCount;

            String errMsg = String.format("[导入结果] 成功数：%d; 失败id：%s, 失败数：%d; 重复id：%s, 重复数：%d " +
                    "可能是与该OJ链接超时或题号格式错误，或者该题目已添加或者题目的比赛展示ID已存在，或者其他报错！",
                    successCount, failedProblemIds, failedCount, existingProblemIds, existCount);

            // 异步同步系统通知
            adminNoticeManager.syncNoticeToNewRemoteProblemBatchUser(errMsg, userRolesVo.getUid());

            log.info("[{}],[{}],cid:[{}],errMsg:[{}],operatorUid:[{}],operatorUsername:[{}]",
                    "Admin_Contest", "Add_Remote_Problem", cid, errMsg, userRolesVo.getUid(),
                    userRolesVo.getUsername());

            throw new StatusFailException(errMsg);
        }

        log.info("[{}],[{}],cid:[{}],problemIds:[{}],operatorUid:[{}],operatorUsername:[{}]",
                "Admin_Contest", "Add_Remote_Problem", cid, successProblemIds, userRolesVo.getUid(),
                userRolesVo.getUsername());
    }

    public String getContestPdf(Long cid, Boolean isCoverPage) throws StatusFailException, StatusForbiddenException {
        Contest contest = contestEntityService.getById(cid);

        if (contest == null) {
            throw new StatusFailException("查询失败：该比赛不存在,请检查参数cid是否准确！");
        }

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");

        // 只有超级管理员和题目管理员、题目创建者才能操作
        if (!isRoot && !userRolesVo.getUsername().equals(contest.getAuthor())) {
            throw new StatusForbiddenException("对不起，你无权限制作Pdf题目！");
        }

        String outputPath = contest.getPdfDescription();
        // 如果 outputName 为空，生成一个唯一 ID
        if (outputPath == null) {
            outputPath = IdUtil.fastSimpleUUID();
        }

        // 异步生成比赛题面
        htmlToPdfUtils.updateContestPDF(contest, outputPath);

        return outputPath;
    }

}