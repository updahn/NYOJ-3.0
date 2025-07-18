package top.hcode.hoj.manager.oj;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.UnicodeUtil;
import cn.hutool.core.util.CharsetUtil;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import top.hcode.hoj.config.NacosSwitchConfig;
import top.hcode.hoj.config.SwitchConfig;
import top.hcode.hoj.config.WebConfig;
import top.hcode.hoj.dao.common.AnnouncementEntityService;
import top.hcode.hoj.dao.common.FileEntityService;
import top.hcode.hoj.dao.contest.ContestEntityService;
import top.hcode.hoj.dao.judge.JudgeEntityService;
import top.hcode.hoj.dao.problem.ProblemEntityService;
import top.hcode.hoj.dao.user.UserRecordEntityService;
import top.hcode.hoj.pojo.dto.ProblemResDTO;
import top.hcode.hoj.pojo.entity.common.File;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.pojo.vo.*;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.utils.RedisUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/10 21:00
 * @Description:
 */
@Component
public class HomeManager {

    @Autowired
    private ContestEntityService contestEntityService;

    @Autowired
    private ConfigVO configVo;

    @Autowired
    private AnnouncementEntityService announcementEntityService;

    @Autowired

    private UserRecordEntityService userRecordEntityService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private FileEntityService fileEntityService;

    @Autowired
    private ProblemEntityService problemEntityService;

    @Autowired
    private JudgeEntityService judgeEntityService;

    @Autowired
    private NacosSwitchConfig nacosSwitchConfig;

    private final static String SUBMISSION_STATISTICS_KEY = "home_submission_statistics";

    /**
     * @MethodName getRecentContest
     * @Params
     * @Description 获取最近14天的比赛信息列表
     * @Return CommonResult
     * @Since 2020/12/29
     */
    public List<ContestVO> getRecentContest() {
        return contestEntityService.getWithinNext14DaysContests();
    }

    /**
     * @MethodName getHomeCarousel
     * @Params
     * @Description 获取主页轮播图
     * @Return
     * @Since 2021/9/4
     */
    public List<HashMap<String, Object>> getHomeCarousel() {
        List<File> fileList = fileEntityService.queryCarouselFileList();
        List<HashMap<String, Object>> apiList = fileList.stream().map(f -> {
            HashMap<String, Object> param = new HashMap<>(2);
            param.put("id", f.getId());
            param.put("url", Constants.File.IMG_API.getPath() + f.getName());
            param.put("link", f.getLink());
            param.put("hint", f.getHint());
            return param;
        }).collect(Collectors.toList());
        return apiList;
    }

    /**
     * @MethodName getHomeCarousel
     * @Params
     * @Description 获取文件柜文件
     * @Return
     * @Since 2021/9/4
     */
    public List<HashMap<String, Object>> getBoxFile() {
        List<File> fileList = fileEntityService.queryBoxFileList();
        List<HashMap<String, Object>> apiList = fileList.stream().map(f -> {
            HashMap<String, Object> param = new HashMap<>(2);
            param.put("id", f.getId());
            param.put("url", Constants.File.FILE_API.getPath() + f.getName());
            param.put("hint", f.getHint());
            return param;
        }).collect(Collectors.toList());
        return apiList;
    }

    /**
     * @MethodName getRecentSevenACRank
     * @Params
     * @Description 获取最近7天用户做题榜单
     * @Return
     * @Since 2021/1/15
     */
    public List<ACMRankVO> getRecentSevenACRank() {
        return userRecordEntityService.getRecent7ACRank();
    }

    /**
     * @MethodName getRecentOtherContest
     * @Params
     * @Description 获取最近其他OJ的比赛信息列表
     * @Return CommonResult
     * @Since 2020/1/15
     */
    @Deprecated
    public List<HashMap<String, Object>> getRecentOtherContest() {
        String redisKey = Constants.Schedule.RECENT_OTHER_CONTEST.getCode();
        // 从redis获取比赛列表
        return (ArrayList<HashMap<String, Object>>) redisUtils.get(redisKey);
    }

    /**
     * @MethodName getCommonAnnouncement
     * @Params
     * @Description 获取主页公告列表
     * @Return CommonResult
     * @Since 2020/12/29
     */
    public IPage<AnnouncementVO> getCommonAnnouncement(Integer limit, Integer currentPage, Long id) {
        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        if (limit == null || limit < 1)
            limit = 10;
        return announcementEntityService.getAnnouncementList(limit, currentPage, true, id);
    }

    /**
     * @MethodName getWebConfig
     * @Params
     * @Description 获取网站的基础配置。例如名字，缩写名字等等。
     * @Return
     * @Since 2020/12/29
     */
    public Map<Object, Object> getWebConfig() {
        SwitchConfig switchConfig = nacosSwitchConfig.getSwitchConfig();
        WebConfig webConfig = nacosSwitchConfig.getWebConfig();

        // 添加友情链接
        List<Map<String, String>> related = webConfig.getRelated();

        return MapUtil.builder().put("baseUrl", UnicodeUtil.toString(webConfig.getBaseUrl()))
                .put("name", UnicodeUtil.toString(webConfig.getName()))
                .put("shortName", UnicodeUtil.toString(webConfig.getShortName()))
                .put("register", webConfig.getRegister())
                .put("duration", UnicodeUtil.toString(webConfig.getDuration()))
                .put("domainInfo", UnicodeUtil.toString(webConfig.getDomainInfo()))
                .put("recordName", UnicodeUtil.toString(webConfig.getRecordName()))
                .put("recordUrl", UnicodeUtil.toString(webConfig.getRecordUrl()))
                .put("description", UnicodeUtil.toString(webConfig.getDescription()))
                .put("email", UnicodeUtil.toString(webConfig.getEmailUsername()))
                .put("projectName", UnicodeUtil.toString(webConfig.getProjectName()))
                .put("projectUrl", UnicodeUtil.toString(webConfig.getProjectUrl()))
                .put("related", related)
                .put("openPublicDiscussion", switchConfig.getOpenPublicDiscussion())
                .put("openGroupDiscussion", switchConfig.getOpenGroupDiscussion())
                .put("openContestComment", switchConfig.getOpenContestComment())
                .map();
    }

    /**
     * @MethodName getRecentUpdatedProblemList
     * @Params
     * @Description 获取最近前十更新的题目（不包括比赛题目、私有题目）
     * @Since 2022/10/15
     */
    public List<RecentUpdatedProblemVO> getRecentUpdatedProblemList() {
        List<ProblemResDTO> problemList = problemEntityService.getRecentUpdatedProblemList();

        // 如果列表的大小大于10，就取前10个，否则取整个列表
        List<ProblemResDTO> top10ProblemList = problemList.size() > 10 ? problemList.subList(0, 10) : problemList;

        if (!CollectionUtils.isEmpty(top10ProblemList)) {
            return top10ProblemList.stream()
                    .map(this::convertUpdatedProblemVO)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private RecentUpdatedProblemVO convertUpdatedProblemVO(ProblemResDTO problem) {
        return RecentUpdatedProblemVO.builder()
                .problemId(problem.getProblemId())
                .id(problem.getId())
                .title(problem.getProblemDescriptionList().get(0).getTitle())
                .gmtCreate(problem.getGmtCreate())
                .gmtModified(problem.getGmtModified())
                .type(problem.getType())
                .build();
    }

    /**
     * 获取网站最近一周的提交状态（ac总量、提交总量）
     *
     * @param forceRefresh
     * @return
     */
    public SubmissionStatisticsVO getLastWeekSubmissionStatistics(Boolean forceRefresh) {
        SubmissionStatisticsVO submissionStatisticsVO = (SubmissionStatisticsVO) redisUtils
                .get(SUBMISSION_STATISTICS_KEY);

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");
        forceRefresh = forceRefresh && isRoot;

        if (submissionStatisticsVO == null || forceRefresh) {
            DateTime dateTime = DateUtil.offsetDay(new Date(), -6);
            String strTime = DateFormatUtils.format(dateTime, "yyyy-MM-dd") + " 00:00:00";
            QueryWrapper<Judge> judgeQueryWrapper = new QueryWrapper<>();
            judgeQueryWrapper.select("submit_id", "status", "gmt_create");
            judgeQueryWrapper.apply("UNIX_TIMESTAMP(gmt_create) >= UNIX_TIMESTAMP('" + strTime + "')");
            List<Judge> judgeList = judgeEntityService.list(judgeQueryWrapper);
            submissionStatisticsVO = buildSubmissionStatisticsVo(judgeList);
            redisUtils.set(SUBMISSION_STATISTICS_KEY, submissionStatisticsVO, 60 * 30);
        }
        return submissionStatisticsVO;
    }

    private SubmissionStatisticsVO buildSubmissionStatisticsVo(List<Judge> judgeList) {
        long acTodayCount = 0;
        long acOneDayAgoCount = 0;
        long acTwoDaysAgoCount = 0;
        long acThreeDaysAgoCount = 0;
        long acFourDaysAgoCount = 0;
        long acFiveDaysAgoCount = 0;
        long acSixDaysAgoCount = 0;

        long totalTodayCount = 0;
        long totalOneDayAgoCount = 0;
        long totalTwoDaysAgoCount = 0;
        long totalThreeDaysAgoCount = 0;
        long totalFourDaysAgoCount = 0;
        long totalFiveDaysAgoCount = 0;
        long totalSixDaysAgoCount = 0;

        Date date = new Date();
        String todayStr = DateUtil.format(date, "MM-dd");
        String oneDayAgoStr = DateFormatUtils.format(DateUtil.offsetDay(date, -1), "MM-dd");
        String twoDaysAgoStr = DateFormatUtils.format(DateUtil.offsetDay(date, -2), "MM-dd");
        String threeDaysAgoStr = DateFormatUtils.format(DateUtil.offsetDay(date, -3), "MM-dd");
        String fourDaysAgoStr = DateFormatUtils.format(DateUtil.offsetDay(date, -4), "MM-dd");
        String fiveDaysAgoStr = DateFormatUtils.format(DateUtil.offsetDay(date, -5), "MM-dd");
        String sixDaysAgoStr = DateFormatUtils.format(DateUtil.offsetDay(date, -6), "MM-dd");

        if (!CollectionUtils.isEmpty(judgeList)) {
            Map<String, List<Judge>> map = judgeList.stream()
                    .collect(
                            Collectors.groupingBy(
                                    o -> DateUtil.format(o.getGmtCreate(), "MM-dd")));
            for (Map.Entry<String, List<Judge>> entry : map.entrySet()) {
                if (Objects.equals(entry.getKey(), todayStr)) {
                    totalTodayCount += entry.getValue().size();
                    long count = entry.getValue()
                            .parallelStream()
                            .filter(judge -> Objects.equals(judge.getStatus(),
                                    Constants.Judge.STATUS_ACCEPTED.getStatus()))
                            .count();
                    acTodayCount += count;
                } else if (Objects.equals(entry.getKey(), oneDayAgoStr)) {
                    totalOneDayAgoCount += entry.getValue().size();
                    long count = entry.getValue()
                            .parallelStream()
                            .filter(judge -> Objects.equals(judge.getStatus(),
                                    Constants.Judge.STATUS_ACCEPTED.getStatus()))
                            .count();
                    acOneDayAgoCount += count;
                } else if (Objects.equals(entry.getKey(), twoDaysAgoStr)) {
                    totalTwoDaysAgoCount += entry.getValue().size();
                    long count = entry.getValue()
                            .parallelStream()
                            .filter(judge -> Objects.equals(judge.getStatus(),
                                    Constants.Judge.STATUS_ACCEPTED.getStatus()))
                            .count();
                    acTwoDaysAgoCount += count;
                } else if (Objects.equals(entry.getKey(), threeDaysAgoStr)) {
                    totalThreeDaysAgoCount += entry.getValue().size();
                    long count = entry.getValue()
                            .parallelStream()
                            .filter(judge -> Objects.equals(judge.getStatus(),
                                    Constants.Judge.STATUS_ACCEPTED.getStatus()))
                            .count();
                    acThreeDaysAgoCount += count;
                } else if (Objects.equals(entry.getKey(), fourDaysAgoStr)) {
                    totalFourDaysAgoCount += entry.getValue().size();
                    long count = entry.getValue()
                            .parallelStream()
                            .filter(judge -> Objects.equals(judge.getStatus(),
                                    Constants.Judge.STATUS_ACCEPTED.getStatus()))
                            .count();
                    acFourDaysAgoCount += count;
                } else if (Objects.equals(entry.getKey(), fiveDaysAgoStr)) {
                    totalFiveDaysAgoCount += entry.getValue().size();
                    long count = entry.getValue()
                            .parallelStream()
                            .filter(judge -> Objects.equals(judge.getStatus(),
                                    Constants.Judge.STATUS_ACCEPTED.getStatus()))
                            .count();
                    acFiveDaysAgoCount += count;
                } else if (Objects.equals(entry.getKey(), sixDaysAgoStr)) {
                    totalSixDaysAgoCount += entry.getValue().size();
                    long count = entry.getValue()
                            .parallelStream()
                            .filter(judge -> Objects.equals(judge.getStatus(),
                                    Constants.Judge.STATUS_ACCEPTED.getStatus()))
                            .count();
                    acSixDaysAgoCount += count;
                }
            }
        }

        SubmissionStatisticsVO submissionStatisticsVO = new SubmissionStatisticsVO();
        submissionStatisticsVO.setDateStrList(Arrays.asList(
                sixDaysAgoStr,
                fiveDaysAgoStr,
                fourDaysAgoStr,
                threeDaysAgoStr,
                twoDaysAgoStr,
                oneDayAgoStr,
                todayStr));

        submissionStatisticsVO.setAcCountList(Arrays.asList(
                acSixDaysAgoCount,
                acFiveDaysAgoCount,
                acFourDaysAgoCount,
                acThreeDaysAgoCount,
                acTwoDaysAgoCount,
                acOneDayAgoCount,
                acTodayCount));

        submissionStatisticsVO.setTotalCountList(Arrays.asList(
                totalSixDaysAgoCount,
                totalFiveDaysAgoCount,
                totalFourDaysAgoCount,
                totalThreeDaysAgoCount,
                totalTwoDaysAgoCount,
                totalOneDayAgoCount,
                totalTodayCount));

        return submissionStatisticsVO;
    }

    /**
     * @MethodName getSchoolList
     * @Params
     * @Description 文件获取学校列表 （废弃）
     * @Return
     */
    public List<SchoolVO> getSchoolList() {
        try {
            // 如果失效，前往下载
            // https://github.com/ATQQ/school-picker/blob/master/components/schoolPicker/school-data/schools.js
            // 并转化为json形式
            String schoolDir = Constants.File.SCHOOL_BASE_FOLDER.getPath() + java.io.File.separator + "school.json";

            // 读取学校的文件
            FileReader inputFile = new FileReader(new java.io.File(schoolDir), CharsetUtil.UTF_8);
            String input = inputFile.readString()
                    .replaceAll("\r\n", "\n") // 避免window系统的换行问题
                    .replaceAll("\r", "\n"); // 避免mac系统的换行问题

            // 转换为List<SchoolVO>
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, String>> data = mapper.readValue(input, new TypeReference<List<Map<String, String>>>() {
            });

            return data.stream()
                    .map(item -> {
                        SchoolVO schoolVo = new SchoolVO();
                        schoolVo.setProvince(item.get("province"));
                        schoolVo.setCity(item.get("city"));
                        schoolVo.setName(item.get("name"));
                        return schoolVo;
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

}