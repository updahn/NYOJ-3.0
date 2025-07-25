package top.hcode.hoj.pojo.dto;

import lombok.Data;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/11 18:15
 * @Description:
 */
@Data
public class CheckUsernameOrEmailDTO {

    private String email;

    private String username;

    /** 是否检查管理员 **/
    private Boolean root;

}