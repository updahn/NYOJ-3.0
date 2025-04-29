package top.hcode.hoj.manager.signup;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.exception.StatusNotFoundException;
import top.hcode.hoj.common.result.Paginate;
import top.hcode.hoj.dao.contest.ContestEntityService;
import top.hcode.hoj.dao.contest.TeamSignEntityService;
import top.hcode.hoj.dao.school.SchoolEntityService;
import top.hcode.hoj.dao.user.UserRoleEntityService;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.entity.contest.TeamSign;
import top.hcode.hoj.pojo.entity.school.School;
import top.hcode.hoj.pojo.vo.CheckCnameOrEnameVO;
import top.hcode.hoj.pojo.vo.TeamSignVO;
import top.hcode.hoj.pojo.vo.UserRolesVO;
import top.hcode.hoj.pojo.vo.UserSignVO;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.utils.RedisUtils;
import top.hcode.hoj.utils.SignupUtils;
import top.hcode.hoj.validator.CommonValidator;

@Component
public class TeamManager {

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private SignupUtils signupUtils;

    @Autowired
    private CommonValidator commonValidator;

    @Autowired
    private UserRoleEntityService userRoleEntityService;

    @Autowired
    private TeamSignEntityService teamSignEntityService;

    @Autowired
    private ContestEntityService contestEntityService;

    @Autowired
    private SchoolEntityService schoolEntityService;

    public IPage<TeamSignVO> getTeamSignList(Integer currentPage, Integer limit, Long cid, Integer type,
            Integer status, String keyword, Long signCid) throws StatusFailException, StatusForbiddenException {

        // 页数，每页题数若为空，设置默认值
        if (currentPage == null || currentPage < 1)
            currentPage = 1;

        List<TeamSign> teamSignList = getTeamSignListByAdmin(cid, false);

        List<TeamSignVO> teamSignVoList = Optional.ofNullable(teamSignList).orElse(Collections.emptyList()).stream()
                .map(teamSign -> {
                    TeamSignVO teamSignVo = new TeamSignVO();
                    BeanUtil.copyProperties(teamSign, teamSignVo, "teamConfig");

                    Contest contest = contestEntityService.getById(signCid);
                    if (contest != null) {
                        teamSignVo.setTitle(contest.getTitle());
                        teamSignVo.setMaxParticipants(contest.getMaxParticipants());
                    } else {
                        teamSignVo.setMaxParticipants(3);
                    }
                    teamSignVo.setTeamConfig(signupUtils.convertUserSignVoListToList(teamSign.getTeamConfig()));

                    return teamSignVo;
                })
                .filter(teamSignVo -> ((type == null || teamSignVo.getType().equals(type))
                        && (status == null || teamSignVo.getStatus().equals(status))
                        && (keyword == null || teamSignVo.getCname().contains(keyword)
                                || teamSignVo.getEname().contains(keyword))
                        && (teamSignVo.getParticipants() == null
                                || teamSignVo.getParticipants() <= teamSignVo.getMaxParticipants()))) // 队伍人员不超过比赛的最多人数
                .collect(Collectors.toList());

        return Paginate.paginateListToIPage(teamSignVoList, currentPage, limit);
    }

    public IPage<Contest> getContestList(Integer limit, Integer currentPage, String status, String keyword) {

        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        if (limit == null || limit < 1)
            limit = 10;

        IPage<Contest> iPage = new Page<>(currentPage, limit);
        QueryWrapper<Contest> queryWrapper = new QueryWrapper<>();
        // 过滤密码
        queryWrapper.select(Contest.class, info -> !info.getColumn().equals("pwd"));

        if (status != null) {
            queryWrapper.eq("status", status);
        }
        if (!StringUtils.isEmpty(keyword)) {
            keyword = keyword.trim();
            queryWrapper.like("title", keyword);
        }

        queryWrapper.eq("is_group", false).orderByDesc("start_time");

        // 筛选正式赛
        queryWrapper.eq("auth", Constants.Contest.AUTH_OFFICIAL.getCode());

        return contestEntityService.page(iPage, queryWrapper);
    }

    public TeamSignVO getTeamSign(Long id) throws StatusFailException, StatusForbiddenException {

        TeamSign teamSign = teamSignEntityService.getById(id);

        if (teamSign == null) {
            throw new StatusFailException("对应队伍信息不存在！");
        }

        List<TeamSign> teamSignList = getTeamSignListByAdmin(teamSign.getCid(), false);

        // 查询用户是否在用户池中
        if (CollectionUtils.isEmpty(teamSignList)
                || !teamSignList.stream().map(TeamSign::getId).collect(Collectors.toList()).contains(id)) {
            throw new StatusForbiddenException("无权限查看队伍, 或者队伍不存在！");
        }

        TeamSignVO teamSignVo = new TeamSignVO();
        BeanUtil.copyProperties(teamSign, teamSignVo, "teamConfig");
        List<UserSignVO> userSignVoList = signupUtils.convertUserSignVoListToList(teamSign.getTeamConfig());
        teamSignVo.setTeamConfig(userSignVoList);
        teamSignVo.setParticipants(userSignVoList.size());

        // 获取本场比赛的状态
        Contest contest = contestEntityService.getById(teamSign.getCid());
        if (contest != null) {
            teamSignVo.setTitle(contest.getTitle());
            teamSignVo.setMaxParticipants(contest.getMaxParticipants());
            teamSignVo.setStartTime(contest.getStartTime());
            teamSignVo.setEndTime(contest.getEndTime());
        } else {
            teamSignVo.setMaxParticipants(3);
        }

        return teamSignVo;
    }

    public void addTeamSign(TeamSignVO teamSignVo) throws StatusFailException, StatusForbiddenException {
        checkTeamSign(teamSignVo);

        // 取出所有的用户名
        List<String> team_names = signupUtils.getTeamNames(teamSignVo.getUsername1(), teamSignVo.getUsername2(),
                teamSignVo.getUsername3());

        if (CollectionUtils.isEmpty(team_names)) {
            throw new StatusFailException("报名失败，没有传入报名用户名！");
        }

        List<UserSignVO> userSignVoList = teamSignVo.getTeamConfig();

        TeamSign teamSign = new TeamSign()
                .setTeamConfig(new JSONObject().set("config", userSignVoList).toString())
                .setCid(teamSignVo.getCid())
                .setParticipants(userSignVoList.size())
                .setStatus(0); // 设置为审核中
        BeanUtil.copyProperties(teamSignVo, teamSign, "teamConfig", "cid", "status", "participants");

        Contest contest = contestEntityService.getById(teamSignVo.getCid());
        if (contest == null) {
            // 添加队伍
            teamSign.setVisible(false);
            if (userSignVoList.size() > 3) {
                throw new StatusFailException("添加队伍失败，队伍人数超过人数上限！");
            }
        } else {
            teamSign.setVisible(true);

            Long now = new Date().getTime();
            if (now < contest.getSignStartTime().getTime() || now > contest.getSignEndTime().getTime()) {
                throw new StatusForbiddenException("不在报名时间内！");
            }

            String lockKey = Constants.Account.CONTEST_SIGN_LOCK.getCode() + teamSignVo.getUsername1();
            if (redisUtils.hasKey(lockKey)) {
                long expire = redisUtils.getExpire(lockKey);
                throw new StatusForbiddenException("提交报名功能限制，请在" + expire + "秒后再进行提交！");
            } else {
                redisUtils.set(lockKey, 1, 10);
            }

            TeamSign teamSignCheck = teamSignEntityService.getOne(new QueryWrapper<TeamSign>()
                    .eq("username1", teamSignVo.getUsername1())
                    .eq("cid", teamSignVo.getCid()), false);

            // 队伍队长没有报名过比赛
            if (teamSignCheck != null) {
                throw new StatusFailException("您已经提交过报名！");
            }

            if (contest.getMaxParticipants() > 1) {
                // 提交报名要求队伍名称不重复
                CheckCnameOrEnameVO CheckCnameOrEnameVo = checkCnameOrEname(teamSignVo.getCid(), teamSignVo.getCname(),
                        teamSignVo.getEname());
                if (CheckCnameOrEnameVo.getCname()) {
                    throw new StatusFailException("报名失败，队伍中文名称已被占用！");
                }
                if (CheckCnameOrEnameVo.getEname()) {
                    throw new StatusFailException("报名失败，队伍英文名称已被占用！");
                }
            }

            // 所有人已经同意
            if (userSignVoList.stream().anyMatch(userSignVo -> userSignVo.getStatus() != 1)) {
                throw new StatusFailException("报名失败，请等待所有人同意！");
            }

            if (userSignVoList.size() > contest.getMaxParticipants()) {
                throw new StatusFailException("报名失败，队伍人数超过比赛人数上限！");
            }

        }

        Boolean isOk = teamSignEntityService.saveOrUpdate(teamSign);

        if (!isOk) {
            if (teamSign.getCid() != null) {
                throw new StatusFailException("提交报名失败！");
            } else {
                throw new StatusFailException("添加队伍池失败！");
            }
        }
    }

    public void updateTeamSign(TeamSignVO teamSignVo) throws StatusFailException, StatusForbiddenException {
        checkTeamSign(teamSignVo);

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        // 取出所有的用户名
        List<String> team_names = signupUtils.getTeamNames(teamSignVo.getUsername1(), teamSignVo.getUsername2(),
                teamSignVo.getUsername3());

        if (CollectionUtils.isEmpty(team_names)) {
            throw new StatusFailException("报名失败，没有传入报名用户名！");
        }

        TeamSign teamSign = teamSignEntityService.getById(teamSignVo.getId());

        if (teamSign == null) {
            throw new StatusFailException("对应队伍信息不存在！");
        }

        List<TeamSign> teamSignList = getTeamSignListByAdmin(teamSign.getCid(), true);

        // 查询用户是否在用户池中
        if (CollectionUtils.isEmpty(teamSignList)
                || !teamSignList.stream().map(TeamSign::getId).collect(Collectors.toList())
                        .contains(teamSignVo.getId())) {
            throw new StatusForbiddenException("不是队长无权限修改队伍, 或者队伍不存在！");
        }

        Contest contest = contestEntityService.getById(teamSign.getCid());

        if (contest != null) {
            // 超管，普通管理员，比赛管理者可任意时间修改比赛队伍信息

            if (!SecurityUtils.getSubject().hasRole("root") && !SecurityUtils.getSubject().hasRole("admin")
                    && !userRolesVo.getUid().equals(contest.getUid())) {

                if (new Date().getTime() > contest.getModifyEndTime().getTime()) {
                    throw new StatusForbiddenException("超过报名修改时间！");
                }
            }

        }

        BeanUtil.copyProperties(teamSignVo, teamSign, "teamConfig");
        teamSign.setTeamConfig(new JSONObject().set("config", teamSignVo.getTeamConfig()).toString());
        Boolean isOk = teamSignEntityService.saveOrUpdate(teamSign);

        if (!isOk) {
            if (teamSign.getCid() != null) {
                throw new StatusFailException("修改报名失败！");
            } else {
                throw new StatusFailException("修改队伍池失败！");
            }
        }
    }

    public void removeTeamSign(Long id) throws StatusFailException, StatusForbiddenException {

        TeamSign teamSign = teamSignEntityService.getById(id);

        if (teamSign == null) {
            throw new StatusFailException("对应队伍信息不存在！");
        }

        List<TeamSign> teamSignList = getTeamSignListByAdmin(teamSign.getCid(), true);

        // 查询用户是否在用户池中
        if (CollectionUtils.isEmpty(teamSignList)
                || !teamSignList.stream().map(TeamSign::getId).collect(Collectors.toList()).contains(id)) {
            throw new StatusForbiddenException("无权限查看队伍, 或者队伍不存在！");
        }

        boolean result = teamSignEntityService.removeById(id);

        if (!result) {
            if (teamSign.getCid() != null) {
                throw new StatusFailException("移除报名失败！");
            } else {
                throw new StatusFailException("移除队伍池失败！");
            }
        }
    }

    public void updateTeamSignStatus(List<Long> ids, Long cid, Integer status, String msg)
            throws StatusFailException, StatusForbiddenException, StatusNotFoundException {

        // 检查是否选择了报名项
        if (CollectionUtils.isEmpty(ids)) {
            throw new StatusFailException("未选择报名项！");
        }

        // 获取当前登录用户及其角色信息
        AccountProfile userRolesVO = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        UserRolesVO userRolesVo = userRoleEntityService.getUserRoles(null, userRolesVO.getUsername());

        Contest contest = contestEntityService.getById(cid);

        if (contest == null) {
            throw new StatusNotFoundException("该比赛不存在！");
        }

        // 批量查询报名信息
        List<TeamSign> teamSignLists = teamSignEntityService.list(new QueryWrapper<TeamSign>().in("id", ids));
        for (TeamSign teamSign : teamSignLists) {
            Long contestId = teamSign.getCid();

            if (!contestId.equals(cid)) {
                throw new StatusFailException("传入的队伍ids的比赛id与cid不一致！");
            }

            // 权限校验：只有超级管理员，普通管理员，教练管理员，比赛管理者才能操作
            if (!SecurityUtils.getSubject().hasRole("root") && !SecurityUtils.getSubject().hasRole("admin")
                    && !(SecurityUtils.getSubject().hasRole("coach_admin")
                            && teamSign.getSchool().equals(userRolesVo.getSchool()))
                    && !(contest.getUid().equals(userRolesVo.getUid()))) {
                throw new StatusForbiddenException("对不起，你无权限操作！");
            }

            List<String> team_names = signupUtils.getTeamNames(teamSign.getUsername1(), teamSign.getUsername2(),
                    teamSign.getUsername3());

            if (status == 1) {
                // 添加该队伍的所有进入比赛权限
                signupUtils.updateTeamContestResigter(cid, team_names, false);
            } else {
                // 回收该队伍的所有进入比赛权限
                signupUtils.updateTeamContestResigter(cid, team_names, true);
            }

            // 更新队伍报名状态
            boolean isOk = teamSignEntityService.updateById(new TeamSign()
                    .setId(teamSign.getId())
                    .setStatus(status)
                    .setMsg(msg != null ? msg : null));

            if (!isOk) {
                throw new StatusFailException("修改失败！");
            }
        }

    }

    public void addTeamSignBatch(List<Long> ids, Long cid, Integer type)
            throws StatusFailException, StatusForbiddenException, StatusNotFoundException {

        // 检查是否选择了报名项
        if (CollectionUtils.isEmpty(ids)) {
            throw new StatusFailException("未选择报名项！");
        }

        // 获取当前登录用户及其角色信息
        AccountProfile userRolesVO = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        UserRolesVO userRolesVo = userRoleEntityService.getUserRoles(null, userRolesVO.getUsername());

        Contest contest = contestEntityService.getById(cid);

        if (contest == null) {
            throw new StatusNotFoundException("该比赛不存在！");
        }

        Long now = new Date().getTime();
        if (now < contest.getSignStartTime().getTime() || now > contest.getSignEndTime().getTime()) {
            throw new StatusForbiddenException("不在报名时间内！");
        }

        // 批量查询报名信息
        List<TeamSign> teamSignList = teamSignEntityService.list(new QueryWrapper<TeamSign>().in("id", ids));

        List<TeamSign> contestteamSignList = teamSignEntityService.list(new QueryWrapper<TeamSign>().eq("cid", cid));

        for (TeamSign teamSign : teamSignList) {

            // 权限校验：只有教练管理员，超级管理员，比赛管理者和队长才能操作
            if (!SecurityUtils.getSubject().hasRole("root") && !SecurityUtils.getSubject().hasRole("admin")
                    && !(SecurityUtils.getSubject().hasRole("coach_admin")
                            && teamSign.getSchool().equals(userRolesVo.getSchool()))
                    && !(contest.getUid().equals(userRolesVo.getUid()))
                    && !teamSign.getUsername1().equals(userRolesVo.getUsername())) {
                throw new StatusForbiddenException("对不起，你无权限操作！");
            }

            List<String> teamNames = signupUtils.getTeamNames(
                    teamSign.getUsername1(), teamSign.getUsername2(), teamSign.getUsername3());

            // 查询如果同一比赛，同一成员，则不能报名
            for (TeamSign contestTeamSign : contestteamSignList) {
                if (!Collections.disjoint(teamNames, signupUtils.getTeamNames(
                        contestTeamSign.getUsername1(),
                        contestTeamSign.getUsername2(),
                        contestTeamSign.getUsername3()))) {
                    throw new StatusFailException("添加失败，该队伍已经报名！");
                }
            }

            TeamSign newTeamSign = new TeamSign().setCid(cid).setStatus(0).setType(type).setVisible(true);
            BeanUtil.copyProperties(teamSign, newTeamSign, "id", "cid", "status", "type", "visible");

            // 添加队伍报名状态
            boolean isOk = teamSignEntityService.saveOrUpdate(newTeamSign);

            if (!isOk) {
                throw new StatusFailException("添加失败！");
            }
        }
    }

    public void checkTeamSign(TeamSignVO teamSignVo) throws StatusFailException {
        commonValidator.validateContentLength(teamSignVo.getCname(), "队伍中文名称", 50);
        commonValidator.validateContentLength(teamSignVo.getEname(), "队伍英文名称", 50);
        commonValidator.validateContentLength(teamSignVo.getSchool(), "学校", 50);
        commonValidator.validateContentLength(teamSignVo.getUsername1(), "队伍队长", 150);
        commonValidator.validateContentLength(teamSignVo.getUsername2(), "队伍队员1", 150);
        commonValidator.validateContentLength(teamSignVo.getUsername3(), "队伍队员2", 150);
    }

    /**
     * @MethodName checkCnameOrEname
     * @Description 查询报名是否有重复的中文/英文名称
     */
    public CheckCnameOrEnameVO checkCnameOrEname(Long cid, String cname, String ename) {
        CheckCnameOrEnameVO cnameOrEnameVo = new CheckCnameOrEnameVO();

        boolean rightCname = false;
        boolean rightEname = false;

        if (!StringUtils.isEmpty(cname)) {
            cname = cname.trim();
            QueryWrapper<TeamSign> wrapper = new QueryWrapper<TeamSign>().eq("cname", cname).eq("cid", cid);
            TeamSign teamSign = teamSignEntityService.getOne(wrapper, false);
            if (teamSign != null) {
                rightCname = true;
            } else {
                rightCname = false;
            }
        }

        if (!StringUtils.isEmpty(ename)) {
            ename = ename.trim();
            QueryWrapper<TeamSign> wrapper = new QueryWrapper<TeamSign>().eq("ename", ename).eq("cid", cid);
            TeamSign teamSign = teamSignEntityService.getOne(wrapper, false);
            if (teamSign != null) {
                rightEname = true;
            } else {
                rightEname = false;
            }
        }

        cnameOrEnameVo.setCname(rightCname);
        cnameOrEnameVo.setEname(rightEname);
        return cnameOrEnameVo;
    }

    /**
     * @MethodName getTeamSignListByAdmin
     * @Description 获取当前用户可查看的队伍列表
     */
    public List<TeamSign> getTeamSignListByAdmin(Long cid, Boolean isTeamLeader) {

        // 获取当前登录的用户
        AccountProfile userRolesVO = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        UserRolesVO userRolesVo = userRoleEntityService.getUserRoles(null, userRolesVO.getUsername());

        School school = schoolEntityService.getOne(new QueryWrapper<School>().eq("name", userRolesVo.getSchool()));

        // 获取本场比赛的状态
        Contest contest = contestEntityService.getById(cid);

        QueryWrapper<TeamSign> teamSignQueryWrapper = new QueryWrapper<>();

        if (contest != null) {
            // 查看比赛
            teamSignQueryWrapper.eq("visible", 1).eq("cid", cid);

            if (SecurityUtils.getSubject().hasRole("root") || SecurityUtils.getSubject().hasRole("admin")
                    || userRolesVo.getUid().equals(contest.getUid())) {
                // 超管，普通管理员，比赛管理者可查看当前比赛的所有队伍信息
            } else {
                // 队长可查看自己所有的队伍
                teamSignQueryWrapper.eq("username1", userRolesVo.getUsername());
            }
        } else {
            // 管理队伍
            teamSignQueryWrapper.eq("visible", 0);

            if (SecurityUtils.getSubject().hasRole("root") || SecurityUtils.getSubject().hasRole("admin")) {
                // 超管，普通管理员可以查看所有的队伍
            } else if (SecurityUtils.getSubject().hasRole("coach_admin")) {
                // 教练可以查看学校的所有队伍
                teamSignQueryWrapper.eq("school", school.getName());
            } else {
                if (isTeamLeader) {
                    // 队长可查看自己所有的队伍
                    teamSignQueryWrapper.eq("username1", userRolesVo.getUsername());
                } else {
                    // 其他角色可查看自己所在的队伍
                    teamSignQueryWrapper.and(wrapper -> wrapper.eq("username1", userRolesVo.getUsername()).or()
                            .eq("username2", userRolesVo.getUsername()).or()
                            .eq("username3", userRolesVo.getUsername()));
                }
            }
        }

        List<TeamSign> teamSignList = teamSignEntityService.list(teamSignQueryWrapper);

        // 排序：本人队伍排在首位，其次按 status 排序
        teamSignList.sort(Comparator
                .comparing((TeamSign teamSign) -> {
                    String uid = userRolesVo.getUid();
                    return !(uid.equals(teamSign.getUsername1() != null ? teamSign.getUsername1() : "")
                            || uid.equals(teamSign.getUsername2() != null ? teamSign.getUsername2() : "")
                            || uid.equals(teamSign.getUsername3() != null ? teamSign.getUsername3() : "")); // 本人队伍排在前
                })
                .thenComparing(TeamSign::getStatus, Comparator.nullsLast(Comparator.reverseOrder())) // 按 status 排序
                .thenComparing(TeamSign::getGmtCreate, Comparator.nullsLast(Comparator.reverseOrder())));

        return teamSignList;
    }
}