package top.hcode.hoj.manager.oj;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import top.hcode.hoj.dao.contest.ContestRecordEntityService;
import top.hcode.hoj.dao.contest.TeamSignEntityService;
import top.hcode.hoj.dao.group.GroupMemberEntityService;
import top.hcode.hoj.dao.school.SchoolEntityService;
import top.hcode.hoj.dao.user.UserInfoEntityService;
import top.hcode.hoj.dao.user.UserRoleEntityService;
import top.hcode.hoj.pojo.bo.Pair_;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.entity.contest.TeamSign;
import top.hcode.hoj.pojo.entity.group.GroupMember;
import top.hcode.hoj.pojo.entity.school.School;
import top.hcode.hoj.pojo.vo.ACMContestRankVO;
import top.hcode.hoj.pojo.vo.ContestAwardConfigVO;
import top.hcode.hoj.pojo.vo.ContestRecordVO;
import top.hcode.hoj.pojo.vo.StatisticVO;
import top.hcode.hoj.pojo.vo.OIContestRankVO;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.utils.RedisUtils;

import javax.annotation.Resource;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/11 20:11
 * @Description:
 */
@Component
public class ContestCalculateRankManager {

    @Resource
    private UserInfoEntityService userInfoEntityService;

    @Resource
    private RedisUtils redisUtils;

    @Resource
    private ContestRecordEntityService contestRecordEntityService;

    @Resource
    private SynchronousManager synchronousManager;

    @Resource
    private ScraperManager scraperManager;

    @Autowired
    private GroupMemberEntityService groupMemberEntityService;

    @Autowired
    private SchoolEntityService schoolEntityService;

    @Autowired
    private TeamSignEntityService teamSignEntityService;

    @Autowired
    private UserRoleEntityService userRoleEntityService;

    public List<ACMContestRankVO> calcACMRank(boolean isOpenSealRank,
            boolean removeStar,
            Contest contest,
            String currentUserId,
            List<String> concernedList,
            List<Integer> externalCidList,
            boolean isContainsAfterContestJudge,
            Long time) {
        return calcACMRank(isOpenSealRank,
                removeStar,
                contest,
                currentUserId,
                concernedList,
                externalCidList,
                false,
                null,
                isContainsAfterContestJudge,
                time);
    }

    public List<ACMContestRankVO> calcSynchronousACMRank(boolean isOpenSealRank,
            boolean removeStar,
            Contest contest,
            String currentUserId,
            List<String> concernedList,
            List<Integer> externalCidList,
            boolean isContainsAfterContestJudge,
            Long time) {
        return calcSynchronousACMRank(isOpenSealRank,
                removeStar,
                contest,
                currentUserId,
                concernedList,
                externalCidList,
                false,
                null,
                isContainsAfterContestJudge,
                time);
    }

    public List<OIContestRankVO> calcOIRank(Boolean isOpenSealRank,
            Boolean removeStar,
            Contest contest,
            String currentUserId,
            List<String> concernedList,
            List<Integer> externalCidList,
            Boolean isContainsAfterContestJudge,
            Long time) {

        return calcOIRank(isOpenSealRank,
                removeStar,
                contest,
                currentUserId,
                concernedList,
                externalCidList,
                false,
                null,
                isContainsAfterContestJudge,
                time);
    }

    public List<ACMContestRankVO> calcEXAMRank(boolean isOpenSealRank,
            boolean removeStar,
            Contest contest,
            String currentUserId,
            List<String> concernedList,
            List<Integer> externalCidList,
            boolean isContainsAfterContestJudge,
            Long time) {
        return calcACMRank(isOpenSealRank,
                removeStar,
                contest,
                currentUserId,
                concernedList,
                externalCidList,
                false,
                null,
                isContainsAfterContestJudge,
                time);
    }

    /**
     * @param isOpenSealRank              是否是查询封榜后的数据
     * @param removeStar                  是否需要移除打星队伍
     * @param contest                     比赛实体信息
     * @param currentUserId               当前查看榜单的用户uuid,不为空则将该数据复制一份放置列表最前
     * @param concernedList               关注的用户（uuid）列表
     * @param externalCidList             榜单额外显示的比赛列表
     * @param useCache                    是否对初始排序计算的结果进行缓存
     * @param cacheTime                   缓存的时间 单位秒
     * @param isContainsAfterContestJudge 是否包含比赛结束后的提交
     * @param time                        距离比赛开始的秒数
     * @MethodName calcACMRank
     * @Description TODO
     * @Return
     * @Since 2021/12/10
     */
    public List<ACMContestRankVO> calcACMRank(boolean isOpenSealRank,
            boolean removeStar,
            Contest contest,
            String currentUserId,
            List<String> concernedList,
            List<Integer> externalCidList,
            boolean useCache,
            Long cacheTime,
            boolean isContainsAfterContestJudge,
            Long time) {
        List<ACMContestRankVO> orderResultList;
        Long minSealRankTime = null;
        Long maxSealRankTime = null;
        if (useCache) {
            String key = null;
            if (isContainsAfterContestJudge) {
                key = Constants.Contest.CONTEST_RANK_CAL_RESULT_CACHE.getName() + "_" + contest.getId();
            } else {
                key = Constants.Contest.CONTEST_RANK_CAL_RESULT_CACHE.getName() + "_contains_after_" + contest.getId();
            }
            orderResultList = (List<ACMContestRankVO>) redisUtils.get(key);
            if (orderResultList == null) {
                if (isOpenSealRank) {
                    minSealRankTime = DateUtil.between(contest.getStartTime(), contest.getSealRankTime(),
                            DateUnit.SECOND);
                    maxSealRankTime = contest.getDuration();
                }
                orderResultList = getACMOrderRank(contest, isOpenSealRank, minSealRankTime, maxSealRankTime,
                        externalCidList, isContainsAfterContestJudge, time);
                redisUtils.set(key, orderResultList, cacheTime);
            }
        } else {
            if (isOpenSealRank) {
                minSealRankTime = DateUtil.between(contest.getStartTime(), contest.getSealRankTime(), DateUnit.SECOND);
                maxSealRankTime = contest.getDuration();
            }
            orderResultList = getACMOrderRank(contest, isOpenSealRank, minSealRankTime, maxSealRankTime,
                    externalCidList, isContainsAfterContestJudge, time);
        }

        // 需要打星的用户名列表
        HashMap<String, Boolean> starAccountMap = starAccountToMap(contest.getStarAccount());

        Queue<ContestAwardConfigVO> awardConfigVoList = null;
        boolean isNeedSetAward = contest.getAwardType() != null && contest.getAwardType() > 0;
        if (removeStar) {
            // 如果选择了移除打星队伍，同时该用户属于打星队伍，则将其移除
            orderResultList.removeIf(acmContestRankVo -> starAccountMap.containsKey(acmContestRankVo.getUsername()));
            if (isNeedSetAward) {
                awardConfigVoList = getContestAwardConfigList(contest.getAwardConfig(), contest.getAwardType(),
                        orderResultList.size());
            }
        } else {
            if (isNeedSetAward) {
                if (contest.getAwardType() == 1) {
                    long count = orderResultList.stream().filter(e -> !starAccountMap.containsKey(e.getUsername()))
                            .count();
                    awardConfigVoList = getContestAwardConfigList(contest.getAwardConfig(), contest.getAwardType(),
                            (int) count);
                } else {
                    awardConfigVoList = getContestAwardConfigList(contest.getAwardConfig(), contest.getAwardType(),
                            orderResultList.size());
                }
            }
        }
        boolean needAddConcernedUser = false;
        if (!CollectionUtils.isEmpty(concernedList)) {
            needAddConcernedUser = true;
            // 移除关注列表与当前用户重复
            concernedList.remove(currentUserId);
        }

        // 重新排序
        List<ACMContestRankVO> result = new ArrayList<>();

        if (contest.getType().intValue() == Constants.Contest.TYPE_EXAM.getCode()) {
            result = orderResultList.stream()
                    .sorted(Comparator.comparing(ACMContestRankVO::getTotalScore, Comparator.reverseOrder()) // 先以总分数降序
                            .thenComparing(ACMContestRankVO::getAc, Comparator.reverseOrder()) // 再以总ac数升序
                            .thenComparing(ACMContestRankVO::getTotalTime) // 再以总耗时升序
                    ).collect(Collectors.toList());
        } else {
            result = orderResultList.stream()
                    .sorted(Comparator.comparing(ACMContestRankVO::getAc, Comparator.reverseOrder()) // 先以总ac数降序
                            .thenComparing(ACMContestRankVO::getTotalTime) // 再以总耗时升序
                    ).collect(Collectors.toList());
        }

        // 将本oj的synchronous状态设为false
        orderResultList.forEach(ACMContestRankvo -> ACMContestRankvo.setSynchronous(false));

        return getTopRank(contest, removeStar, isNeedSetAward, currentUserId, concernedList, result, starAccountMap,
                awardConfigVoList,
                needAddConcernedUser);
    }

    /**
     * @param isOpenSealRank              是否是查询封榜后的数据
     * @param removeStar                  是否需要移除打星队伍
     * @param contest                     比赛实体信息
     * @param currentUserId               当前查看榜单的用户uuid,不为空则将该数据复制一份放置列表最前
     * @param concernedList               关注的用户（uuid）列表
     * @param externalCidList             榜单额外显示的比赛列表
     * @param useCache                    是否对初始排序计算的结果进行缓存
     * @param cacheTime                   缓存的时间 单位秒
     * @param isContainsAfterContestJudge 是否包含比赛结束后的提交
     * @param selectedTime                比赛跳转榜单的时间
     * @MethodName calcSynchronousACMRank
     * @Description TODO
     * @Return
     * @Since 2021/12/10
     */
    public List<ACMContestRankVO> calcSynchronousACMRank(boolean isOpenSealRank,
            boolean removeStar,
            Contest contest,
            String currentUserId,
            List<String> concernedList,
            List<Integer> externalCidList,
            boolean useCache,
            Long cacheTime,
            boolean isContainsAfterContestJudge,
            Long selectedTime) {
        List<ACMContestRankVO> orderResultList;
        Long minSealRankTime = null;
        Long maxSealRankTime = null;
        if (useCache) {
            String key = null;
            if (isContainsAfterContestJudge) {
                key = Constants.Contest.CONTEST_RANK_CAL_RESULT_CACHE.getName() + "_" + contest.getId();
            } else {
                key = Constants.Contest.CONTEST_RANK_CAL_RESULT_CACHE.getName() + "_contains_after_" + contest.getId();
            }
            orderResultList = (List<ACMContestRankVO>) redisUtils.get(key);
            if (orderResultList == null) {
                if (isOpenSealRank) {
                    minSealRankTime = DateUtil.between(contest.getStartTime(), contest.getSealRankTime(),
                            DateUnit.SECOND);
                    maxSealRankTime = contest.getDuration();
                }
                orderResultList = getACMOrderRank(contest, isOpenSealRank, minSealRankTime, maxSealRankTime,
                        externalCidList, isContainsAfterContestJudge, selectedTime);
                redisUtils.set(key, orderResultList, cacheTime);
            }
        } else {
            if (isOpenSealRank) {
                minSealRankTime = DateUtil.between(contest.getStartTime(), contest.getSealRankTime(), DateUnit.SECOND);
                maxSealRankTime = contest.getDuration();
            }
            orderResultList = getACMOrderRank(contest, isOpenSealRank, minSealRankTime, maxSealRankTime,
                    externalCidList, isContainsAfterContestJudge, selectedTime);
        }

        // 需要打星的用户名列表
        HashMap<String, Boolean> starAccountMap = starAccountToMap(contest.getStarAccount());

        Queue<ContestAwardConfigVO> awardConfigVoList = null;
        boolean isNeedSetAward = contest.getAwardType() != null && contest.getAwardType() > 0;
        if (removeStar) {
            // 如果选择了移除打星队伍，同时该用户属于打星队伍，则将其移除
            orderResultList.removeIf(acmContestRankVo -> starAccountMap.containsKey(acmContestRankVo.getUsername()));
            if (isNeedSetAward) {
                awardConfigVoList = getContestAwardConfigList(contest.getAwardConfig(), contest.getAwardType(),
                        orderResultList.size());
            }
        } else {
            if (isNeedSetAward) {
                if (contest.getAwardType() == 1) {
                    long count = orderResultList.stream().filter(e -> !starAccountMap.containsKey(e.getUsername()))
                            .count();
                    awardConfigVoList = getContestAwardConfigList(contest.getAwardConfig(), contest.getAwardType(),
                            (int) count);
                } else {
                    awardConfigVoList = getContestAwardConfigList(contest.getAwardConfig(), contest.getAwardType(),
                            orderResultList.size());
                }
            }
        }

        boolean needAddConcernedUser = false;
        if (!CollectionUtils.isEmpty(concernedList)) {
            needAddConcernedUser = true;
            // 移除关注列表与当前用户重复
            concernedList.remove(currentUserId);
        }

        // 将本oj的synchronous状态设为false
        orderResultList.forEach(ACMContestRankvo -> ACMContestRankvo.setSynchronous(false));

        // 是否开启同步赛
        if (contest.getAuth().intValue() == Constants.Contest.AUTH_SYNCHRONOUS.getCode()) {
            List<ACMContestRankVO> synchronousResultList = synchronousManager.getSynchronousRankList(contest,
                    isContainsAfterContestJudge, removeStar, selectedTime);
            if (!CollectionUtils.isEmpty(synchronousResultList)) {
                // 将两个列表合并
                orderResultList.addAll(synchronousResultList);

                // TODO 同步赛首A修复
                // 将所有的ACMContestRankVO中的Submisson拆分
                List<HashMap<String, Object>> submissions = orderResultList.stream()
                        .map(ACMContestRankVO::getSubmissionInfo)
                        .flatMap(submissionInfo -> getSubmissions(submissionInfo).stream())
                        .collect(Collectors.toList());

                // 按照时间从小到大进行排序
                Collections.sort(submissions, new SubmissonComparator());

                HashMap<String, Long> firstACMap = new HashMap<>();

                for (HashMap<String, Object> submission : submissions) {
                    Boolean isAC = false;
                    String displayId = "";
                    Iterator<Map.Entry<String, Object>> internalIterator = submission.entrySet().iterator();
                    while (internalIterator.hasNext()) {
                        Map.Entry<String, Object> internalEntry = internalIterator.next();
                        String key = internalEntry.getKey();
                        Object value = internalEntry.getValue();

                        if ("isAC".equals(key) && value != null) {
                            isAC = true;
                        }

                        if ("displayId".equals(key)) {
                            displayId = value.toString();
                        }

                        // 已经通过题目
                        if ("ACTime".equals(key) && isAC && displayId != "") { // 检查键是否为"ACTime"
                            // 记录当前记录的提交时间
                            Long ACTime = ((Number) value).longValue();

                            Long time = firstACMap.getOrDefault(displayId, null);
                            if (time == null) {
                                firstACMap.put(displayId, ACTime);
                            } else {
                                // 相同提交时间也是first AC
                                if (time.longValue() == ACTime.longValue()) {
                                }
                            }
                            break;
                        }
                    }
                }
                for (ACMContestRankVO contestRankVO : orderResultList) {
                    HashMap<String, HashMap<String, Object>> submission = contestRankVO.getSubmissionInfo();
                    // 遍历 submissionInfos
                    Iterator<Map.Entry<String, HashMap<String, Object>>> iterator = submission.entrySet().iterator();

                    while (iterator.hasNext()) {
                        Map.Entry<String, HashMap<String, Object>> entry = iterator.next();
                        String problemKey = entry.getKey();
                        HashMap<String, Object> submissionData = entry.getValue();

                        Boolean isAC = false;
                        int errorNumber = -1;
                        Long ACTime = -1L;
                        // 遍历内部HashMap
                        Iterator<Map.Entry<String, Object>> internalIterator = submissionData.entrySet().iterator();
                        while (internalIterator.hasNext()) {
                            Map.Entry<String, Object> internalEntry = internalIterator.next();
                            String key = internalEntry.getKey();
                            Object value = internalEntry.getValue();

                            if ("isAC".equals(key) && value != null) {
                                isAC = true;
                            }

                            if ("errorNum".equals(key) && value != null) {
                                errorNumber = (int) value;
                            }

                            // 已经通过题目
                            if ("ACTime".equals(key) && isAC) { // 检查键是否为"ACTime"
                                // 判断是不是first AC
                                boolean isFirstAC = false;

                                // 记录当前记录的提交时间
                                ACTime = ((Number) value).longValue();

                                Long firstACValue = firstACMap.get(problemKey);

                                if (firstACMap != null) {
                                    // 相同提交时间也是first AC
                                    if (firstACValue.longValue() == ACTime.longValue()) {
                                        isFirstAC = true;
                                    }
                                    submissionData.put("isFirstAC", isFirstAC);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 重新排序
        List<ACMContestRankVO> result = orderResultList.stream()
                .sorted(Comparator.comparing(ACMContestRankVO::getAc, Comparator.reverseOrder()) // 先以总ac数降序
                        .thenComparing(ACMContestRankVO::getTotalTime) // 再以总耗时升序
                ).collect(Collectors.toList());

        return getTopRank(contest, removeStar, isNeedSetAward, currentUserId, concernedList, result, starAccountMap,
                awardConfigVoList,
                needAddConcernedUser);
    }

    public List<ACMContestRankVO> calcStatisticRank(StatisticVO statisticVo) throws Exception {
        List<Contest> contestList = statisticVo.getContestList();
        List<Pair_<String, String>> accountList = statisticVo.getAccountList();
        HashMap<String, String> data = statisticVo.getData();

        // 暂存所有登录所需的cookies
        HashMap<String, Map<String, String>> cookies = new HashMap<>();
        // 使用 parallelStream 进行并行处理
        for (Contest contest : contestList) {
            try {
                String key = contest.getOj();

                // 如果 cookies 已经包含 key，跳过处理
                if (cookies.containsKey(key))
                    continue;

                // 获取账户信息并执行 scraperManager.getLoginCookies
                Pair_<String, String> account = accountList.get(contestList.indexOf(contest));
                Map<String, String> loginCookies = scraperManager.getLoginCookies(key, account.getKey(),
                        account.getValue());

                // 将获取到的 cookies 放入 map 中
                cookies.put(key, loginCookies);
            } catch (Exception e) {
                e.printStackTrace(); // 处理异常并打印堆栈信息
            }
        }

        HashMap<String, Integer> uidMapIndex = new HashMap<>();
        HashMap<String, Boolean> uidOjMapIndex = new HashMap<>();

        List<ACMContestRankVO> resultList = new ArrayList<>();
        AtomicInteger index = new AtomicInteger(0);
        Map<String, String> usernameToUidMap = new HashMap<>();
        try {
            contestList.parallelStream().forEach(contest -> {
                List<ACMContestRankVO> orderResultList = new ArrayList<>();

                if (!contest.getOj().equals("default")) {
                    try {
                        String key = contest.getOj();
                        Pair_<String, String> account = accountList.get(contestList.indexOf(contest));
                        orderResultList = scraperManager.getScraperInfo(contest.getOj(), contest.getTitle(),
                                cookies.get(key), account.getKey(), account.getValue(), usernameToUidMap);

                    } catch (Exception e) {
                        // 将异常封装为 RuntimeException 以便抛出
                        throw new RuntimeException(e);
                    }
                } else {
                    // 进行排序计算得到用户的排名
                    orderResultList = calcACMRank(
                            false,
                            true,
                            contest,
                            null,
                            null,
                            null,
                            false,
                            null);
                }

                if (orderResultList.size() > 0) {
                    orderResultList.forEach(orderResult -> {
                        ACMContestRankVO ACMContestRankVO;

                        synchronized (uidMapIndex) {
                            String username = orderResult.getUsername();
                            String realname = getValueForKey(data, username, orderResult.getRealname());
                            orderResult.setRealname(realname);

                            String key = StringUtils.isEmpty(realname) ? orderResult.getUid()
                                    : orderResult.getRealname();

                            if (!uidMapIndex.containsKey(key)) { // 如果用户信息还没有记录，初始化参数
                                ACMContestRankVO = new ACMContestRankVO()
                                        .setRealname(orderResult.getRealname())
                                        .setAvatar(orderResult.getAvatar())
                                        .setSchool(orderResult.getSchool())
                                        .setFaculty(orderResult.getFaculty())
                                        .setCourse(orderResult.getCourse())
                                        .setGender(orderResult.getGender())
                                        .setUid(orderResult.getUid())
                                        .setUsername("")
                                        .setNickname(orderResult.getNickname())
                                        .setAc(0.0)
                                        .setTotalTime(0.0)
                                        .setTotal(0)
                                        .setCid(orderResult.getCid())
                                        .setTitle(orderResult.getTitle())
                                        .setStartTime(orderResult.getStartTime())
                                        .setLink(orderResult.getLink())
                                        .setSynchronous(
                                                orderResult.getSynchronous() != null ? orderResult.getSynchronous()
                                                        : false)
                                        .setSubmissionInfo(new HashMap<>());

                                synchronized (resultList) {
                                    resultList.add(ACMContestRankVO);
                                }

                                uidMapIndex.put(key, index.getAndIncrement());
                            } else {
                                ACMContestRankVO = resultList.get(uidMapIndex.get(key));
                            }

                            String cid_username = contest.getOj().equals("default") ? orderResult.getUsername()
                                    : (contest.getOj() + ": " + orderResult.getUsername()) + " \n";

                            if (!uidOjMapIndex.containsKey(cid_username)) {
                                ACMContestRankVO.setUsername(ACMContestRankVO.getUsername() + cid_username);
                                uidOjMapIndex.put(cid_username, true);
                            }
                        }

                        synchronized (ACMContestRankVO) {
                            // 将该场比赛的总AC计入
                            ACMContestRankVO.setAc(ACMContestRankVO.getAc() + orderResult.getAc());
                            // 将该场比赛的总罚时计入
                            ACMContestRankVO.setTotalTime(ACMContestRankVO.getTotalTime() + orderResult.getTotalTime());

                            if (orderResult.getTotal() != null) {
                                // 将该场比赛的总提交数计入
                                ACMContestRankVO.setTotal(ACMContestRankVO.getTotal() + orderResult.getTotal());
                            }

                            String contestKey = (!"default".equals(contest.getOj()) ? contest.getOj() : "")
                                    + contest.getTitle().toString();

                            HashMap<String, Object> submissionInfo = ACMContestRankVO.getSubmissionInfo()
                                    .get(contestKey);

                            if (submissionInfo == null) {
                                submissionInfo = new HashMap<>();
                                submissionInfo.put("title", contest.getTitle());
                                submissionInfo.put("ac", orderResult.getAc());
                                submissionInfo.put("totalTime", orderResult.getTotalTime());
                                submissionInfo.put("link", orderResult.getLink());
                                submissionInfo.put("startTime", orderResult.getStartTime());
                                submissionInfo.put("title", orderResult.getTitle());
                                submissionInfo.put("synchronous", orderResult.getSynchronous());
                            }

                            // 根据条件选择 key 并计入比赛信息
                            ACMContestRankVO.getSubmissionInfo().put(
                                    ("default".equals(contest.getOj()) ? contest.getId().toString()
                                            : contest.getOj() + contest.getTitle().toString()),
                                    submissionInfo);
                        }
                    });
                }
            });

        } catch (RuntimeException e) {
            // 如果并行流中抛出 RuntimeException，将其解包并重新抛出原始异常
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            } else {
                throw e; // 不应该到达这里
            }
        }

        return resultList;
    }

    private List<ACMContestRankVO> getACMOrderRank(Contest contest,
            Boolean isOpenSealRank,
            Long minSealRankTime,
            Long maxSealRankTime,
            List<Integer> externalCidList,
            Boolean isContainsAfterContestJudge,
            Long selectedTime) {

        List<ContestRecordVO> contestRecordList = contestRecordEntityService.getACMContestRecord(contest.getUid(),
                contest.getId(),
                externalCidList,
                contest.getStartTime());

        List<String> superAdminUidList = getSuperAdminUidList(contest.getGid(), contest.getId());

        List<ACMContestRankVO> result = new ArrayList<>();

        HashMap<String, Integer> uidMapIndex = new HashMap<>();

        int index = 0;

        HashMap<String, Long> firstACMap = new HashMap<>();

        HashMap<String, TeamSign> teamMapIndex = new HashMap<>();

        Boolean isOfficial = contest.getAuth().intValue() == Constants.Contest.AUTH_OFFICIAL.getCode();

        if (isOfficial) {
            // 查询该比赛对应的队伍信息
            List<TeamSign> teamSignList = teamSignEntityService
                    .list(new QueryWrapper<TeamSign>().eq("cid", contest.getId()).eq("visible", true));

            teamSignList.forEach(ts -> {
                // 将 username1, username2, username3 组成 Stream，过滤掉 null，然后放入 Map
                Stream.of(ts.getUsername1(), ts.getUsername2(), ts.getUsername3())
                        .filter(Objects::nonNull)
                        .forEach(u -> teamMapIndex.put(u, ts));
            });
        }

        for (ContestRecordVO contestRecord : contestRecordList) {

            TeamSign teamSign = new TeamSign();

            if (isOfficial && teamMapIndex.containsKey(contestRecord.getUsername())) {

                teamSign = teamMapIndex.get(contestRecord.getUsername());

                // 如果这几个人是一个队伍，将队员的提交替换为队长提交
                if (teamSign != null) {
                    contestRecord.setUid(userRoleEntityService.getUidByUsername(teamSign.getUsername1()));
                }
            }

            if (selectedTime != null) {
                Date thisTime = addSeconds(contest.getStartTime(), selectedTime);
                // 提交时间大于查询的时间
                if (contestRecord.getSubmitTime().compareTo(thisTime) > 0) {
                    // 将超过查询时间的数据排除
                    continue;
                }
            }

            if (superAdminUidList.contains(contestRecord.getUid())) { // 超级管理员的提交不入排行榜
                continue;
            }

            boolean isAfterContestJudge = contestRecord.getSubmitTime().getTime() >= contest.getEndTime().getTime();
            boolean isBeforeContestJudge = contestRecord.getSubmitTime().getTime() < contest.getStartTime().getTime();

            if (isBeforeContestJudge) {
                // 比赛开始前的提交记录不入排行榜，跳过
                continue;
            }

            if ((!isContainsAfterContestJudge || isOpenSealRank) && isAfterContestJudge) {
                // 如果不包含比赛结束后的提交 或者 处于封榜状态，则跳过比赛后的提交
                continue;
            }

            ACMContestRankVO ACMContestRankVo;
            if (!uidMapIndex.containsKey(contestRecord.getUid())) { // 如果该用户信息没还记录

                // 初始化参数
                ACMContestRankVo = new ACMContestRankVO();
                ACMContestRankVo.setRealname(contestRecord.getRealname())
                        .setAvatar(contestRecord.getAvatar())
                        .setSchool(contestRecord.getSchool())
                        .setFaculty(contestRecord.getFaculty())
                        .setCourse(contestRecord.getCourse())
                        .setGender(contestRecord.getGender())
                        .setUid(contestRecord.getUid())
                        .setUsername(contestRecord.getUsername())
                        .setNickname(contestRecord.getNickname())
                        .setAc(0.0)
                        .setTotalTime(0.0)
                        .setTotalScore(0)
                        .setTotal(0);

                // 如果为正式赛，将头像替换为学校校徽
                if (isOfficial) {
                    School school = schoolEntityService
                            .getOne(new QueryWrapper<School>().eq("name", contestRecord.getSchool()));

                    ACMContestRankVo.setAvatar(Constants.File.IMG_API.getPath() + school.getFileName());

                    // 在 ACMContestRankVo 中添加对应队员的信息
                    if (teamSign != null) {
                        ACMContestRankVo
                                .setCname(teamSign.getCname())
                                .setEname(teamSign.getEname())
                                .setUsername1(teamSign.getUsername1())
                                .setUsername2(teamSign.getUsername2())
                                .setUsername3(teamSign.getUsername3())
                                .setType(teamSign.getType())
                                .setInstructor(teamSign.getInstructor());
                    }
                }

                HashMap<String, HashMap<String, Object>> submissionInfo = new HashMap<>();
                ACMContestRankVo.setSubmissionInfo(submissionInfo);

                result.add(ACMContestRankVo);
                uidMapIndex.put(contestRecord.getUid(), index);
                index++;
            } else {
                ACMContestRankVo = result.get(uidMapIndex.get(contestRecord.getUid())); // 根据记录的index进行获取
            }

            HashMap<String, Object> problemSubmissionInfo = ACMContestRankVo.getSubmissionInfo()
                    .get(contestRecord.getDisplayId());

            if (problemSubmissionInfo == null) {
                problemSubmissionInfo = new HashMap<>();
                problemSubmissionInfo.put("errorNum", 0);
            }

            ACMContestRankVo.setTotal(ACMContestRankVo.getTotal() + 1);

            // 如果是当前是开启封榜的时段和同时该提交是处于封榜时段 尝试次数+1
            if (isOpenSealRank && isInSealTimeSubmission(minSealRankTime, maxSealRankTime, contestRecord.getTime())) {

                int tryNum = (int) problemSubmissionInfo.getOrDefault("tryNum", 0);
                problemSubmissionInfo.put("tryNum", tryNum + 1);

            } else {

                // 如果该题目已经AC过了，其它都不记录了
                if ((Boolean) problemSubmissionInfo.getOrDefault("isAC", false)) {
                    continue;
                }

                // 记录已经按题目提交耗时time升序了

                if (contestRecord.getScore() != null) {
                    // 总得分加上该题得到的分数
                    ACMContestRankVo.setTotalScore(ACMContestRankVo.getTotalScore() + contestRecord.getScore());
                }

                // 通过的话
                if (contestRecord.getStatus().intValue() == Constants.Contest.RECORD_AC.getCode()) {
                    // 总解决题目次数ac+1
                    ACMContestRankVo.setAc(ACMContestRankVo.getAc() + 1);

                    // 判断是不是first AC
                    boolean isFirstAC = false;
                    Long time = firstACMap.getOrDefault(contestRecord.getDisplayId(), null);
                    if (time == null) {
                        isFirstAC = true;
                        firstACMap.put(contestRecord.getDisplayId(), contestRecord.getTime());
                    } else {
                        // 相同提交时间也是first AC
                        if (time.longValue() == contestRecord.getTime().longValue()) {
                            isFirstAC = true;
                        }
                    }

                    int errorNumber = (int) problemSubmissionInfo.getOrDefault("errorNum", 0);
                    problemSubmissionInfo.put("isAC", true);
                    problemSubmissionInfo.put("isFirstAC", isFirstAC);
                    problemSubmissionInfo.put("ACTime", contestRecord.getTime());
                    problemSubmissionInfo.put("errorNum", errorNumber);
                    problemSubmissionInfo.put("score", contestRecord.getScore());

                    if (isAfterContestJudge) {
                        problemSubmissionInfo.put("isAfterContest", true);
                    }

                    // 同时计算总耗时，总耗时加上 该题目未AC前的错误次数*20*60+题目AC耗时
                    ACMContestRankVo.setTotalTime(
                            ACMContestRankVo.getTotalTime() + errorNumber * 20 * 60 + contestRecord.getTime());

                    // 未通过同时需要记录罚时次数
                } else if (contestRecord.getStatus().intValue() == Constants.Contest.RECORD_NOT_AC_PENALTY.getCode()) {

                    int errorNumber = (int) problemSubmissionInfo.getOrDefault("errorNum", 0);
                    problemSubmissionInfo.put("errorNum", errorNumber + 1);
                } else {

                    int errorNumber = (int) problemSubmissionInfo.getOrDefault("errorNum", 0);
                    problemSubmissionInfo.put("errorNum", errorNumber);
                }
            }
            ACMContestRankVo.getSubmissionInfo().put(contestRecord.getDisplayId(), problemSubmissionInfo);
        }

        List<ACMContestRankVO> orderResultList = new ArrayList<>();

        if (contest.getType().intValue() == Constants.Contest.TYPE_EXAM.getCode()) {
            orderResultList = result.stream()
                    .sorted(Comparator.comparing(ACMContestRankVO::getTotalScore, Comparator.reverseOrder()) // 先以总分数降序
                            .thenComparing(ACMContestRankVO::getAc, Comparator.reverseOrder()) // 再以总ac数升序
                            .thenComparing(ACMContestRankVO::getTotalTime) // 再以总耗时升序
                    ).collect(Collectors.toList());
        } else {
            orderResultList = result.stream()
                    .sorted(Comparator.comparing(ACMContestRankVO::getAc, Comparator.reverseOrder()) // 先以总ac数降序
                            .thenComparing(ACMContestRankVO::getTotalTime) // 再以总耗时升序
                    ).collect(Collectors.toList());
        }

        return orderResultList;
    }

    /**
     * @param isOpenSealRank              是否是查询封榜后的数据
     * @param removeStar                  是否需要移除打星队伍
     * @param contest                     比赛实体信息
     * @param currentUserId               当前查看榜单的用户uuid,不为空则将该数据复制一份放置列表最前
     * @param concernedList               关注的用户（uuid）列表
     * @param externalCidList             榜单额外显示比赛列表
     * @param useCache                    是否对初始排序计算的结果进行缓存
     * @param cacheTime                   缓存的时间 单位秒
     * @param isContainsAfterContestJudge 是否包含比赛结束后的提交
     * @param time                        距离比赛开始的秒数
     * @MethodName calcOIRank
     * @Description TODO
     * @Return
     * @Since 2021/12/10
     */
    public List<OIContestRankVO> calcOIRank(boolean isOpenSealRank,
            boolean removeStar,
            Contest contest,
            String currentUserId,
            List<String> concernedList,
            List<Integer> externalCidList,
            boolean useCache,
            Long cacheTime,
            boolean isContainsAfterContestJudge,
            Long time) {

        List<OIContestRankVO> orderResultList;
        if (useCache) {
            String key = null;
            if (isContainsAfterContestJudge) {
                key = Constants.Contest.CONTEST_RANK_CAL_RESULT_CACHE.getName() + "_contains_after_" + contest.getId();
            } else {
                key = Constants.Contest.CONTEST_RANK_CAL_RESULT_CACHE.getName() + "_" + contest.getId();
            }
            orderResultList = (List<OIContestRankVO>) redisUtils.get(key);
            if (orderResultList == null) {
                orderResultList = getOIOrderRank(contest, externalCidList, isOpenSealRank, isContainsAfterContestJudge,
                        time);
                redisUtils.set(key, orderResultList, cacheTime);
            }
        } else {
            orderResultList = getOIOrderRank(contest, externalCidList, isOpenSealRank, isContainsAfterContestJudge,
                    time);
        }

        // 需要打星的用户名列表
        HashMap<String, Boolean> starAccountMap = starAccountToMap(contest.getStarAccount());

        Queue<ContestAwardConfigVO> awardConfigVoList = null;
        boolean isNeedSetAward = contest.getAwardType() != null && contest.getAwardType() > 0;
        if (removeStar) {
            // 如果选择了移除打星队伍，同时该用户属于打星队伍，则将其移除
            orderResultList.removeIf(acmContestRankVo -> starAccountMap.containsKey(acmContestRankVo.getUsername()));
            if (isNeedSetAward) {
                awardConfigVoList = getContestAwardConfigList(contest.getAwardConfig(), contest.getAwardType(),
                        orderResultList.size());
            }
        } else {
            if (isNeedSetAward) {
                if (contest.getAwardType() == 1) {
                    long count = orderResultList.stream().filter(e -> !starAccountMap.containsKey(e.getUsername()))
                            .count();
                    awardConfigVoList = getContestAwardConfigList(contest.getAwardConfig(), contest.getAwardType(),
                            (int) count);
                } else {
                    awardConfigVoList = getContestAwardConfigList(contest.getAwardConfig(), contest.getAwardType(),
                            orderResultList.size());
                }
            }
        }

        // 记录当前用户排名数据和关注列表的用户排名数据
        List<OIContestRankVO> topOIRankVoList = new ArrayList<>();
        boolean needAddConcernedUser = false;
        if (!CollectionUtils.isEmpty(concernedList)) {
            needAddConcernedUser = true;
            // 移除关注列表与当前用户重复
            concernedList.remove(currentUserId);
        }

        int rankNum = 1;

        int schoolRankNum = 1;
        HashMap<String, Integer> firstSchoolTotalMap = new HashMap<>();
        HashMap<String, Integer> firstSchoolRankMap = new HashMap<>();

        OIContestRankVO lastOIRankVo = null;
        ContestAwardConfigVO configVo = null;
        int len = orderResultList.size();
        for (int i = 0; i < len; i++) {
            OIContestRankVO currentOIRankVo = orderResultList.get(i);
            if (!removeStar && starAccountMap.containsKey(currentOIRankVo.getUsername())) {
                // 打星队伍排名为-1
                currentOIRankVo.setRank(-1);
                currentOIRankVo.setIsWinAward(false);
            } else {
                if (rankNum == 1) {
                    currentOIRankVo.setRank(rankNum);
                    // 判断是不是学校的first AC
                    if (!StringUtils.isEmpty(currentOIRankVo.getSchool())) {
                        firstSchoolTotalMap.put(currentOIRankVo.getSchool(), currentOIRankVo.getTotalTime());
                        firstSchoolRankMap.put(currentOIRankVo.getSchool(), schoolRankNum);
                        currentOIRankVo.setSchoolRank(schoolRankNum);
                        schoolRankNum++;
                    }
                } else {
                    // 当前用户的程序总运行时间和总得分跟前一个用户一样的话，同时前一个不应该为打星用户，排名则一样
                    if (lastOIRankVo.getTotalScore().equals(currentOIRankVo.getTotalScore())
                            && lastOIRankVo.getTotalTime().equals(currentOIRankVo.getTotalTime())) {
                        currentOIRankVo.setRank(lastOIRankVo.getRank());

                        // 判断是不是学校的first AC
                        if (!StringUtils.isEmpty(lastOIRankVo.getSchool())
                                && firstSchoolRankMap.getOrDefault(currentOIRankVo.getSchool(), null) == null) {
                            Integer rank = firstSchoolRankMap.getOrDefault(lastOIRankVo.getSchool(), null);
                            Integer schoolTime = firstSchoolTotalMap.getOrDefault(lastOIRankVo.getSchool(), null);
                            firstSchoolTotalMap.put(currentOIRankVo.getSchool(), schoolTime);
                            firstSchoolRankMap.put(currentOIRankVo.getSchool(), rank);
                            currentOIRankVo.setSchoolRank(rank);
                        }
                    } else {
                        currentOIRankVo.setRank(rankNum);
                        // 判断是不是学校的first AC
                        if (!StringUtils.isEmpty(currentOIRankVo.getSchool())) {
                            Integer schoolTime = firstSchoolTotalMap.getOrDefault(currentOIRankVo.getSchool(), null);
                            if (schoolTime == null) {
                                firstSchoolTotalMap.put(currentOIRankVo.getSchool(), currentOIRankVo.getTotalTime());
                                firstSchoolRankMap.put(currentOIRankVo.getSchool(), schoolRankNum);
                                currentOIRankVo.setSchoolRank(schoolRankNum);
                                schoolRankNum++;
                            }
                        }
                    }
                }

                if (isNeedSetAward && currentOIRankVo.getTotalScore() > 0) {
                    if (configVo == null || configVo.getNum() == 0) {
                        if (!awardConfigVoList.isEmpty()) {
                            configVo = awardConfigVoList.poll();
                            currentOIRankVo.setAwardName(configVo.getName());
                            currentOIRankVo.setAwardBackground(configVo.getBackground());
                            currentOIRankVo.setAwardColor(configVo.getColor());
                            currentOIRankVo.setIsWinAward(true);
                            configVo.setNum(configVo.getNum() - 1);
                        } else {
                            isNeedSetAward = false;
                            currentOIRankVo.setIsWinAward(false);
                        }
                    } else {
                        currentOIRankVo.setAwardName(configVo.getName());
                        currentOIRankVo.setAwardBackground(configVo.getBackground());
                        currentOIRankVo.setAwardColor(configVo.getColor());
                        currentOIRankVo.setIsWinAward(true);
                        configVo.setNum(configVo.getNum() - 1);
                    }
                } else {
                    currentOIRankVo.setIsWinAward(false);
                }

                lastOIRankVo = currentOIRankVo;
                rankNum++;
            }

            // 默认当前请求用户的排名显示在最顶行
            if (!StringUtils.isEmpty(currentUserId) &&
                    currentOIRankVo.getUid().equals(currentUserId)) {
                topOIRankVoList.add(0, currentOIRankVo);
            }

            // 需要添加关注用户
            if (needAddConcernedUser) {
                if (concernedList.contains(currentOIRankVo.getUid())) {
                    topOIRankVoList.add(currentOIRankVo);
                }
            }
        }
        topOIRankVoList.addAll(orderResultList);
        return topOIRankVoList;
    }

    private List<OIContestRankVO> getOIOrderRank(Contest contest,
            List<Integer> externalCidList,
            Boolean isOpenSealRank,
            Boolean isContainsAfterContestJudge,
            Long selectedTime) {

        List<ContestRecordVO> oiContestRecord = contestRecordEntityService.getOIContestRecord(contest,
                externalCidList, isOpenSealRank, isContainsAfterContestJudge);

        List<String> superAdminUidList = getSuperAdminUidList(contest.getGid(), contest.getId());

        List<OIContestRankVO> result = new ArrayList<>();

        HashMap<String, Integer> uidMapIndex = new HashMap<>();

        HashMap<String, HashMap<String, Integer>> uidMapTime = new HashMap<>();

        boolean isHighestRankScore = Constants.Contest.OI_RANK_HIGHEST_SCORE.getName()
                .equals(contest.getOiRankScoreType());

        int index = 0;

        for (ContestRecordVO contestRecord : oiContestRecord) {

            if (selectedTime != null) {
                Date thisTime = addSeconds(contest.getStartTime(), selectedTime);
                // 提交时间大于查询的时间
                if (contestRecord.getSubmitTime().compareTo(thisTime) > 0) {
                    // 将超过查询时间的数据排除
                    continue;
                }
            }

            if (superAdminUidList.contains(contestRecord.getUid())) { // 超级管理员的提交不入排行榜
                continue;
            }

            boolean isAfterContestJudge = contestRecord.getSubmitTime().getTime() >= contest.getEndTime().getTime();
            boolean isBeforeContestJudge = contestRecord.getSubmitTime().getTime() < contest.getStartTime().getTime();

            if (isBeforeContestJudge) {
                // 比赛开始前的提交记录不入排行榜，跳过
                continue;
            }

            if ((!isContainsAfterContestJudge || isOpenSealRank) && isAfterContestJudge) {
                // 如果不包含比赛结束后的提交 或者 处于封榜状态，则跳过比赛后的提交
                continue;
            }

            if (contestRecord.getStatus().equals(Constants.Contest.RECORD_AC.getCode())) { // AC
                HashMap<String, Integer> pidMapTime = uidMapTime.get(contestRecord.getUid());
                if (pidMapTime != null) {
                    Integer useTime = pidMapTime.get(contestRecord.getDisplayId());
                    if (useTime != null) {
                        if (useTime > contestRecord.getUseTime()) { // 如果时间消耗比原来的少
                            pidMapTime.put(contestRecord.getDisplayId(), contestRecord.getUseTime());
                        }
                    } else {
                        pidMapTime.put(contestRecord.getDisplayId(), contestRecord.getUseTime());
                    }
                } else {
                    HashMap<String, Integer> tmp = new HashMap<>();
                    tmp.put(contestRecord.getDisplayId(), contestRecord.getUseTime());
                    uidMapTime.put(contestRecord.getUid(), tmp);
                }
            }

            OIContestRankVO oiContestRankVo;
            if (!uidMapIndex.containsKey(contestRecord.getUid())) { // 如果该用户信息没还记录
                // 初始化参数
                oiContestRankVo = new OIContestRankVO();
                oiContestRankVo.setRealname(contestRecord.getRealname())
                        .setUid(contestRecord.getUid())
                        .setUsername(contestRecord.getUsername())
                        .setSchool(contestRecord.getSchool())
                        .setAvatar(contestRecord.getAvatar())
                        .setGender(contestRecord.getGender())
                        .setNickname(contestRecord.getNickname())
                        .setTotalScore(0);

                HashMap<String, Integer> submissionInfo = new HashMap<>();
                oiContestRankVo.setSubmissionInfo(submissionInfo);

                result.add(oiContestRankVo);
                uidMapIndex.put(contestRecord.getUid(), index);
                index++;
            } else {
                oiContestRankVo = result.get(uidMapIndex.get(contestRecord.getUid())); // 根据记录的index进行获取
            }

            // 记录总分
            HashMap<String, Integer> submissionInfo = oiContestRankVo.getSubmissionInfo();
            Integer score = submissionInfo.get(contestRecord.getDisplayId());
            if (isHighestRankScore) {
                if (score == null) {
                    oiContestRankVo.setTotalScore(oiContestRankVo.getTotalScore() + contestRecord.getScore());
                    submissionInfo.put(contestRecord.getDisplayId(), contestRecord.getScore());
                }
            } else {
                if (contestRecord.getScore() != null) {
                    if (score != null) { // 为了避免同个提交时间的重复计算
                        oiContestRankVo
                                .setTotalScore(oiContestRankVo.getTotalScore() - score + contestRecord.getScore());
                    } else {
                        oiContestRankVo.setTotalScore(oiContestRankVo.getTotalScore() + contestRecord.getScore());
                    }
                }
                submissionInfo.put(contestRecord.getDisplayId(), contestRecord.getScore());
            }

        }

        for (OIContestRankVO oiContestRankVo : result) {
            HashMap<String, Integer> pidMapTime = uidMapTime.get(oiContestRankVo.getUid());
            int sumTime = 0;
            if (pidMapTime != null) {
                for (String key : pidMapTime.keySet()) {
                    Integer time = pidMapTime.get(key);
                    sumTime += time == null ? 0 : time;
                }
            }
            oiContestRankVo.setTotalTime(sumTime);
            oiContestRankVo.setTimeInfo(pidMapTime);
        }

        // 根据总得分进行降序,再根据总时耗升序排序
        List<OIContestRankVO> orderResultList = result.stream()
                .sorted(Comparator.comparing(OIContestRankVO::getTotalScore, Comparator.reverseOrder())
                        .thenComparing(OIContestRankVO::getTotalTime, Comparator.naturalOrder()))
                .collect(Collectors.toList());
        return orderResultList;
    }

    public List<String> getSuperAdminUidList(Long gid, Long cid) {

        List<String> superAdminUidList = userInfoEntityService.getNowContestAdmin(cid);

        if (gid != null) {
            QueryWrapper<GroupMember> groupMemberQueryWrapper = new QueryWrapper<>();
            groupMemberQueryWrapper.eq("gid", gid).eq("auth", 5);

            List<GroupMember> groupRootList = groupMemberEntityService.list(groupMemberQueryWrapper);

            for (GroupMember groupMember : groupRootList) {
                superAdminUidList.add(groupMember.getUid());
            }
        }
        return superAdminUidList;
    }

    private boolean isInSealTimeSubmission(Long minSealRankTime, Long maxSealRankTime, Long time) {
        return time >= minSealRankTime && time < maxSealRankTime;
    }

    private HashMap<String, Boolean> starAccountToMap(String starAccountStr) {
        if (StringUtils.isEmpty(starAccountStr)) {
            return new HashMap<>();
        }
        JSONObject jsonObject = JSONUtil.parseObj(starAccountStr);
        List<String> list = jsonObject.get("star_account", List.class);
        HashMap<String, Boolean> res = new HashMap<>();
        for (String str : list) {
            if (!StringUtils.isEmpty(str)) {
                res.put(str, true);
            }
        }
        return res;
    }

    private Queue<ContestAwardConfigVO> getContestAwardConfigList(String awardConfig, Integer awardType,
            Integer totalUser) {
        if (StringUtils.isEmpty(awardConfig)) {
            return new LinkedList<>();
        }
        JSONObject jsonObject = JSONUtil.parseObj(awardConfig);
        List<JSONObject> list = jsonObject.get("config", List.class);

        Queue<ContestAwardConfigVO> queue = new LinkedList<>();

        if (awardType == 1) {
            // 占比转换成具体人数
            for (JSONObject object : list) {
                ContestAwardConfigVO configVo = JSONUtil.toBean(object, ContestAwardConfigVO.class);
                if (configVo.getNum() != null && configVo.getNum() > 0) {
                    int num = (int) (configVo.getNum() * 0.01 * totalUser);
                    if (num > 0) {
                        configVo.setNum(num);
                        queue.offer(configVo);
                    }
                }
            }
        } else if (awardType == 2) {
            for (JSONObject object : list) {
                ContestAwardConfigVO configVo = JSONUtil.toBean(object, ContestAwardConfigVO.class);
                if (configVo.getNum() != null && configVo.getNum() > 0) {
                    queue.offer(configVo);
                }
            }
        } else if (awardType == 3) {
            // 筛选总得分大于，按num从大到小排序
            list.stream()
                    .sorted((o1, o2) -> o2.getInt("num", 0) - o1.getInt("num", 0))
                    .map(object -> JSONUtil.toBean(object, ContestAwardConfigVO.class))
                    .filter(configVo -> configVo.getNum() != null && configVo.getNum() > 0)
                    .forEach(configVo -> {
                        configVo.setNum(1);
                        queue.offer(configVo);
                    });
        }
        return queue;
    }

    private static Date addSeconds(Date date, Long seconds) {
        if (seconds != null) {
            Instant instant = date.toInstant().plusSeconds(seconds);
            return Date.from(instant);
        } else {
            return null;
        }
    }

    public List<ACMContestRankVO> getTopRank(
            Contest contest,
            boolean removeStar,
            boolean isNeedSetAward,
            String currentUserId,
            List<String> concernedList,
            List<ACMContestRankVO> result,
            HashMap<String, Boolean> starAccountMap,
            Queue<ContestAwardConfigVO> awardConfigVoList,
            boolean needAddConcernedUser) {

        List<ACMContestRankVO> topACMRankVoList = new ArrayList<>();
        int rankNum = 1;

        int schoolRankNum = 1;
        HashMap<String, Double> firstSchoolTotalMap = new HashMap<>();
        HashMap<String, Integer> firstSchoolRankMap = new HashMap<>();

        int len = result.size();
        ACMContestRankVO lastACMRankVo = null;
        ContestAwardConfigVO configVo = null;
        for (int i = 0; i < len; i++) {
            ACMContestRankVO currentACMRankVo = result.get(i);
            if (!removeStar && starAccountMap.containsKey(currentACMRankVo.getUsername())) {
                // 打星队伍排名为-1
                currentACMRankVo.setRank(-1);
                currentACMRankVo.setIsWinAward(false);
            } else {
                if (rankNum == 1) {
                    currentACMRankVo.setRank(rankNum);
                    // 判断是不是学校的first AC
                    if (!StringUtils.isEmpty(currentACMRankVo.getSchool())) {
                        firstSchoolTotalMap.put(currentACMRankVo.getSchool(), (double) currentACMRankVo.getTotalTime());
                        firstSchoolRankMap.put(currentACMRankVo.getSchool(), schoolRankNum);
                        currentACMRankVo.setSchoolRank(schoolRankNum);
                        schoolRankNum++;
                    }
                } else {
                    // 当前用户的总罚时和AC数跟前一个用户一样的话，同时前一个不应该为打星，排名则一样
                    if (Objects.equals(lastACMRankVo.getAc(), currentACMRankVo.getAc())
                            && lastACMRankVo.getTotalTime().equals(currentACMRankVo.getTotalTime())) {
                        currentACMRankVo.setRank(lastACMRankVo.getRank());

                        // 判断是不是学校的first AC
                        if (!StringUtils.isEmpty(lastACMRankVo.getSchool())
                                && firstSchoolRankMap.getOrDefault(currentACMRankVo.getSchool(), null) == null) {
                            Integer rank = firstSchoolRankMap.getOrDefault(lastACMRankVo.getSchool(), null);
                            Double schoolTime = firstSchoolTotalMap.getOrDefault(lastACMRankVo.getSchool(), null);
                            firstSchoolTotalMap.put(currentACMRankVo.getSchool(), schoolTime);
                            firstSchoolRankMap.put(currentACMRankVo.getSchool(), rank);
                            currentACMRankVo.setSchoolRank(rank);
                        }
                    } else {
                        currentACMRankVo.setRank(rankNum);
                        // 判断是不是学校的first AC
                        if (!StringUtils.isEmpty(currentACMRankVo.getSchool())) {
                            Double schoolTime = firstSchoolTotalMap.getOrDefault(currentACMRankVo.getSchool(), null);
                            if (schoolTime == null) {
                                firstSchoolTotalMap.put(currentACMRankVo.getSchool(), currentACMRankVo.getTotalTime());
                                firstSchoolRankMap.put(currentACMRankVo.getSchool(), schoolRankNum);
                                currentACMRankVo.setSchoolRank(schoolRankNum);
                                schoolRankNum++;
                            }
                        }
                    }
                }

                if (isNeedSetAward && currentACMRankVo.getAc() > 0) {
                    if (contest.getType().intValue() == Constants.Contest.TYPE_EXAM.getCode()) {
                        if (!awardConfigVoList.isEmpty()) {
                            // 直接从队列中查找符合条件的奖项
                            awardConfigVoList.stream()
                                    .filter(award -> award != null && award.getNum() != null && award.getNum() > 0)
                                    .filter(award -> currentACMRankVo.getTotalScore() != null
                                            && currentACMRankVo.getTotalScore() > 0
                                            && currentACMRankVo.getTotalScore() >= award.getNum())
                                    .findFirst()
                                    .ifPresent(award -> {
                                        currentACMRankVo.setAwardName(award.getName());
                                        currentACMRankVo.setAwardBackground(award.getBackground());
                                        currentACMRankVo.setAwardColor(award.getColor());
                                        currentACMRankVo.setIsWinAward(true);
                                    });
                        }
                    } else {
                        if (configVo == null || configVo.getNum() == 0) {
                            if (!awardConfigVoList.isEmpty()) {
                                configVo = awardConfigVoList.poll();
                                currentACMRankVo.setAwardName(configVo.getName());
                                currentACMRankVo.setAwardBackground(configVo.getBackground());
                                currentACMRankVo.setAwardColor(configVo.getColor());
                                currentACMRankVo.setIsWinAward(true);
                                configVo.setNum(configVo.getNum() - 1);
                            } else {
                                isNeedSetAward = false;
                                currentACMRankVo.setIsWinAward(false);
                            }
                        } else {
                            currentACMRankVo.setAwardName(configVo.getName());
                            currentACMRankVo.setAwardBackground(configVo.getBackground());
                            currentACMRankVo.setAwardColor(configVo.getColor());
                            currentACMRankVo.setIsWinAward(true);
                            configVo.setNum(configVo.getNum() - 1);
                        }
                    }
                } else {
                    currentACMRankVo.setIsWinAward(false);
                }

                lastACMRankVo = currentACMRankVo;
                rankNum++;
            }
            // 默认将请求用户的排名置为最顶
            if (!StringUtils.isEmpty(currentUserId) &&
                    currentACMRankVo.getUid().equals(currentUserId)) {
                topACMRankVoList.add(0, currentACMRankVo);
            }

            // 需要添加关注用户
            if (needAddConcernedUser) {
                if (concernedList.contains(currentACMRankVo.getUid())) {
                    topACMRankVoList.add(currentACMRankVo);
                }
            }
        }
        topACMRankVoList.addAll(result);
        return topACMRankVoList;
    }

    public List<HashMap<String, Object>> getSubmissions(HashMap<String, HashMap<String, Object>> submissionInfo) {
        List<HashMap<String, Object>> submissions = new ArrayList<>();
        // 遍历 submissionInfos
        Iterator<Map.Entry<String, HashMap<String, Object>>> iterator = submissionInfo.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, HashMap<String, Object>> entry = iterator.next();
            String key = entry.getKey();
            HashMap<String, Object> submissionData = entry.getValue();

            submissionData.put("displayId", key);

            submissions.add(submissionData);
        }
        return submissions;
    }

    // 自定义的比较器
    static class SubmissonComparator implements Comparator<HashMap<String, Object>> {
        @Override
        public int compare(HashMap<String, Object> o1, HashMap<String, Object> o2) {
            Long actime1 = findACTime(o1);
            Long actime2 = findACTime(o2);

            // 根据 ACTime 的大小进行比较
            return Long.compare(actime1, actime2);
        }

        // 辅助方法，用于找到 ACTime 的值
        private Long findACTime(HashMap<String, Object> submissionInfo) {
            // 遍历 submissionInfo
            Boolean isAC = false;

            Iterator<Map.Entry<String, Object>> iterator = submissionInfo.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Object> entry = iterator.next();
                String key = entry.getKey();
                Object value = entry.getValue();

                if ("isAC".equals(key) && value != null) {
                    isAC = true;
                }

                // TODO 同步赛时间筛选
                if ("ACTime".equals(key) && isAC) { // 检查键是否为"ACTime"
                    Long ACTime = ((Number) value).longValue();
                    return ACTime;
                }
            }

            // 如果找不到 ACTime，可以返回一个默认值或者抛出异常，具体根据需求而定
            return 0L;
        }
    }

    public List<ACMContestRankVO> getSortedRankList(List<ACMContestRankVO> resultList) {
        // 重新排序
        List<ACMContestRankVO> result = resultList.stream()
                .sorted(Comparator.comparing(ACMContestRankVO::getAc, Comparator.reverseOrder()) // 先以总ac数降序
                        .thenComparing(ACMContestRankVO::getTotalTime) // 再以总耗时升序
                ).collect(Collectors.toList());

        int rankNum = 1;
        int len = result.size();
        ACMContestRankVO lastACMRankVo = null;

        // 设置每个人的排名
        for (int i = 0; i < len; i++) {
            ACMContestRankVO ACMContestRankVO = result.get(i);

            if (rankNum == 1) {
                ACMContestRankVO.setRank(rankNum);
            } else {
                // 当前用户的总罚时和AC数跟前一个用户一样的话，同时前一个不应该为打星，排名则一样
                if (Objects.equals(lastACMRankVo.getAc(), ACMContestRankVO.getAc())
                        && lastACMRankVo.getTotalTime().equals(ACMContestRankVO.getTotalTime())) {
                    ACMContestRankVO.setRank(lastACMRankVo.getRank());
                } else {
                    ACMContestRankVO.setRank(rankNum);
                }
            }
            lastACMRankVo = ACMContestRankVO;
            rankNum++;
        }

        return result;
    }

    public String getValueForKey(Map<String, String> data, String username, String realname) {

        // 如果 data 为空，直接返回 realname
        if (data == null) {
            return realname;
        }

        // 获取 username 对应的值
        String value = data.get(username);

        // 如果 value 不为空且包含 "-", 则分割、排序并重新连接
        if (value != null && value.contains("-")) {
            return Arrays.stream(value.split("-"))
                    .sorted()
                    .collect(Collectors.joining("-"));
        }

        // 返回找到的 value 或 realname
        return value != null ? value : realname;
    }

}