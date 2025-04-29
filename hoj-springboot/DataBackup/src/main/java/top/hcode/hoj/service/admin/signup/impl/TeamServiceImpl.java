package top.hcode.hoj.service.admin.signup.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.exception.StatusNotFoundException;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.common.result.ResultStatus;
import top.hcode.hoj.manager.signup.TeamManager;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.vo.TeamSignVO;
import top.hcode.hoj.service.admin.signup.TeamService;

import java.util.List;

import javax.annotation.Resource;

@Service
public class TeamServiceImpl implements TeamService {

    @Resource
    private TeamManager teamManager;

    @Override
    public CommonResult<IPage<TeamSignVO>> getTeamSignList(Integer currentPage, Integer limit, Long cid,
            Integer type, Integer status, String keyword, Long signCid) {
        try {
            return CommonResult
                    .successResponse(
                            teamManager.getTeamSignList(currentPage, limit, cid, type, status, keyword, signCid));
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<IPage<Contest>> getContestList(Integer limit, Integer currentPage, String status,
            String keyword) {
        IPage<Contest> contestList = teamManager.getContestList(limit, currentPage, status, keyword);
        return CommonResult.successResponse(contestList);
    }

    @Override
    public CommonResult<TeamSignVO> getTeamSign(Long id) {
        try {
            return CommonResult.successResponse(teamManager.getTeamSign(id));
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<Void> addTeamSign(TeamSignVO teamSignVo) {
        try {
            teamManager.addTeamSign(teamSignVo);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<Void> updateTeamSign(TeamSignVO teamSign) {
        try {
            teamManager.updateTeamSign(teamSign);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<Void> removeTeamSign(Long id) {
        try {
            teamManager.removeTeamSign(id);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<Void> updateTeamSignStatus(List<Long> ids, Long cid, Integer status, String msg) {
        try {
            teamManager.updateTeamSignStatus(ids, cid, status, msg);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        } catch (StatusNotFoundException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.NOT_FOUND);
        }
    }

    @Override
    public CommonResult<Void> addTeamSignBatch(List<Long> ids, Long cid, Integer type) {
        try {
            teamManager.addTeamSignBatch(ids, cid, type);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        } catch (StatusNotFoundException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.NOT_FOUND);
        }
    }
}