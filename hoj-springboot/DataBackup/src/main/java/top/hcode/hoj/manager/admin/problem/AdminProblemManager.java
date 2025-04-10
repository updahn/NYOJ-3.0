package top.hcode.hoj.manager.admin.problem;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReUtil;

import com.alibaba.excel.util.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.exception.StatusNotFoundException;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.crawler.problem.ProblemStrategy;
import top.hcode.hoj.dao.judge.JudgeEntityService;
import top.hcode.hoj.dao.problem.ProblemCaseEntityService;
import top.hcode.hoj.dao.problem.ProblemDescriptionEntityService;
import top.hcode.hoj.dao.problem.ProblemEntityService;
import top.hcode.hoj.judge.Dispatcher;
import top.hcode.hoj.manager.msg.AdminNoticeManager;
import top.hcode.hoj.pojo.bo.Pair_;
import top.hcode.hoj.pojo.dto.CompileDTO;
import top.hcode.hoj.pojo.dto.ProblemDTO;
import top.hcode.hoj.pojo.dto.ProblemResDTO;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.problem.ProblemCase;
import top.hcode.hoj.pojo.entity.problem.ProblemDescription;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.validator.ProblemValidator;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/9 16:32
 * @Description:
 */

@Component
@RefreshScope
@Slf4j(topic = "hoj")
public class AdminProblemManager {
    @Autowired
    private ProblemEntityService problemEntityService;

    @Autowired
    private ProblemCaseEntityService problemCaseEntityService;

    @Autowired
    private Dispatcher dispatcher;

    @Value("${hoj.judge.token:no_judge_token}")
    private String judgeToken;

    @Resource
    private JudgeEntityService judgeEntityService;

    @Resource
    private ProblemValidator problemValidator;

    @Autowired
    private RemoteProblemManager remoteProblemManager;

    @Autowired
    private ProblemDescriptionEntityService problemDescriptionEntityService;

    @Autowired
    private AdminNoticeManager adminNoticeManager;

    public IPage<ProblemResDTO> getProblemList(Integer limit, Integer currentPage, String keyword, Integer auth,
            String oj,
            Integer difficulty, Integer type) {
        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        if (limit == null || limit < 1)
            limit = 10;

        IPage<ProblemResDTO> iPage = new Page<>(currentPage, limit);

        Boolean isRemote = Constants.RemoteOJ.isRemoteOJ(oj);

        return problemEntityService.getAdminProblemList(iPage, keyword, auth, oj, difficulty, type, isRemote);
    }

    public ProblemResDTO getProblem(Long pid, Long peid) throws StatusForbiddenException, StatusFailException {
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

    public void deleteProblem(Long pid) throws StatusFailException, StatusForbiddenException {
        ProblemResDTO problem = problemEntityService.getProblemResDTO(pid, null, null, null);

        if (problem != null) { // 查询成功
            // 获取当前登录的用户
            AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

            boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                    || SecurityUtils.getSubject().hasRole("admin");

            // 只有超级管理员和题目管理员、题目创建者才能操作
            if (!isRoot && !userRolesVo.getUsername().equals(problem.getAuthor())) {
                throw new StatusForbiddenException("对不起，你无权限删除题目！");
            }

            boolean isOk = problemEntityService.removeById(pid);
            /*
             * problem的id为其他表的外键的表中的对应数据都会被一起删除！
             */
            if (isOk) { // 删除成功
                FileUtil.del(
                        new File(Constants.File.TESTCASE_BASE_FOLDER.getPath() + File.separator + "problem_" + pid));
                log.info("[{}],[{}],pid:[{}],operatorUid:[{}],operatorUsername:[{}]",
                        "Admin_Problem", "Delete", pid, userRolesVo.getUid(), userRolesVo.getUsername());
            } else {
                throw new StatusFailException("删除失败！");
            }
        } else {
            throw new StatusFailException("删除失败！");
        }
    }

    public void addProblem(ProblemDTO problemDto) throws StatusFailException {

        if (StringUtils.isEmpty(problemDto.getProblem().getProblemId())) {
            String lastProblemId = problemEntityService.getProblemLastId(problemDto.getProblem().getGid());
            // 设置题目自增的problemId
            problemDto.getProblem().setProblemId(lastProblemId);
        }

        problemValidator.validateProblem(problemDto.getProblem());

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

        boolean isOk = problemEntityService.adminAddProblem(problemDto);
        if (!isOk) {
            throw new StatusFailException("添加失败");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateProblem(ProblemDTO problemDto) throws StatusForbiddenException, StatusFailException {

        problemValidator.validateProblemUpdate(problemDto.getProblem());

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");
        // 只有超级管理员和题目管理员、题目创建者才能操作
        if (!isRoot && !userRolesVo.getUsername().equals(problemDto.getProblem().getAuthor())) {
            throw new StatusForbiddenException("对不起，你无权限修改题目！");
        }

        String problemId = problemDto.getProblem().getProblemId().toUpperCase();
        QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("problem_id", problemId);
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

        boolean result = problemEntityService.adminUpdateProblem(problemDto);
        if (result) { // 更新成功
            if (problem == null) { // 说明改了problemId，同步一下judge表
                UpdateWrapper<Judge> judgeUpdateWrapper = new UpdateWrapper<>();
                judgeUpdateWrapper.eq("pid", problemDto.getProblem().getId())
                        .set("display_pid", problemId);
                judgeEntityService.update(judgeUpdateWrapper);
            }

        } else {
            throw new StatusFailException("修改失败");
        }
    }

    public List<ProblemCase> getProblemCases(Long pid, Long peid, Boolean isUpload) throws StatusForbiddenException {
        ProblemResDTO problem = problemEntityService.getProblemResDTO(pid, peid, null, null);

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");

        // 只有超级管理员和题目管理员、题目创建者才能操作
        if (!isRoot && !userRolesVo.getUsername().equals(problem.getAuthor())) {
            throw new StatusForbiddenException("对不起，你无权限获取题目样例！");
        }
        QueryWrapper<ProblemCase> problemCaseQueryWrapper = new QueryWrapper<>();
        problemCaseQueryWrapper.eq("pid", pid).eq("status", 0);
        if (isUpload) {
            problemCaseQueryWrapper.last("order by length(input) asc,input asc");
        }
        return problemCaseEntityService.list(problemCaseQueryWrapper);
    }

    public CommonResult compileSpj(CompileDTO compileDTO) {
        if (StringUtils.isEmpty(compileDTO.getCode()) ||
                StringUtils.isEmpty(compileDTO.getLanguage())) {
            return CommonResult.errorResponse("参数不能为空！");
        }

        compileDTO.setToken(judgeToken);
        return dispatcher.dispatch(Constants.TaskType.COMPILE_SPJ, compileDTO);
    }

    public CommonResult compileInteractive(CompileDTO compileDTO) {
        if (StringUtils.isEmpty(compileDTO.getCode()) ||
                StringUtils.isEmpty(compileDTO.getLanguage())) {
            return CommonResult.errorResponse("参数不能为空！");
        }

        compileDTO.setToken(judgeToken);
        return dispatcher.dispatch(Constants.TaskType.COMPILE_INTERACTIVE, compileDTO);
    }

    @Async
    public void importRemoteOJProblem(String name, String problemIds, Long gid) throws StatusFailException {
        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

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

        // 分割并处理每个题目ID
        problemIdList.parallelStream().forEach(problemId -> {

            // 检查题目是否已存在
            QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();

            queryWrapper.eq("problem_id",
                    ojName.equals("VJ") ? "VJ(" + problemId + ")" : ojName + "-" + problemId.toUpperCase());

            if (finalGid == null) {
                queryWrapper.isNull("gid");
            } else {
                queryWrapper.eq("gid", finalGid);
            }

            Problem problem = problemEntityService.getOne(queryWrapper);
            if (problem != null) {
                existingProblemIds.add(problemId);
                return;
            }

            // 尝试导入题目
            try {
                ProblemStrategy.RemoteProblemInfo problemInfo = remoteProblemManager.getOtherOJProblemInfo(ojName,
                        problemId, finalUsername);

                if (problemInfo == null
                        || remoteProblemManager.adminAddOtherOJProblem(problemInfo, ojName, finalGid) == null) {
                    failedProblemIds.add(problemId);
                    return;
                }
            } catch (Exception e) {
                log.error("导入题目 [" + ojName + "]" + " [" + problemId + "] 失败，原因: " + e.getMessage(), e);
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
                    "可能是与该OJ链接超时或题号格式错误，或者其他报错！",
                    successCount, failedProblemIds, failedCount, existingProblemIds, existCount);

            // 异步同步系统通知
            adminNoticeManager.syncNoticeToNewRemoteProblemBatchUser(errMsg, userRolesVo.getUid());

            log.info("[{}],[{}],errMsg:[{}],operatorUid:[{}],operatorUsername:[{}]",
                    "Admin_Problem", "Add_Remote_Problem", errMsg, userRolesVo.getUid(), userRolesVo.getUsername());

            throw new StatusFailException(errMsg);
        }

        log.info("[{}],[{}],problemIds:[{}],operatorUid:[{}],operatorUsername:[{}]",
                "Admin_Problem", "Add_Remote_Problem", successProblemIds, userRolesVo.getUid(),
                userRolesVo.getUsername());
    }

    public void changeProblemAuth(ProblemResDTO problem) throws StatusFailException, StatusForbiddenException {
        // 普通管理员只能将题目变成隐藏题目和比赛题目
        boolean root = SecurityUtils.getSubject().hasRole("root");

        if (!root && problem.getAuth() == 1) {
            throw new StatusForbiddenException("修改失败！你无权限公开题目！");
        }

        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");

        if (!isRoot && !userRolesVo.getUsername().equals(problem.getAuthor())) {
            throw new StatusForbiddenException("修改失败！你无权限修改此题目权限！");
        }

        UpdateWrapper<Problem> problemUpdateWrapper = new UpdateWrapper<>();
        problemUpdateWrapper.eq("id", problem.getId())
                .set("auth", problem.getAuth())
                .set("modified_user", userRolesVo.getUsername());

        boolean isOk = problemEntityService.update(problemUpdateWrapper);
        if (!isOk) {
            throw new StatusFailException("修改失败");
        }
        log.info("[{}],[{}],value:[{}],pid:[{}],operatorUid:[{}],operatorUsername:[{}]",
                "Admin_Problem", "Change_Auth", problem.getAuth(), problem.getId(), userRolesVo.getUid(),
                userRolesVo.getUsername());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateRemoteDescription(Long pid) throws StatusFailException, StatusNotFoundException, Exception {
        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        Problem problem = problemEntityService.getById(pid);

        if (problem == null) {
            throw new StatusNotFoundException("该题目不存在");
        }

        List<ProblemDescription> problemDescriptionList = problemEntityService
                .getProblemDescriptionList(problem.getId(), null, null, null);

        // 按照 rank 从大到小排序
        problemDescriptionList.sort((desc1, desc2) -> Integer.compare(desc2.getRank(), desc1.getRank()));

        int rankIndex = problemDescriptionList.isEmpty() ? 1 : problemDescriptionList.get(0).getRank() + 1;

        String problemId = problem.getProblemId().toUpperCase();
        String remoteOj = problemId.startsWith("VJ") ? "VJ" : problemId.split("-")[0];
        String remoteProblemId = problemId.startsWith("VJ")
                ? ReUtil.get("\\(([^)]+)\\)", problemId, 1)
                : problemId.split("-", 2)[1].split("\\(")[0];

        // 批量保存或更新的列表
        List<ProblemDescription> descriptionsToUpdate = new ArrayList<>();

        // 获取远程题面信息
        ProblemStrategy.RemoteProblemInfo otherOJProblemInfo = remoteProblemManager.getOtherOJProblemInfo(remoteOj,
                remoteProblemId, userRolesVo.getUsername());

        if (otherOJProblemInfo == null) {
            throw new StatusFailException("更新题面失败！原因：可能是与该OJ链接超时或题号格式错误！");
        }

        for (ProblemDescription description : otherOJProblemInfo.getProblemDescriptionList()) {
            description.setPid(problem.getId());

            // 如果爬取的题面无作者，则填充当前账号为作者
            if (description.getAuthor() == null) {
                description.setAuthor(userRolesVo.getUsername());
            }

            // 设置描述的 rank
            description.setRank(rankIndex++);

            // 添加到批量更新列表
            descriptionsToUpdate.add(description);

        }

        if (!CollectionUtils.isEmpty(descriptionsToUpdate)) {
            // 批量保存或更新
            boolean isUpdated = problemDescriptionEntityService.saveBatch(descriptionsToUpdate);

            UpdateWrapper<Problem> problemUpdateWrapper = new UpdateWrapper<>();
            problemUpdateWrapper.eq("id", pid).set("modified_user", userRolesVo.getUsername());
            boolean isProblemUpdated = problemEntityService.update(problemUpdateWrapper);

            if (!isUpdated || !isProblemUpdated) {
                throw new StatusFailException("更新题面失败！请重新尝试！");
            }
        }
    }

}