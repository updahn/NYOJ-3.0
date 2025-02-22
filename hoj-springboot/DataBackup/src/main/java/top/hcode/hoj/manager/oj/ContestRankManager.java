package top.hcode.hoj.manager.oj;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.apache.shiro.SecurityUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.vo.ACMContestRankVO;
import top.hcode.hoj.pojo.vo.OIContestRankVO;
import top.hcode.hoj.pojo.vo.UserContestsRankingVO;
import top.hcode.hoj.shiro.AccountProfile;

import javax.annotation.Resource;
import java.util.stream.Collectors;

import java.util.*;
import java.util.regex.*;

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
            Boolean isContainsAfterContestJudge) {

        // 进行排序计算
        List<ACMContestRankVO> orderResultList = contestCalculateRankManager.calcACMRank(isOpenSealRank,
                removeStar,
                contest,
                currentUserId,
                concernedList,
                externalCidList,
                isContainsAfterContestJudge);

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
        return getPagingRankList(orderResultList, currentPage, limit);
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
            Boolean isContainsAfterContestJudge) {

        List<OIContestRankVO> orderResultList = contestCalculateRankManager.calcOIRank(isOpenSealRank,
                removeStar,
                contest,
                currentUserId,
                concernedList,
                externalCidList,
                isContainsAfterContestJudge);

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
        return getPagingRankList(orderResultList, currentPage, limit);
    }

    /**
     * @param isOpenSealRank              是否封榜
     * @param removeStar                  是否移除打星队伍
     * @param currentUserId               当前用户id
     * @param concernedList               关联比赛的id列表
     * @param contest                     比赛信息
     * @param isContainsAfterContestJudge 是否包含比赛结束后的提交
     * @param selectedTime                比赛跳转榜单的时间
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
            Long selectedTime) {

        // 进行排序计算
        List<ACMContestRankVO> orderResultList = contestCalculateRankManager.calcACMRank(
                isOpenSealRank,
                removeStar,
                contest,
                currentUserId,
                concernedList,
                externalCidList,
                isContainsAfterContestJudge);

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
        List<ACMContestRankVO> acmContestRankVOS = contestCalculateRankManager.calcACMRank(isOpenSealRank,
                removeStar,
                contest,
                currentUserId,
                concernedList,
                externalCidList,
                useCache,
                cacheTime,
                isContainsAfterContestJudge);

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
        return getPagingRankList(acmContestRankVOS, currentPage, limit);
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
                isContainsAfterContestJudge);

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
        return getPagingRankList(oiContestRankVOList, currentPage, limit);
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

    private <T> Page<T> getPagingRankList(List<T> rankList, int currentPage, int limit) {
        Page<T> page = new Page<>(currentPage, limit);
        int count = rankList.size();
        List<T> pageList = new ArrayList<>();
        int currId = currentPage > 1 ? (currentPage - 1) * limit : 0;
        for (int i = 0; i < limit && i < count - currId; i++) {
            pageList.add(rankList.get(currId + i));
        }
        page.setSize(limit);
        page.setCurrent(currentPage);
        page.setTotal(count);
        page.setRecords(pageList);
        return page;
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

}