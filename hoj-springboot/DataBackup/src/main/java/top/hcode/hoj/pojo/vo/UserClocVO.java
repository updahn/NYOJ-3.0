package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class UserClocVO {

    private Long id;

    private String uid;

    @ApiModelProperty(value = "用户名")
    private String username;

    @ApiModelProperty(value = "真实姓名")
    private String realname;

    @ApiModelProperty(value = "日期")
    private String time;

    @ApiModelProperty(value = "代码数据")
    private String json;

    @ApiModelProperty(value = "代码量")
    private Long sum;

    @ApiModelProperty(value = "创建时间")
    private Date gmtCreate;

    @ApiModelProperty(value = "修改时间")
    private Date gmtModified;

}