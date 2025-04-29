package top.hcode.hoj.utils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import top.hcode.hoj.dao.contest.ContestRegisterEntityService;
import top.hcode.hoj.dao.msg.MsgRemindEntityService;
import top.hcode.hoj.dao.user.UserRoleEntityService;
import top.hcode.hoj.dao.user.UserSignEntityService;
import top.hcode.hoj.pojo.entity.contest.ContestRegister;
import top.hcode.hoj.pojo.entity.user.UserSign;
import top.hcode.hoj.pojo.vo.UserInventStatusVO;
import top.hcode.hoj.pojo.vo.UserSignVO;

@Component
public class SignupUtils {

    @Autowired
    private ContestRegisterEntityService contestRegisterEntityService;

    @Autowired
    private MsgRemindEntityService msgRemindEntityService;

    @Autowired
    private UserRoleEntityService userRoleEntityService;

    @Autowired
    private UserSignEntityService userSignEntityService;

    /**
     * 管理进入比赛的权限
     */

    /**
     * @MethodName updateTeamContestResigter
     * @Description 修改队伍进入比赛权限
     */
    public void updateTeamContestResigter(Long cid, List<String> team_names, Boolean isRemove) {
        if (!CollectionUtils.isEmpty(team_names)) {
            String username = team_names.get(0); // 队长

            UserSign userSign = getUserSign(username);
            updateContestResigter(cid, userSign.getUid(), isRemove);
            for (int i = 1; i < team_names.size(); i++) {
                UserSign toUserSign = getUserSign(team_names.get(i));

                if (getInventedStatusBl(userSign.getUid(), toUserSign.getUid()) == 1) {
                    updateContestResigter(cid, toUserSign.getUid(), isRemove);
                }
            }
        }
    }

    /**
     * @MethodName updateContestResigter
     * @Description 修改用户进入比赛权限 （当被邀请的队伍处于审核阶段，或者队伍解散 -> 回收）
     */
    public void updateContestResigter(Long cid, String uid, Boolean isRemove) {
        ContestRegister contestRegister = contestRegisterEntityService
                .getOne(new QueryWrapper<ContestRegister>().eq("cid", cid).eq("uid", uid), false);

        if (isRemove) {
            // 如果存在进入比赛权限
            if (contestRegister != null) {
                contestRegisterEntityService.removeById(contestRegister.getId());
            }
        } else {
            if (contestRegister == null) {
                contestRegister = new ContestRegister().setCid(cid).setUid(uid).setStatus(0);

                contestRegisterEntityService.saveOrUpdate(contestRegister);
            }
        }
    }

    /**
     * @MethodName getInventedStatusBl
     * @Description 查询对应比赛,该用户接受邀请对应的处理状态
     * @return 1 表示已经被成功邀请，2表示拒绝邀请且还未加入队伍，-1表示邀请未处理
     */
    public Integer getInventedStatusBl(String uid, String toUid) {
        List<UserInventStatusVO> inventStatusList = msgRemindEntityService.getUserInventedStatus(uid, toUid);

        // 加入队伍
        if (!CollectionUtils.isEmpty(inventStatusList)) {

            String statusList = inventStatusList.get(0).getContent();

            // 如果全是空值，说明该用户接受邀请未处理
            if (StringUtils.isEmpty(statusList)) {
                return -1;
            }
            // 如果其中有true值，说明已经被邀请
            if ("true".equals(statusList)) {
                return 1;
            }
            // 如果其中有false值，说明该用户拒绝加入队伍，并且没有新的邀请
            if (inventStatusList.stream().anyMatch(statusVo -> "false".equals(statusVo.getContent()))) {
                return 2;
            }
        }
        return -1;
    }

    /**
     * @MethodName getInventStatusBl
     * @Description 查询对应比赛,该用户发送邀请对应的处理状态
     * @return 1 表示已经接受，2表示拒绝邀请，-1表示未处理
     */
    public Integer getInventStatusBl(String uid, String toUid) {
        List<UserInventStatusVO> inventStatusList = msgRemindEntityService.getUserInventStatus(uid, toUid);

        // 加入队伍
        if (!CollectionUtils.isEmpty(inventStatusList)) {

            String statusList = inventStatusList.get(0).getContent();
            // 如果全是空值，说明该用户接受邀请未处理
            if (StringUtils.isEmpty(statusList)) {
                return -1;
            }
            // 如果其中有true值，说明已经被邀请
            if ("true".equals(statusList)) {
                return 1;
            }
            // 如果其中有false值，说明该用户拒绝加入队伍，并且没有新的邀请
            if (inventStatusList.stream().anyMatch(statusVo -> "false".equals(statusVo.getContent()))) {
                return 2;
            }
        }
        return -1;
    }

    public UserSign getUserSign(String username) {
        return userSignEntityService.getOne(
                new QueryWrapper<UserSign>().eq("uid", userRoleEntityService.getUidByUsername(username)), false);
    }

    public List<String> getTeamNames(String leader, String memberFirst, String memberSecond) {
        return Arrays.asList(leader, memberFirst, memberSecond).stream().filter(name -> !StringUtils.isEmpty(name))
                .collect(Collectors.toList());
    }

    public String convertUserSignVoListToString(List<UserSignVO> userSignVoList) {
        return new JSONObject().set("config", userSignVoList).toString();
    }

    public List<UserSignVO> convertUserSignVoListToList(String teamConfig) {
        return JSONUtil.parseObj(teamConfig).get("config", List.class);
    }

}
