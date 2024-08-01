package top.hcode.hoj.pojo.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class PasswordBarVO {

    @ApiModelProperty(value = "班级")
    private String course;

    @ApiModelProperty(value = "姓名")
    private String name;

    @ApiModelProperty(value = "账号")
    private String account;

    @ApiModelProperty(value = "密码")
    private String password;

    @ApiModelProperty(value = "md5加密后的密码")
    private String passwordMd5;

    public PasswordBarVO(String course, String name, String account, String password, String passwordMd5) {
        this.course = course;
        this.name = name;
        this.account = account;
        this.password = password;
        this.passwordMd5 = passwordMd5;
    }

    public PasswordBarVO(String account, String passwordMd5) {
        this.account = account;
        this.passwordMd5 = passwordMd5;
    }

}