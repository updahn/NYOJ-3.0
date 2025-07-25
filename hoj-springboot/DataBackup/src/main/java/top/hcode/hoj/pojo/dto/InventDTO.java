package top.hcode.hoj.pojo.dto;

import javax.validation.constraints.NotBlank;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 *
 * @Description:
 */
@Data
@Accessors(chain = true)
public class InventDTO {

    @NotBlank(message = "邀请人不能为空")
    private String username;

    @NotBlank(message = "被邀请人不能为空")
    private String toUsername;

    private String content;

}