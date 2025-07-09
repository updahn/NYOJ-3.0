package top.hcode.hoj.config;

import top.hcode.hoj.util.Constants;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.dockerjava.core.*;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.api.DockerClient;

import lombok.extern.slf4j.Slf4j;

import java.time.*;
import javax.net.ssl.SSLException;

/**
 * @Description: Docker配置TCP连接池
 */
@Configuration
@Slf4j(topic = "hoj")
public class DockerConfig {

    @Value("${hoj-judge-server.ip:localhost}")
    private String dockerHost;

    /**
     * Docker WebClient Bean，用于与Docker API通信
     *
     * @return WebClient实例
     * @throws SSLException 如果SSL配置失败
     */
    @Bean(name = "dockerClient")
    public DockerClient dockerClient() throws SSLException {

        String dockerDir = Constants.JudgeDir.DOCKER_DIR.getContent();

        // 配置docker CLI的一些选项
        DefaultDockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .withDockerTlsVerify(true)
                .withDockerCertPath(dockerDir)
                .withDockerHost(String.format("tcp://%s:%s", dockerHost, "2376"))
                .build();

        // 创建DockerHttpClient
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .maxConnections(100)
                .connectionTimeout(Duration.ofSeconds(30))
                .responseTimeout(Duration.ofSeconds(45))
                .build();

        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);
        return dockerClient;
    }
}