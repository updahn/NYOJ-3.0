package top.hcode.hoj.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @Author Himit_ZH
 * @Date 2022/5/9
 */
@Data
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SwitchConfigDTO {

    /**
     * SCPC 超管账号
     */
    private String scpcSuperAdminAccount;

    /**
     * SCPC 超管密码
     */
    private String scpcSuperAdminPassword;

    /**
     * 是否开启公开评论区
     */
    private Boolean openPublicDiscussion;

    /**
     * 是否开启团队评论区
     */
    private Boolean openGroupDiscussion;

    /**
     * 是否开启比赛讨论区
     */
    private Boolean openContestComment;

    /**
     * 是否开启公开评测
     */
    private Boolean openPublicJudge;

    /**
     * 是否开启团队评测
     */
    private Boolean openGroupJudge;

    /**
     * 是否开启比赛评测
     */
    private Boolean openContestJudge;

    /**
     * 是否隐藏非比赛提交详情的代码(超管不受限制)
     */
    private Boolean hideNonContestSubmissionCode;

    /**
     * 非比赛的提交间隔秒数
     */
    private Integer defaultSubmitInterval;

    /**
     * 每天可以创建的团队数量
     */
    private Integer defaultCreateGroupDailyLimit;

    /**
     * 总共可以拥有的团队数量
     */
    private Integer defaultCreateGroupLimit;

    /**
     * 创建团队的前提
     */
    private Integer defaultCreateGroupACInitValue;

    /**
     * 每天可以创建的帖子数量
     */
    private Integer defaultCreateDiscussionDailyLimit;

    /**
     * 创建讨论帖子的前提
     */
    private Integer defaultCreateDiscussionACInitValue;

    /**
     * 评论和回复的前提
     */
    private Integer defaultCreateCommentACInitValue;

    /**
     * 各个remote judge 的账号与密码列表
     */
    private List<String> hduUsernameList;

    private List<String> hduPasswordList;

    private List<String> cfUsernameList;

    private List<String> cfPasswordList;

    private List<String> pojUsernameList;

    private List<String> pojPasswordList;

    private List<String> atcoderUsernameList;

    private List<String> atcoderPasswordList;

    private List<String> spojUsernameList;

    private List<String> spojPasswordList;

    private List<String> libreojUsernameList;

    private List<String> libreojPasswordList;

    private List<String> scpcUsernameList;

    private List<String> scpcPasswordList;

    private List<String> qojUsernameList;

    private List<String> qojPasswordList;

    private List<String> mossUsernameList;
}
