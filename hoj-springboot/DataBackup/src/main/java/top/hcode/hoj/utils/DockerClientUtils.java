package top.hcode.hoj.utils;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.concurrent.*;
import java.util.*;
import java.time.*;

import com.github.dockerjava.core.*;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.command.*;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;

import com.alibaba.druid.util.StringUtils;

public class DockerClientUtils {

    public DockerClient connect(String dockerHost, String dockerPort) {

        if (StringUtils.isEmpty(dockerHost)) {
            dockerHost = "localhost";
        }

        if (StringUtils.isEmpty(dockerPort)) {
            dockerPort = "2376";
        }

        String dockerDir = Constants.File.DOCKER_CERT_PATH.getPath();

        // 配置docker CLI的一些选项
        DefaultDockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .withDockerTlsVerify(true)
                .withDockerCertPath(dockerDir)
                .withDockerHost(String.format("tcp://%s:%s", dockerHost, dockerPort))
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

    /**
     * @param dockerClient
     *
     * @return { List<Container> }
     * @Description: 获取所有容器列表
     */
    public static List<Container> getAllContainers(DockerClient dockerClient) {
        return dockerClient.listContainersCmd().withShowAll(true).exec();
    }

    /**
     * @param dockerClient
     * @param containerId  容器ID
     *
     * @return { Container }
     * @Description: 获取单个容器内容
     */
    public static Map<String, String> getContainer(DockerClient dockerClient, String containerId) {
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
    }

    /**
     *
     * @param dockerClient
     * @param containerId  容器ID
     *
     * @return 是否成功启动容器
     * @Description: 启动容器
     */
    public static Boolean startContainer(DockerClient dockerClient, String containerId) {
        try {
            dockerClient.startContainerCmd(containerId).exec();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     *
     * @param dockerClient
     * @param containerId  容器ID
     *
     * @return 是否成功重启容器
     * @Description: 重启容器
     */
    public static Boolean restartContainer(DockerClient dockerClient, String containerId) {
        try {
            dockerClient.restartContainerCmd(containerId).exec();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     *
     * @param dockerClient
     * @param containerId  容器ID
     *
     * @return 是否成功停止容器
     * @Description: 停止容器
     */
    public static Boolean stopContainer(DockerClient dockerClient, String containerId) {
        try {
            dockerClient.stopContainerCmd(containerId).exec();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     *
     * @param dockerClient
     * @param containerId  容器ID
     *
     * @return 是否成功拉取镜像
     * @Description: 现有容器拉取镜像
     **/
    public static Boolean pullImage(DockerClient dockerClient, String containerId) {
        try {
            Map<String, String> map = getContainer(dockerClient, containerId);

            if (map != null) {
                String repository = map.get("image");
                dockerClient.pullImageCmd(repository);
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
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

    public static List<List<String>> getDockerContainerDetails(DockerClient dockerClient) {
        List<Container> containers = getAllContainers(dockerClient);

        List<List<String>> results = new ArrayList<>();

        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<List<String>>> futureList = new ArrayList<>();

        for (Container container : containers) {
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
                e.printStackTrace();
            }
        }

        executor.shutdown();

        // 补充元素
        for (List<String> row : results) {
            if (row.size() < 12) {
                row.addAll(Collections.nCopies(12 - row.size(), "--"));
            }
        }

        return results;
    }

}
