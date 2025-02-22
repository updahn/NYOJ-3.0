package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

@Data
public class ContestPrintVO {

    private Long id;

    private Long cid;

    @ApiModelProperty(value = "提交打印文本的用户")
    private String username;

    @ApiModelProperty(value = "真实姓名")
    private String realname;

    @ApiModelProperty(value = "内容")
    private String content;

    @ApiModelProperty(value = "状态")
    private Integer status;

    @ApiModelProperty(value = "创建时间")
    private Date gmtCreate;

    @ApiModelProperty(value = "修改时间")
    private Date gmtModified;

}