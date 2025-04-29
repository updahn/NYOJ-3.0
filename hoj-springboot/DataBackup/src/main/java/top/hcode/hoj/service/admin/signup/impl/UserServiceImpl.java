package top.hcode.hoj.service.admin.signup.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.common.result.ResultStatus;
import top.hcode.hoj.manager.signup.UserManager;
import top.hcode.hoj.pojo.dto.UserSignDTO;
import top.hcode.hoj.pojo.vo.UserInfoVO;
import top.hcode.hoj.pojo.vo.UserSignVO;
import top.hcode.hoj.service.admin.signup.UserService;

import java.util.List;

import javax.annotation.Resource;

@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserManager userManager;

    @Override
    public CommonResult<IPage<UserSignVO>> getUserSignList(Integer currentPage, Integer limit, String keyword,
            String startYear, String school) {
        try {
            return CommonResult
                    .successResponse(userManager.getUserSignList(currentPage, limit, keyword, startYear, school));
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<UserSignVO> getUserSign(String username) {
        try {
            return CommonResult.successResponse(userManager.getUserSign(username));
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<Void> addUserSign(UserSignDTO userSignDto) {
        try {
            userManager.addUserSign(userSignDto);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<UserInfoVO> updateUserSign(UserSignDTO userSignDto) {
        try {
            return CommonResult.successResponse(userManager.updateUserSign(userSignDto));
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<Void> removeUserSign(String username, Long id) {
        try {
            userManager.removeUserSign(username, id);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<Void> addUserSignBatch(List<UserSignDTO> userSignDtoList) {
        try {
            userManager.addUserSignBatch(userSignDtoList);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

}