package top.hcode.hoj.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import com.github.dockerjava.api.DockerClient;

import lombok.extern.slf4j.Slf4j;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.*;
import java.time.*;
import javax.net.ssl.SSLException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.api.async.ResultCallback;

@Component
@Slf4j
public class DockerClientUtils {

    private static DockerClient dockerClient;

    private static String[] HEADERS = {
            "CONTAINER ID", "NAME", "IMAGE", "COMMAND", "CREATED", "STATUS",
            "PORTS", "CPU %", "MEM USAGE / LIMIT", "MEM %", "NET I/O", "BLOCK I/O"
    };

    /**
     * 构造函数，注入WebClient Bean
     *
     * @param dockerClient 在DockerConfig中配置的WebClient Bean
     */
    @Autowired
    public void DockerClientUtils(@Qualifier("dockerClient") DockerClient webClient) throws SSLException {
        DockerClientUtils.dockerClient = webClient;
    }

    /**
     * 获取单个容器详情
     *
     * @param containerId 容器ID
     * @return 容器详情
     */
    public Map<String, String> getContainer(String containerId) {
        try {
            InspectContainerResponse info = dockerClient.inspectContainerCmd(containerId).exec();
            Map<String, String> map = new HashMap<>();

            map.put("name", Optional.ofNullable(info.getName()).orElse("").replaceFirst("^/", ""));
            map.put("command", Optional.ofNullable(info.getConfig().getCmd())
                    .map(cmd -> String.join(" ", cmd))
                    .orElse(""));
            map.put("image", info.getConfig().getImage());
            map.put("status", info.getState().getStatus());
            map.put("created", formattedTimeZone(info.getCreated()));

            // 端口信息拼接
            Ports ports = info.getNetworkSettings().getPorts();
            String portInfo = "";
            if (ports != null && ports.getBindings() != null) {
                portInfo = ports.getBindings().entrySet().stream()
                        .map(entry -> {
                            ExposedPort ep = entry.getKey();
                            Ports.Binding[] bindings = entry.getValue();
                            if (bindings != null && bindings.length > 0 && bindings[0] != null) {
                                Ports.Binding b = bindings[0];
                                String hostIp = b.getHostIp() != null ? b.getHostIp() + ":" : "";
                                String hostPort = b.getHostPortSpec() != null ? b.getHostPortSpec() : "";
                                return String.format("%s%s->%d/%s", hostIp, hostPort, ep.getPort(), ep.getProtocol());
                            }
                            return ep.getPort() + "/" + ep.getProtocol();
                        })
                        .collect(Collectors.joining(", "));
            }
            map.put("ports", portInfo);

            return map;
        } catch (Exception e) {
            log.error("Failed to get container {}: {}", containerId, e.getMessage());
            return null;
        }
    }

    /**
     * 获取所有容器列表
     *
     * @param selectContainerNameList 筛选的容器名称列表
     *
     * @return 容器列表
     */
    public List<Container> getAllContainerList(List<String> selectContainerNameList) {
        try {
            return dockerClient.listContainersCmd().withShowAll(true).withNameFilter(selectContainerNameList).exec();
        } catch (Exception e) {
            log.error("Failed to get containers: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 启动容器
     *
     * @param containerId 容器ID
     * @return 是否成功启动容器
     */
    public Boolean startContainer(String containerId) {
        try {
            dockerClient.startContainerCmd(containerId).exec();
            return true;
        } catch (Exception e) {
            log.error("Failed to start container {}: {}", containerId, e.getMessage());
            return false;
        }
    }

    /**
     * 重启容器
     *
     * @param containerId 容器ID
     * @return 是否成功重启容器
     */
    public Boolean restartContainer(String containerId) {
        try {
            dockerClient.restartContainerCmd(containerId).exec();
            return true;
        } catch (Exception e) {
            log.error("Failed to restart container {}: {}", containerId, e.getMessage());
            return false;
        }
    }

    /**
     * 停止容器
     *
     * @param containerId 容器ID
     * @return 是否成功停止容器
     */
    public Boolean stopContainer(String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).exec();
            return true;
        } catch (Exception e) {
            log.error("Failed to stop container {}: {}", containerId, e.getMessage());
            return false;
        }
    }

    /**
     * 拉取镜像
     *
     * @param containerId 容器ID
     * @return 是否成功拉取镜像
     */
    public Boolean pullImage(String containerId) {
        try {
            Map<String, String> map = getContainer(containerId);

            if (map != null) {
                String repository = map.get("image");
                dockerClient.pullImageCmd(repository);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to pull image for container {}: {}", containerId, e.getMessage());
            return false;
        }
    }

    /**
     * 获取Docker容器详情列表
     *
     * @param selectContainerNameList 筛选的容器名称列表
     *
     * @return 容器详情列表
     */
    public List<Map<String, String>> getDockerContainerDetailList(List<String> selectContainerNameList) {

        // 获取所有容器
        List<Container> containerList = getAllContainerList(selectContainerNameList);

        if (CollectionUtils.isEmpty(containerList)) {
            return null;
        }

        List<List<String>> results = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        List<Future<List<String>>> futureList = new ArrayList<>();

        for (Container container : containerList) {
            List<String> info = new ArrayList<>();

            String id = container.getId().substring(0, 12);
            String name = Optional.ofNullable(container.getNames()).filter(n -> n.length > 0).map(n -> n[0]).orElse("");
            String image = container.getImage();
            String command = container.getCommand();
            String status = container.getStatus();
            String created = formatTimestamp(container.getCreated());
            String ports = Arrays.stream(container.getPorts())
                    .map(p -> String.format("%s%s->%s/%s",
                            p.getIp() != null ? p.getIp() + ":" : "",
                            p.getPublicPort() != null ? p.getPublicPort() : "",
                            p.getPrivatePort(), p.getType()))
                    .collect(Collectors.joining(", "));

            info.addAll(Arrays.asList(id, name, image, command, created, status, ports));

            // 提交异步任务获取统计信息
            Future<List<String>> future = executor.submit(() -> {
                List<String> statsInfo = new ArrayList<>(info); // 拷贝基础信息

                CountDownLatch latch = new CountDownLatch(1);
                try (StatsCmd statsCmd = dockerClient.statsCmd(id)) {
                    AtomicBoolean skipped = new AtomicBoolean(false);

                    statsCmd.exec(new ResultCallback.Adapter<Statistics>() {
                        @Override
                        public void onNext(Statistics stats) {
                            if (!skipped.getAndSet(true))
                                return;

                            DecimalFormat df = new DecimalFormat("0.00");

                            long cpuDelta = getSafe(() -> stats.getCpuStats().getCpuUsage().getTotalUsage() -
                                    stats.getPreCpuStats().getCpuUsage().getTotalUsage());
                            long systemDelta = getSafe(() -> stats.getCpuStats().getSystemCpuUsage() -
                                    stats.getPreCpuStats().getSystemCpuUsage());
                            long cpus = getSafe(() -> stats.getCpuStats().getOnlineCpus());
                            String cpuPercent = (cpuDelta > 0 && systemDelta > 0 && cpus > 0)
                                    ? df.format((double) cpuDelta / systemDelta * cpus * 100.0) + "%"
                                    : "0.00%";

                            long memUsage = getSafe(() -> stats.getMemoryStats().getUsage());
                            long memLimit = getSafe(() -> stats.getMemoryStats().getLimit());
                            String memUsageStr = (memLimit > 0) ? formatBytes(memUsage) + " / " + formatBytes(memLimit)
                                    : "--";
                            String memPercent = (memLimit > 0)
                                    ? df.format((double) memUsage / memLimit * 100.0) + "%"
                                    : "--";

                            long rx = stats.getNetworks() != null
                                    ? stats.getNetworks().values().stream().mapToLong(n -> n.getRxBytes()).sum()
                                    : -1L;
                            long tx = stats.getNetworks() != null
                                    ? stats.getNetworks().values().stream().mapToLong(n -> n.getTxBytes()).sum()
                                    : -1L;

                            String netIO = (rx >= 0 && tx >= 0) ? formatBytes(rx) + " / " + formatBytes(tx) : "--";

                            long blkRead = 0, blkWrite = 0;
                            if (stats.getBlkioStats() != null
                                    && stats.getBlkioStats().getIoServiceBytesRecursive() != null) {
                                blkRead = stats.getBlkioStats().getIoServiceBytesRecursive().stream()
                                        .filter(io -> "Read".equalsIgnoreCase(io.getOp()))
                                        .mapToLong(BlkioStatEntry::getValue).sum();
                                blkWrite = stats.getBlkioStats().getIoServiceBytesRecursive().stream()
                                        .filter(io -> "Write".equalsIgnoreCase(io.getOp()))
                                        .mapToLong(BlkioStatEntry::getValue).sum();
                            }
                            String blockIO = formatBytes(blkRead) + " / " + formatBytes(blkWrite);

                            statsInfo.addAll(Arrays.asList(cpuPercent, memUsageStr, memPercent, netIO, blockIO));
                            latch.countDown();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            latch.countDown();
                        }
                    });

                    latch.await(5, TimeUnit.SECONDS); // 可以适当缩短超时
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return statsInfo;
            });

            futureList.add(future);
        }

        // 收集结果
        for (Future<List<String>> future : futureList) {
            try {
                results.add(future.get(6, TimeUnit.SECONDS)); // 设置获取结果的最大等待时间
            } catch (Exception e) {
                log.error("Error getting container stats: {}", e.getMessage());
            }
        }

        executor.shutdown();

        // 补全缺失字段
        for (List<String> row : results) {
            if (row.size() < 12)
                row.addAll(Collections.nCopies(12 - row.size(), "--"));
        }

        // 状态优先级排序
        Map<String, Integer> statusPriority = new HashMap<>();
        statusPriority.put("Up", 1);
        statusPriority.put("Created", 2);
        statusPriority.put("Exited", 3);

        results.sort(Comparator.comparingInt(row -> statusPriority.entrySet().stream()
                .filter(entry -> row.size() > 5 && row.get(5).startsWith(entry.getKey()))
                .map(Map.Entry::getValue).findFirst().orElse(Integer.MAX_VALUE)));

        // 转换为 Map 列表
        return results.stream().map(row -> {
            Map<String, String> json = new HashMap<>();
            for (int i = 0; i < HEADERS.length && i < row.size(); i++)
                json.put(HEADERS[i], row.get(i));
            return json;
        }).collect(Collectors.toList());
    }

    /**
     * @param isoTimeSeconds Docker容器创建时间戳（秒）
     * @return {@code String } 格式化后的时间
     *
     * @Description: 格式化时间
     */
    private static String formatTimestamp(Long isoTimeSeconds) {
        // Docker容器的时间戳是以秒为单位，需要转换为毫秒
        Instant instant = Instant.ofEpochSecond(isoTimeSeconds);
        // 将Instant对象转换为ZonedDateTime对象（UTC时区）
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"));
        // 转换为本地时区
        ZonedDateTime localZonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault());
        // 定义时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 格式化时间
        return localZonedDateTime.format(formatter);
    }

    private static String formattedTimeZone(String isoTimeMillis) {
        // 解析ISO 8601格式的字符串为Instant对象
        Instant instant = Instant.parse(isoTimeMillis);
        // 将Instant对象转换为ZonedDateTime对象（UTC时区）
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC"));
        // 转换为本地时区（如果需要）
        ZonedDateTime localZonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.systemDefault());
        // 定义时间格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 格式化时间
        return localZonedDateTime.format(formatter);
    }

    /**
     * @param bytes Docker容器资源占用比特
     * @return {@code String } 格式化后的资源占用
     *
     * @Description: 格式化资源占用
     */
    private static String formatBytes(long bytes) {
        double kb = bytes / 1024.0;
        double mb = kb / 1024.0;
        double gb = mb / 1024.0;
        if (gb >= 1)
            return String.format("%.3f GiB", gb);
        if (mb >= 1)
            return String.format("%.3f MiB", mb);
        if (kb >= 1)
            return String.format("%.3f kB", kb);
        return bytes + " B";
    }

    public static <T> T getSafe(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            return null;
        }
    }

}
