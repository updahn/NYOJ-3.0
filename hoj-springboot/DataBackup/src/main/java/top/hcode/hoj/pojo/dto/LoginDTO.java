package top.hcode.hoj.pojo.dto;

import lombok.Data;
import io.swagger.annotations.ApiModelProperty;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * @Author: Himit_ZH
 * @Date: 2020/7/20 00:23
 * @Description: 登录数据实体类
 */
@Data
public class LoginDTO implements Serializable {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    @ApiModelProperty(value = "新密码")
    private String newPassword;

    @ApiModelProperty(value = "真实姓名")
    private String realname;

    @ApiModelProperty(value = "邮箱")
    @Email(message = "邮箱格式错误")
    private String email;

    @ApiModelProperty(value = "验证码")
    private String code;

    @ApiModelProperty(value = "学校")
    private String school;

}