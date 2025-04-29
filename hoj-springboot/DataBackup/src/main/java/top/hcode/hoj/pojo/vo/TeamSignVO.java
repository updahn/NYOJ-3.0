package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 *
 * @Date: 2022/3/11 16:48
 * @Description:
 */
@Data
public class TeamSignVO {

    private Long id;

    private Long cid;

    @ApiModelProperty(value = "队伍中文名称")
    private String cname;

    @ApiModelProperty(value = "队伍英文名称")
    private String ename;

    @ApiModelProperty(value = "学校")
    private String school;

    @ApiModelProperty(value = "报名类型（0为正式名额，1为女队名额，2为打星名额，3为外卡名额）")
    private Integer type;

    @ApiModelProperty(value = "报名审核状态（-1表示未报名，0表示审核中，1为审核通过，2为审核不通过。）")
    private Integer status;

    @ApiModelProperty(value = "教练")
    private String instructor;

    @ApiModelProperty(value = "审核不通过原因")
    private String msg;

    @ApiModelProperty(value = "队长用户名")
    private String username1;

    @ApiModelProperty(value = "队员1用户名")
    private String username2;

    @ApiModelProperty(value = "队员2用户名")
    private String username3;

    @ApiModelProperty(value = "队伍信息")
    private List<UserSignVO> teamConfig;

    @ApiModelProperty(value = "队伍人数")
    private Integer participants;

    @ApiModelProperty(value = "队员上限(最大为3)")
    private Integer maxParticipants;

    @ApiModelProperty(value = "比赛标题")
    private String title;

    @ApiModelProperty(value = "开始时间")
    private Date startTime;

    @ApiModelProperty(value = "结束时间")
    private Date endTime;

    @ApiModelProperty(value = "是否为队伍池中的")
    private Boolean visible;

    @ApiModelProperty(value = "创建时间")
    private Date gmtCreate;

}
