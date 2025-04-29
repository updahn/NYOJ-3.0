package top.hcode.hoj.pojo.dto;

import top.hcode.hoj.pojo.vo.UserMsgVO;

import javax.validation.constraints.NotBlank;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 *
 * @Description:用户是否接受邀请
 */
@Data
@Accessors(chain = true)
public class InventedDTO {

    @NotBlank(message = "被邀请人不能为空")
    private String username;

    @NotBlank(message = "被邀请人是否同意不能为空")
    private Boolean isAccept;

    @NotBlank(message = "被邀请人消息不能为空")
    private UserMsgVO userMsgVo;

}