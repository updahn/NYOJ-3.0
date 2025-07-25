package top.hcode.hoj.controller.oj;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.hcode.hoj.annotation.AnonApi;
import top.hcode.hoj.pojo.dto.*;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.vo.*;
import top.hcode.hoj.service.oj.AccountService;

/**
 * @Author: Himit_ZH
 * @Date: 2020/10/23 12:00
 * @Description: 主要负责处理账号的相关操作
 */
@RestController
@RequestMapping("/api")
public class AccountController {

    @Autowired
    private AccountService accountService;

    /**
     * @MethodName checkUsernameOrEmail
     * @Description 检验用户名和邮箱是否存在
     * @Return
     * @Since 2020/11/5
     */
    @RequestMapping(value = "/check-username-or-email", method = RequestMethod.POST)
    @AnonApi
    public CommonResult<CheckUsernameOrEmailVO> checkUsernameOrEmail(
            @RequestBody CheckUsernameOrEmailDTO checkUsernameOrEmailDto) {
        return accountService.checkUsernameOrEmail(checkUsernameOrEmailDto);
    }

    /**
     * @param uid
     * @MethodName getUserHomeInfo
     * @Description 前端userHome用户个人主页的数据请求，主要是返回解决题目数，AC的题目列表，提交总数，AC总数
     * @Return CommonResult
     * @Since 2021/01/07
     */
    @GetMapping("/get-user-home-info")
    public CommonResult<UserHomeVO> getUserHomeInfo(@RequestParam(value = "uid", required = false) String uid,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "gid", required = false) Long gid) {
        return accountService.getUserHomeInfo(uid, username, gid);
    }

    /**
     * @param uid
     * @param username
     * @return
     * @Description 获取用户最近一年的提交热力图数据
     */
    @GetMapping("/get-user-calendar-heatmap")
    @AnonApi
    public CommonResult<UserCalendarHeatmapVO> getUserCalendarHeatmap(
            @RequestParam(value = "uid", required = false) String uid,
            @RequestParam(value = "username", required = false) String username) {
        return accountService.getUserCalendarHeatmap(uid, username);
    }

    /**
     * @MethodName changeUsername
     * @Params * @param null
     * @Description 修改账户名的操作，一个月只能更改一次
     * @Return
     */

    @PostMapping("/change-username")
    @RequiresAuthentication
    public CommonResult<ChangeAccountVO> changeUsername(@RequestBody ChangeUsernameDTO changeUsernameDto) {
        return accountService.changeUsername(changeUsernameDto);
    }

    /**
     * @MethodName changePassword
     * @Params * @param null
     * @Description 修改密码的操作，连续半小时内修改密码错误5次，则需要半个小时后才可以再次尝试修改密码
     * @Return
     * @Since 2021/1/8
     */

    @PostMapping("/change-password")
    @RequiresAuthentication
    public CommonResult<ChangeAccountVO> changePassword(@RequestBody ChangePasswordDTO changePasswordDto) {
        return accountService.changePassword(changePasswordDto);
    }

    /**
     * 获取修改邮箱的验证码
     *
     * @param email
     * @return
     */
    @GetMapping("/get-change-email-code")
    @RequiresAuthentication
    public CommonResult<Void> getChangeEmailCode(@RequestParam("email") String email) {
        return accountService.getChangeEmailCode(email);
    }

    /**
     * @MethodName changeEmail
     * @Params * @param null
     * @Description 修改邮箱的操作，连续半小时内密码错误5次，则需要半个小时后才可以再次尝试修改
     * @Return
     * @Since 2021/1/9
     */
    @PostMapping("/change-email")
    @RequiresAuthentication
    public CommonResult<ChangeAccountVO> changeEmail(@RequestBody ChangeEmailDTO changeEmailDto) {
        return accountService.changeEmail(changeEmailDto);
    }

    @PostMapping("/change-userInfo")
    @RequiresAuthentication
    public CommonResult<UserInfoVO> changeUserInfo(@RequestBody UserInfoVO userInfoVo) {
        return accountService.changeUserInfo(userInfoVo);
    }

    @PostMapping("/change-userPreferences")
    @RequiresAuthentication
    public CommonResult<UserInfoVO> changeUserPreferences(@RequestBody UserPreferencesVO UserPreferencesVo) {
        return accountService.changeUserPreferences(UserPreferencesVo);
    }

    @PostMapping("/change-userRace")
    @RequiresAuthentication
    public CommonResult<UserInfoVO> changeUserRace(@RequestBody UserSignVO UserSignVo) {
        return accountService.changeUserRace(UserSignVo);
    }

    @PostMapping("/change-userMultiOj")
    @RequiresAuthentication
    public CommonResult<UserInfoVO> changeUserMultiOj(@RequestBody UserMultiOjVO UserMultiOjVo) {
        return accountService.changeUserMultiOj(UserMultiOjVo);
    }

    @GetMapping("/get-user-auth-info")
    @RequiresAuthentication
    public CommonResult<UserAuthInfoVO> getUserAuthInfo() {
        return accountService.getUserAuthInfo();
    }

}