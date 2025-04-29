package top.hcode.hoj.manager.signup;

import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;

import cn.hutool.core.bean.BeanUtil;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.dao.discussion.CommentEntityService;
import top.hcode.hoj.dao.discussion.ReplyEntityService;
import top.hcode.hoj.dao.msg.MsgRemindEntityService;
import top.hcode.hoj.dao.school.SchoolUserEntityService;
import top.hcode.hoj.dao.user.UserRoleEntityService;
import top.hcode.hoj.pojo.dto.InventDTO;
import top.hcode.hoj.pojo.dto.InventedDTO;
import top.hcode.hoj.pojo.entity.discussion.Comment;
import top.hcode.hoj.pojo.entity.discussion.Reply;
import top.hcode.hoj.pojo.entity.msg.MsgRemind;
import top.hcode.hoj.pojo.entity.school.SchoolUser;
import top.hcode.hoj.pojo.entity.user.Role;
import top.hcode.hoj.pojo.entity.user.UserSign;
import top.hcode.hoj.pojo.vo.ReplyVO;
import top.hcode.hoj.pojo.vo.UserInventVO;
import top.hcode.hoj.pojo.vo.UserMsgVO;
import top.hcode.hoj.pojo.vo.UserRolesVO;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.utils.RedisUtils;
import top.hcode.hoj.utils.SignupUtils;

import java.util.stream.Collectors;
import java.util.*;

@Component
public class InventManager {

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private SignupUtils signupUtils;

    @Autowired
    private UserRoleEntityService userRoleEntityService;

    @Autowired
    private SchoolUserEntityService schoolUserEntityService;

    @Autowired
    private CommentEntityService commentEntityService;

    @Autowired
    private MsgRemindEntityService msgRemindEntityService;

    @Autowired
    private ReplyEntityService replyEntityService;

    public UserInventVO addInvent(InventDTO inventDto) throws StatusFailException, StatusForbiddenException {

        String username = inventDto.getUsername();
        String toUsername = inventDto.getToUsername();
        String content = inventDto.getContent();

        UserRolesVO userRolesVo = userRoleEntityService.getUserRoles(null, username);
        UserRolesVO toUserRolesVo = userRoleEntityService.getUserRoles(null, toUsername);

        // 检查用户是否存在
        if (toUserRolesVo == null) {
            throw new StatusFailException("发送邀请失败，用户名 : " + toUsername + "不存在！");
        }
        // 检查用户是否为管理员
        List<String> roles = toUserRolesVo.getRoles().stream().map(Role::getRole).collect(Collectors.toList()); // 获取该用户角色所有的权限
        if (roles.contains("root") || roles.contains("admin") || roles.contains("coach_admin")) {
            throw new StatusFailException("发送邀请失败，用户名 : " + toUsername + "为管理员！");
        }

        String lockKey = Constants.Account.CONTEST_INVENT_LOCK.getCode() + userRolesVo.getUid();
        if (redisUtils.hasKey(lockKey)) {
            long expire = redisUtils.getExpire(lockKey);
            throw new StatusForbiddenException("提交邀请功能限制，请在" + expire + "秒后再进行提交！");
        } else {
            redisUtils.set(lockKey, 1, 10);
        }

        // 判断被邀请者是否加入过队伍
        List<SchoolUser> schoolUserList = schoolUserEntityService
                .list(new QueryWrapper<SchoolUser>().eq("uid", toUserRolesVo.getUid()));

        for (SchoolUser schoolUser : schoolUserList) {
            String coachUid = schoolUser.getCoachUid();

            UserRolesVO coachUserRolesVo = userRoleEntityService.getUserRoles(coachUid, null);
            List<String> coachRoles = coachUserRolesVo.getRoles().stream().map(Role::getRole)
                    .collect(Collectors.toList());

            // 不是超级管理员，普通管理员，教练管理员
            if (!coachRoles.contains("root") && !coachRoles.contains("admin") && !(coachRoles.contains("coach_admin")
                    && coachUserRolesVo.getSchool().equals(toUserRolesVo.getSchool()))) {
                throw new StatusFailException(
                        "发送邀请失败，用户名：“" + toUsername + "” 已经加入队长为：“" + coachUserRolesVo.getUsername() + "” 的队伍");
            }
        }

        // 判断发出者是否发送过邀请
        Comment comment = commentEntityService
                .getOne(new QueryWrapper<Comment>().eq("from_name", username).eq("status", 2), false);

        if (comment == null) {
            // 首次发送邀请
            String fromRole = SecurityUtils.getSubject().hasRole("root") ? "root"
                    : SecurityUtils.getSubject().hasRole("admin") ? "admin" : "user";

            comment = new Comment()
                    .setContent("<< " + username + " >> Invent Action")
                    .setFromUid(userRolesVo.getUid())
                    .setFromName(userRolesVo.getUsername())
                    .setFromAvatar(userRolesVo.getAvatar())
                    .setFromRole(fromRole)
                    .setStatus(2);// 设置消息类型为邀请信息

            boolean isOk = commentEntityService.saveOrUpdate(comment);

            if (!isOk) {
                throw new StatusFailException("发送邀请失败，请重新尝试！");
            }
        }

        // 发送邮箱提醒邀请信息
        sentInvent(signupUtils.getUserSign(username).getUid(), signupUtils.getUserSign(toUsername).getUid(), content,
                comment);

        // 返回被邀请者的信息和邀请的id
        UserInventVO userInventVo = new UserInventVO();
        userInventVo.setId(comment.getId());
        userInventVo.setUsername(username);
        userInventVo.setToUsername(toUsername);
        userInventVo.setStatus(signupUtils.getInventedStatusBl(userRolesVo.getUid(), toUserRolesVo.getUid()));
        return userInventVo;
    }

    public void removeInvent(String username, String toUsername)
            throws StatusForbiddenException, StatusFailException {

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root") || SecurityUtils.getSubject().hasRole("admin");

        UserSign userSign = signupUtils.getUserSign(username);
        UserSign toUserSign = signupUtils.getUserSign(toUsername);

        Comment comment = commentEntityService.getOne(
                new QueryWrapper<Comment>().eq("from_uid", userSign.getUid()).eq("status", 2), false);

        if (comment == null) {
            throw new StatusFailException("删除失败，当前邀请已不存在！");
        }

        // 如果不是评论本人 或者不是管理员 无权限删除该邀请
        if (!isRoot && !comment.getFromUid().equals(userRolesVo.getUid())) {
            throw new StatusForbiddenException("无权删除该邀请");
        }

        // 删除对该用户的所有邀请数据
        msgRemindEntityService.remove(new UpdateWrapper<MsgRemind>()
                .eq("quote_id", comment.getId())
                .eq("action", "Invent")
                .eq("sender_id", userSign.getUid())
                .eq("recipient_id", toUserSign.getUid()));

        replyEntityService.remove(new UpdateWrapper<Reply>()
                .eq("comment_id", comment.getId())
                .eq("to_uid", userSign.getUid())
                .eq("from_uid", toUserSign.getUid()));

    }

    public ReplyVO handleInvent(InventedDTO inventedDto) throws StatusFailException, StatusForbiddenException {

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root") || SecurityUtils.getSubject().hasRole("admin");

        String username = inventedDto.getUsername();
        Boolean isAccept = inventedDto.getIsAccept();
        UserMsgVO userMsgVo = inventedDto.getUserMsgVo();
        Integer id = userMsgVo.getQuoteId();

        // 获取被邀请人的用户信息
        UserSign userSign = signupUtils.getUserSign(username);
        if (userSign == null) {
            throw new StatusFailException("回复邀请失败，被邀请人不存在！");
        }

        MsgRemind msgRemind = msgRemindEntityService.getOne(new QueryWrapper<MsgRemind>()
                .eq("quote_id", id).eq("recipient_id", userSign.getUid()), false);

        // 不存在邀请
        if (!isRoot && msgRemind == null) {
            throw new StatusForbiddenException("对不起，您无权限回复！");
        }

        // 判断是否存在处理邀请记录
        Reply reply = replyEntityService.getOne(new QueryWrapper<Reply>().eq("comment_id", id)
                .eq("from_name", username).eq("to_name", userMsgVo.getSenderUsername()), false);

        if (reply == null) {
            // 未处理过邀请
            String fromRole = SecurityUtils.getSubject().hasRole("root") ? "root"
                    : SecurityUtils.getSubject().hasRole("admin") ? "admin" : "user";

            reply = new Reply()
                    .setCommentId(id)
                    .setFromAvatar(userRolesVo.getAvatar())
                    .setFromName(userRolesVo.getUsername())
                    .setFromUid(userRolesVo.getUid())
                    .setToAvatar(userMsgVo.getSenderAvatar())
                    .setToName(userMsgVo.getSenderUsername())
                    .setToUid(userMsgVo.getSenderId())
                    .setContent(isAccept.toString())
                    .setStatus(2) // 设置为邀请信息
                    .setFromRole(fromRole);

            boolean isOk = replyEntityService.saveOrUpdate(reply);

            if (!isOk) {
                throw new StatusFailException("回复邀请失败，请重新尝试！");
            }

        } else {
            replyEntityService.update(new UpdateWrapper<Reply>().eq("comment_id", id)
                    .eq("from_name", username).eq("to_name", userMsgVo.getSenderUsername())
                    .set("content", isAccept.toString()));
        }

        // 如果同意
        if (isAccept) {
            Comment comment = commentEntityService
                    .getOne(new QueryWrapper<Comment>().eq("status", 2).eq("from_name", username), false);

            if (comment != null) {
                // 查询自己发出邀请的队伍
                msgRemindEntityService.remove(new UpdateWrapper<MsgRemind>().eq("quote_id", comment.getId()));
                replyEntityService.remove(new UpdateWrapper<Reply>().eq("comment_id", comment.getId()));
            }

            // 将school_user的status更新为0
            Boolean isOk = schoolUserEntityService.update(new UpdateWrapper<SchoolUser>()
                    .eq("uid", userSign.getUid()).eq("coach_uid", userMsgVo.getSenderId()).set("status", 0));

            if (!isOk) {
                throw new StatusFailException("更新队伍失败，请重新尝试！");
            }
        }

        ReplyVO replyVo = new ReplyVO();
        BeanUtil.copyProperties(reply, replyVo);
        replyVo.setFromTitleName(userRolesVo.getTitleName());
        replyVo.setFromTitleColor(userRolesVo.getTitleColor());
        return replyVo;
    }

    /**
     * @MethodName sentInvent
     * @Description 比赛中，发送邀请信息
     */
    public void sentInvent(String uid, String toUid, String content, Comment comment) {

        MsgRemind msgRemind = msgRemindEntityService.getOne(new QueryWrapper<MsgRemind>()
                .eq("sender_id", uid).eq("recipient_id", toUid).eq("action", "Invent").eq("source_type", "Invent"),
                false);

        if (msgRemind == null) {
            // 发送邀请消息
            commentEntityService.updateInventMsg(toUid, uid, content, comment.getId());
        } else {
            // 同时需要删除该评论的回复表数据
            replyEntityService
                    .remove(new UpdateWrapper<Reply>().eq("content", "false").eq("comment_id", comment.getId()));

            // 更新邀请信息
            msgRemindEntityService.update(new UpdateWrapper<MsgRemind>()
                    .eq("id", msgRemind.getId()).set("source_content", content).set("state", 0));// 更改为未读
        }
    }

}