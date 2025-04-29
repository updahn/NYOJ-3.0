package top.hcode.hoj.manager.signup;

import java.security.SecureRandom;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.result.Paginate;
import top.hcode.hoj.dao.school.SchoolEntityService;
import top.hcode.hoj.dao.school.SchoolUserEntityService;
import top.hcode.hoj.dao.user.UserInfoEntityService;
import top.hcode.hoj.dao.user.UserPreferencesEntityService;
import top.hcode.hoj.dao.user.UserRecordEntityService;
import top.hcode.hoj.dao.user.UserRoleEntityService;
import top.hcode.hoj.dao.user.UserSignEntityService;
import top.hcode.hoj.manager.oj.AccountManager;
import top.hcode.hoj.pojo.bo.Pair_;
import top.hcode.hoj.pojo.dto.CheckUsernameOrEmailDTO;
import top.hcode.hoj.pojo.dto.InventDTO;
import top.hcode.hoj.pojo.dto.UserSignDTO;
import top.hcode.hoj.pojo.entity.school.School;
import top.hcode.hoj.pojo.entity.school.SchoolUser;
import top.hcode.hoj.pojo.entity.user.Role;
import top.hcode.hoj.pojo.entity.user.UserInfo;
import top.hcode.hoj.pojo.entity.user.UserPreferences;
import top.hcode.hoj.pojo.entity.user.UserRecord;
import top.hcode.hoj.pojo.entity.user.UserRole;
import top.hcode.hoj.pojo.entity.user.UserSign;
import top.hcode.hoj.pojo.vo.CheckUsernameOrEmailVO;
import top.hcode.hoj.pojo.vo.CoachInfoVO;
import top.hcode.hoj.pojo.vo.UserInfoVO;
import top.hcode.hoj.pojo.vo.UserInventVO;
import top.hcode.hoj.pojo.vo.UserRolesVO;
import top.hcode.hoj.pojo.vo.UserSignVO;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.utils.Md5Utils;
import top.hcode.hoj.validator.CommonValidator;

@Component
public class UserManager {

    @Autowired
    private AccountManager accountManager;

    @Autowired
    private InventManager inventManager;

    @Autowired
    private CommonValidator commonValidator;

    @Autowired
    private SchoolEntityService schoolEntityService;

    @Autowired
    private SchoolUserEntityService schoolUserEntityService;

    @Autowired
    private UserRoleEntityService userRoleEntityService;

    @Autowired
    private UserSignEntityService userSignEntityService;

    @Autowired
    private UserInfoEntityService userInfoEntityService;

    @Autowired
    private UserRecordEntityService userRecordEntityService;

    @Autowired
    private UserPreferencesEntityService userPreferencesEntityService;

    public IPage<UserSignVO> getUserSignList(Integer currentPage, Integer limit, String keyword, String startYear,
            String school) throws StatusFailException {

        // 页数若为空，设置默认值
        if (currentPage == null || currentPage < 1)
            currentPage = 1;

        List<SchoolUser> schoolUserList = getSchoolUserListByAdmin(school);

        List<UserSignVO> userSignVoList = new ArrayList<>();
        LinkedHashMap<String, UserSignVO> uidMap = new LinkedHashMap<>(); // 顺序字典
        Map<String, Boolean> rootMap = new HashMap<>(); // 教练/队长是否为管理员

        Optional.ofNullable(schoolUserList).orElse(Collections.emptyList())
                .forEach(schoolUser -> {

                    // 根据 schoolUser 构造 UserSignVO（包含用户角色信息等）
                    UserSignVO userSignVo = new UserSignVO();

                    UserRolesVO userRolesVo = userRoleEntityService.getUserRoles(schoolUser.getUid(), null);

                    BeanUtil.copyProperties(userRolesVo, userSignVo);
                    userSignVo.setId(schoolUser.getId());

                    // 过滤条件：按 startYear 与 keyword 过滤
                    boolean match = (startYear == null ||
                            (!StringUtils.isEmpty(userSignVo.getStSchool()) &&
                                    String.valueOf(userSignVo.getStSchool()
                                            .toInstant()
                                            .atZone(ZoneId.systemDefault())
                                            .getYear())
                                            .equals(startYear)))
                            && (keyword == null || keyword.equals(userSignVo.getRealname()));

                    // 学校和邮箱不能为空
                    if (StringUtils.isEmpty(userRolesVo.getSchool()) || StringUtils.isEmpty(userRolesVo.getEmail())) {
                        return;
                    }

                    if (match) {
                        String uid = schoolUser.getUid();
                        String coachUid = schoolUser.getCoachUid();

                        Boolean isRoot = false;

                        if (rootMap.containsKey(coachUid)) {
                            isRoot = rootMap.get(coachUid);
                        } else {
                            // 检查用户是否为管理员
                            List<String> coach_roles = userRoleEntityService.getRolesByUid(coachUid).stream()
                                    .map(Role::getRole).collect(Collectors.toList());

                            if (coach_roles.contains("root") || coach_roles.contains("admin")
                                    || coach_roles.contains("coach_admin")) {
                                isRoot = true;
                            }
                            rootMap.put(coachUid, isRoot);
                        }

                        // 构造 coach 信息
                        CoachInfoVO coachInfo = new CoachInfoVO(
                                userRoleEntityService.getRealNameByUid(schoolUser.getCoachUid()),
                                userRoleEntityService.getUsernameByUid(schoolUser.getCoachUid()),
                                schoolUser.getCoachUid(),
                                isRoot);

                        // 如果该 uid 已存在，则追加 coach 信息；否则，新建 coachInfoVoList 并放入 Map
                        if (uidMap.containsKey(uid)) {
                            uidMap.get(uid).getCoachInfoVoList().add(coachInfo);
                        } else {
                            List<CoachInfoVO> coachInfos = new ArrayList<>();
                            coachInfos.add(coachInfo);
                            userSignVo.setCoachInfoVoList(coachInfos);
                            uidMap.put(uid, userSignVo);
                        }
                    }
                });

        userSignVoList.addAll(uidMap.values());

        return Paginate.paginateListToIPage(userSignVoList, currentPage, limit);
    }

    public UserSignVO getUserSign(String username) throws StatusFailException, StatusForbiddenException {
        // 查询对象
        UserRolesVO toUserRolesVo = userRoleEntityService.getUserRoles(null, username);
        if (toUserRolesVo == null) {
            throw new StatusFailException("修改的用户不存在！");
        }

        List<SchoolUser> schoolUserList = getSchoolUserListByAdmin(null);

        // 查询用户是否在用户池中
        if (CollectionUtils.isEmpty(schoolUserList)
                || !schoolUserList.stream().map(SchoolUser::getUid).collect(Collectors.toList())
                        .contains(toUserRolesVo.getUid())) {
            throw new StatusForbiddenException("无权限查询用户, 或者用户不存在！");
        }

        UserSignVO userSignVo = new UserSignVO();

        BeanUtil.copyProperties(toUserRolesVo, userSignVo);

        return userSignVo;
    }

    public void addUserSign(UserSignDTO userSignDto) throws StatusFailException, StatusForbiddenException {
        checkUserSign(userSignDto);

        // 默认设置可以加入
        userSignDto.setStatus(1);

        // 获取当前登录的用户
        AccountProfile userRolesVO = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        UserRolesVO userRolesVo = userRoleEntityService.getUserRoles(null, userRolesVO.getUsername());

        // 查询对象
        UserRolesVO toUserRolesVo = userSignDto.getUsername() != null
                ? userRoleEntityService.getUserRoles(null, userSignDto.getUsername())
                : null;

        if (toUserRolesVo == null) {
            toUserRolesVo = new UserRolesVO();

            if (userSignDto.getRealname() == null || userSignDto.getEmail() == null || userSignDto.getSchool() == null
                    || userSignDto.getNumber() == null) {
                throw new StatusFailException("真实姓名，邮箱，学校，学号不能为空！");
            }

            // 如果是不是超管，普通管理员，教练管理员，不能直接添加新用户
            if (!SecurityUtils.getSubject().hasRole("root") &&
                    !SecurityUtils.getSubject().hasRole("admin") &&
                    !SecurityUtils.getSubject().hasRole("coach_admin")) {
                throw new StatusFailException("您没有权限添加未知用户到学生池！");
            }

            List<SchoolUser> schoolUserList = getSchoolUserListByAdmin(null);

            // 判断真实姓名和学号是否重复，已经在学生池中
            QueryWrapper<UserSign> userSignQueryWrapper = new QueryWrapper<>();
            userSignQueryWrapper.eq("realname", userSignDto.getRealname());
            userSignQueryWrapper.eq("number", userSignDto.getNumber());
            UserSign checkUserSign = userSignEntityService.getOne(userSignQueryWrapper, false);

            // 查询用户是否在用户池中
            if (checkUserSign != null && !CollectionUtils.isEmpty(schoolUserList)
                    && schoolUserList.stream().map(SchoolUser::getUid).collect(Collectors.toList())
                            .contains(checkUserSign.getUid())) {
                throw new StatusFailException("该用户已经添加！");
            }

            // 创建用户
            String uuid = IdUtil.simpleUUID();

            String DIGITS = "0123456789";

            // 查询对象用户对应的学校
            School school = schoolEntityService.getOne(new QueryWrapper<School>().eq("name", userSignDto.getSchool()));

            // 学校id + 学生学号 + 随机数字
            String username = school.getId()
                    + userSignDto.getNumber()
                    + DIGITS.charAt(new SecureRandom().nextInt(DIGITS.length()));

            // 检查用户是否存在
            CheckUsernameOrEmailDTO CheckUsernameOrEmailDto = new CheckUsernameOrEmailDTO();
            CheckUsernameOrEmailDto.setEmail(userSignDto.getEmail());
            CheckUsernameOrEmailVO CheckUsernameOrEmailvo = accountManager
                    .checkUsernameOrEmail(CheckUsernameOrEmailDto);
            if (CheckUsernameOrEmailvo.getEmail()) {
                throw new StatusFailException("邮箱已注册，请在本校学生名单中查询是否有对应的账号！");
            }

            UserInfo userInfo = new UserInfo()
                    .setUuid(uuid)
                    .setUsername(username)
                    .setPassword(Md5Utils.generateSaltMD5Password(username))
                    .setEmail(userSignDto.getEmail())
                    .setGender(userSignDto.getGender());
            UserRole userRole = new UserRole().setRoleId(1002L).setUid(uuid);
            UserRecord userRecord = new UserRecord().setUid(uuid);
            UserPreferences userPreferences = new UserPreferences().setUid(uuid);
            UserSign userSign = new UserSign().setUid(uuid);
            BeanUtil.copyProperties(userSignDto, userSign, "uid");

            boolean result1 = userInfoEntityService.save(userInfo);
            boolean result2 = userRoleEntityService.save(userRole);
            boolean result3 = userRecordEntityService.save(userRecord);
            boolean result4 = userPreferencesEntityService.save(userPreferences);
            boolean result5 = userSignEntityService.save(userSign);
            boolean result6 = schoolUserEntityService
                    .save(new SchoolUser().setSid(school.getId()).setUid(uuid).setCoachUid(userRolesVo.getUid())); // 将用户添加到学校对应的学生池

            if (!result1 || !result2 || !result3 || !result4 || !result5 || !result6) {
                throw new StatusFailException("添加用户失败！");
            }

            toUserRolesVo.setSchool(school.getName());
            toUserRolesVo.setUid(uuid);
        }
        // 用户存在
        else {
            // 检查发送邀请者的信息是否补全
            if (toUserRolesVo.getSchool() == null || toUserRolesVo.getEmail() == null
                    || toUserRolesVo.getRealname() == null) {
                throw new StatusFailException("该用户信息未补全（学校，邮箱，真实姓名）！");
            }

            Boolean isRoot = SecurityUtils.getSubject().hasRole("root") || SecurityUtils.getSubject().hasRole("admin")
                    || (SecurityUtils.getSubject().hasRole("coach_admin")
                            && toUserRolesVo.getSchool().equals(userRolesVo.getSchool()));

            // 如果是不是超管，普通管理员，教练管理员，可以直接添加学生到学生池
            if (!isRoot) {

                // 发送邀请
                UserInventVO userinventVo = inventManager.addInvent(new InventDTO().setContent(userSignDto.getContent())
                        .setUsername(userRolesVo.getUsername()).setToUsername(toUserRolesVo.getUsername()));

                // 如果邀请没有同意，则返回；否则添加到学生池
                if (userinventVo.getStatus() != 1) {
                    userSignDto.setStatus(-1);
                }
            }

            // 查询对象用户对应的学校
            School school = schoolEntityService
                    .getOne(new QueryWrapper<School>().eq("name", toUserRolesVo.getSchool()));

            List<SchoolUser> schoolUserList = schoolUserEntityService
                    .list(new QueryWrapper<SchoolUser>().eq("sid", school.getId()).eq("uid", toUserRolesVo.getUid()));

            // 根据是否为管理员来判断是否允许添加
            Boolean Adding = schoolUserList.stream().noneMatch(schoolUser -> {
                if (!isRoot) {
                    List<String> coachRoles = userRoleEntityService.getRolesByUid(schoolUser.getCoachUid())
                            .stream().map(Role::getRole).collect(Collectors.toList());

                    // 如果 coach 的角色中不包含管理员角色，则表示已加入其他队伍
                    return !coachRoles.contains("root") && !coachRoles.contains("admin")
                            && !coachRoles.contains("coach_admin");
                } else {
                    // 如果是 root 用户，判断是否存在 coachUid 等于当前用户 uid 的情况
                    return schoolUser.getCoachUid().equals(userRolesVo.getUid());
                }
            });

            if (!Adding) {
                throw new StatusFailException("该用户已经添加到学生池！");
            }
        }

        // 查询对象用户对应的学校
        School school = schoolEntityService
                .getOne(new QueryWrapper<School>().eq("name", toUserRolesVo.getSchool()));

        boolean result = schoolUserEntityService.save(new SchoolUser()
                .setSid(school.getId()).setUid(toUserRolesVo.getUid()).setCoachUid(userRolesVo.getUid())
                .setStatus(userSignDto.getStatus() == -1 ? 1 : 0));

        if (!result) {
            throw new StatusFailException("添加用户到学生池失败！");
        }

    }

    public UserInfoVO updateUserSign(UserSignDTO userSignDto) throws StatusFailException, StatusForbiddenException {
        checkUserSign(userSignDto);

        // 查询对象
        UserRolesVO toUserRolesVo = userRoleEntityService.getUserRoles(userSignDto.getUid(), null);
        if (toUserRolesVo == null) {
            throw new StatusFailException("修改的用户不存在！");
        }

        UserSign userSign = userSignEntityService.getOne(new QueryWrapper<UserSign>().eq("uid", userSignDto.getUid()));

        // 本校教练管理员不允许修改学校
        if (SecurityUtils.getSubject().hasRole("coach_admin")
                && !userSignDto.getSchool().equals(userSign.getSchool())) {
            throw new StatusForbiddenException("对不起，教练不允许修改学校！");
        }

        // 如果是不是超管，普通管理员，教练管理员，不能直接修改不是本人的邮件
        if (!SecurityUtils.getSubject().hasRole("root") &&
                !SecurityUtils.getSubject().hasRole("admin") &&
                !SecurityUtils.getSubject().hasRole("coach_admin") &&
                (!userSignDto.getEmail().equals(userSignDto.getEmail())
                        && !userSignDto.getUid().equals(userSignDto.getUid()))) {
            throw new StatusForbiddenException("对不起，无权限直接修改邮箱！");
        }

        List<SchoolUser> schoolUserList = getSchoolUserListByAdmin(null);

        // 查询用户是否在用户池中
        if (CollectionUtils.isEmpty(schoolUserList)
                || !schoolUserList.stream().map(SchoolUser::getUid).collect(Collectors.toList())
                        .contains(toUserRolesVo.getUid())) {
            throw new StatusForbiddenException("无权限查询用户, 或者用户不存在！");
        }

        // 检查邮箱是否注册
        if (!StringUtils.isEmpty(userSignDto.getEmail())) {
            CheckUsernameOrEmailDTO CheckUsernameOrEmailDto = new CheckUsernameOrEmailDTO();
            CheckUsernameOrEmailDto.setEmail(userSignDto.getEmail());
            CheckUsernameOrEmailVO CheckUsernameOrEmailvo = accountManager
                    .checkUsernameOrEmail(CheckUsernameOrEmailDto);
            if (toUserRolesVo.getEmail() == null && CheckUsernameOrEmailvo.getEmail()) {
                throw new StatusFailException("邮箱已注册，请在本校学生名单中查询是否有对应的账号！");
            }
        }

        boolean isOk1 = userInfoEntityService.update(new UpdateWrapper<UserInfo>().eq("uuid", userSignDto.getUid())
                .set(!StringUtils.isEmpty(userSignDto.getGender()), "gender", userSignDto.getGender())
                .set(!StringUtils.isEmpty(userSignDto.getEmail()), "email", userSignDto.getEmail()));

        BeanUtil.copyProperties(userSignDto, userSign, "uid");
        boolean isOk2 = userSignEntityService.saveOrUpdate(userSign);

        if (isOk1 && isOk2) {
            // 获取当前登录的用户
            AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

            // 如果修改的用户是本人，返回更新后的信息
            if (userRolesVo.getUsername().equals(userSignDto.getUsername())) {
                UserRolesVO userRoles = userRoleEntityService.getUserRoles(userRolesVo.getUid(), null);
                // 更新session
                BeanUtil.copyProperties(userRoles, userRolesVo);
                UserInfoVO userInfoVO = new UserInfoVO();
                BeanUtil.copyProperties(userRoles, userInfoVO, "roles");
                userInfoVO.setRoleList(userRoles.getRoles().stream().map(Role::getRole).collect(Collectors.toList()));
                return userInfoVO;
            } else {
                return null;
            }
        } else {
            throw new StatusFailException("更新个人信息失败！");
        }
    }

    public void removeUserSign(String username, Long id) throws StatusFailException, StatusForbiddenException {
        // 查询对象的
        String uid = userRoleEntityService.getUidByUsername(username);
        if (uid == null) {
            throw new StatusFailException("修改的用户不存在！");
        }

        List<SchoolUser> schoolUserList = getSchoolUserListByAdmin(null);

        // 查询用户是否在用户池中
        if (CollectionUtils.isEmpty(schoolUserList)
                || !schoolUserList.stream().map(SchoolUser::getUid).collect(Collectors.toList()).contains(uid)) {
            throw new StatusForbiddenException("无权限查询用户, 或者用户不存在！");
        }

        boolean result = schoolUserEntityService.removeById(id);

        if (!result) {
            throw new StatusFailException("移除学生池失败！");
        }
    }

    public void addUserSignBatch(List<UserSignDTO> userSignVoList)
            throws StatusFailException, StatusForbiddenException {

        if (CollectionUtils.isEmpty(userSignVoList)) {
            throw new StatusFailException("未传入学生数据！");
        }

        List<String> successUidList = Collections.synchronizedList(new LinkedList<>());
        Set<String> failedUserNameSet = ConcurrentHashMap.newKeySet();

        userSignVoList.stream().forEach(userSignVo -> {
            String key = (userSignVo.getUid() != null ? userSignVo.getUsername() : userSignVo.getRealname());

            try {
                addUserSign(userSignVo);
                successUidList.add(key);
            } catch (StatusFailException | StatusForbiddenException e) {
                failedUserNameSet.add(key);
            }
        });

        if (!failedUserNameSet.isEmpty()) {
            int failedCount = failedUserNameSet.size();
            int successCount = userSignVoList.size() - failedCount;
            String errMsg = "[导入结果] 成功数：" + successCount + ", 失败数：" + failedCount +
                    ", 失败的用户名：" + failedUserNameSet + " 注意：新用户请注意邮箱是否注册，已有用户添加到学生池不能重复添加！";
            throw new StatusFailException(errMsg);
        }
    }

    public void checkUserSign(UserSignDTO userSignDto) throws StatusFailException {
        commonValidator.validateContentLength(userSignDto.getRealname(), "真实姓名", 50);
        commonValidator.validateContentLength(userSignDto.getEnglishname(), "英文姓名", 50);
        commonValidator.validateContentLength(userSignDto.getSchool(), "学校", 50);
        commonValidator.validateContentLength(userSignDto.getFaculty(), "院系", 50);
        commonValidator.validateContentLength(userSignDto.getCourse(), "专业/班级", 50);
        commonValidator.validateContentLength(userSignDto.getNumber(), "学号", 30);
        commonValidator.validateContentLength(userSignDto.getClothesSize(), "衣服尺寸", 10);
        commonValidator.validateContentLength(userSignDto.getPhoneNumber(), "联系方式", 20);
        commonValidator.validateContentLength(userSignDto.getSchool(), "学校", 100);
        commonValidator.validateContentLength(userSignDto.getEmail(), "邮箱", 100);
    }

    /**
     * @MethodName getSchoolUserListByAdmin
     * @Description 获取当前用户可管理的学生列表
     */
    public List<SchoolUser> getSchoolUserListByAdmin(String schoolName) throws StatusFailException {
        // 获取当前登录的用户
        AccountProfile userRolesVO = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        UserRolesVO userRolesVo = userRoleEntityService.getUserRoles(null, userRolesVO.getUsername());

        List<SchoolUser> schoolUserList = new ArrayList<>();

        // 如果学校不为空，则查询 user_sign 中所有school为schoolName
        School school = schoolEntityService.getOne(new QueryWrapper<School>().eq("name", userRolesVo.getSchool()));

        QueryWrapper<SchoolUser> schoolUserQueryWrapper = new QueryWrapper<>();
        schoolUserQueryWrapper.eq("status", 0);

        if (SecurityUtils.getSubject().hasRole("root") || SecurityUtils.getSubject().hasRole("admin")) {
            // 超级管理员和普通管理员查询所有学生
        } else if (SecurityUtils.getSubject().hasRole("coach_admin")) {
            // 教练管理员管理学校内的学生
            schoolUserQueryWrapper.eq("sid", school.getId());
        } else {
            // 如果不是管理员，则查询本人的个人信息和队员的个人信息
            schoolUserQueryWrapper.eq("coach_uid", userRolesVo.getUid());
            schoolUserList.add(new SchoolUser().setSid(school.getId()).setUid(userRolesVo.getUid()));
        }

        // 添加符合条件的学生信息
        schoolUserList.addAll(schoolUserEntityService.list(schoolUserQueryWrapper));

        // 排序：本人的信息排在首位，其次按 sid 排序
        schoolUserList.sort(Comparator
                .comparing((SchoolUser schoolUser) -> !schoolUser.getUid().equals(userRolesVo.getUid())) // 本人排在前
                .thenComparing(SchoolUser::getSid, Comparator.nullsLast(Comparator.reverseOrder())) // 按 sid 排序
                .thenComparing(SchoolUser::getCoachUid, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(SchoolUser::getGmtCreate, Comparator.nullsLast(Comparator.reverseOrder()))); // 按加入顺序排序

        Map<String, Long> schoolMap = new HashMap<>();

        if (!StringUtils.isEmpty(schoolName)) {
            if (SecurityUtils.getSubject().hasRole("root") || SecurityUtils.getSubject().hasRole("admin")
                    || SecurityUtils.getSubject().hasRole("coach_admin")) {

                Long sid = 0L;

                if (schoolMap.containsKey(schoolName)) {
                    sid = schoolMap.get(schoolName);
                } else {
                    // 超级管理员，普通管理员，教练管理员查询学校的学生
                    School schoolByName = schoolEntityService.getOne(new QueryWrapper<School>().eq("name", schoolName));
                    sid = schoolByName.getId();
                    schoolMap.put(schoolName, sid);
                }

                List<UserSign> userSignList = userSignEntityService.list(new QueryWrapper<UserSign>()
                        .eq("school", schoolName).isNotNull("realname")); // 真实姓名不能为空

                final Long school_id = sid;
                List<SchoolUser> schoolUsers = userSignList.stream()
                        .map(userSign -> new SchoolUser().setSid(school_id).setUid(userSign.getUid()))
                        .collect(Collectors.toList());

                // 去重逻辑，优先保留 schoolUserList 里的数据
                Map<Pair_<String, String>, SchoolUser> uniqueUsers = new LinkedHashMap<>();
                Stream.concat(schoolUserList.stream(), schoolUsers.stream())
                        .forEach(user -> uniqueUsers.putIfAbsent(new Pair_<>(user.getUid(),
                                Optional.ofNullable(user.getCoachUid()).orElse("")), user));

                // 更新去重后的列表
                schoolUserList = new ArrayList<>(uniqueUsers.values());
            } else {
                throw new StatusFailException("您无权查看学校中的用户！");
            }
        }

        return schoolUserList;
    }

}