package top.hcode.hoj.pojo.dto;

import lombok.*;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DockerConfigDTO {

    /**
     * 容器的Id
     */
    private String containerId;

    /**
     * 操作（start, stop, restart, pull）
     */
    private String method;

    /**
     * 服务器IP
     */
    private String serverIp;

    /**
     * 是否为判题机
     */
    private Boolean isJudge;

}
