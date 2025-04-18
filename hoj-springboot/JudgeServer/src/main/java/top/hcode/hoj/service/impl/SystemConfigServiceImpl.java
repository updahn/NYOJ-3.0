package top.hcode.hoj.service.impl;

import cn.hutool.system.oshi.OshiUtil;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.github.dockerjava.api.DockerClient;

import top.hcode.hoj.service.SystemConfigService;
import top.hcode.hoj.util.DockerClientUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @Author: Himit_ZH
 * @Date: 2020/12/3 20:15
 * @Description:
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    @Value("${hoj-judge-server.ip}")
    private String dockerHost;

    public HashMap<String, Object> getSystemConfig() {
        HashMap<String, Object> result = new HashMap<String, Object>();
        int cpuCores = Runtime.getRuntime().availableProcessors(); // cpu核数
        double cpuLoad = 100 - OshiUtil.getCpuInfo().getFree();
        String percentCpuLoad = String.format("%.2f", cpuLoad) + "%"; // cpu使用率

        double totalVirtualMemory = OshiUtil.getMemory().getTotal(); // 总内存
        double freePhysicalMemorySize = OshiUtil.getMemory().getAvailable(); // 空闲内存
        double value = freePhysicalMemorySize / totalVirtualMemory;
        String percentMemoryLoad = String.format("%.2f", (1 - value) * 100) + "%"; // 内存使用率

        result.put("cpuCores", cpuCores);
        result.put("percentCpuLoad", percentCpuLoad);
        result.put("percentMemoryLoad", percentMemoryLoad);

        try {
            String[] headers = {
                    "CONTAINER ID", "NAME", "IMAGE", "COMMAND", "CREATED", "STATUS",
                    "PORTS", "CPU %", "MEM USAGE / LIMIT", "MEM %", "NET I/O", "BLOCK I/O"
            };

            DockerClient dockerClient = new DockerClientUtils().connect(dockerHost, null);
            List<List<String>> containerDetails = DockerClientUtils.getDockerContainerDetails(dockerClient);

            List<Map<String, String>> dockerList = containerDetails.stream()
                    .filter(c -> c.size() > 1
                            && ("/hoj-rsync-slave".equals(c.get(1)) || "/hoj-judgeserver".equals(c.get(1))))
                    .map(container -> {
                        Map<String, String> map = new HashMap<>();
                        IntStream.range(0, Math.min(headers.length, container.size()))
                                .forEach(i -> map.put(headers[i], container.get(i)));
                        return map;
                    })
                    .collect(Collectors.toList());

            if (!dockerList.isEmpty()) {
                result.put("docker", dockerList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

}