package top.hcode.hoj.pojo.entity.contest;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "TeamSign对象", description = "")
public class TeamSign implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long cid;

    @ApiModelProperty(value = "队伍中文名称")
    private String cname;

    @ApiModelProperty(value = "队伍英文名称")
    private String ename;

    @ApiModelProperty(value = "学校")
    private String school;

    @ApiModelProperty(value = "队长用户名")
    private String username1;

    @ApiModelProperty(value = "队员1用户名")
    private String username2;

    @ApiModelProperty(value = "队员2用户名")
    private String username3;

    @ApiModelProperty(value = "队伍信息")
    private String teamConfig;

    @ApiModelProperty(value = "队伍人数")
    private Integer participants;

    @ApiModelProperty(value = "报名类型（0为正式名额，1为女队名额，2为打星名额，3为外卡名额）")
    private Integer type;

    @ApiModelProperty(value = "报名审核状态（-1表示未报名，0表示审核中，1为审核通过，2为审核不通过。）")
    private Integer status;

    @ApiModelProperty(value = "审核不通过原因")
    private String msg;

    @ApiModelProperty(value = "教练")
    private String instructor;

    @ApiModelProperty(value = "是否为队伍池中的，0为队伍池中的")
    private Boolean visible;

    @TableField(fill = FieldFill.INSERT)
    private Date gmtCreate;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date gmtModified;

}
