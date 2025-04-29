package top.hcode.hoj.manager.signup;

import cn.hutool.core.bean.BeanUtil;

import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;

import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.config.NacosSwitchConfig;
import top.hcode.hoj.dao.user.SessionEntityService;
import top.hcode.hoj.dao.user.UserInfoEntityService;
import top.hcode.hoj.dao.user.UserRoleEntityService;
import top.hcode.hoj.dao.user.UserSignEntityService;
import top.hcode.hoj.manager.oj.AccountManager;
import top.hcode.hoj.pojo.dto.CheckUsernameOrEmailDTO;
import top.hcode.hoj.pojo.dto.LoginDTO;
import top.hcode.hoj.pojo.entity.user.Role;
import top.hcode.hoj.pojo.entity.user.Session;
import top.hcode.hoj.pojo.entity.user.UserInfo;
import top.hcode.hoj.pojo.entity.user.UserSign;
import top.hcode.hoj.pojo.vo.CheckUsernameOrEmailVO;
import top.hcode.hoj.pojo.vo.UserInfoVO;
import top.hcode.hoj.pojo.vo.UserRolesVO;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.utils.IpUtils;
import top.hcode.hoj.utils.JwtUtils;
import top.hcode.hoj.utils.Md5Utils;
import top.hcode.hoj.utils.RedisUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class SignupManager {

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private NacosSwitchConfig nacosSwitchConfig;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private AccountManager accountManager;

    @Autowired
    private SessionEntityService sessionEntityService;

    @Autowired
    private UserRoleEntityService userRoleEntityService;

    @Autowired
    private UserInfoEntityService userInfoEntityService;

    @Autowired
    private UserSignEntityService userSignEntityService;

    public UserInfoVO login(LoginDTO loginDto) throws StatusFailException, StatusForbiddenException {
        // 去掉账号密码首尾的空格
        loginDto.setPassword(loginDto.getPassword().trim());
        loginDto.setUsername(loginDto.getUsername().trim());

        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        HttpServletRequest request = servletRequestAttributes.getRequest();
        HttpServletResponse response = servletRequestAttributes.getResponse();

        String userIpAddr = IpUtils.getUserIpAddr(request);
        String key = Constants.Account.TRY_LOGIN_NUM.getCode() + loginDto.getUsername() + "_" + userIpAddr;
        Integer tryLoginCount = (Integer) redisUtils.get(key);

        if (tryLoginCount != null && tryLoginCount >= 20) {
            throw new StatusFailException("对不起！登录失败次数过多！您的账号有风险，半个小时内暂时无法登录！");
        }

        UserRolesVO userRolesVo = userRoleEntityService.getUserRoles(null, loginDto.getUsername());

        if (userRolesVo == null) {
            throw new StatusFailException("用户名或密码错误");
        }

        if (!Md5Utils.verifySaltPassword(loginDto.getPassword(), userRolesVo.getPassword())) {
            if (tryLoginCount == null) {
                redisUtils.set(key, 1, 60 * 30); // 三十分钟不尝试，该限制会自动清空消失
            } else {
                redisUtils.set(key, tryLoginCount + 1, 60 * 30);
            }
            throw new StatusFailException("用户名或密码错误");
        }

        if (userRolesVo.getStatus() != 0) {
            throw new StatusFailException("该账户已被封禁，请联系管理员进行处理！");
        }

        // 认证成功，清除锁定限制
        if (tryLoginCount != null) {
            redisUtils.del(key);
        }

        // 如果原有用户信息中不包含邮箱和学校和真实姓名，并且不传入邮件和学校和真实姓名
        if ((userRolesVo.getEmail() == null || userRolesVo.getSchool() == null || userRolesVo.getRealname() == null)
                && (loginDto.getEmail() == null || loginDto.getCode() == null || loginDto.getSchool() == null
                        || loginDto.getRealname() == null)) {
            throw new StatusForbiddenException("用户需要补全信息才能登录（学校，邮箱，真实姓名）！");
        }

        UserInfoVO userInfoVo = new UserInfoVO();
        BeanUtil.copyProperties(userRolesVo, userInfoVo, "roles");

        // 检查用户信息是否包含邮箱和学校
        if (loginDto.getSchool() != null && loginDto.getEmail() != null && loginDto.getCode() != null) {
            String codeKey = Constants.Email.REGISTER_KEY_PREFIX.getValue() + loginDto.getEmail();
            if (!redisUtils.hasKey(codeKey)) {
                throw new StatusFailException("验证码不存在或已过期");
            }

            if (!redisUtils.get(codeKey).equals(loginDto.getCode())) { // 验证码判断
                throw new StatusFailException("验证码不正确");
            }

            // 检查用户是否存在
            CheckUsernameOrEmailDTO CheckUsernameOrEmailDto = new CheckUsernameOrEmailDTO();
            CheckUsernameOrEmailDto.setEmail(loginDto.getEmail());
            CheckUsernameOrEmailVO CheckUsernameOrEmailvo = accountManager
                    .checkUsernameOrEmail(CheckUsernameOrEmailDto);

            if (CheckUsernameOrEmailvo.getEmail()) {
                throw new StatusFailException("邮箱已注册！");
            }

            // 设置用户的邮箱和学校
            UpdateWrapper<UserInfo> userInfoUpdateWrapper = new UpdateWrapper<>();
            userInfoUpdateWrapper.set("email", loginDto.getEmail()).eq("uuid", userRolesVo.getUid());
            boolean isOk = userInfoEntityService.update(userInfoUpdateWrapper);

            UpdateWrapper<UserSign> userSignUupdateWrapper = new UpdateWrapper<>();
            userSignUupdateWrapper
                    .set("school", loginDto.getSchool())
                    .set("realname", loginDto.getRealname())
                    .eq("uid", userRolesVo.getUid());
            boolean isOk2 = userSignEntityService.update(userSignUupdateWrapper);

            if (!isOk || !isOk2) {
                throw new StatusFailException("邮箱或者学校设置失败！");
            }

            userInfoVo.setEmail(loginDto.getEmail());
            userInfoVo.setSchool(loginDto.getSchool());
        }

        // 查询用户角色
        List<String> rolesList = new LinkedList<>();
        userRolesVo.getRoles().stream().forEach(role -> rolesList.add(role.getRole()));

        String jwt = jwtUtils.generateToken(userRolesVo.getUid());
        response.setHeader("Authorization", jwt); // 放到信息头部
        response.setHeader("Access-Control-Expose-Headers", "Authorization");

        // 会话记录
        sessionEntityService.save(new Session()
                .setUid(userRolesVo.getUid())
                .setIp(IpUtils.getUserIpAddr(request))
                .setUserAgent(request.getHeader("User-Agent")));

        // 异步检查是否异地登录
        sessionEntityService.checkRemoteLogin(userRolesVo.getUid());

        userInfoVo.setRoleList(userRolesVo.getRoles()
                .stream()
                .map(Role::getRole)
                .collect(Collectors.toList()));
        return userInfoVo;
    }

    public void logout() {
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        jwtUtils.cleanToken(userRolesVo.getUid());
        SecurityUtils.getSubject().logout();
    }

}