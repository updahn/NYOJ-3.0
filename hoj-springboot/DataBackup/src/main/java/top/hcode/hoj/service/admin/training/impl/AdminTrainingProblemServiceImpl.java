package top.hcode.hoj.service.admin.training.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.common.result.ResultStatus;
import top.hcode.hoj.manager.admin.training.AdminTrainingProblemManager;
import top.hcode.hoj.pojo.dto.TrainingProblemDTO;
import top.hcode.hoj.pojo.entity.training.TrainingProblem;
import top.hcode.hoj.service.admin.training.AdminTrainingProblemService;

import java.util.HashMap;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/9 20:53
 * @Description:
 */
@Service
public class AdminTrainingProblemServiceImpl implements AdminTrainingProblemService {

    @Autowired
    private AdminTrainingProblemManager adminTrainingProblemManager;

    @Override
    public CommonResult<HashMap<String, Object>> getProblemList(Integer limit, Integer currentPage, String keyword,
            Boolean queryExisted, Long tid) {
        try {
            HashMap<String, Object> problemMap = adminTrainingProblemManager.getProblemList(limit, currentPage, keyword,
                    queryExisted, tid);
            return CommonResult.successResponse(problemMap);
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<Void> updateProblem(TrainingProblem trainingProblem) {
        try {
            adminTrainingProblemManager.updateProblem(trainingProblem);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<Void> deleteProblem(Long pid, Long tid) {
        try {
            adminTrainingProblemManager.deleteProblem(pid, tid);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<Void> addProblemFromPublic(TrainingProblemDTO trainingProblemDto) {
        try {
            adminTrainingProblemManager.addProblemFromPublic(trainingProblemDto);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<Void> changeProblemDescription(TrainingProblemDTO trainingProblemDto) {
        try {
            adminTrainingProblemManager.changeProblemDescription(trainingProblemDto);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<Void> importTrainingRemoteOJProblem(String name, String problemId, Long tid, Long gid) {
        try {
            adminTrainingProblemManager.importTrainingRemoteOJProblem(name, problemId, tid, gid);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }
}