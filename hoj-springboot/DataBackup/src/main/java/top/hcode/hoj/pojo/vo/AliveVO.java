package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

@Data
@ApiModel(value = "保活信息", description = "")
@Accessors(chain = true)
public class AliveVO implements Serializable {

    @ApiModelProperty(value = "平台")
    private String oj;

    @ApiModelProperty(value = "标题")
    private String title;

    @ApiModelProperty(value = "账号")
    private String user;

    @ApiModelProperty(value = "网址")
    private String link;

}