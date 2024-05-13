package top.hcode.hoj.pojo.dto;

import lombok.*;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SshConfigDTO {

    /**
     * SSH 主机
     */
    private String sshHost;

    /**
     * SSH 密码/授权码
     */
    private String sshPassword;

    /**
     * SSH 端口
     */
    private Integer sshPort;

    /**
     * SSH 邮箱
     */
    private String sshUsername;

    /**
     * SSH 项目路径
     */
    private String sshPath;

    /**
     * SSH 项目前端相对项目路径
     */

    private String sshFronted;

    /**
     * SSH 项目后端相对项目路径
     */
    private String sshBackend;

    /**
     * SSH 项目判题机相对项目路径
     */
    private String sshJudgeserver;
}
