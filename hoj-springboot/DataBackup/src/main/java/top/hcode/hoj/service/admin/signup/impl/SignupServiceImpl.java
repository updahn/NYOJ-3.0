package top.hcode.hoj.service.admin.signup.impl;

import org.springframework.stereotype.Service;

import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.common.result.ResultStatus;
import top.hcode.hoj.manager.signup.SignupManager;
import top.hcode.hoj.pojo.vo.UserInfoVO;
import top.hcode.hoj.service.admin.signup.SignupService;
import top.hcode.hoj.pojo.dto.LoginDTO;

import javax.annotation.Resource;

@Service
public class SignupServiceImpl implements SignupService {

    @Resource
    private SignupManager signupManager;

    @Override
    public CommonResult<UserInfoVO> login(LoginDTO loginDto) {
        try {
            return CommonResult.successResponse(signupManager.login(loginDto));
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<Void> logout() {
        signupManager.logout();
        return CommonResult.successResponse("退出登录成功！");
    }

}