package top.hcode.hoj.service.impl;

import cn.hutool.system.oshi.OshiUtil;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import top.hcode.hoj.service.SystemConfigService;
import top.hcode.hoj.util.DockerClientUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: Himit_ZH
 * @Date: 2020/12/3 20:15
 * @Description:
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    @Autowired
    private DockerClientUtils dockerClientUtils;

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
            List<String> selectContainerNameList = Arrays.asList("hoj-rsync-slave", "hoj-judgeserver");

            List<Map<String, String>> containerDetailList = dockerClientUtils
                    .getDockerContainerDetailList(selectContainerNameList);

            if (!CollectionUtils.isEmpty(containerDetailList)) {
                result.put("docker", containerDetailList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

}