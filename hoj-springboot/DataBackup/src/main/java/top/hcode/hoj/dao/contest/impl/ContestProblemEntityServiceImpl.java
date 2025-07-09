package top.hcode.hoj.dao.contest.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.CollectionUtils;

import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.entity.contest.ContestProblem;
import top.hcode.hoj.mapper.ContestProblemMapper;
import top.hcode.hoj.pojo.entity.contest.ContestRecord;
import top.hcode.hoj.pojo.vo.ContestProblemVO;
import top.hcode.hoj.dao.contest.ContestEntityService;
import top.hcode.hoj.dao.contest.ContestProblemEntityService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.hcode.hoj.dao.contest.ContestRecordEntityService;
import top.hcode.hoj.dao.user.UserInfoEntityService;
import top.hcode.hoj.pojo.vo.ProblemFullScreenListVO;
import top.hcode.hoj.utils.Constants;

import java.util.Date;
import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Himit_ZH
 * @since 2020-10-23
 */
@Service
public class ContestProblemEntityServiceImpl extends ServiceImpl<ContestProblemMapper, ContestProblem>
        implements ContestProblemEntityService {

    @Autowired
    private ContestProblemMapper contestProblemMapper;

    @Autowired
    private UserInfoEntityService userInfoEntityService;

    @Autowired
    private ContestRecordEntityService contestRecordEntityService;

    @Autowired
    private ContestEntityService contestEntityService;

    @Override
    public List<ContestProblemVO> getContestProblemList(Long cid,
            Date startTime,
            Date endTime,
            Date sealTime,
            Boolean isAdmin,
            String contestAuthorUid,
            List<String> groupRootUidList,
            Boolean isContainsContestEndJudge,
            Date selectedTime) {

        // 筛去 比赛管理员, 题目管理和超级管理员的提交
        List<String> AdminUidList = userInfoEntityService.getNowContestAdmin(cid);
        AdminUidList.add(contestAuthorUid);

        if (!CollectionUtils.isEmpty(groupRootUidList)) {
            AdminUidList.addAll(groupRootUidList);
        }
        List<ContestProblemVO> contestProblemList = contestProblemMapper.getContestProblemVoList(cid, startTime,
                endTime, sealTime, isAdmin, AdminUidList, !isContainsContestEndJudge, selectedTime);

        Contest contest = contestEntityService.getById(cid);

        if (contest.getAuth().intValue() == Constants.Contest.AUTH_EXAMINATION.getCode()) {
            // 过滤掉ac和total
            contestProblemList.forEach(item -> {
                item.setAc(null);
                item.setTotal(null);
            });
        }

        return contestProblemList;
    }

    @Override
    public List<ProblemFullScreenListVO> getContestFullScreenProblemList(Long cid) {
        return contestProblemMapper.getContestFullScreenProblemList(cid);
    }

    @Async
    @Override
    public void syncContestRecord(Long pid, Long cid, String displayId) {

        UpdateWrapper<ContestRecord> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("pid", pid)
                .eq("cid", cid)
                .set("display_id", displayId);
        contestRecordEntityService.update(updateWrapper);
    }
}
