package top.hcode.hoj.service.admin.signup;

import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.dto.LoginDTO;
import top.hcode.hoj.pojo.vo.UserInfoVO;

public interface SignupService {

    public CommonResult<UserInfoVO> login(LoginDTO loginDto);

    public CommonResult<Void> logout();

}