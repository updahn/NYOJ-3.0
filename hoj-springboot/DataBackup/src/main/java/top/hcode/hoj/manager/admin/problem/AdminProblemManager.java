package top.hcode.hoj.manager.admin.problem;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.crawler.problem.ProblemStrategy;
import top.hcode.hoj.dao.judge.JudgeEntityService;
import top.hcode.hoj.dao.problem.ProblemCaseEntityService;
import top.hcode.hoj.dao.problem.ProblemEntityService;
import top.hcode.hoj.judge.Dispatcher;
import top.hcode.hoj.pojo.dto.CompileDTO;
import top.hcode.hoj.pojo.dto.ProblemDTO;
import top.hcode.hoj.pojo.dto.ProblemResDTO;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.problem.ProblemCase;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.validator.ProblemValidator;

import javax.annotation.Resource;
import java.io.File;
import java.util.List;

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

    @Transactional(rollbackFor = Exception.class)
    public void importRemoteOJProblem(String name, String problemId, Long gid) throws StatusFailException {
        QueryWrapper<Problem> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("problem_id", name.toUpperCase() + "-" + problemId);
        if (gid == null) {
            queryWrapper.isNull("gid");
        } else {
            queryWrapper.eq("gid", gid);
        }

        Problem problem = problemEntityService.getOne(queryWrapper);
        if (problem != null) {
            throw new StatusFailException("该题目已添加，请勿重复添加！");
        }

        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        try {
            ProblemStrategy.RemoteProblemInfo otherOJProblemInfo = remoteProblemManager
                    .getOtherOJProblemInfo(name.toUpperCase(), problemId, userRolesVo.getUsername());
            if (otherOJProblemInfo != null) {
                Problem importProblem = remoteProblemManager.adminAddOtherOJProblem(otherOJProblemInfo, name, gid);
                if (importProblem == null) {
                    throw new StatusFailException("导入新题目失败！请重新尝试！");
                }
            } else {
                throw new StatusFailException("导入新题目失败！原因：可能是与该OJ链接超时或题号格式错误！");
            }
        } catch (Exception e) {
            throw new StatusFailException(e.getMessage());
        }
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

}