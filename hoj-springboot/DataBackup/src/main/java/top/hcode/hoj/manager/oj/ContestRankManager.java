package top.hcode.hoj.manager.oj;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;

import org.apache.shiro.SecurityUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.result.Paginate;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.vo.ACMContestRankVO;
import top.hcode.hoj.pojo.vo.StatisticVO;
import top.hcode.hoj.pojo.vo.OIContestRankVO;
import top.hcode.hoj.pojo.vo.UserContestsRankingVO;
import top.hcode.hoj.shiro.AccountProfile;

import javax.annotation.Resource;
import java.util.stream.Collectors;
import java.util.*;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/11 20:30
 * @Description:
 */
@Component
public class ContestRankManager {

    @Resource
    private ContestCalculateRankManager contestCalculateRankManager;

    /**
     * @param isOpenSealRank              是否封榜
     * @param removeStar                  是否移除打星队伍
     * @param currentUserId               当前用户id
     * @param concernedList               关联比赛的id列表
     * @param contest                     比赛信息
     * @param currentPage                 当前页面
     * @param limit                       分页大小
     * @param keyword                     搜索关键词：匹配学校或榜单显示名称
     * @param isContainsAfterContestJudge 是否包含比赛结束后的提交
     * @param time                        距离比赛开始的秒数
     * @desc 获取ACM比赛排行榜
     */
    public IPage<ACMContestRankVO> getContestACMRankPage(Boolean isOpenSealRank,
            Boolean removeStar,
            String currentUserId,
            List<String> concernedList,
            List<Integer> externalCidList,
            Contest contest,
            int currentPage,
            int limit,
            String keyword,
            Boolean isContainsAfterContestJudge,
            Long time) {

        List<ACMContestRankVO> orderResultList = getContestACMRankList(
                isOpenSealRank,
                removeStar,
                currentUserId,
                concernedList,
                externalCidList,
                contest,
                isContainsAfterContestJudge,
                time);

        if (StrUtil.isNotBlank(keyword)) {
            String finalKeyword = keyword.trim().toLowerCase();
            orderResultList = orderResultList.stream()
                    .filter(rankVo -> filterBySchoolORRankShowName(finalKeyword,
                            rankVo.getSchool(),
                            getUserRankShowName(contest.getRankShowName(),
                                    rankVo.getUsername(),
                                    rankVo.getRealname(),
                                    rankVo.getNickname())))
                    .collect(Collectors.toList());
        }
        // 计算好排行榜，然后进行分页
        return Paginate.paginateListToIPage(orderResultList, currentPage, limit);
    }

    public IPage<ACMContestRankVO> getSynchronousACMRankPage(Boolean isOpenSealRank,
            Boolean removeStar,
            String currentUserId,
            List<String> concernedList,
            List<Integer> externalCidList,
            Contest contest,
            int currentPage,
            int limit,
            String keyword,
            Boolean isContainsAfterContestJudge,
            Long time) {

        // 进行排序计算
        List<ACMContestRankVO> orderResultList = contestCalculateRankManager.calcSynchronousACMRank(isOpenSealRank,
                removeStar,
                contest,
                currentUserId,
                concernedList,
                externalCidList,
                isContainsAfterContestJudge,
                time);

        if (StrUtil.isNotBlank(keyword)) {
            String finalKeyword = keyword.trim().toLowerCase();
            orderResultList = orderResultList.stream()
                    .filter(rankVo -> filterBySchoolORRankShowName(finalKeyword,
                            rankVo.getSchool(),
                            getUserRankShowName(contest.getRankShowName(),
                                    rankVo.getUsername(),
                                    rankVo.getRealname(),
                                    rankVo.getNickname())))
                    .collect(Collectors.toList());
        }

        // 计算好排行榜，然后进行分页
        return Paginate.paginateListToIPage(orderResultList, currentPage, limit);
    }

    /**
     * @param isOpenSealRank              是否封榜
     * @param removeStar                  是否移除打星队伍
     * @param currentUserId               当前用户id
     * @param concernedList               关联比赛的id列表
     * @param contest                     比赛信息
     * @param currentPage                 当前页面
     * @param limit                       分页大小
     * @param keyword                     搜索关键词：匹配学校或榜单显示名称
     * @param isContainsAfterContestJudge 是否包含比赛结束后的提交
     * @param time                        距离比赛开始的秒数
     * @desc 获取OI比赛排行榜
     */
    public IPage<OIContestRankVO> getContestOIRankPage(Boolean isOpenSealRank,
            Boolean removeStar,
            String currentUserId,
            List<String> concernedList,
            List<Integer> externalCidList,
            Contest contest,
            int currentPage,
            int limit,
            String keyword,
            Boolean isContainsAfterContestJudge,
            Long time) {

        List<OIContestRankVO> orderResultList = contestCalculateRankManager.calcOIRank(isOpenSealRank,
                removeStar,
                contest,
                currentUserId,
                concernedList,
                externalCidList,
                isContainsAfterContestJudge,
                time);

        if (StrUtil.isNotBlank(keyword)) {
            String finalKeyword = keyword.trim().toLowerCase();
            orderResultList = orderResultList.stream()
                    .filter(rankVo -> filterBySchoolORRankShowName(finalKeyword,
                            rankVo.getSchool(),
                            getUserRankShowName(contest.getRankShowName(),
                                    rankVo.getUsername(),
                                    rankVo.getRealname(),
                                    rankVo.getNickname())))
                    .collect(Collectors.toList());
        }

        // 计算好排行榜，然后进行分页
        return Paginate.paginateListToIPage(orderResultList, currentPage, limit);
    }

    /**
     * @param isOpenSealRank              是否封榜
     * @param removeStar                  是否移除打星队伍
     * @param currentUserId               当前用户id
     * @param concernedList               关联比赛的id列表
     * @param contest                     比赛信息
     * @param isContainsAfterContestJudge 是否包含比赛结束后的提交
     * @param time                        距离比赛开始的秒数
     * @desc 获取ACM比赛排行榜
     */
    public List<ACMContestRankVO> getContestACMRankList(
            Boolean isOpenSealRank,
            Boolean removeStar,
            String currentUserId,
            List<String> concernedList,
            List<Integer> externalCidList,
            Contest contest,
            Boolean isContainsAfterContestJudge,
            Long time) {

        // 进行排序计算
        List<ACMContestRankVO> orderResultList = contestCalculateRankManager.calcACMRank(
                isOpenSealRank,
                removeStar,
                contest,
                currentUserId,
                concernedList,
                externalCidList,
                isContainsAfterContestJudge,
                time);

        return orderResultList;
    }

    /**
     * 获取ACM比赛排行榜外榜
     *
     * @param isOpenSealRank              是否开启封榜
     * @param removeStar                  是否移除打星队伍
     * @param contest                     比赛信息
     * @param currentUserId               当前用户id
     * @param concernedList               关注用户uid列表
     * @param externalCidList             关联比赛id列表
     * @param currentPage                 当前页码
     * @param limit                       分页大小
     * @param keyword                     搜索关键词
     * @param useCache                    是否启用缓存
     * @param cacheTime                   缓存时间（秒）
     * @param isContainsAfterContestJudge 是否包含比赛结束后的提交
     * @return
     */
    public IPage<ACMContestRankVO> getACMContestScoreboard(Boolean isOpenSealRank,
            Boolean removeStar,
            Contest contest,
            String currentUserId,
            List<String> concernedList,
            List<Integer> externalCidList,
            int currentPage,
            int limit,
            String keyword,
            Boolean useCache,
            Long cacheTime,
            Boolean isContainsAfterContestJudge) {
        if (CollectionUtil.isNotEmpty(externalCidList)) {
            useCache = false;
        }

        List<ACMContestRankVO> acmContestRankVOS = getContestACMRankList(
                isOpenSealRank,
                removeStar,
                currentUserId,
                concernedList,
                externalCidList,
                contest,
                isContainsAfterContestJudge,
                null);

        if (StrUtil.isNotBlank(keyword)) {
            String finalKeyword = keyword.trim().toLowerCase();
            acmContestRankVOS = acmContestRankVOS.stream()
                    .filter(rankVo -> filterBySchoolORRankShowName(finalKeyword,
                            rankVo.getSchool(),
                            getUserRankShowName(contest.getRankShowName(),
                                    rankVo.getUsername(),
                                    rankVo.getRealname(),
                                    rankVo.getNickname())))
                    .collect(Collectors.toList());
        }
        return Paginate.paginateListToIPage(acmContestRankVOS, currentPage, limit);
    }

    // 筛选关键词 + 比例调整 + 重新排序
    public List<ACMContestRankVO> getStatisticRankList(StatisticVO statisticVo)
            throws StatusFailException, StatusForbiddenException, Exception {
        List<Contest> contestList = statisticVo.getContestList();
        List<Integer> percentList = statisticVo.getPercentList();
        String keyword = statisticVo.getKeyword();

        List<ACMContestRankVO> result = contestCalculateRankManager.calcStatisticRank(statisticVo);

        return getDealList(percentList, contestList, keyword, result);
    }

    /**
     * 获取OI比赛排行榜外榜
     *
     * @param isOpenSealRank              是否开启封榜
     * @param removeStar                  是否移除打星队伍
     * @param contest                     比赛信息
     * @param currentUserId               当前用户id
     * @param concernedList               关注用户uid列表
     * @param externalCidList             关联比赛id列表
     * @param currentPage                 当前页码
     * @param limit                       分页大小
     * @param keyword                     搜索关键词
     * @param useCache                    是否启用缓存
     * @param cacheTime                   缓存时间（秒）
     * @param isContainsAfterContestJudge 是否包含比赛结束后的提交
     * @return
     */
    public IPage<OIContestRankVO> getOIContestScoreboard(Boolean isOpenSealRank,
            Boolean removeStar,
            Contest contest,
            String currentUserId,
            List<String> concernedList,
            List<Integer> externalCidList,
            int currentPage,
            int limit,
            String keyword,
            Boolean useCache,
            Long cacheTime,
            Boolean isContainsAfterContestJudge) {

        if (CollectionUtil.isNotEmpty(externalCidList)) {
            useCache = false;
        }
        List<OIContestRankVO> oiContestRankVOList = contestCalculateRankManager.calcOIRank(isOpenSealRank,
                removeStar,
                contest,
                currentUserId,
                concernedList,
                externalCidList,
                useCache,
                cacheTime,
                isContainsAfterContestJudge,
                null);

        if (StrUtil.isNotBlank(keyword)) {
            String finalKeyword = keyword.trim().toLowerCase();
            oiContestRankVOList = oiContestRankVOList.stream()
                    .filter(rankVo -> filterBySchoolORRankShowName(finalKeyword,
                            rankVo.getSchool(),
                            getUserRankShowName(contest.getRankShowName(),
                                    rankVo.getUsername(),
                                    rankVo.getRealname(),
                                    rankVo.getNickname())))
                    .collect(Collectors.toList());
        }
        return Paginate.paginateListToIPage(oiContestRankVOList, currentPage, limit);
    }

    /**
     * @param uid
     * @param username
     * @return
     * @Description 获取用户的比赛名次变化图
     */
    public UserContestsRankingVO getContestsRanking(
            List<Contest> contestList,
            String uid,
            String username) throws StatusFailException {

        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        if (StringUtils.isEmpty(uid) && StringUtils.isEmpty(username)) {
            if (userRolesVo != null) {
                uid = userRolesVo.getUid();
            } else {
                throw new StatusFailException("请求参数错误：uid和username不能都为空！");
            }
        }

        UserContestsRankingVO userContestsRankingVO = new UserContestsRankingVO();
        userContestsRankingVO.setEndDate(DateUtil.format(new Date(), "yyyy-MM-dd"));

        if (CollectionUtils.isEmpty(contestList)) {
            userContestsRankingVO.setDataList(new ArrayList<>());
            userContestsRankingVO.setSolvedList(new ArrayList<>());
            return userContestsRankingVO;
        }

        List<HashMap<String, Object>> dataList = new ArrayList<>();
        List<Long> contestPids = new ArrayList<>();
        String finalKeyword = username.trim().toLowerCase();
        // 将 uid 声明为 final
        final String finalUid = uid;

        // 并行流处理
        contestList.parallelStream().forEach(contest -> {
            List<ACMContestRankVO> orderResultList = getContestACMRankList(
                    false,
                    true,
                    null,
                    null,
                    null,
                    contest,
                    false,
                    null);

            orderResultList = orderResultList.stream()
                    .filter(rankVo -> filterBySchoolORRankShowName(finalKeyword,
                            rankVo.getSchool(),
                            getUserRankShowName(contest.getRankShowName(),
                                    rankVo.getUsername(),
                                    rankVo.getRealname(),
                                    rankVo.getNickname())))
                    .collect(Collectors.toList());

            if (orderResultList.size() > 0) {
                String user_uid = orderResultList.get(0).getUid();
                if (user_uid.equals(finalUid)) {
                    contestPids.add(contest.getId());
                    Integer rank = orderResultList.get(0).getRank();
                    Date startTime = contest.getStartTime();
                    String dateStr = DateUtil.format(startTime, "yyyy-MM-dd HH:mm");
                    HashMap<String, Object> tmp = new HashMap<>(4);
                    tmp.put("date", dateStr);
                    tmp.put("rank", rank);
                    tmp.put("cid", contest.getId());
                    tmp.put("title", contest.getTitle());
                    synchronized (dataList) {
                        dataList.add(tmp);
                    }
                }
            }
        });

        if (CollectionUtils.isEmpty(dataList)) {
            userContestsRankingVO.setSolvedList(new ArrayList<>());
            userContestsRankingVO.setDataList(new ArrayList<>());
            return userContestsRankingVO;
        }
        userContestsRankingVO.setSolvedList(contestPids);
        userContestsRankingVO.setDataList(dataList);
        return userContestsRankingVO;
    }

    private String getUserRankShowName(String contestRankShowName, String username, String realName, String nickname) {
        switch (contestRankShowName) {
            case "username":
                return username;
            case "realname":
                return realName;
            case "nickname":
                return nickname;
        }
        return null;
    }

    private boolean filterBySchoolORRankShowName(String keyword, String school, String rankShowName) {
        if (StrUtil.isNotEmpty(school) && school.toLowerCase().contains(keyword)) {
            return true;
        }
        return StrUtil.isNotEmpty(rankShowName) && rankShowName.toLowerCase().contains(keyword);
    }

    public List<ACMContestRankVO> getDealList(List<Integer> percentList, List<Contest> contestList, String keyword,
            List<ACMContestRankVO> result) {
        // 筛选关键词
        result = getKeywordedList(keyword, contestList, result);
        // 添加比例
        result = getPercentedList(percentList, contestList, result);
        // 重新排序
        result = getPercentedList(result);

        return result;
    }

    public List<ACMContestRankVO> getPercentedList(List<Integer> percentList, List<Contest> contestList,
            List<ACMContestRankVO> result) {
        // 计算比例
        if (!CollectionUtils.isEmpty(percentList)) {
            // 判断是否存在不是100的元素
            boolean hasNot100 = percentList.stream().anyMatch(percent -> percent != 100);
            if (hasNot100) {
                for (ACMContestRankVO acmContestRankVO : result) {
                    double totalAc = 0.0;
                    double totalTime = 0.0;

                    HashMap<String, HashMap<String, Object>> submissionInfo = acmContestRankVO.getSubmissionInfo();

                    for (int j = 0; j < contestList.size(); j++) {
                        Contest contest = contestList.get(j); // 提取 contestList.get(j) 为局部变量
                        String key = contest.getOj().equals("default")
                                ? contest.getId().toString()
                                : contest.getOj() + contest.getTitle(); // 简化 key 生成

                        HashMap<String, Object> contestInfo = submissionInfo.get(key);

                        if (contestInfo == null) {
                            continue;
                        }

                        // 使用正则表达式去除原有的可能的 " * xx%" 后缀
                        String acString = String.valueOf(contestInfo.get("ac")).replaceAll("\\s\\*\\s\\d+%", "");
                        String totalTimeString = String.valueOf(contestInfo.get("totalTime"))
                                .replaceAll("\\s\\*\\s\\d+%", "");

                        // 先将字符串转换为数字，确保是可以转换的
                        double acValue = Double.parseDouble(acString);
                        double totalTimeValue = Double.parseDouble(totalTimeString);

                        int percentValue = percentList.get(j);

                        // 进行累加
                        totalAc += acValue * percentValue / 100.0;
                        totalTime += totalTimeValue * percentValue / 100.0;

                        if (percentValue != 100) {
                            String suffix = " * " + percentValue + "%";

                            // 重新添加新的后缀
                            contestInfo.put("ac", acString + suffix);
                            contestInfo.put("totalTime", totalTimeString + suffix);
                        }

                    }

                    // 保留三位小数
                    acmContestRankVO.setAc(Double.parseDouble(String.format("%.3f", totalAc)));
                    acmContestRankVO.setTotalTime(Double.parseDouble(String.format("%.3f", totalTime)));
                }
            }
        }
        return result;
    }

    public List<ACMContestRankVO> getPercentedList(List<ACMContestRankVO> result) {
        return contestCalculateRankManager.getSortedRankList(result);
    }

    public List<ACMContestRankVO> getKeywordedList(String keyword, List<Contest> contestList,
            List<ACMContestRankVO> result) {
        // 筛选关键词
        if (StrUtil.isNotBlank(keyword)) {
            String finalKeyword = keyword.trim().toLowerCase();
            List<ACMContestRankVO> filteredResult = result.stream()
                    .filter(rankVo -> contestList.stream()
                            .anyMatch(contest -> filterBySchoolORRankShowName(
                                    finalKeyword,
                                    rankVo.getSchool(),
                                    "default".equals(contest.getOj())
                                            ? getUserRankShowName(contest.getRankShowName(), rankVo.getUsername(),
                                                    rankVo.getRealname(), rankVo.getNickname())
                                            : rankVo.getRealname())))
                    .collect(Collectors.toList());

            // 如果筛选结果不为空，则更新对应的result
            if (!CollectionUtils.isEmpty(filteredResult)) {
                result = filteredResult;
            }
        }
        return result;
    }
}