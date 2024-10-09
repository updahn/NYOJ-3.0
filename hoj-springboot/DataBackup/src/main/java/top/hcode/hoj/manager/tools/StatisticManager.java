package top.hcode.hoj.manager.tools;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.config.NacosSwitchConfig;
import top.hcode.hoj.config.SwitchConfig;
import top.hcode.hoj.dao.tools.StatisticContestEntityService;
import top.hcode.hoj.dao.tools.StatisticRankEntityService;
import top.hcode.hoj.manager.oj.ContestRankManager;
import top.hcode.hoj.mapper.StatisticContestMapper;
import top.hcode.hoj.pojo.bo.Pair_;
import top.hcode.hoj.pojo.dto.StatisticRankDTO;
import top.hcode.hoj.pojo.entity.contest.*;
import top.hcode.hoj.pojo.vo.*;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.validator.ContestValidator;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Resource;

@Component
public class StatisticManager {

    @Autowired
    private ContestValidator contestValidator;

    @Autowired
    private ContestRankManager contestRankManager;

    @Autowired
    private NacosSwitchConfig nacosSwitchConfig;

    @Autowired
    private StatisticContestMapper statisticContestMapper;

    @Resource
    private StatisticRankEntityService statisticRankEntityService;

    @Resource
    private StatisticContestEntityService statisticContestEntityService;

    public IPage<StatisticContest> getStatisticList(Integer currentPage, Integer limit,
            String keyword) {

        // 页数，每页题数若为空，设置默认值
        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        if (limit == null || limit < 1)
            limit = 30;

        IPage<StatisticContest> iPage = new Page<>(currentPage, limit);

        return statisticContestMapper.getStatisticContestList(iPage, keyword);
    }

    public String getStatisticRankCids(String scid) {
        return contestValidator.getStatisticRankCids(scid);
    }

    public IPage<ACMContestRankVO> getStatisticRank(StatisticRankDTO statisticRankDto)
            throws StatusFailException, StatusForbiddenException, Exception {

        Integer currentPage = statisticRankDto.getCurrentPage();
        Integer limit = statisticRankDto.getLimit();

        // 页数，每页题数若为空，设置默认值
        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        if (limit == null || limit < 1)
            limit = 30;

        List<ACMContestRankVO> acmContestRankList = getStatisticRankList(statisticRankDto);
        return contestRankManager.getPagingRankList(acmContestRankList, currentPage, limit);
    }

    public IPage<StatisticContest> getAdminStatisticList(Integer currentPage, Integer limit,
            String keyword) {

        // 页数，每页题数若为空，设置默认值
        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        if (limit == null || limit < 1)
            limit = 30;

        IPage<StatisticContest> iPage = new Page<>(currentPage, limit);

        return statisticContestMapper.getAdminStatisticContestList(iPage, keyword);
    }

    public void addStatisticRank(StatisticRankDTO statisticRankDto)
            throws StatusFailException, StatusForbiddenException {
        List<ACMContestRankVO> acmContestRankVoList = statisticRankDto.getAcmContestRankVoList();
        String title = statisticRankDto.getTitle();
        String cids = statisticRankDto.getCids();
        String percents = statisticRankDto.getPercents();
        HashMap<String, String> data = statisticRankDto.getData();
        HashMap<String, String> account = statisticRankDto.getAccount();

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        String scid = statisticRankDto.getScid() != null ? statisticRankDto.getScid() : IdUtil.fastSimpleUUID();

        StatisticContest statisticContest = statisticContestEntityService.getOne(
                new QueryWrapper<StatisticContest>().eq("scid", scid), false);

        // 创建 StatisticContest 并保存
        statisticContest = new StatisticContest()
                .setScid(scid)
                .setTitle(title)
                .setCids(cids)
                .setPercents(percents)
                .setData(hashMapToString(data))
                .setAccount(hashMapToString(account))
                .setAuthor(userRolesVo.getUsername());

        Boolean isOk = statisticContestEntityService.saveOrUpdate(statisticContest);

        // 遍历并保存 StatisticRank，使用一个新的变量收集结果
        isOk &= acmContestRankVoList.stream().allMatch(acmContestRankVO -> {
            String submissionInfoStr = hashMapToString(acmContestRankVO.getSubmissionInfo());

            StatisticRank statisticRank = new StatisticRank();
            BeanUtil.copyProperties(acmContestRankVO, statisticRank);

            return statisticRankEntityService.save(statisticRank.setJson(submissionInfoStr).setScid(scid));
        });

        if (!isOk) {
            throw new StatusFailException("添加失败");
        }
    }

    public void updateStatisticRank(StatisticRankDTO statisticRankDto)
            throws StatusFailException, StatusForbiddenException, Exception {

        String scid = statisticRankDto.getScid();

        if (scid == null) {
            throw new StatusFailException("请传入scid！");
        }

        StatisticContest statisticContest = statisticContestEntityService.getOne(
                new QueryWrapper<StatisticContest>().eq("scid", scid), false);

        if (statisticContest == null) {
            throw new StatusFailException("对应scid的系列比赛不存在！");
        }

        // 删除原来的系列比赛对应的榜单数据，重新填写
        UpdateWrapper<StatisticRank> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("scid", scid);
        statisticRankEntityService.remove(updateWrapper);

        // 重新爬取信息
        statisticRankDto.setRefresh(true);
        List<ACMContestRankVO> acmContestRankList = getStatisticRankList(statisticRankDto);

        statisticRankDto.setAcmContestRankVoList(acmContestRankList);

        addStatisticRank(statisticRankDto);
    }

    public void deleteStatisticRank(String scid) throws StatusForbiddenException {
        // 删除对应的 StatisticContest
        statisticContestEntityService.removeById(scid);
    }

    public void changeStatisticVisible(String scid, Boolean show) throws StatusForbiddenException {
        UpdateWrapper<StatisticContest> queryWrapper = new UpdateWrapper<>();
        queryWrapper.eq("scid", scid).set("visible", show);
        statisticContestEntityService.update(queryWrapper);
    }

    private String getLoginPassword(String loginUsername, List<String> usernameList, List<String> passwordList) {
        if (StringUtils.isEmpty(loginUsername)) {
            return "";
        }
        for (int i = 0; i < usernameList.size(); i++) {
            if (usernameList.get(i).equals(loginUsername)) {
                return passwordList.get(i);
            }
        }
        return "";
    }

    private String hashMapToString(Object data) {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            // 判断输入类型并转换为字符串
            if (data instanceof HashMap) {
                return objectMapper.writeValueAsString(data); // 正确的转换方式
            } else {
                throw new IllegalArgumentException("Unsupported type. Expected HashMap.");
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace(); // 打印异常信息以供调试
        }
        return null;
    }

    public List<ACMContestRankVO> getStatisticRankList(StatisticRankDTO statisticRankDto)
            throws StatusFailException, StatusForbiddenException, Exception {

        Integer currentPage = statisticRankDto.getCurrentPage();
        Integer limit = statisticRankDto.getLimit();
        String keyword = statisticRankDto.getKeyword();
        String scid = statisticRankDto.getScid();
        String cids = statisticRankDto.getCids();
        String percents = statisticRankDto.getPercents();
        HashMap<String, String> data = statisticRankDto.getData();
        HashMap<String, String> account = statisticRankDto.getAccount();
        Boolean refresh = statisticRankDto.getRefresh() != null ? statisticRankDto.getRefresh() : false;

        // 页数，每页题数若为空，设置默认值
        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        if (limit == null || limit < 1)
            limit = 30;

        List<ACMContestRankVO> acmContestRankVoList = new ArrayList<>();

        // 如果传入的是scid
        StatisticContest statisticContest = statisticContestEntityService.getOne(
                new QueryWrapper<StatisticContest>().eq("scid", scid), false);

        if (statisticContest != null && !refresh) {

            List<StatisticRank> statisticRankList = statisticRankEntityService.list(
                    new QueryWrapper<StatisticRank>().in("scid", statisticContest.getScid()));

            // 转换 StatisticRank 为 ACMContestRankVO
            acmContestRankVoList = (List<ACMContestRankVO>) statisticRankList.stream()
                    .map(statisticRank -> {
                        ACMContestRankVO acmContestRankVo = new ACMContestRankVO();
                        // 复制属性
                        BeanUtil.copyProperties(statisticContest, acmContestRankVo, "account", "data");
                        BeanUtil.copyProperties(statisticRank, acmContestRankVo, "submissionInfo");

                        // 解析 JSON 数据并设置到相应的字段
                        acmContestRankVo.setSubmissionInfo(parseJsonWithType(statisticRank.getJson(),
                                new TypeReference<HashMap<String, HashMap<String, Object>>>() {
                                }));
                        acmContestRankVo.setAccount(parseJsonWithType(statisticContest.getAccount(),
                                new TypeReference<HashMap<String, String>>() {
                                }));
                        acmContestRankVo.setData(parseJsonWithType(statisticContest.getData(),
                                new TypeReference<HashMap<String, String>>() {
                                }));

                        acmContestRankVo.setPercents(statisticContest.getPercents());
                        return acmContestRankVo; // 返回ACMContestRankVO对象
                    })
                    .collect(Collectors.toList());

        } else {
            List<Contest> contestList = contestValidator.validateContestList(cids);
            List<Integer> percentList = contestValidator.validatePercentList(percents);

            List<Pair_<String, String>> accountList = new ArrayList<>();
            SwitchConfig switchConfig = nacosSwitchConfig.getSwitchConfig();
            for (Contest contest : contestList) {
                String oj = contest.getOj();
                String loginUsername = "";
                String loginPassword = "";

                switch (oj) {
                    case "cf":
                    case "gym":
                        // 获取 cf 账号，可能为 null
                        loginUsername = account.get("cf");
                        loginPassword = getLoginPassword(loginUsername, switchConfig.getCfUsernameList(),
                                switchConfig.getCfPasswordList());
                        break;
                    case "hdu":
                        // 获取 hdu 账号，可能为 null
                        loginUsername = account.get("hdu");
                        loginPassword = getLoginPassword(loginUsername, switchConfig.getHduUsernameList(),
                                switchConfig.getHduPasswordList());
                        break;
                    default:
                        loginUsername = "";
                        loginPassword = "";
                }
                accountList.add(new Pair_<>(loginUsername, loginPassword));
            }

            // 处理传入信息
            StatisticVO statisticVo = new StatisticVO()
                    .setKeyword(keyword)
                    .setContestList(contestList)
                    .setPercentList(percentList)
                    .setAccountList(accountList)
                    .setData(data);

            acmContestRankVoList = contestRankManager.getStatisticRankList(statisticVo);
        }

        return acmContestRankVoList;
    }

    private static <T> T parseJsonWithType(String json, TypeReference<T> typeReference) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
