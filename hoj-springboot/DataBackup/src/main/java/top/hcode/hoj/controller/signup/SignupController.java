package top.hcode.hoj.controller.signup;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import top.hcode.hoj.common.result.CommonResult;

import top.hcode.hoj.pojo.dto.LoginDTO;
import top.hcode.hoj.pojo.vo.UserInfoVO;
import top.hcode.hoj.service.admin.signup.SignupService;

@RestController
@RequestMapping("/api/signup")
public class SignupController {

    @Autowired
    private SignupService signupService;

    @PostMapping("/login")
    public CommonResult<UserInfoVO> login(@Validated @RequestBody LoginDTO loginDto) {

        return signupService.login(loginDto);
    }

    @GetMapping("/logout")
    @RequiresAuthentication
    public CommonResult<Void> logout() {

        return signupService.logout();
    }

}