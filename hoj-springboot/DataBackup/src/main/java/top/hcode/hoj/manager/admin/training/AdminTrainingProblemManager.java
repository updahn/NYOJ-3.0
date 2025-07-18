package top.hcode.hoj.manager.admin.training;

import cn.hutool.core.io.FileUtil;
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
import top.hcode.hoj.crawler.problem.ProblemStrategy;
import top.hcode.hoj.dao.problem.ProblemEntityService;
import top.hcode.hoj.dao.training.TrainingEntityService;
import top.hcode.hoj.dao.training.TrainingProblemEntityService;
import top.hcode.hoj.manager.admin.problem.RemoteProblemManager;
import top.hcode.hoj.manager.group.GroupManager;
import top.hcode.hoj.manager.msg.AdminNoticeManager;
import top.hcode.hoj.pojo.dto.ProblemResDTO;
import top.hcode.hoj.pojo.dto.TrainingProblemDTO;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.problem.ProblemDescription;
import top.hcode.hoj.pojo.entity.training.Training;
import top.hcode.hoj.pojo.entity.training.TrainingProblem;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.utils.Constants;

import javax.annotation.Resource;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/9 20:20
 * @Description:
 */
@Component
@Slf4j(topic = "hoj")
public class AdminTrainingProblemManager {

    @Resource
    private TrainingProblemEntityService trainingProblemEntityService;

    @Resource
    private TrainingEntityService trainingEntityService;

    @Resource
    private ProblemEntityService problemEntityService;

    @Resource
    private AdminTrainingRecordManager adminTrainingRecordManager;

    @Resource
    private RemoteProblemManager remoteProblemManager;

    @Autowired
    private GroupManager groupManager;

    @Autowired
    private AdminNoticeManager adminNoticeManager;

    public HashMap<String, Object> getProblemList(Integer limit, Integer currentPage, String keyword,
            Boolean queryExisted, Long tid) throws StatusFailException, StatusForbiddenException {

        // 获取本场训练的信息
        Training training = trainingEntityService.getById(tid);
        if (training == null) { // 查询不存在
            throw new StatusFailException("查询失败：该训练不存在,请检查参数tid是否准确！");
        }

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        Long gid = training.getGid();

        boolean isRoot = groupManager.getGroupAuthAdmin(gid);

        // 只有超级管理员和题目管理和训练拥有者才能操作
        if (!isRoot && !userRolesVo.getUsername().equals(training.getAuthor())) {
            throw new StatusForbiddenException("对不起，你无权限操作！");
        }

        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        if (limit == null || limit < 1)
            limit = 10;

        IPage<ProblemResDTO> iPage = new Page<>(currentPage, limit);
        // 根据tid在TrainingProblem表中查询到对应pid集合
        QueryWrapper<TrainingProblem> trainingProblemQueryWrapper = new QueryWrapper<>();
        trainingProblemQueryWrapper.eq("tid", tid).orderByAsc("display_id");
        List<Long> pidList = new LinkedList<>();
        List<TrainingProblem> trainingProblemList = trainingProblemEntityService.list(trainingProblemQueryWrapper);
        HashMap<Long, TrainingProblem> trainingProblemMap = new HashMap<>();
        trainingProblemList.forEach(trainingProblem -> {
            if (!trainingProblemMap.containsKey(trainingProblem.getPid())) {
                trainingProblemMap.put(trainingProblem.getPid(), trainingProblem);
            }
            pidList.add(trainingProblem.getPid());
        });

        HashMap<String, Object> trainingProblemHashMap = new HashMap<>();

        IPage<ProblemResDTO> problemListPage = problemEntityService.getAdminTrainingProblemList(iPage, keyword,
                queryExisted, tid, pidList);

        if (queryExisted && pidList.size() > 0) {
            List<ProblemResDTO> problemListPageRecords = problemListPage.getRecords();

            problemListPageRecords.forEach(problemResDto -> {
                TrainingProblem trainingProblem = trainingProblemMap.get(problemResDto.getId());
                List<ProblemDescription> problemDescriptionList = problemResDto.getProblemDescriptionList();

                // 获取对应的 peid 或第一个 problemDescription 的 title 和 id
                problemDescriptionList.stream()
                        .filter(pd -> trainingProblem.getPeid() == null || pd.getId().equals(trainingProblem.getPeid()))
                        .findFirst()
                        .ifPresent(problemDescription -> {
                            problemResDto.setTitle(problemDescription.getTitle())
                                    .setPeid(problemDescription.getId());
                        });
            });

            List<ProblemResDTO> sortProblemList = problemListPageRecords
                    .stream()
                    .sorted(Comparator.comparingInt(problem -> trainingProblemMap.get(problem.getId()).getRank()))
                    .collect(Collectors.toList());
            problemListPage.setRecords(sortProblemList);
        }

        trainingProblemHashMap.put("problemList", problemListPage);
        trainingProblemHashMap.put("trainingProblemMap", trainingProblemMap);

        return trainingProblemHashMap;
    }

    public void updateProblem(TrainingProblem trainingProblem) throws StatusFailException, StatusForbiddenException {

        Long tid = trainingProblem.getTid();

        // 获取本场训练的信息
        Training training = trainingEntityService.getById(tid);
        if (training == null) { // 查询不存在
            throw new StatusFailException("查询失败：该训练不存在,请检查参数tid是否准确！");
        }

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");

        // 只有超级管理员和题目管理和训练拥有者才能操作
        if (!isRoot && !userRolesVo.getUsername().equals(training.getAuthor())) {
            throw new StatusForbiddenException("对不起，你无权限操作！");
        }

        boolean isOk = trainingProblemEntityService.saveOrUpdate(trainingProblem);

        if (!isOk) {
            throw new StatusFailException("修改失败！");
        }
    }

    public void deleteProblem(Long pid, Long tid) throws StatusFailException, StatusForbiddenException {
        // 获取本场训练的信息
        Training training = trainingEntityService.getById(tid);
        if (training == null) { // 查询不存在
            throw new StatusFailException("查询失败：该训练不存在,请检查参数tid是否准确！");
        }

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");

        // 只有超级管理员和题目管理和训练拥有者才能操作
        if (!isRoot && !userRolesVo.getUsername().equals(training.getAuthor())) {
            throw new StatusForbiddenException("对不起，你无权限操作！");
        }

        boolean isOk = false;
        // 训练id不为null，表示就是从比赛列表移除而已
        if (tid != null) {
            QueryWrapper<TrainingProblem> trainingProblemQueryWrapper = new QueryWrapper<>();
            trainingProblemQueryWrapper.eq("tid", tid).eq("pid", pid);
            isOk = trainingProblemEntityService.remove(trainingProblemQueryWrapper);
        } else {
            /*
             * problem的id为其他表的外键的表中的对应数据都会被一起删除！
             */
            isOk = problemEntityService.removeById(pid);
        }

        if (isOk) { // 删除成功
            if (tid == null) {
                FileUtil.del(
                        new File(Constants.File.TESTCASE_BASE_FOLDER.getPath() + File.separator + "problem_" + pid));
                log.info("[{}],[{}],tid:[{}],pid:[{}],operatorUid:[{}],operatorUsername:[{}]",
                        "Admin_Training", "Delete_Problem", tid, pid, userRolesVo.getUid(), userRolesVo.getUsername());
            } else {
                log.info("[{}],[{}],tid:[{}],pid:[{}],operatorUid:[{}],operatorUsername:[{}]",
                        "Admin_Training", "Remove_Problem", tid, pid, userRolesVo.getUid(), userRolesVo.getUsername());
            }
            // 更新训练最近更新时间
            UpdateWrapper<Training> trainingUpdateWrapper = new UpdateWrapper<>();
            trainingUpdateWrapper.set("gmt_modified", new Date())
                    .eq("id", tid);
            trainingEntityService.update(trainingUpdateWrapper);
        } else {
            String msg = "删除失败！";
            if (tid != null) {
                msg = "移除失败！";
            }
            throw new StatusFailException(msg);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void addProblemFromPublic(TrainingProblemDTO trainingProblemDto)
            throws StatusFailException, StatusForbiddenException {

        Long pid = trainingProblemDto.getPid();
        Long peid = trainingProblemDto.getPeid();
        Long tid = trainingProblemDto.getTid();
        String displayId = trainingProblemDto.getDisplayId();

        // 获取本场训练的信息
        Training training = trainingEntityService.getById(tid);
        if (training == null) { // 查询不存在
            throw new StatusFailException("查询失败：该训练不存在,请检查参数tid是否准确！");
        }

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");

        // 只有超级管理员和题目管理和训练拥有者才能操作
        if (!isRoot && !userRolesVo.getUsername().equals(training.getAuthor())) {
            throw new StatusForbiddenException("对不起，你无权限操作！");
        }
        QueryWrapper<TrainingProblem> trainingProblemQueryWrapper = new QueryWrapper<>();
        trainingProblemQueryWrapper.eq("tid", tid)
                .and(wrapper -> wrapper.eq("pid", pid)
                        .or()
                        .eq("display_id", displayId));
        TrainingProblem trainingProblem = trainingProblemEntityService.getOne(trainingProblemQueryWrapper, false);
        if (trainingProblem != null) {
            throw new StatusFailException("添加失败，该题目已添加或者题目的训练展示ID已存在！");
        }

        TrainingProblem newTProblem = new TrainingProblem();
        boolean isOk = trainingProblemEntityService.saveOrUpdate(newTProblem
                .setTid(tid).setPid(pid).setPeid(peid).setDisplayId(displayId));
        if (isOk) { // 添加成功

            // 更新训练最近更新时间
            UpdateWrapper<Training> trainingUpdateWrapper = new UpdateWrapper<>();
            trainingUpdateWrapper.set("gmt_modified", new Date())
                    .eq("id", tid);
            trainingEntityService.update(trainingUpdateWrapper);

            log.info("[{}],[{}],tid:[{}],pid:[{}],operatorUid:[{}],operatorUsername:[{}]",
                    "Admin_Training", "Add_Public_Problem", tid, pid, userRolesVo.getUid(), userRolesVo.getUsername());

            // 异步地同步用户对该题目的提交数据
            adminTrainingRecordManager.syncAlreadyRegisterUserRecord(tid, pid, newTProblem.getId());
        } else {
            throw new StatusFailException("添加失败！");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeProblemDescription(TrainingProblemDTO trainingProblemDTO)
            throws StatusFailException, StatusForbiddenException {
        Long pid = trainingProblemDTO.getPid();
        Long peid = trainingProblemDTO.getPeid();
        Long tid = trainingProblemDTO.getTid();

        ProblemResDTO problem = problemEntityService.getProblemResDTO(pid, peid, null, null);

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");
        // 只有超级管理员和题目管理员、题目创建者才能操作
        if (!isRoot && !userRolesVo.getUsername().equals(problem.getAuthor())) {
            throw new StatusForbiddenException("对不起，你无权限修改题目！");
        }

        List<ProblemDescription> problemDescriptionList = problemEntityService.getProblemDescriptionList(pid, peid,
                null, null);

        boolean isOk = problemDescriptionList.stream()
                .anyMatch(desc -> {
                    if (peid.equals(desc.getId())) {
                        UpdateWrapper<TrainingProblem> trainingProblemUpdateWrapper = new UpdateWrapper<>();
                        trainingProblemUpdateWrapper.eq("tid", tid).eq("pid", pid).set("peid", peid);
                        return trainingProblemEntityService.update(trainingProblemUpdateWrapper);
                    }
                    return false;
                });

        if (!isOk) {
            throw new StatusFailException("更新失败");
        }
    }

    @Async
    public void importTrainingRemoteOJProblem(String name, String problemIds, Long tid, Long gid)
            throws StatusFailException, StatusForbiddenException {
        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        // 获取本场训练的信息
        Training training = trainingEntityService.getById(tid);
        if (training == null) { // 查询不存在
            throw new StatusFailException("查询失败：该训练不存在,请检查参数tid是否准确！");
        }

        boolean isRoot = groupManager.getGroupAuthAdmin(gid);

        // 只有超级管理员和题目管理和训练拥有者才能操作
        if (!isRoot && !userRolesVo.getUsername().equals(training.getAuthor())) {
            throw new StatusForbiddenException("对不起，你无权限操作！");
        }

        final Long finalGid = gid;
        final String ojName = name.toUpperCase();
        final String finalUsername = userRolesVo.getUsername();

        // 记录导入结果
        Set<String> failedProblemIds = new HashSet<>();
        Set<String> existingProblemIds = new HashSet<>();
        Set<String> successProblemIds = new HashSet<>();

        List<String> problemIdList;

        if (problemIds.contains("-")) {
            String[] pr = problemIds.trim().split("-");
            if (pr.length != 2)
                throw new StatusFailException("范围格式错误！");

            String psStr = pr[0].trim(), peStr = pr[1].trim();

            if (!psStr.matches("\\d+") || !peStr.matches("\\d+"))
                throw new StatusFailException("题目ID范围应为纯数字！");

            int ps = Integer.parseInt(psStr), pe = Integer.parseInt(peStr);

            if (ps > pe)
                throw new StatusFailException("题目ID范围错误！");

            problemIdList = IntStream.rangeClosed(ps, pe).mapToObj(String::valueOf).collect(Collectors.toList());
        } else if (problemIds.contains(",")) {
            String[] pr = problemIds.split(",");

            problemIdList = Arrays.stream(pr).map(String::trim).collect(Collectors.toList());
        } else {
            problemIdList = Collections.singletonList(problemIds.trim());
        }

        problemIdList.parallelStream().forEach(problemId -> {

            // 检查题目是否已存在
            QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();

            queryWrapper.eq("problem_id",
                    ojName.equals("VJ") ? "VJ(" + problemId + ")" : ojName + "-" + problemId.toUpperCase());

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

            QueryWrapper<TrainingProblem> trainingProblemQueryWrapper = new QueryWrapper<>();
            Problem finalProblem = problem;
            trainingProblemQueryWrapper.eq("tid", tid)
                    .and(wrapper -> wrapper.eq("pid", finalProblem.getId())
                            .or()
                            .eq("display_id", finalProblem.getProblemId()));
            TrainingProblem trainingProblem = trainingProblemEntityService.getOne(trainingProblemQueryWrapper, false);
            if (trainingProblem != null) {
                existingProblemIds.add(problemId);
                return;
            }

            TrainingProblem newTProblem = new TrainingProblem();
            boolean isOk = trainingProblemEntityService.saveOrUpdate(newTProblem
                    .setTid(tid).setPid(problem.getId()).setDisplayId(problem.getProblemId()));
            if (isOk) { // 添加成功

                // 更新训练最近更新时间
                UpdateWrapper<Training> trainingUpdateWrapper = new UpdateWrapper<>();
                trainingUpdateWrapper.set("gmt_modified", new Date())
                        .eq("id", tid);
                trainingEntityService.update(trainingUpdateWrapper);

                // 异步地同步用户对该题目的提交数据
                adminTrainingRecordManager.syncAlreadyRegisterUserRecord(tid, problem.getId(), newTProblem.getId());
            } else {
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
                    "可能是与该OJ链接超时或题号格式错误，或者该题目已添加或者题目的训练展示ID已存在，或者其他报错！",
                    successCount, failedProblemIds, failedCount, existingProblemIds, existCount);

            // 异步同步系统通知
            adminNoticeManager.syncNoticeToNewRemoteProblemBatchUser(errMsg, userRolesVo.getUid());

            log.info("[{}],[{}],tid:[{}],errMsg:[{}],operatorUid:[{}],operatorUsername:[{}]",
                    "Admin_Training", "Add_Remote_Problem", tid, errMsg, userRolesVo.getUid(),
                    userRolesVo.getUsername());

            throw new StatusFailException(errMsg);
        }

        log.info("[{}],[{}],tid:[{}],problemIds:[{}],operatorUid:[{}],operatorUsername:[{}]",
                "Admin_Training", "Add_Remote_Problem", tid, successProblemIds, userRolesVo.getUid(),
                userRolesVo.getUsername());
    }
}