package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class CoachInfoVO {

    @ApiModelProperty(value = "教练/队长用户编号")
    private String coachUid;

    @ApiModelProperty(value = "教练/队长用户名")
    private String coachUsername;

    @ApiModelProperty(value = "教练/队长真实姓名")
    private String coach;

    @ApiModelProperty(value = "是否为教练")
    private Boolean root;

    public CoachInfoVO(String coach, String coachUsername, String coachUid, Boolean root) {
        this.coach = coach;
        this.coachUsername = coachUsername;
        this.coachUid = coachUid;
        this.root = root;
    }

}