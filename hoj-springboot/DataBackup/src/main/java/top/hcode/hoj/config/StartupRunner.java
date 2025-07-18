package top.hcode.hoj.config;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import top.hcode.hoj.crawler.language.LanguageContext;
import top.hcode.hoj.dao.judge.RemoteJudgeAccountEntityService;
import top.hcode.hoj.dao.problem.LanguageEntityService;
import top.hcode.hoj.dao.problem.ProblemEntityService;
import top.hcode.hoj.dao.problem.ProblemLanguageEntityService;
import top.hcode.hoj.manager.admin.system.ConfigManager;
import top.hcode.hoj.pojo.entity.judge.RemoteJudgeAccount;
import top.hcode.hoj.pojo.entity.problem.Language;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.problem.ProblemLanguage;
import top.hcode.hoj.pojo.vo.ConfigVO;
import top.hcode.hoj.utils.Constants;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: Himit_ZH
 * @Date: 2021/2/19 22:11
 * @Description:项目启动后，初始化运行该run方法
 */
@Component
@Slf4j(topic = "hoj")
public class StartupRunner implements CommandLineRunner {

    @Autowired
    private ConfigVO configVo;

    @Autowired
    private ConfigManager configManager;

    @Autowired
    private NacosSwitchConfig nacosSwitchConfig;

    @Autowired
    private RemoteJudgeAccountEntityService remoteJudgeAccountEntityService;

    @Autowired
    private LanguageEntityService languageEntityService;

    @Autowired
    private ProblemEntityService problemEntityService;

    @Autowired
    private ProblemLanguageEntityService problemLanguageEntityService;

    @Value("${open-remote-judge}")
    private String openRemoteJudge;

    // jwt配置
    @Value("${jwt-token-secret}")
    private String tokenSecret;

    @Value("${jwt-token-expire:86400}")
    private String tokenExpire;

    @Value("${jwt-token-fresh-expire:43200}")
    private String checkRefreshExpire;

    // 数据库配置
    @Value("${mysql-username:root}")
    private String mysqlUsername;

    @Value("${mysql-password:hoj123456}")
    private String mysqlPassword;

    @Value("${mysql-name:hoj}")
    private String mysqlDBName;

    @Value("${mysql-host:172.20.0.3}")
    private String mysqlHost;

    @Value("${mysql-public-host:172.20.0.3}")
    private String mysqlPublicHost;

    @Value("${mysql-port:3306}")
    private Integer mysqlPort;

    @Value("${mysql-public-port:3306}")
    private Integer mysqlPublicPort;

    // 缓存配置
    @Value("${redis-host:172.20.0.2}")
    private String redisHost;

    @Value("${redis-port:6379}")
    private Integer redisPort;

    @Value("${redis-password:hoj123456}")
    private String redisPassword;
    // 判题服务token
    @Value("${judge-token}")
    private String judgeToken;

    // 邮箱配置
    @Value("${email-username}")
    private String emailUsername;

    @Value("${email-password}")
    private String emailPassword;

    @Value("${email-host}")
    private String emailHost;

    @Value("${email-port}")
    private Integer emailPort;

    // htmltopdf配置
    @Value("${htmltopdf-host}")
    private String htmltopdfHost;

    @Value("${htmltopdf-port}")
    private Integer htmltopdfPort;

    @Value("${htmltopdf-ec}")
    private Boolean htmltopdfEc;

    // cloc配置
    @Value("${cloc-host}")
    private String clocHost;

    @Value("${cloc-port}")
    private Integer clocPort;

    @Value("${cloc-start-time}")
    private String clocstartTime;

    @Value("${hdu-username-list}")
    private List<String> hduUsernameList;

    @Value("${hdu-password-list}")
    private List<String> hduPasswordList;

    @Value("${cf-username-list}")
    private List<String> cfUsernameList;

    @Value("${cf-password-list}")
    private List<String> cfPasswordList;

    @Value("${poj-username-list}")
    private List<String> pojUsernameList;

    @Value("${poj-password-list}")
    private List<String> pojPasswordList;

    @Value("${atcoder-username-list}")
    private List<String> atcoderUsernameList;

    @Value("${atcoder-password-list}")
    private List<String> atcoderPasswordList;

    @Value("${spoj-username-list}")
    private List<String> spojUsernameList;

    @Value("${spoj-password-list}")
    private List<String> spojPasswordList;

    @Value("${libreoj-username-list}")
    private List<String> libreojUsernameList;

    @Value("${libreoj-password-list}")
    private List<String> libreojPasswordList;

    @Value("${scpc-username-list}")
    private List<String> scpcUsernameList;

    @Value("${scpc-password-list}")
    private List<String> scpcPasswordList;

    @Value("${qoj-username-list}")
    private List<String> qojUsernameList;

    @Value("${qoj-password-list}")
    private List<String> qojPasswordList;

    @Value("${nswoj-username-list}")
    private List<String> nswojUsernameList;

    @Value("${nswoj-password-list}")
    private List<String> nswojPasswordList;

    @Value("${newoj-username-list}")
    private List<String> newojUsernameList;

    @Value("${newoj-password-list}")
    private List<String> newojPasswordList;

    @Value("${vj-username-list}")
    private List<String> vjUsernameList;

    @Value("${vj-password-list}")
    private List<String> vjPasswordList;

    @Value("${dotcpp-username-list}")
    private List<String> dotcppUsernameList;

    @Value("${dotcpp-password-list}")
    private List<String> dotcppPasswordList;

    @Value("${nowcoder-username-list}")
    private List<String> nowcoderUsernameList;

    @Value("${nowcoder-password-list}")
    private List<String> nowcoderPasswordList;

    @Value("${acwing-username-list}")
    private List<String> acwingUsernameList;

    @Value("${acwing-password-list}")
    private List<String> acwingPasswordList;

    @Value("${moss-username-list}")
    private List<String> mossUsernameList;

    @Value("${forced-update-remote-judge-account}")
    private Boolean forcedUpdateRemoteJudgeAccount;

    @Resource
    private CheckLanguageConfig checkLanguageConfig;

    @Override
    public void run(String... args) throws Exception {

        // 修改nacos上的默认、web、switch、wkhtmltopdf、cloc配置文件
        initDefaultConfig();

        initWebConfig();

        initSwitchConfig();

        initWKHTMLTOPDFConfig();

        initCLOCConfig();

        upsertHOJLanguageV2();
        // upsertHOJLanguage("PHP", "PyPy2", "PyPy3", "JavaScript Node", "JavaScript
        // V8");
        // checkAllLanguageUpdate();

        checkLanguageUpdate();

        upsertHOJLanguageV3();

        upsertHOJLanguageV4();

        upsertHOJLanguageV5();

        upsertHOJLanguageV6();

        upsertHOJLanguageV7();
    }

    /**
     * 更新修改基础的配置
     */
    private void initDefaultConfig() {
        if (judgeToken.equals("default")) {
            configVo.setJudgeToken(IdUtil.fastSimpleUUID());
        } else {
            configVo.setJudgeToken(judgeToken);
        }

        if (tokenSecret.equals("default")) {
            if (StrUtil.isBlank(configVo.getTokenSecret())) {
                configVo.setTokenSecret(IdUtil.fastSimpleUUID());
            }
        } else {
            configVo.setTokenSecret(tokenSecret);
        }
        configVo.setTokenExpire(tokenExpire);
        configVo.setCheckRefreshExpire(checkRefreshExpire);

        configVo.setMysqlUsername(mysqlUsername);
        configVo.setMysqlPassword(mysqlPassword);
        configVo.setMysqlHost(mysqlHost);
        configVo.setMysqlPublicHost(mysqlPublicHost);
        configVo.setMysqlPort(mysqlPort);
        configVo.setMysqlPublicPort(mysqlPublicPort);
        configVo.setMysqlDBName(mysqlDBName);

        configVo.setRedisHost(redisHost);
        configVo.setRedisPort(redisPort);
        configVo.setRedisPassword(redisPassword);

        configManager.sendNewConfigToNacos();
    }

    private void initWebConfig() {
        WebConfig webConfig = nacosSwitchConfig.getWebConfig();
        boolean isChanged = false;
        if (!Objects.equals(webConfig.getEmailHost(), emailHost)
                && (webConfig.getEmailHost() == null || !"your_email_host".equals(emailHost))) {
            webConfig.setEmailHost(emailHost);
            isChanged = true;
        }
        if (!Objects.equals(webConfig.getEmailPort(), emailPort)
                && (webConfig.getEmailPort() == null || emailPort != 456)) {
            webConfig.setEmailPort(emailPort);
            isChanged = true;
        }
        if (!Objects.equals(webConfig.getEmailUsername(), emailUsername)
                && (webConfig.getEmailUsername() == null || !"your_email_username".equals(emailUsername))) {
            webConfig.setEmailUsername(emailUsername);
            isChanged = true;
        }
        if (!Objects.equals(webConfig.getEmailPassword(), emailPassword)
                && (webConfig.getEmailPassword() == null || !"your_email_password".equals(emailPassword))) {
            webConfig.setEmailPassword(emailPassword);
            isChanged = true;
        }
        if (isChanged) {
            nacosSwitchConfig.publishWebConfig();
        }
    }

    private void initSwitchConfig() {

        SwitchConfig switchConfig = nacosSwitchConfig.getSwitchConfig();

        boolean isChanged = false;
        if ((CollectionUtils.isEmpty(switchConfig.getHduUsernameList())
                && !CollectionUtils.isEmpty(hduUsernameList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setHduUsernameList(hduUsernameList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getHduPasswordList())
                && !CollectionUtils.isEmpty(hduPasswordList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setHduPasswordList(hduPasswordList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getCfUsernameList())
                && !CollectionUtils.isEmpty(cfUsernameList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setCfUsernameList(cfUsernameList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getCfPasswordList())
                && !CollectionUtils.isEmpty(cfPasswordList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setCfPasswordList(cfPasswordList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getPojUsernameList())
                && !CollectionUtils.isEmpty(pojUsernameList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setPojUsernameList(pojUsernameList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getPojPasswordList())
                && !CollectionUtils.isEmpty(pojPasswordList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setPojPasswordList(pojPasswordList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getAtcoderUsernameList())
                && !CollectionUtils.isEmpty(atcoderUsernameList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setAtcoderUsernameList(atcoderUsernameList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getAtcoderPasswordList())
                && !CollectionUtils.isEmpty(atcoderPasswordList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setAtcoderPasswordList(atcoderPasswordList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getSpojUsernameList())
                && !CollectionUtils.isEmpty(spojUsernameList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setSpojUsernameList(spojUsernameList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getSpojPasswordList())
                && !CollectionUtils.isEmpty(spojPasswordList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setSpojPasswordList(spojPasswordList);
            isChanged = true;
        }
        if ((CollectionUtils.isEmpty(switchConfig.getScpcUsernameList())
                && !CollectionUtils.isEmpty(scpcUsernameList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setSpojUsernameList(scpcUsernameList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getScpcPasswordList())
                && !CollectionUtils.isEmpty(scpcPasswordList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setSpojPasswordList(scpcPasswordList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getQojUsernameList())
                && !CollectionUtils.isEmpty(qojUsernameList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setSpojUsernameList(qojUsernameList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getQojPasswordList())
                && !CollectionUtils.isEmpty(qojPasswordList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setSpojPasswordList(qojPasswordList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getNswojUsernameList())
                && !CollectionUtils.isEmpty(nswojUsernameList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setSpojUsernameList(nswojUsernameList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getNswojPasswordList())
                && !CollectionUtils.isEmpty(nswojPasswordList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setSpojPasswordList(nswojPasswordList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getLibreojUsernameList())
                && !CollectionUtils.isEmpty(libreojUsernameList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setLibreojUsernameList(libreojUsernameList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getNewojUsernameList())
                && !CollectionUtils.isEmpty(newojUsernameList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setNewojUsernameList(newojUsernameList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getNewojPasswordList())
                && !CollectionUtils.isEmpty(newojPasswordList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setNewojPasswordList(newojPasswordList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getLibreojPasswordList())
                && !CollectionUtils.isEmpty(libreojPasswordList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setLibreojPasswordList(libreojPasswordList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getVjUsernameList())
                && !CollectionUtils.isEmpty(vjUsernameList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setVjUsernameList(vjUsernameList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getVjPasswordList())
                && !CollectionUtils.isEmpty(vjPasswordList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setVjPasswordList(vjPasswordList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getDotcppUsernameList())
                && !CollectionUtils.isEmpty(dotcppUsernameList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setDotcppUsernameList(dotcppUsernameList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getDotcppPasswordList())
                && !CollectionUtils.isEmpty(dotcppPasswordList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setDotcppPasswordList(dotcppPasswordList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getNowcoderUsernameList())
                && !CollectionUtils.isEmpty(nowcoderUsernameList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setNowcoderUsernameList(nowcoderUsernameList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getNowcoderPasswordList())
                && !CollectionUtils.isEmpty(nowcoderPasswordList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setNowcoderPasswordList(nowcoderPasswordList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getAcwingUsernameList())
                && !CollectionUtils.isEmpty(acwingUsernameList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setAcwingUsernameList(acwingUsernameList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getAcwingPasswordList())
                && !CollectionUtils.isEmpty(acwingPasswordList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setAcwingPasswordList(acwingPasswordList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getMossUsernameList())
                && !CollectionUtils.isEmpty(mossUsernameList))
                || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setMossUsernameList(mossUsernameList);
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getVjAliveList())) || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setVjAliveList(
                    new ArrayList<>(Collections.nCopies(switchConfig.getVjUsernameList().size(), false)));
            isChanged = true;
        }

        if ((CollectionUtils.isEmpty(switchConfig.getCfAliveList())) || forcedUpdateRemoteJudgeAccount) {
            switchConfig.setCfAliveList(
                    new ArrayList<>(Collections.nCopies(switchConfig.getCfUsernameList().size(), false)));
            isChanged = true;
        }

        if (isChanged) {
            nacosSwitchConfig.publishWebConfig();
        }

        if (openRemoteJudge.equals("true")) {
            // 初始化清空表
            remoteJudgeAccountEntityService.remove(new QueryWrapper<>());
            addRemoteJudgeAccountToMySQL(Constants.RemoteOJ.HDU.getName(),
                    switchConfig.getHduUsernameList(),
                    switchConfig.getHduPasswordList());
            addRemoteJudgeAccountToMySQL(Constants.RemoteOJ.POJ.getName(),
                    switchConfig.getPojUsernameList(),
                    switchConfig.getPojPasswordList());
            addRemoteJudgeAccountToMySQL2(Constants.RemoteOJ.CODEFORCES.getName(),
                    switchConfig.getCfUsernameList(),
                    switchConfig.getCfPasswordList(),
                    switchConfig.getCfAliveList(),
                    null, null);
            addRemoteJudgeAccountToMySQL(Constants.RemoteOJ.SPOJ.getName(),
                    switchConfig.getSpojUsernameList(),
                    switchConfig.getSpojPasswordList());
            addRemoteJudgeAccountToMySQL(Constants.RemoteOJ.ATCODER.getName(),
                    switchConfig.getAtcoderUsernameList(),
                    switchConfig.getAtcoderPasswordList());
            addRemoteJudgeAccountToMySQL(Constants.RemoteOJ.LIBRE.getName(),
                    switchConfig.getLibreojUsernameList(),
                    switchConfig.getLibreojPasswordList());
            addRemoteJudgeAccountToMySQL(Constants.RemoteOJ.SCPC.getName(),
                    switchConfig.getScpcUsernameList(),
                    switchConfig.getScpcPasswordList());
            addRemoteJudgeAccountToMySQL(Constants.RemoteOJ.QOJ.getName(),
                    switchConfig.getQojUsernameList(),
                    switchConfig.getQojPasswordList());
            addRemoteJudgeAccountToMySQL(Constants.RemoteOJ.NSWOJ.getName(),
                    switchConfig.getNswojUsernameList(),
                    switchConfig.getNswojPasswordList());
            addRemoteJudgeAccountToMySQL(Constants.RemoteOJ.NEWOJ.getName(),
                    switchConfig.getNewojUsernameList(),
                    switchConfig.getNewojPasswordList());
            addRemoteJudgeAccountToMySQL2(Constants.RemoteOJ.VJ.getName(),
                    switchConfig.getVjUsernameList(),
                    switchConfig.getVjPasswordList(),
                    switchConfig.getVjAliveList(),
                    null, null);
            addRemoteJudgeAccountToMySQL(Constants.RemoteOJ.DOTCPP.getName(),
                    switchConfig.getDotcppUsernameList(),
                    switchConfig.getDotcppPasswordList());
            addRemoteJudgeAccountToMySQL2(Constants.RemoteOJ.NOWCODER.getName(),
                    switchConfig.getNowcoderUsernameList(),
                    switchConfig.getNowcoderPasswordList(),
                    switchConfig.getNowcoderAliveList(),
                    switchConfig.getNowcoderTitleList(),
                    switchConfig.getNowcoderLinkList());
            addRemoteJudgeAccountToMySQL2(Constants.RemoteOJ.ACWING.getName(),
                    switchConfig.getAcwingUsernameList(),
                    switchConfig.getAcwingPasswordList(),
                    switchConfig.getAcwingAliveList(),
                    switchConfig.getAcwingTitleList(),
                    switchConfig.getAcwingLinkList());
            addRemoteJudgeAccountToMySQL(Constants.RemoteOJ.MOSS.getName(),
                    switchConfig.getMossUsernameList(),
                    null);
            checkRemoteOJLanguage(Constants.RemoteOJ.SPOJ, Constants.RemoteOJ.ATCODER, Constants.RemoteOJ.VJ);
        }
    }

    private void initWKHTMLTOPDFConfig() {
        WebConfig webConfig = nacosSwitchConfig.getWebConfig();
        boolean isChanged = false;
        if (!Objects.equals(webConfig.getHtmltopdfHost(), htmltopdfHost)
                && (webConfig.getHtmltopdfHost() == null || !"http://172.17.0.1".equals(htmltopdfHost))) {
            webConfig.setHtmltopdfHost(htmltopdfHost);
            isChanged = true;
        }
        if (!Objects.equals(webConfig.getHtmltopdfPort(), htmltopdfPort)
                && (webConfig.getHtmltopdfPort() == null || htmltopdfPort != 8001)) {
            webConfig.setHtmltopdfPort(htmltopdfPort);
            isChanged = true;
        }
        if (!Objects.equals(webConfig.getHtmltopdfEc(), htmltopdfEc)
                && (webConfig.getHtmltopdfEc() == null)) {
            webConfig.setHtmltopdfEc(htmltopdfEc);
            isChanged = true;
        }

        if (isChanged) {
            nacosSwitchConfig.publishWebConfig();
        }
    }

    private void initCLOCConfig() {
        WebConfig webConfig = nacosSwitchConfig.getWebConfig();
        boolean isChanged = false;
        if (!Objects.equals(webConfig.getClochost(), clocHost)
                && (webConfig.getClochost() == null || !"http://172.17.0.1".equals(clocHost))) {
            webConfig.setClochost(clocHost);
            isChanged = true;
        }
        if (!Objects.equals(webConfig.getClocport(), clocPort)
                && (webConfig.getClocport() == null || clocPort != 8002)) {
            webConfig.setClocport(clocPort);
            isChanged = true;
        }
        if (!Objects.equals(webConfig.getClocstartTime(), clocstartTime)
                && (webConfig.getClocstartTime() == null || !"2024-07-18T16:00:00.000Z".equals(clocstartTime))) {
            webConfig.setClocstartTime(clocstartTime);
            isChanged = true;
        }
        if (isChanged) {
            nacosSwitchConfig.publishWebConfig();
        }
    }

    /**
     * @param oj
     * @param usernameList
     * @param passwordList
     * @MethodName addRemoteJudgeAccountToRedis
     * @Description 将传入的对应oj账号写入到mysql
     * @Return
     * @Since 2021/5/18
     */
    private void addRemoteJudgeAccountToMySQL(String oj, List<String> usernameList, List<String> passwordList) {

        if (oj.equals(Constants.RemoteOJ.MOSS.getName())) {
            if (CollectionUtils.isEmpty(usernameList)) {
                log.error("[Init System Config] [{}]: There is no account or password configured for remote judge, " +
                        "username list:{}", oj, Arrays.toString(usernameList.toArray()));
            }
        } else {
            if (CollectionUtils.isEmpty(usernameList) || CollectionUtils.isEmpty(passwordList)
                    || usernameList.size() != passwordList.size()) {
                log.error("[Init System Config] [{}]: There is no account or password configured for remote judge, " +
                        "username list:{}, password list:{}", oj, Arrays.toString(usernameList.toArray()),
                        Arrays.toString(passwordList.toArray()));
            }
        }

        List<RemoteJudgeAccount> remoteAccountList = new LinkedList<>();
        for (int i = 0; i < usernameList.size(); i++) {
            RemoteJudgeAccount account = new RemoteJudgeAccount()
                    .setUsername(usernameList.get(i))
                    .setStatus(true)
                    .setVersion(0L)
                    .setOj(oj);

            if (!CollectionUtils.isEmpty(passwordList)) {
                account.setPassword(passwordList.get(i));
            }

            remoteAccountList.add(account);
        }

        if (remoteAccountList.size() > 0) {
            boolean addOk = remoteJudgeAccountEntityService.saveOrUpdateBatch(remoteAccountList);
            if (!addOk) {
                log.error(
                        "[Init System Config] Remote judge initialization failed. Failed to add account for: [{}]. Please check the configuration file and restart!",
                        oj);
            }
        }
    }

    /**
     * @param oj
     * @param usernameList
     * @param passwordList
     * @param aliveList
     * @param titleList
     * @param linkList
     * @MethodName addRemoteJudgeAccountToRedis
     * @Description 将传入的对应保活账号写入到mysql
     * @Return
     */
    private void addRemoteJudgeAccountToMySQL2(String oj, List<String> usernameList, List<String> passwordList,
            List<Boolean> aliveList, List<String> titleList, List<String> linkList) {

        if (CollectionUtils.isEmpty(aliveList)) {
            aliveList = new ArrayList<>(Collections.nCopies(usernameList.size(), false));
        }

        if (CollectionUtils.isEmpty(usernameList) || CollectionUtils.isEmpty(passwordList)
                || CollectionUtils.isEmpty(aliveList)
                || usernameList.size() != passwordList.size() || usernameList.size() != aliveList.size()) {
            log.error("[Init System Config] [{}]: There is no account or link or title configured for cookie, " +
                    "username list:{}, password list:{}, alive list:{}", oj, Arrays.toString(usernameList.toArray()),
                    Arrays.toString(passwordList.toArray()), Arrays.toString(aliveList.toArray()));
        }

        List<RemoteJudgeAccount> remoteAccountList = new LinkedList<>();

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

            remoteAccountList.add(account);
        }

        if (remoteAccountList.size() > 0) {
            boolean addOk = remoteJudgeAccountEntityService.saveOrUpdateBatch(remoteAccountList);
            if (!addOk) {
                log.error(
                        "[Init System Config] Remote judge initialization failed. Failed to add account for: [{}]. Please check the configuration file and restart!",
                        oj);
            }
        }
    }

    private void upsertHOJLanguageV2() {
        /**
         * 2023.06.27 新增ruby、rust语言
         */
        QueryWrapper<Language> rubyLanguageQueryWrapper = new QueryWrapper<>();
        rubyLanguageQueryWrapper.eq("oj", "ME")
                .eq("name", "Ruby");
        int countRuby = languageEntityService.count(rubyLanguageQueryWrapper);
        if (countRuby == 0) {
            Language rubyLanguage = new Language();
            rubyLanguage.setName("Ruby")
                    .setCompileCommand("/usr/bin/ruby {src_path}")
                    .setContentType("text/x-ruby")
                    .setDescription("Ruby 2.5.1")
                    .setTemplate("a, b = gets.split.map(&:to_i)\n" +
                            "puts(a + b)")
                    .setIsSpj(false)
                    .setOj("ME");
            boolean isOk = languageEntityService.save(rubyLanguage);
            if (!isOk) {
                log.error(
                        "[Init System Config] [HOJ] Failed to add new language [{}]! Please check whether the language table corresponding to the database has the language!",
                        "Ruby");
            }
        }

        QueryWrapper<Language> rustLanguageQueryWrapper = new QueryWrapper<>();
        rustLanguageQueryWrapper.eq("oj", "ME")
                .eq("name", "Rust");
        int countRust = languageEntityService.count(rustLanguageQueryWrapper);
        if (countRust == 0) {
            Language rustLanguage = new Language();
            rustLanguage.setName("Rust")
                    .setCompileCommand("/usr/bin/rustc -O -o {exe_path} {src_path}")
                    .setContentType("text/x-rustsrc")
                    .setDescription("Rust 1.65.0")
                    .setTemplate("use std::io;\n" +
                            " \n" +
                            "fn main() {\n" +
                            "    let mut line = String::new();\n" +
                            "    io::stdin().read_line(&mut line).expect(\"stdin\");\n" +
                            " \n" +
                            "    let sum: i32 = line.split_whitespace()\n" +
                            "                       .map(|x| x.parse::<i32>().expect(\"integer\"))\n" +
                            "                       .sum(); \n" +
                            "    println!(\"{}\", sum);\n" +
                            "}")
                    .setIsSpj(false)
                    .setOj("ME");
            boolean isOk = languageEntityService.save(rustLanguage);
            if (!isOk) {
                log.error(
                        "[Init System Config] [HOJ] Failed to add new language [{}]! Please check whether the language table corresponding to the database has the language!",
                        "Rust");
            }
        }
    }

    @Deprecated
    private void upsertHOJLanguage(String... languageList) {
        /**
         * 2022.02.25 新增js、pypy、php语言
         */
        for (String language : languageList) {
            QueryWrapper<Language> languageQueryWrapper = new QueryWrapper<>();
            languageQueryWrapper.eq("oj", "ME")
                    .eq("name", language);
            int count = languageEntityService.count(languageQueryWrapper);
            if (count == 0) {
                Language newLanguage = buildHOJLanguage(language);
                boolean isOk = languageEntityService.save(newLanguage);
                if (!isOk) {
                    log.error(
                            "[Init System Config] [HOJ] Failed to add new language [{}]! Please check whether the language table corresponding to the database has the language!",
                            language);
                }
            }
        }
    }

    @Deprecated
    private void checkAllLanguageUpdate() {

        /**
         * 2022.02.25 更新原有的python3.6.9为python3.7.5
         */
        UpdateWrapper<Language> languageUpdateWrapper = new UpdateWrapper<>();
        languageUpdateWrapper.eq("oj", "ME")
                .eq("name", "Python3")
                .set("description", "Python 3.7.5");
        languageEntityService.update(languageUpdateWrapper);

        /**
         * 2022.02.25 删除cf的Microsoft Visual C++ 2010
         */
        UpdateWrapper<Language> deleteWrapper = new UpdateWrapper<>();
        deleteWrapper.eq("name", "Microsoft Visual C++ 2010")
                .eq("oj", "CF");
        languageEntityService.remove(deleteWrapper);

        /**
         * 2022.09.20 增加hdu的Java和C#支持
         */
        List<Language> newHduLanguageList = new ArrayList<>();
        QueryWrapper<Language> languageQueryWrapper = new QueryWrapper<>();
        languageQueryWrapper.select("id", "name");
        languageQueryWrapper.eq("oj", "HDU");
        List<Language> hduLanguageList = languageEntityService.list(languageQueryWrapper);
        List<String> collect = hduLanguageList.stream()
                .map(Language::getName)
                .collect(Collectors.toList());
        if (!collect.contains("Java")) {
            Language hduJavaLanguage = new Language();
            hduJavaLanguage.setContentType("text/x-java")
                    .setName("Java")
                    .setDescription("Java")
                    .setIsSpj(false)
                    .setOj("HDU");
            newHduLanguageList.add(hduJavaLanguage);
        }
        if (!collect.contains("C#")) {
            Language hduCSharpLanguage = new Language();
            hduCSharpLanguage.setContentType("text/x-csharp")
                    .setName("C#")
                    .setDescription("C#")
                    .setIsSpj(false)
                    .setOj("HDU");
            newHduLanguageList.add(hduCSharpLanguage);
        }
        if (newHduLanguageList.size() > 0) {
            languageEntityService.saveBatch(newHduLanguageList);
        }
    }

    private void checkRemoteOJLanguage(Constants.RemoteOJ... remoteOJList) {
        for (Constants.RemoteOJ remoteOJ : remoteOJList) {
            QueryWrapper<Language> languageQueryWrapper = new QueryWrapper<>();
            if (!Objects.equals(remoteOJ, Constants.RemoteOJ.VJ)) {
                languageQueryWrapper.eq("oj", remoteOJ.getName());
            } else {
                languageQueryWrapper.like("oj", remoteOJ.getName());
            }
            if (Objects.equals(remoteOJ, Constants.RemoteOJ.ATCODER)) {
                // 2023.09.24 由于atcoder官网废弃之前全部的语言，所以根据新语言来判断是否需要重新清空，添加最新的语言
                languageQueryWrapper.eq("name", "なでしこ (cnako3 3.4.20)");
            }
            int count = languageEntityService.count(languageQueryWrapper);
            if (count == 0) {
                if (Objects.equals(remoteOJ, Constants.RemoteOJ.ATCODER)) {
                    // 2023.09.24 由于atcoder官网废弃之前全部的语言，所以根据新语言来判断是否需要重新清空，添加最新的语言
                    UpdateWrapper<Language> languageUpdateWrapper = new UpdateWrapper<>();
                    languageUpdateWrapper.eq("oj", remoteOJ.getName());
                    languageEntityService.remove(languageUpdateWrapper);
                }
                List<Language> languageList = new LanguageContext(remoteOJ).buildLanguageList();
                boolean isOk = languageEntityService.saveBatch(languageList);
                if (!isOk) {
                    log.error(
                            "[Init System Config] [{}] Failed to initialize language list! Please check whether the language table corresponding to the database has the OJ language!",
                            remoteOJ.getName());
                }
                if (Objects.equals(remoteOJ, Constants.RemoteOJ.ATCODER)) {
                    // 2023.09.24 同时需要把所有atcoder的题目都重新关联上新language的id
                    QueryWrapper<Problem> problemQueryWrapper = new QueryWrapper<>();
                    problemQueryWrapper.select("id");
                    problemQueryWrapper.eq("is_remote", true);
                    problemQueryWrapper.like("problem_id", "AC-");
                    List<Problem> problemList = problemEntityService.list(problemQueryWrapper);
                    if (!CollectionUtils.isEmpty(problemList)) {
                        List<Long> problemIdList = problemList.stream().map(Problem::getId)
                                .collect(Collectors.toList());
                        List<ProblemLanguage> problemLanguageList = new LinkedList<>();
                        QueryWrapper<Language> newLanguageQueryWrapper = new QueryWrapper<>();
                        newLanguageQueryWrapper.eq("oj", remoteOJ.getName());
                        List<Language> newLanguageList = languageEntityService.list(newLanguageQueryWrapper);
                        for (Long id : problemIdList) {
                            for (Language language : newLanguageList) {
                                problemLanguageList.add(new ProblemLanguage().setPid(id).setLid(language.getId()));
                            }
                        }
                        problemLanguageEntityService.saveOrUpdateBatch(problemLanguageList);
                    }
                }
            }
        }
    }

    private Language buildHOJLanguage(String lang) {
        Language language = new Language();
        switch (lang) {
            case "PHP":
                language.setName("PHP")
                        .setCompileCommand("/usr/bin/php {src_path}")
                        .setContentType("text/x-php")
                        .setDescription("PHP 7.3.33")
                        .setTemplate("<?=array_sum(fscanf(STDIN, \"%d %d\"));")
                        .setIsSpj(false)
                        .setOj("ME");
                return language;
            case "JavaScript Node":
                language.setName("JavaScript Node")
                        .setCompileCommand("/usr/bin/node {src_path}")
                        .setContentType("text/javascript")
                        .setDescription("Node.js 14.19.0")
                        .setTemplate("var readline = require('readline');\n" +
                                "const rl = readline.createInterface({\n" +
                                "        input: process.stdin,\n" +
                                "        output: process.stdout\n" +
                                "});\n" +
                                "rl.on('line', function(line){\n" +
                                "   var tokens = line.split(' ');\n" +
                                "    console.log(parseInt(tokens[0]) + parseInt(tokens[1]));\n" +
                                "});")
                        .setIsSpj(false)
                        .setOj("ME");
                return language;
            case "JavaScript V8":
                language.setName("JavaScript V8")
                        .setCompileCommand("/usr/bin/jsv8/d8 {src_path}")
                        .setContentType("text/javascript")
                        .setDescription("JavaScript V8 8.4.109")
                        .setTemplate("const [a, b] = readline().split(' ').map(n => parseInt(n, 10));\n" +
                                "print((a + b).toString());")
                        .setIsSpj(false)
                        .setOj("ME");
                return language;
            case "PyPy2":
                language.setName("PyPy2")
                        .setContentType("text/x-python")
                        .setCompileCommand("/usr/bin/pypy -m py_compile {src_path}")
                        .setDescription("PyPy 2.7.18 (7.3.8)")
                        .setTemplate("print sum(int(x) for x in raw_input().split(' '))")
                        .setCodeTemplate("//PREPEND BEGIN\n" +
                                "//PREPEND END\n" +
                                "\n" +
                                "//TEMPLATE BEGIN\n" +
                                "def add(a, b):\n" +
                                "    return a + b\n" +
                                "//TEMPLATE END\n" +
                                "\n" +
                                "\n" +
                                "if __name__ == '__main__':  \n" +
                                "    //APPEND BEGIN\n" +
                                "    a, b = 1, 1\n" +
                                "    print add(a, b)\n" +
                                "    //APPEND END")
                        .setIsSpj(false)
                        .setOj("ME");
                return language;
            case "PyPy3":
                language.setName("PyPy3")
                        .setContentType("text/x-python")
                        .setDescription("PyPy 3.8.12 (7.3.8)")
                        .setCompileCommand("/usr/bin/pypy3 -m py_compile {src_path}")
                        .setTemplate("print(sum(int(x) for x in input().split(' ')))")
                        .setCodeTemplate("//PREPEND BEGIN\n" +
                                "//PREPEND END\n" +
                                "\n" +
                                "//TEMPLATE BEGIN\n" +
                                "def add(a, b):\n" +
                                "    return a + b\n" +
                                "//TEMPLATE END\n" +
                                "\n" +
                                "\n" +
                                "if __name__ == '__main__':  \n" +
                                "    //APPEND BEGIN\n" +
                                "    a, b = 1, 1\n" +
                                "    print(add(a, b))\n" +
                                "    //APPEND END")
                        .setIsSpj(false)
                        .setOj("ME");
                return language;
        }
        return null;
    }

    private void checkLanguageUpdate() {
        if (CollectionUtil.isNotEmpty(checkLanguageConfig.getList())) {
            for (Language language : checkLanguageConfig.getList()) {
                UpdateWrapper<Language> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("oj", language.getOj())
                        .eq("name", language.getName())
                        .eq("is_spj", language.getIsSpj()) // 这三个条件确定唯一性
                        .set(StrUtil.isNotEmpty(language.getContentType()), "content_type", language.getContentType())
                        .set(StrUtil.isNotEmpty(language.getDescription()), "description", language.getDescription())
                        .set(StrUtil.isNotEmpty(language.getCompileCommand()), "compile_command",
                                language.getCompileCommand())
                        .set(StrUtil.isNotEmpty(language.getTemplate()), "template", language.getTemplate())
                        .set(StrUtil.isNotEmpty(language.getCodeTemplate()), "code_template",
                                language.getCodeTemplate())
                        .set(language.getSeq() != null, "seq", language.getSeq());
                languageEntityService.update(updateWrapper);
            }
        }
    }

    private void upsertHOJLanguageV3() {
        /**
         * 2024.02.23 新增loj语言支持
         */

        int count = languageEntityService.count(new QueryWrapper<Language>()
                .eq("oj", Constants.RemoteOJ.LIBRE.getName()));
        if (count == 0) {
            List<String> languageList = Arrays.asList("text/x-c++src", "C++ 11 (G++)", "C++ 11 (G++)", "text/x-c++src",
                    "C++ 17 (G++)", "C++ 17 (G++)", "text/x-c++src", "C++ 11 (Clang++) ", "C++ 11 (Clang++) ",
                    "text/x-c++src", "C++ 17 (Clang++)", "C++ 17 (Clang++)", "text/x-c++src", "C++ 11 O2(G++)",
                    "C++ 11 O2(G++)", "text/x-c++src", "C++ 17 O2(G++)", "C++ 17 O2(G++)", "text/x-c++src",
                    "C++ 11 O2(Clang++) ", "C++ 11 O2(Clang++)", "text/x-c++src", "C++ 17 O2(Clang++)",
                    "C++ 17 O2(Clang++)", "text/x-csrc", "C 11 (GCC)", "C 11 (GCC)", "text/x-csrc", "C 17 (GCC)",
                    "C 17 (GCC)", "text/x-csrc", "C 11 (Clang)", "C 11 (Clang)", "text/x-csrc", "C 17 (Clang)",
                    "C 17 (Clang)", "text/x-java", "Java", "Java", "text/x-java", "Kotlin 1.8 (JVM)",
                    "Kotlin 1.8 (JVM)", "text/x-pascal", "Pascal", "Pascal", "text/x-python", "Python 3.10",
                    "Python 3.10", "text/x-python", "Python 3.9", "Python 3.9", "text/x-python", "Python 2.7",
                    "Python 2.7", "text/x-rustsrc", "Rust 2021", "Rust 2021", "text/x-rustsrc", "Rust 2018",
                    "Rust 2018", "text/x-rustsrc", "Rust 2015", "Rust 2015", "go", "Go 1.x", "Go 1.x", "text/x-csharp",
                    "C# 9", "C# 9", "text/x-csharp", "C# 7.3", "C# 7.3");
            List<Language> languages = new ArrayList<>();
            for (int i = 0; i <= languageList.size() - 3; i += 3) {
                languages.add(new Language()
                        .setContentType(languageList.get(i))
                        .setDescription(languageList.get(i + 1))
                        .setName(languageList.get(i + 2))
                        .setOj(Constants.RemoteOJ.LIBRE.getName())
                        .setSeq(0)
                        .setIsSpj(false));
            }
            languageEntityService.saveBatch(languages);
        }

    }

    private void upsertHOJLanguageV4() {
        /**
         * 2024.03.18 新增qoj语言支持
         */

        int count = languageEntityService.count(new QueryWrapper<Language>()
                .eq("oj", Constants.RemoteOJ.QOJ.getName()));
        if (count == 0) {
            List<String> languageList = Arrays.asList("text/x-c++src", "C++ 98", "C++ 98",
                    "text/x-c++src", "C++ 11", "C++ 11",
                    "text/x-c++src", "C++ 14", "C++ 14",
                    "text/x-c++src", "C++ 17", "C++ 17",
                    "text/x-c++src", "C++ 20", "C++ 20",
                    "text/x-c++src", "C++ 23", "C++ 23",
                    "text/x-csrc", "C 89", "C 89",
                    "text/x-csrc", "C 99", "C 99",
                    "text/x-csrc", "C 11", "C 11",
                    "text/x-d", "D", "D",
                    "text/x-java", "Java 8", "Java 8",
                    "text/x-java", "Java 11", "Java 11",
                    "text/x-pascal", "Pascal", "Pascal",
                    "text/x-python", "Python 3", "Python 3",
                    "text/x-rustsrc", "Rust", "Rust");

            List<Language> languages = new ArrayList<>();
            for (int i = 0; i <= languageList.size() - 3; i += 3) {
                languages.add(new Language()
                        .setContentType(languageList.get(i))
                        .setDescription(languageList.get(i + 1))
                        .setName(languageList.get(i + 2))
                        .setOj(Constants.RemoteOJ.QOJ.getName())
                        .setSeq(0)
                        .setIsSpj(false));
            }
            languageEntityService.saveBatch(languages);
        }

    }

    private void upsertHOJLanguageV5() {
        /**
         * 2024.03.18 新增nswoj语言支持
         */

        int count = languageEntityService.count(new QueryWrapper<Language>()
                .eq("oj", Constants.RemoteOJ.NSWOJ.getName()));
        if (count == 0) {
            List<String> languageList = Arrays.asList("text/x-c++src", "C++", "C++",
                    "text/x-c++src", "C++ 98", "C++ 98",
                    "text/x-c++src", "C++ 11", "C++ 11",
                    "text/x-c++src", "C++ 14", "C++ 14",
                    "text/x-c++src", "C++ 17", "C++ 17",
                    "text/x-csrc", "C", "C",
                    "text/x-java", "Java", "Java",
                    "text/x-pascal", "Pascal", "Pascal",
                    "text/x-python", "Python", "Python",
                    "text/x-python", "Python 2", "Python 2",
                    "text/x-python", "Python 3", "Python 3",
                    "text/x-php", "PHP", "PHP",
                    "text/x-rustsrc", "Rust", "Rust",
                    "text/javascript", "Javascript", "Javascript",
                    "text/golang", "Golang", "Golang",
                    "text/x-ruby", "Ruby", "Ruby",
                    "text/x-csharp", "C#", "C#",
                    "text/x-rustsrc", "Rust", "Rust");

            List<Language> languages = new ArrayList<>();
            for (int i = 0; i <= languageList.size() - 3; i += 3) {
                languages.add(new Language()
                        .setContentType(languageList.get(i))
                        .setDescription(languageList.get(i + 1))
                        .setName(languageList.get(i + 2))
                        .setOj(Constants.RemoteOJ.NSWOJ.getName())
                        .setSeq(0)
                        .setIsSpj(false));
            }
            languageEntityService.saveBatch(languages);
        }

    }

    private void upsertHOJLanguageV6() {

        int count = languageEntityService.count(new QueryWrapper<Language>()
                .eq("oj", Constants.RemoteOJ.NEWOJ.getName()));
        if (count == 0) {
            List<String> languageList = Arrays.asList("text/x-c++src", "C++", "C++",
                    "text/x-csrc", "C", "C",
                    "text/x-java", "Java", "Java",
                    "text/x-python", "Python", "Python");

            List<Language> languages = new ArrayList<>();
            for (int i = 0; i <= languageList.size() - 3; i += 3) {
                languages.add(new Language()
                        .setContentType(languageList.get(i))
                        .setDescription(languageList.get(i + 1))
                        .setName(languageList.get(i + 2))
                        .setOj(Constants.RemoteOJ.NEWOJ.getName())
                        .setSeq(0)
                        .setIsSpj(false));
            }
            languageEntityService.saveBatch(languages);
        }

    }

    private void upsertHOJLanguageV7() {

        int count = languageEntityService.count(new QueryWrapper<Language>()
                .eq("oj", Constants.RemoteOJ.DOTCPP.getName()));
        if (count == 0) {
            List<String> languageList = Arrays.asList(
                    "text/x-csrc", "C", "C", "0",
                    "text/x-c++src", "C++", "C++", "1",
                    "text/x-java", "Java", "Java", "3",
                    "text/x-python", "Python", "Python", "6",
                    "text/x-php", "PHP", "PHP", "7",
                    "text/x-csrc", "C O2", "C O2", "0",
                    "text/x-c++src", "C++ O2", "C++ O2", "1",
                    "text/x-java", "Java O2", "Java O2", "3",
                    "text/x-python", "Python O2", "Python O2", "6",
                    "text/x-php", "PHP O2", "PHP O2", "7");

            List<Language> languages = new ArrayList<>();
            for (int i = 0; i <= languageList.size() - 4; i += 4) {
                languages.add(new Language()
                        .setContentType(languageList.get(i))
                        .setDescription(languageList.get(i + 1))
                        .setName(languageList.get(i + 2))
                        .setOj(Constants.RemoteOJ.DOTCPP.getName())
                        .setKey(languageList.get(i + 3))
                        .setSeq(0)
                        .setIsSpj(false));
            }
            languageEntityService.saveBatch(languages);
        }

    }
}
