package top.hcode.hoj.manager.admin.system;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.text.UnicodeUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.system.oshi.OshiUtil;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.exception.NacosException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.dockerjava.api.DockerClient;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.config.NacosSwitchConfig;
import top.hcode.hoj.config.SwitchConfig;
import top.hcode.hoj.config.WebConfig;
import top.hcode.hoj.dao.common.FileEntityService;
import top.hcode.hoj.dao.judge.RemoteJudgeAccountEntityService;
import top.hcode.hoj.manager.email.EmailManager;
import top.hcode.hoj.pojo.dto.*;
import top.hcode.hoj.pojo.entity.common.File;
import top.hcode.hoj.pojo.entity.judge.RemoteJudgeAccount;
import top.hcode.hoj.pojo.vo.ConfigVO;
import top.hcode.hoj.utils.ConfigUtils;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.utils.DockerClientUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/9 21:50
 * @Description: 动态修改网站配置，获取后台服务状态及判题服务器的状态
 */

@Component
@RefreshScope
@Slf4j(topic = "hoj")
public class ConfigManager {
    @Autowired
    private ConfigVO configVo;

    @Autowired
    private EmailManager emailManager;

    @Autowired
    private FileEntityService fileEntityService;

    @Autowired
    private RemoteJudgeAccountEntityService remoteJudgeAccountEntityService;

    @Autowired
    private ConfigUtils configUtils;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DiscoveryClient discoveryClient;

    @Autowired
    private NacosSwitchConfig nacosSwitchConfig;

    @Value("${service-url.name}")
    private String judgeServiceName;

    @Value("${spring.application.name}")
    private String currentServiceName;

    @Value("${spring.cloud.nacos.url}")
    private String NACOS_URL;

    @Value("${spring.cloud.nacos.config.prefix}")
    private String prefix;

    @Value("${spring.profiles.active}")
    private String active;

    @Value("${spring.cloud.nacos.config.file-extension}")
    private String fileExtension;

    @Value("${spring.cloud.nacos.config.group}")
    private String GROUP;

    @Value("${spring.cloud.nacos.config.type}")
    private String TYPE;

    @Value("${spring.cloud.nacos.config.username}")
    private String nacosUsername;

    @Value("${spring.cloud.nacos.config.password}")
    private String nacosPassword;

    @Value("${backend-server-ip:172.17.0.1}")
    private String backendServerIp;

    /**
     * @MethodName getServiceInfo
     * @Params * @param null
     * @Description 获取当前服务的相关信息以及当前系统的cpu情况，内存使用情况
     * @Return CommonResult
     * @Since 2020/12/3
     */

    public JSONObject getServiceInfo() {

        JSONObject result = new JSONObject();

        List<ServiceInstance> serviceInstances = discoveryClient.getInstances(currentServiceName);

        // 获取nacos中心配置所在的机器环境
        String response = restTemplate.getForObject(NACOS_URL + "/nacos/v1/ns/operator/metrics", String.class);

        JSONObject jsonObject = JSONUtil.parseObj(response);
        // 获取当前数据后台所在机器环境
        int cores = OshiUtil.getCpuInfo().getCpuNum(); // 当前机器的cpu核数
        double cpuLoad = 100 - OshiUtil.getCpuInfo().getFree();
        String percentCpuLoad = String.format("%.2f", cpuLoad) + "%"; // 当前服务所在机器cpu使用率

        double totalVirtualMemory = OshiUtil.getMemory().getTotal(); // 当前服务所在机器总内存
        double freePhysicalMemorySize = OshiUtil.getMemory().getAvailable(); // 当前服务所在机器空闲内存
        double value = freePhysicalMemorySize / totalVirtualMemory;
        String percentMemoryLoad = String.format("%.2f", (1 - value) * 100) + "%"; // 当前服务所在机器内存使用率

        result.put("nacos", jsonObject);
        result.put("backupCores", cores);
        result.put("backupService", serviceInstances);
        result.put("backupPercentCpuLoad", percentCpuLoad);
        result.put("backupPercentMemoryLoad", percentMemoryLoad);
        return result;
    }

    public List<JSONObject> getJudgeServiceInfo() {
        List<JSONObject> serviceInfoList = new LinkedList<>();
        List<ServiceInstance> serviceInstances = discoveryClient.getInstances(judgeServiceName);
        for (ServiceInstance serviceInstance : serviceInstances) {
            try {
                String result = restTemplate.getForObject(serviceInstance.getUri() + "/get-sys-config", String.class);
                JSONObject jsonObject = JSONUtil.parseObj(result, false);
                jsonObject.put("service", serviceInstance);
                serviceInfoList.add(jsonObject);
            } catch (Exception e) {
                log.error("[Admin Dashboard] get judge service info error, uri={}, error={}", serviceInstance.getUri(),
                        e);
            }
        }

        // 对 serviceInfoList 按 judgeName 进行排序
        serviceInfoList.sort(
                Comparator.comparing(o -> o.getJSONObject("service").getJSONObject("metadata").getStr("judgeName")));

        return serviceInfoList;
    }

    public List<JSONObject> getDockerServiceInfo() throws StatusFailException {
        String[] headers = {
                "CONTAINER ID", "NAME", "IMAGE", "COMMAND", "CREATED", "STATUS",
                "PORTS", "CPU %", "MEM USAGE / LIMIT", "MEM %", "NET I/O", "BLOCK I/O"
        };

        try {
            DockerClient dockerclient = new DockerClientUtils().connect(backendServerIp, null);
            List<List<String>> containerDetails = DockerClientUtils.getDockerContainerDetails(dockerclient);

            // 定义状态优先级映射
            Map<String, Integer> statusPriority = new HashMap<>();
            statusPriority.put("Up", 1);
            statusPriority.put("Created", 2);
            statusPriority.put("Exited", 3);

            // 对容器详情按状态进行排序
            containerDetails.sort(Comparator.comparingInt(row -> {
                String status = row.size() > 5 ? row.get(5) : "";
                return statusPriority.entrySet().stream()
                        .filter(entry -> status.startsWith(entry.getKey()))
                        .map(Map.Entry::getValue)
                        .findFirst()
                        .orElse(Integer.MAX_VALUE);
            }));

            return containerDetails.stream().map(row -> {
                JSONObject json = new JSONObject();
                for (int i = 0; i < headers.length && i < row.size(); i++) {
                    json.put(headers[i], row.get(i));
                }
                return json;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("[Admin Dashboard] get docker service info error, uri={}, error={}", backendServerIp,
                    e.getMessage());
            throw new StatusFailException("获取容器详情失败！");
        }
    }

    public void setDockerServer(DockerConfigDTO config) throws StatusFailException {
        String containerId = config.getContainerId();
        String method = config.getMethod().toLowerCase();
        String serverIp = config.getServerIp();

        if (StringUtils.isEmpty(serverIp)) {
            serverIp = backendServerIp;
        }

        try {
            DockerClient dockerclient = new DockerClientUtils().connect(serverIp, null);

            Boolean isOk = false;

            if (method.equals("start")) {
                isOk = DockerClientUtils.startContainer(dockerclient, containerId);
            } else if (method.equals("stop")) {
                isOk = DockerClientUtils.stopContainer(dockerclient, containerId);
            } else if (method.equals("restart")) {
                isOk = DockerClientUtils.restartContainer(dockerclient, containerId);
            } else if (method.equals("pull")) {
                isOk = DockerClientUtils.pullImage(dockerclient, containerId);
            } else {
                throw new StatusFailException("未知的命令！");
            }

            if (!isOk) {
                throw new StatusFailException("操作失败！");
            }
        } catch (Exception e) {
            throw new StatusFailException("操作失败！");
        }
    }

    public WebConfigDTO getWebConfig() {
        WebConfig webConfig = nacosSwitchConfig.getWebConfig();
        WebConfigDTO webConfigDto = WebConfigDTO.builder()
                .baseUrl(UnicodeUtil.toString(webConfig.getBaseUrl()))
                .name(UnicodeUtil.toString(webConfig.getName()))
                .shortName(UnicodeUtil.toString(webConfig.getShortName()))
                .description(UnicodeUtil.toString(webConfig.getDescription()))
                .register(webConfig.getRegister())
                .duration(webConfig.getDuration())
                .domainInfo(webConfig.getDomainInfo())
                .recordName(UnicodeUtil.toString(webConfig.getRecordName()))
                .recordUrl(UnicodeUtil.toString(webConfig.getRecordUrl()))
                .projectName(UnicodeUtil.toString(webConfig.getProjectName()))
                .projectUrl(UnicodeUtil.toString(webConfig.getProjectUrl()))
                .build();

        webConfigDto.setRelatedByList(webConfig.getRelated());
        return webConfigDto;
    }

    public void deleteHomeCarousel(Long id) throws StatusFailException {

        File imgFile = fileEntityService.getById(id);
        if (imgFile == null) {
            throw new StatusFailException("文件id错误，图片不存在");
        }
        boolean isOk = fileEntityService.removeById(id);
        if (isOk) {
            FileUtil.del(new java.io.File(imgFile.getFilePath()));
        } else {
            throw new StatusFailException("删除失败！");
        }
    }

    public void editHomeCarousel(Long id, String addLink, String addHint) throws StatusFailException {

        File imgFile = fileEntityService.getById(id);
        if (imgFile == null) {
            throw new StatusFailException("文件id错误，图片不存在");
        }
        boolean isOk = fileEntityService.editHomeCarousel(id, addLink, addHint);
        if (!isOk) {
            throw new StatusFailException("更新失败！");
        }
    }

    public void editFileHint(Long id, String hint) throws StatusFailException, StatusForbiddenException {

        File file = fileEntityService.getById(id);
        if (file == null) {
            throw new StatusFailException("错误：文件不存在！");
        }
        if (!file.getType().equals("file")) {
            throw new StatusForbiddenException("错误：不支持更新！");
        }
        boolean isOk = fileEntityService.editFileHint(id, hint);
        if (!isOk) {
            throw new StatusFailException("更新失败！");
        }
    }

    public void setWebConfig(WebConfigDTO config) throws StatusFailException {

        WebConfig webConfig = nacosSwitchConfig.getWebConfig();

        if (!StringUtils.isEmpty(config.getBaseUrl())) {
            webConfig.setBaseUrl(config.getBaseUrl());
        }
        if (!StringUtils.isEmpty(config.getName())) {
            webConfig.setName(config.getName());
        }
        if (!StringUtils.isEmpty(config.getShortName())) {
            webConfig.setShortName(config.getShortName());
        }
        if (!StringUtils.isEmpty(config.getDescription())) {
            webConfig.setDescription(config.getDescription());
        }
        if (config.getRegister() != null) {
            webConfig.setRegister(config.getRegister());
        }
        if (!StringUtils.isEmpty(config.getDuration())) {
            webConfig.setDuration(config.getDuration());
        }
        if (!StringUtils.isEmpty(config.getDomainInfo())) {
            webConfig.setDomainInfo(config.getDomainInfo());
        }
        if (!StringUtils.isEmpty(config.getRecordName())) {
            webConfig.setRecordName(config.getRecordName());
        }
        if (!StringUtils.isEmpty(config.getRecordUrl())) {
            webConfig.setRecordUrl(config.getRecordUrl());
        }
        if (!StringUtils.isEmpty(config.getProjectName())) {
            webConfig.setProjectName(config.getProjectName());
        }
        if (!StringUtils.isEmpty(config.getProjectUrl())) {
            webConfig.setProjectUrl(config.getProjectUrl());
        }
        // 修改网站设置中的友情链接
        webConfig.setRelatedByList(config.getRelated());
        boolean isOk = nacosSwitchConfig.publishWebConfig();
        if (!isOk) {
            throw new StatusFailException("修改失败");
        }
    }

    public EmailConfigDTO getEmailConfig() {
        WebConfig webConfig = nacosSwitchConfig.getWebConfig();
        return EmailConfigDTO.builder()
                .emailUsername(webConfig.getEmailUsername())
                .emailPassword(webConfig.getEmailPassword())
                .emailHost(webConfig.getEmailHost())
                .emailPort(webConfig.getEmailPort())
                .emailBGImg(webConfig.getEmailBGImg())
                .emailSsl(webConfig.getEmailSsl())
                .build();
    }

    public HtmltopdfDTO getHtmltopdfConfig() {
        WebConfig webConfig = nacosSwitchConfig.getWebConfig();
        return HtmltopdfDTO.builder()
                .htmltopdfHost(webConfig.getHtmltopdfHost())
                .htmltopdfPort(webConfig.getHtmltopdfPort())
                .htmltopdfEc(webConfig.getHtmltopdfEc())
                .build();
    }

    public ClocDTO getClocConfig() {
        WebConfig webConfig = nacosSwitchConfig.getWebConfig();
        return ClocDTO.builder()
                .clochost(webConfig.getClochost())
                .clocport(webConfig.getClocport())
                .clocstartTime(webConfig.getClocstartTime())
                .build();
    }

    public void setEmailConfig(EmailConfigDTO config) throws StatusFailException {
        WebConfig webConfig = nacosSwitchConfig.getWebConfig();
        if (!StringUtils.isEmpty(config.getEmailHost())) {
            webConfig.setEmailHost(config.getEmailHost());
        }
        if (!StringUtils.isEmpty(config.getEmailPassword())) {
            webConfig.setEmailPassword(config.getEmailPassword());
        }

        if (config.getEmailPort() != null) {
            webConfig.setEmailPort(config.getEmailPort());
        }

        if (!StringUtils.isEmpty(config.getEmailUsername())) {
            webConfig.setEmailUsername(config.getEmailUsername());
        }

        if (!StringUtils.isEmpty(config.getEmailBGImg())) {
            webConfig.setEmailBGImg(config.getEmailBGImg());
        }

        if (config.getEmailSsl() != null) {
            webConfig.setEmailSsl(config.getEmailSsl());
        }

        boolean isOk = nacosSwitchConfig.publishWebConfig();
        if (!isOk) {
            throw new StatusFailException("修改失败");
        }
    }

    public void setHtmltopdfConfig(HtmltopdfDTO config) throws StatusFailException {
        WebConfig webConfig = nacosSwitchConfig.getWebConfig();
        if (!StringUtils.isEmpty(config.getHtmltopdfHost())) {
            webConfig.setHtmltopdfHost(config.getHtmltopdfHost());
        }
        if (config.getHtmltopdfPort() != null) {
            webConfig.setHtmltopdfPort(config.getHtmltopdfPort());
        }
        if (config.getHtmltopdfEc() != null) {
            webConfig.setHtmltopdfEc(config.getHtmltopdfEc());
        }
        boolean isOk = nacosSwitchConfig.publishWebConfig();
        if (!isOk) {
            throw new StatusFailException("修改失败");
        }
    }

    public void setClocConfig(ClocDTO config) throws StatusFailException {
        WebConfig webConfig = nacosSwitchConfig.getWebConfig();
        if (!StringUtils.isEmpty(config.getClochost())) {
            webConfig.setClochost(config.getClochost());
        }
        if (config.getClocport() != null) {
            webConfig.setClocport(config.getClocport());
        }
        if (!StringUtils.isEmpty(config.getClocstartTime())) {
            webConfig.setClocstartTime(config.getClocstartTime());
        }

        boolean isOk = nacosSwitchConfig.publishWebConfig();
        if (!isOk) {
            throw new StatusFailException("修改失败");
        }
    }

    public void testEmail(TestEmailDTO testEmailDto) throws StatusFailException {
        String email = testEmailDto.getEmail();
        if (StringUtils.isEmpty(email)) {
            throw new StatusFailException("测试的邮箱不能为空！");
        }
        boolean isEmail = Validator.isEmail(email);
        if (isEmail) {
            emailManager.testEmail(email);
        } else {
            throw new StatusFailException("测试的邮箱格式不正确！");
        }
    }

    public DBAndRedisConfigDTO getDBAndRedisConfig() {
        return DBAndRedisConfigDTO.builder()
                .dbName(configVo.getMysqlDBName())
                .dbHost(configVo.getMysqlHost())
                .dbPort(configVo.getMysqlPort())
                .dbUsername(configVo.getMysqlUsername())
                .dbPassword(configVo.getMysqlPassword())
                .redisHost(configVo.getRedisHost())
                .redisPort(configVo.getRedisPort())
                .redisPassword(configVo.getRedisPassword())
                .build();
    }

    public void setDBAndRedisConfig(DBAndRedisConfigDTO config) throws StatusFailException {

        if (!StringUtils.isEmpty(config.getDbName())) {
            configVo.setMysqlDBName(config.getDbName());
        }

        if (!StringUtils.isEmpty(config.getDbHost())) {
            configVo.setMysqlHost(config.getDbHost());
        }
        if (config.getDbPort() != null) {
            configVo.setMysqlPort(config.getDbPort());
        }
        if (!StringUtils.isEmpty(config.getDbUsername())) {
            configVo.setMysqlUsername(config.getDbUsername());
        }
        if (!StringUtils.isEmpty(config.getDbPassword())) {
            configVo.setMysqlPassword(config.getDbPassword());
        }

        if (!StringUtils.isEmpty(config.getRedisHost())) {
            configVo.setRedisHost(config.getRedisHost());
        }

        if (config.getRedisPort() != null) {
            configVo.setRedisPort(config.getRedisPort());
        }
        if (!StringUtils.isEmpty(config.getRedisPassword())) {
            configVo.setRedisPassword(config.getRedisPassword());
        }

        boolean isOk = sendNewConfigToNacos();

        if (!isOk) {
            throw new StatusFailException("修改失败");
        }
    }

    public SwitchConfigDTO getSwitchConfig() {
        SwitchConfig switchConfig = nacosSwitchConfig.getSwitchConfig();
        SwitchConfigDTO switchConfigDTO = new SwitchConfigDTO();
        BeanUtil.copyProperties(switchConfig, switchConfigDTO);
        return switchConfigDTO;
    }

    public void setSwitchConfig(SwitchConfigDTO config) throws StatusFailException {

        SwitchConfig switchConfig = nacosSwitchConfig.getSwitchConfig();

        if (config.getScpcSuperAdminAccount() != null) {
            switchConfig.setScpcSuperAdminAccount(config.getScpcSuperAdminAccount());
        }
        if (config.getScpcSuperAdminPassword() != null) {
            switchConfig.setScpcSuperAdminPassword(config.getScpcSuperAdminPassword());
        }
        if (config.getOpenPublicDiscussion() != null) {
            switchConfig.setOpenPublicDiscussion(config.getOpenPublicDiscussion());
        }
        if (config.getOpenGroupDiscussion() != null) {
            switchConfig.setOpenGroupDiscussion(config.getOpenGroupDiscussion());
        }
        if (config.getOpenContestComment() != null) {
            switchConfig.setOpenContestComment(config.getOpenContestComment());
        }
        if (config.getOpenPublicJudge() != null) {
            switchConfig.setOpenPublicJudge(config.getOpenPublicJudge());
        }
        if (config.getOpenGroupJudge() != null) {
            switchConfig.setOpenGroupJudge(config.getOpenGroupJudge());
        }
        if (config.getOpenContestJudge() != null) {
            switchConfig.setOpenContestJudge(config.getOpenContestJudge());
        }

        if (config.getHideNonContestSubmissionCode() != null) {
            switchConfig.setHideNonContestSubmissionCode(config.getHideNonContestSubmissionCode());
        }

        if (config.getDefaultCreateDiscussionACInitValue() != null) {
            switchConfig.setDefaultCreateDiscussionACInitValue(config.getDefaultCreateDiscussionACInitValue());
        }

        if (config.getDefaultCreateDiscussionDailyLimit() != null) {
            switchConfig.setDefaultCreateDiscussionDailyLimit(config.getDefaultCreateDiscussionDailyLimit());
        }

        if (config.getDefaultCreateCommentACInitValue() != null) {
            switchConfig.setDefaultCreateCommentACInitValue(config.getDefaultCreateCommentACInitValue());
        }

        if (config.getDefaultSubmitInterval() != null) {
            if (config.getDefaultSubmitInterval() >= 0) {
                switchConfig.setDefaultSubmitInterval(config.getDefaultSubmitInterval());
            } else {
                switchConfig.setDefaultSubmitInterval(0);
            }
        }

        if (config.getDefaultCreateGroupACInitValue() != null) {
            switchConfig.setDefaultCreateGroupACInitValue(config.getDefaultCreateGroupACInitValue());
        }

        if (config.getDefaultCreateGroupDailyLimit() != null) {
            switchConfig.setDefaultCreateGroupDailyLimit(config.getDefaultCreateGroupDailyLimit());
        }

        if (config.getDefaultCreateGroupLimit() != null) {
            switchConfig.setDefaultCreateGroupLimit(config.getDefaultCreateGroupLimit());
        }

        if (checkListDiff(config.getCfUsernameList(), switchConfig.getCfUsernameList()) ||
                checkListDiff(config.getCfPasswordList(), switchConfig.getCfPasswordList()) ||
                checkListDiff2(config.getCfAliveList(), switchConfig.getCfAliveList())) {
            switchConfig.setCfUsernameList(config.getCfUsernameList());
            switchConfig.setCfPasswordList(config.getCfPasswordList());
            switchConfig.setCfAliveList(config.getCfAliveList());
            changeRemoteJudgeAccount2(config.getCfUsernameList(),
                    config.getCfPasswordList(),
                    config.getCfAliveList(),
                    null, null,
                    Constants.RemoteOJ.CODEFORCES.getName());
        }

        if (checkListDiff(config.getHduUsernameList(), switchConfig.getHduUsernameList()) ||
                checkListDiff(config.getHduPasswordList(), switchConfig.getHduPasswordList())) {
            switchConfig.setHduUsernameList(config.getHduUsernameList());
            switchConfig.setHduPasswordList(config.getHduPasswordList());
            changeRemoteJudgeAccount(config.getHduUsernameList(),
                    config.getHduPasswordList(),
                    Constants.RemoteOJ.HDU.getName());
        }

        if (checkListDiff(config.getPojUsernameList(), switchConfig.getPojUsernameList()) ||
                checkListDiff(config.getPojPasswordList(), switchConfig.getPojPasswordList())) {
            switchConfig.setPojUsernameList(config.getPojUsernameList());
            switchConfig.setPojPasswordList(config.getPojPasswordList());
            changeRemoteJudgeAccount(config.getPojUsernameList(),
                    config.getPojPasswordList(),
                    Constants.RemoteOJ.POJ.getName());
        }

        if (checkListDiff(config.getSpojUsernameList(), switchConfig.getSpojUsernameList()) ||
                checkListDiff(config.getSpojPasswordList(), switchConfig.getSpojPasswordList())) {
            switchConfig.setSpojUsernameList(config.getSpojUsernameList());
            switchConfig.setSpojPasswordList(config.getSpojPasswordList());
            changeRemoteJudgeAccount(config.getSpojUsernameList(),
                    config.getSpojPasswordList(),
                    Constants.RemoteOJ.SPOJ.getName());
        }

        if (checkListDiff(config.getAtcoderUsernameList(), switchConfig.getAtcoderUsernameList()) ||
                checkListDiff(config.getAtcoderPasswordList(), switchConfig.getAtcoderPasswordList())) {
            switchConfig.setAtcoderUsernameList(config.getAtcoderUsernameList());
            switchConfig.setAtcoderPasswordList(config.getAtcoderPasswordList());
            changeRemoteJudgeAccount(config.getAtcoderUsernameList(),
                    config.getAtcoderPasswordList(),
                    Constants.RemoteOJ.ATCODER.getName());
        }
        if (checkListDiff(config.getScpcUsernameList(), switchConfig.getScpcUsernameList()) ||
                checkListDiff(config.getScpcPasswordList(), switchConfig.getScpcPasswordList())) {
            switchConfig.setScpcUsernameList(config.getScpcUsernameList());
            switchConfig.setScpcPasswordList(config.getScpcPasswordList());
            changeRemoteJudgeAccount(config.getScpcUsernameList(),
                    config.getScpcPasswordList(),
                    Constants.RemoteOJ.SCPC.getName());
        }
        if (checkListDiff(config.getQojUsernameList(), switchConfig.getQojUsernameList()) ||
                checkListDiff(config.getQojPasswordList(), switchConfig.getQojPasswordList())) {
            switchConfig.setQojUsernameList(config.getQojUsernameList());
            switchConfig.setQojPasswordList(config.getQojPasswordList());
            changeRemoteJudgeAccount(config.getQojUsernameList(),
                    config.getQojPasswordList(),
                    Constants.RemoteOJ.QOJ.getName());
        }
        if (checkListDiff(config.getMossUsernameList(), switchConfig.getMossUsernameList())) {
            switchConfig.setMossUsernameList(config.getMossUsernameList());
            changeRemoteJudgeAccount(config.getMossUsernameList(), null,
                    Constants.RemoteOJ.MOSS.getName());
        }

        if (checkListDiff(config.getLibreojUsernameList(), switchConfig.getLibreojUsernameList()) ||
                checkListDiff(config.getLibreojPasswordList(), switchConfig.getLibreojPasswordList())) {
            switchConfig.setLibreojUsernameList(config.getLibreojUsernameList());
            switchConfig.setLibreojPasswordList(config.getLibreojPasswordList());
            changeRemoteJudgeAccount(config.getLibreojUsernameList(),
                    config.getLibreojPasswordList(),
                    Constants.RemoteOJ.LIBRE.getName());
        }

        if (checkListDiff(config.getNswojUsernameList(), switchConfig.getNswojUsernameList()) ||
                checkListDiff(config.getNswojPasswordList(), switchConfig.getNswojPasswordList())) {
            switchConfig.setNswojUsernameList(config.getNswojUsernameList());
            switchConfig.setNswojPasswordList(config.getNswojPasswordList());
            changeRemoteJudgeAccount(config.getNswojUsernameList(),
                    config.getNswojPasswordList(),
                    Constants.RemoteOJ.NSWOJ.getName());
        }

        if (checkListDiff(config.getNewojUsernameList(), switchConfig.getNewojUsernameList()) ||
                checkListDiff(config.getNewojPasswordList(), switchConfig.getNewojPasswordList())) {
            switchConfig.setNewojUsernameList(config.getNewojUsernameList());
            switchConfig.setNewojPasswordList(config.getNewojPasswordList());
            changeRemoteJudgeAccount(config.getNewojUsernameList(),
                    config.getNewojPasswordList(),
                    Constants.RemoteOJ.NEWOJ.getName());
        }

        if (checkListDiff(config.getVjUsernameList(), switchConfig.getVjUsernameList()) ||
                checkListDiff(config.getVjPasswordList(), switchConfig.getVjPasswordList()) ||
                checkListDiff2(config.getVjAliveList(), switchConfig.getVjAliveList())) {
            switchConfig.setVjUsernameList(config.getVjUsernameList());
            switchConfig.setVjPasswordList(config.getVjPasswordList());
            switchConfig.setVjAliveList(config.getVjAliveList());
            changeRemoteJudgeAccount2(config.getVjUsernameList(),
                    config.getVjPasswordList(),
                    config.getVjAliveList(),
                    null, null,
                    Constants.RemoteOJ.VJ.getName());
        }

        if (checkListDiff(config.getDotcppUsernameList(), switchConfig.getDotcppUsernameList()) ||
                checkListDiff(config.getDotcppPasswordList(), switchConfig.getDotcppPasswordList())) {
            switchConfig.setDotcppUsernameList(config.getDotcppUsernameList());
            switchConfig.setDotcppPasswordList(config.getDotcppPasswordList());
            changeRemoteJudgeAccount(config.getDotcppUsernameList(),
                    config.getDotcppPasswordList(),
                    Constants.RemoteOJ.DOTCPP.getName());
        }

        if (checkListDiff(config.getNowcoderUsernameList(), switchConfig.getNowcoderUsernameList()) ||
                checkListDiff(config.getNowcoderPasswordList(), switchConfig.getNowcoderPasswordList()) ||
                checkListDiff2(config.getNowcoderAliveList(), switchConfig.getNowcoderAliveList()) ||
                checkListDiff(config.getNowcoderTitleList(), switchConfig.getNowcoderTitleList()) ||
                checkListDiff(config.getNowcoderLinkList(), switchConfig.getNowcoderLinkList())) {
            switchConfig.setNowcoderUsernameList(config.getNowcoderUsernameList());
            switchConfig.setNowcoderPasswordList(config.getNowcoderPasswordList());
            switchConfig.setNowcoderAliveList(config.getNowcoderAliveList());
            switchConfig.setNowcoderTitleList(config.getNowcoderTitleList());
            switchConfig.setNowcoderLinkList(config.getNowcoderLinkList());
            changeRemoteJudgeAccount2(config.getNowcoderUsernameList(),
                    config.getNowcoderPasswordList(),
                    config.getNowcoderAliveList(),
                    config.getNowcoderTitleList(),
                    config.getNowcoderLinkList(),
                    Constants.RemoteOJ.NOWCODER.getName());
        }

        if (checkListDiff(config.getAcwingUsernameList(), switchConfig.getAcwingUsernameList()) ||
                checkListDiff(config.getAcwingPasswordList(), switchConfig.getAcwingPasswordList()) ||
                checkListDiff2(config.getAcwingAliveList(), switchConfig.getAcwingAliveList()) ||
                checkListDiff(config.getAcwingTitleList(), switchConfig.getAcwingTitleList()) ||
                checkListDiff(config.getAcwingLinkList(), switchConfig.getAcwingLinkList())) {
            switchConfig.setAcwingUsernameList(config.getAcwingUsernameList());
            switchConfig.setAcwingPasswordList(config.getAcwingPasswordList());
            switchConfig.setAcwingAliveList(config.getAcwingAliveList());
            switchConfig.setAcwingTitleList(config.getAcwingTitleList());
            switchConfig.setAcwingLinkList(config.getAcwingLinkList());
            changeRemoteJudgeAccount2(config.getAcwingUsernameList(),
                    config.getAcwingPasswordList(),
                    config.getAcwingAliveList(),
                    config.getAcwingTitleList(),
                    config.getAcwingLinkList(),
                    Constants.RemoteOJ.ACWING.getName());
        }

        boolean isOk = nacosSwitchConfig.publishSwitchConfig();
        if (!isOk) {
            throw new StatusFailException("修改失败");
        }
    }

    private boolean checkListDiff(List<String> list1, List<String> list2) {
        if (list1.size() != list2.size()) {
            return true;
        }
        return !list1.toString().equals(list2.toString());
    }

    private boolean checkListDiff2(List<Boolean> list1, List<Boolean> list2) {
        if (list1.size() != list2.size()) {
            return true;
        }
        return !list1.equals(list2);
    }

    private void changeRemoteJudgeAccount(List<String> usernameList,
            List<String> passwordList,
            String oj) {

        if (oj.equals(Constants.RemoteOJ.MOSS.getName())) {
            if (CollectionUtils.isEmpty(usernameList)) {
                log.error("[Change by Switch] [{}]: There is no account or password configured for remote judge, " +
                        "username list:{}", oj, Arrays.toString(usernameList.toArray()));
            }
            // Moss 账号即是密码
            passwordList = new ArrayList<>(Collections.nCopies(usernameList.size(), " "));
        } else {
            if (CollectionUtils.isEmpty(usernameList) || CollectionUtils.isEmpty(passwordList)
                    || usernameList.size() != passwordList.size()) {
                log.error("[Change by Switch] [{}]: There is no account or password configured for remote judge, " +
                        "username list:{}, password list:{}", oj, Arrays.toString(usernameList.toArray()),
                        Arrays.toString(passwordList.toArray()));
            }
        }

        QueryWrapper<RemoteJudgeAccount> remoteJudgeAccountQueryWrapper = new QueryWrapper<>();
        remoteJudgeAccountQueryWrapper.eq("oj", oj);
        remoteJudgeAccountEntityService.remove(remoteJudgeAccountQueryWrapper);

        List<RemoteJudgeAccount> newRemoteJudgeAccountList = new ArrayList<>();

        for (int i = 0; i < usernameList.size(); i++) {
            newRemoteJudgeAccountList.add(new RemoteJudgeAccount()
                    .setUsername(usernameList.get(i))
                    .setPassword(passwordList.get(i))
                    .setStatus(true)
                    .setVersion(0L)
                    .setOj(oj));
        }

        if (newRemoteJudgeAccountList.size() > 0) {
            boolean addOk = remoteJudgeAccountEntityService.saveOrUpdateBatch(newRemoteJudgeAccountList);
            if (!addOk) {
                log.error(
                        "Remote judge initialization failed. Failed to add account for: [{}]. Please check the configuration file and restart!",
                        oj);
            }
        }
    }

    private void changeRemoteJudgeAccount2(List<String> usernameList, List<String> passwordList,
            List<Boolean> aliveList, List<String> titleList, List<String> linkList, String oj) {

        if (CollectionUtils.isEmpty(usernameList) || CollectionUtils.isEmpty(passwordList)
                || CollectionUtils.isEmpty(aliveList)
                || usernameList.size() != passwordList.size() || usernameList.size() != aliveList.size()) {
            log.error("[Change by Switch] [{}]: There is no account or password or alive configured for cookie, " +
                    "username list:{}, password list:{}, alive list:{}", oj, Arrays.toString(usernameList.toArray()),
                    Arrays.toString(passwordList.toArray()), Arrays.toString(aliveList.toArray()));
        }

        QueryWrapper<RemoteJudgeAccount> remoteJudgeAccountQueryWrapper = new QueryWrapper<>();
        remoteJudgeAccountQueryWrapper.eq("oj", oj);
        remoteJudgeAccountEntityService.remove(remoteJudgeAccountQueryWrapper);

        List<RemoteJudgeAccount> newRemoteJudgeAccountList = new ArrayList<>();

        boolean hasTitlesAndLinks = !CollectionUtils.isEmpty(titleList) && !CollectionUtils.isEmpty(linkList)
                && titleList.size() == linkList.size();

        for (int i = 0; i < usernameList.size(); i++) {
            RemoteJudgeAccount account = new RemoteJudgeAccount()
                    .setUsername(usernameList.get(i))
                    .setPassword(passwordList.get(i))
                    .setIsAlive(aliveList.get(i))
                    .setStatus(true)
                    .setVersion(0L)
                    .setOj(oj);

            if (hasTitlesAndLinks) {
                account.setTitle(titleList.get(i)).setLink(linkList.get(i));
            }

            newRemoteJudgeAccountList.add(account);
        }

        if (newRemoteJudgeAccountList.size() > 0) {
            boolean addOk = remoteJudgeAccountEntityService.saveOrUpdateBatch(newRemoteJudgeAccountList);
            if (!addOk) {
                log.error(
                        "Remote judge initialization failed. Failed to add account for: [{}]. Please check the configuration file and restart!",
                        oj);
            }
        }
    }

    public boolean sendNewConfigToNacos() {

        Properties properties = new Properties();
        properties.put("serverAddr", NACOS_URL);

        // if need username and password to login
        properties.put("username", nacosUsername);
        properties.put("password", nacosPassword);

        com.alibaba.nacos.api.config.ConfigService configService = null;
        boolean isOK = false;
        try {
            configService = NacosFactory.createConfigService(properties);
            isOK = configService.publishConfig(prefix + "-" + active + "." + fileExtension, GROUP,
                    configUtils.getConfigContent(), TYPE);
        } catch (NacosException e) {
            log.error("通过nacos修改网站配置异常--------------->{}", e.getMessage());
        }
        return isOK;
    }
}