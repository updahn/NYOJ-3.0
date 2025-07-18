package top.hcode.hoj.service.admin.problem.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.exception.StatusNotFoundException;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.common.result.ResultStatus;
import top.hcode.hoj.manager.admin.problem.AdminProblemManager;
import top.hcode.hoj.pojo.dto.ProblemDTO;
import top.hcode.hoj.pojo.dto.ProblemResDTO;
import top.hcode.hoj.pojo.dto.CompileDTO;
import top.hcode.hoj.pojo.entity.problem.ProblemCase;
import top.hcode.hoj.service.admin.problem.AdminProblemService;

import java.util.List;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/9 16:33
 * @Description:
 */

@Service
public class AdminProblemServiceImpl implements AdminProblemService {

    @Autowired
    private AdminProblemManager adminProblemManager;

    @Override
    public CommonResult<IPage<ProblemResDTO>> getProblemList(Integer limit, Integer currentPage, String keyword,
            Integer auth, String oj, Integer difficulty, Integer type) {
        IPage<ProblemResDTO> problemList = adminProblemManager.getProblemList(limit, currentPage, keyword, auth, oj,
                difficulty, type);
        return CommonResult.successResponse(problemList);
    }

    @Override
    public CommonResult<ProblemResDTO> getProblem(Long pid, Long peid) {
        try {
            ProblemResDTO problem = adminProblemManager.getProblem(pid, peid);
            return CommonResult.successResponse(problem);
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<Void> deleteProblem(Long pid) {
        try {
            adminProblemManager.deleteProblem(pid);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<Void> addProblem(ProblemDTO problemDto) {
        try {
            adminProblemManager.addProblem(problemDto);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<Void> updateProblem(ProblemDTO problemDto) {
        try {
            adminProblemManager.updateProblem(problemDto);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<List<ProblemCase>> getProblemCases(Long pid, Long peid, Boolean isUpload) {
        try {
            List<ProblemCase> problemCaseList = adminProblemManager.getProblemCases(pid, peid, isUpload);
            return CommonResult.successResponse(problemCaseList);
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult compileSpj(CompileDTO compileDTO) {
        return adminProblemManager.compileSpj(compileDTO);
    }

    @Override
    public CommonResult compileInteractive(CompileDTO compileDTO) {
        return adminProblemManager.compileInteractive(compileDTO);
    }

    @Override
    public CommonResult<Void> importRemoteOJProblem(String name, String problemId, Long gid) {
        try {
            adminProblemManager.importRemoteOJProblem(name, problemId, gid);
            return CommonResult.successResponse("导入新题目成功");
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<Void> changeProblemAuth(ProblemResDTO problem) {
        try {
            adminProblemManager.changeProblemAuth(problem);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<Void> updateRemoteDescription(Long pid) {
        try {
            adminProblemManager.updateRemoteDescription(pid);
            return CommonResult.successResponse("更新题目题面成功");
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusNotFoundException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.NOT_FOUND);
        } catch (Exception e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FAIL);
        }
    }
}