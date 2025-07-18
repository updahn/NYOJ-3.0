package top.hcode.hoj.service.oj.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import top.hcode.hoj.common.exception.StatusAccessDeniedException;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.exception.StatusNotFoundException;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.common.result.ResultStatus;
import top.hcode.hoj.manager.oj.ProblemManager;
import top.hcode.hoj.pojo.dto.LastAcceptedCodeVO;
import top.hcode.hoj.pojo.dto.PidListDTO;
import top.hcode.hoj.pojo.vo.ProblemFullScreenListVO;
import top.hcode.hoj.pojo.vo.ProblemInfoVO;
import top.hcode.hoj.pojo.vo.ProblemVO;
import top.hcode.hoj.pojo.vo.RandomProblemVO;
import top.hcode.hoj.pojo.vo.RemotejudgeVO;
import top.hcode.hoj.service.oj.ProblemService;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/11 11:06
 * @Description:
 */
@Service
public class ProblemServiceImpl implements ProblemService {

    @Resource
    private ProblemManager problemManager;

    @Override
    public CommonResult<Page<ProblemVO>> getProblemList(Integer limit, Integer currentPage, String keyword,
            List<Long> tagId, Integer difficulty, Integer type, String oj) {
        return CommonResult
                .successResponse(
                        problemManager.getProblemList(limit, currentPage, keyword, tagId, difficulty, type, oj));
    }

    @Override
    public CommonResult<RandomProblemVO> getRandomProblem(String oj) {
        try {
            return CommonResult.successResponse(problemManager.getRandomProblem(oj));
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<HashMap<Long, Object>> getUserProblemStatus(PidListDTO pidListDto) {
        try {
            return CommonResult.successResponse(problemManager.getUserProblemStatus(pidListDto));
        } catch (StatusNotFoundException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.NOT_FOUND);
        }
    }

    @Override
    public CommonResult<ProblemInfoVO> getProblemInfo(String problemId, Long gid, Long tid, Long peid) {
        try {
            return CommonResult.successResponse(problemManager.getProblemInfo(problemId, gid, tid, peid));
        } catch (StatusNotFoundException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.NOT_FOUND);
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<String> getProblemPdf(Long pid, Long peid, Long cid) {
        try {
            return CommonResult.successResponse(problemManager.getProblemPdf(pid, peid, cid));
        } catch (StatusNotFoundException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.NOT_FOUND);
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        } catch (IOException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<LastAcceptedCodeVO> getUserLastAcceptedCode(Long pid, Long cid) {
        return CommonResult.successResponse(problemManager.getUserLastAcceptedCode(pid, cid));
    }

    @Override
    public CommonResult<List<ProblemFullScreenListVO>> getFullScreenProblemList(Long tid, Long cid) {
        try {
            return CommonResult.successResponse(problemManager.getFullScreenProblemList(tid, cid));
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        } catch (StatusAccessDeniedException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.ACCESS_DENIED);
        }
    }

    @Override
    public CommonResult<List<RemotejudgeVO>> getRemoteJudgeStatusList(String remoteOj) {
        return CommonResult.successResponse(problemManager.getRemoteJudgeStatusList(remoteOj));
    }
}