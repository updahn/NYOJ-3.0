package top.hcode.hoj.pojo.entity.judge;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@ApiModel(value = "RemoteJudge对象", description = "远程判题服务")
public class RemoteJudge {

    @TableId(value = "oj")
    private String oj;

    @ApiModelProperty(value = "通过率, 0~100")
    private Integer percent;

    @TableField(fill = FieldFill.INSERT)
    private Date gmtCreate;

}