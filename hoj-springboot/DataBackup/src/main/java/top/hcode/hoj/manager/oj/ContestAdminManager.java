package top.hcode.hoj.manager.oj;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import cn.hutool.core.bean.BeanUtil;

import java.util.stream.Collectors;
import javax.annotation.Resource;
import java.util.*;

import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.result.Paginate;
import top.hcode.hoj.dao.contest.ContestEntityService;
import top.hcode.hoj.dao.contest.ContestPrintEntityService;
import top.hcode.hoj.dao.contest.ContestRecordEntityService;
import top.hcode.hoj.dao.judge.JudgeEntityService;
import top.hcode.hoj.dao.user.UserRoleEntityService;
import top.hcode.hoj.manager.group.GroupManager;
import top.hcode.hoj.mapper.SessionMapper;
import top.hcode.hoj.pojo.dto.CheckACDTO;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.entity.contest.ContestPrint;
import top.hcode.hoj.pojo.entity.contest.ContestRecord;
import top.hcode.hoj.pojo.entity.judge.Judge;
import top.hcode.hoj.pojo.vo.ContestPrintVO;
import top.hcode.hoj.pojo.vo.SessionVO;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.validator.GroupValidator;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/11 19:40
 * @Description:
 */
@Component
public class ContestAdminManager {

    @Autowired
    private ContestEntityService contestEntityService;

    @Autowired
    private ContestRecordEntityService contestRecordEntityService;

    @Autowired
    private ContestPrintEntityService contestPrintEntityService;

    @Resource
    private UserRoleEntityService userRoleEntityService;

    @Autowired
    private JudgeEntityService judgeEntityService;

    @Resource
    private SessionMapper sessionMapper;

    @Autowired
    private GroupValidator groupValidator;

    @Autowired
    private GroupManager groupManager;

    public IPage<ContestRecord> getContestACInfo(Long cid, Integer currentPage, Integer limit)
            throws StatusForbiddenException {

        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        // 获取本场比赛的状态
        Contest contest = contestEntityService.getById(cid);

        boolean isRoot = groupManager.getGroupAuthAdmin(contest.getGid());

        if (!isRoot
                && !contest.getUid().equals(userRolesVo.getUid())
                && !(contest.getIsGroup() && groupValidator.isGroupRoot(userRolesVo.getUid(), contest.getGid()))) {
            throw new StatusForbiddenException("对不起，您无权限操作！");
        }

        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        if (limit == null || limit < 1)
            limit = 30;

        // 获取当前比赛的，状态为ac，未被校验的排在前面
        return contestRecordEntityService.getACInfo(currentPage,
                limit,
                Constants.Contest.RECORD_AC.getCode(),
                cid,
                contest.getUid(),
                contest.getStartTime(),
                contest.getEndTime());

    }

    public void checkContestACInfo(CheckACDTO checkACDto) throws StatusFailException, StatusForbiddenException {

        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        // 获取本场比赛的状态
        Contest contest = contestEntityService.getById(checkACDto.getCid());

        boolean isRoot = groupManager.getGroupAuthAdmin(contest.getGid());

        if (!isRoot
                && !contest.getUid().equals(userRolesVo.getUid())
                && !(contest.getIsGroup() && groupValidator.isGroupRoot(userRolesVo.getUid(), contest.getGid()))) {
            throw new StatusForbiddenException("对不起，您无权限操作！");
        }

        boolean isOk = contestRecordEntityService.updateById(
                new ContestRecord().setChecked(checkACDto.getChecked()).setId(checkACDto.getId()));

        if (!isOk) {
            throw new StatusFailException("修改失败！");
        }

    }

    public IPage<ContestPrintVO> getContestPrint(Long cid, Integer currentPage, Integer limit)
            throws StatusForbiddenException {

        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        // 获取本场比赛的状态
        Contest contest = contestEntityService.getById(cid);

        boolean isRoot = groupManager.getGroupAuthAdmin(contest.getGid());

        if (!isRoot
                && !contest.getUid().equals(userRolesVo.getUid())
                && !(contest.getIsGroup() && groupValidator.isGroupRoot(userRolesVo.getUid(), contest.getGid()))) {
            throw new StatusForbiddenException("对不起，您无权限操作！");
        }

        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        if (limit == null || limit < 1)
            limit = 30;

        // 获取当前比赛的，未被确定的排在签名

        QueryWrapper<ContestPrint> contestPrintQueryWrapper = new QueryWrapper<>();
        contestPrintQueryWrapper.select("id", "cid", "uid", "status", "gmt_create")
                .eq("cid", cid)
                .orderByAsc("status")
                .orderByDesc("gmt_create");

        List<ContestPrint> contestPrintList = contestPrintEntityService.list(contestPrintQueryWrapper);

        List<ContestPrintVO> contestPrintVoList = contestPrintList
                .stream()
                .filter(cp -> cp.getUid() != null)
                .map(cp -> {
                    ContestPrintVO vo = new ContestPrintVO();
                    BeanUtil.copyProperties(cp, vo);
                    vo.setRealname(userRoleEntityService.getRealNameByUid(cp.getUid()));
                    vo.setUsername(userRoleEntityService.getUsernameByUid(cp.getUid()));
                    return vo;
                })
                .collect(Collectors.toList());

        return Paginate.paginateListToIPage(contestPrintVoList, currentPage, limit);
    }

    public void checkContestPrintStatus(Long id, Long cid) throws StatusFailException, StatusForbiddenException {

        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();
        // 获取本场比赛的状态
        Contest contest = contestEntityService.getById(cid);

        boolean isRoot = groupManager.getGroupAuthAdmin(contest.getGid());

        if (!isRoot && !contest.getUid().equals(userRolesVo.getUid())
                && !(contest.getIsGroup() && groupValidator.isGroupRoot(userRolesVo.getUid(), contest.getGid()))) {
            throw new StatusForbiddenException("对不起，您无权限操作！");
        }

        boolean isOk = contestPrintEntityService.updateById(new ContestPrint().setId(id).setStatus(1));

        if (!isOk) {
            throw new StatusFailException("修改失败！");
        }
    }

    public IPage<SessionVO> getContestSession(Long cid, Integer currentPage, Integer limit, String keyword,
            String unkeyword)
            throws StatusForbiddenException {

        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        // 获取本场比赛的状态
        Contest contest = contestEntityService.getById(cid);

        boolean isRoot = groupManager.getGroupAuthAdmin(contest.getGid());

        if (!isRoot
                && !contest.getUid().equals(userRolesVo.getUid())
                && !(contest.getIsGroup() && groupValidator.isGroupRoot(userRolesVo.getUid(), contest.getGid()))) {
            throw new StatusForbiddenException("对不起，您无权限操作！");
        }

        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        if (limit == null || limit < 1)
            limit = 10;

        IPage<SessionVO> iPage = new Page<>(currentPage, limit);
        return sessionMapper.getContestSessionList(iPage, cid, keyword, unkeyword);

    }

    public IPage<SessionVO> getContestIp(Long cid, Integer currentPage, Integer limit)
            throws StatusForbiddenException {

        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        // 获取本场比赛的状态
        Contest contest = contestEntityService.getById(cid);

        boolean isRoot = groupManager.getGroupAuthAdmin(contest.getGid());

        if (!isRoot
                && !contest.getUid().equals(userRolesVo.getUid())
                && !(contest.getIsGroup() && groupValidator.isGroupRoot(userRolesVo.getUid(), contest.getGid()))) {
            throw new StatusForbiddenException("对不起，您无权限操作！");
        }

        return Paginate.paginateListToIPage(judgeEntityService.getContestJudgeUserList(cid), currentPage, limit);
    }

    public void rejudgeContestIp(Long cid, String uid) throws StatusForbiddenException, StatusFailException {

        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        // 获取本场比赛的状态
        Contest contest = contestEntityService.getById(cid);

        boolean isRoot = groupManager.getGroupAuthAdmin(contest.getGid());

        if (!isRoot
                && !contest.getUid().equals(userRolesVo.getUid())
                && !(contest.getIsGroup() && groupValidator.isGroupRoot(userRolesVo.getUid(), contest.getGid()))) {
            throw new StatusForbiddenException("对不起，您无权限操作！");
        }

        QueryWrapper<Judge> judgeServerQueryWrapper = new QueryWrapper<>();
        judgeServerQueryWrapper.eq("cid", cid).eq("uid", uid);
        List<Judge> judgeServerList = judgeEntityService.list(judgeServerQueryWrapper);

        // 将该用户所有的提交设置为已重置IP
        for (Judge judge : judgeServerList) {
            judge.setIsReset(true);
        }

        Boolean isOk = judgeEntityService.updateBatchById(judgeServerList);
        if (!isOk) {
            throw new StatusFailException("修改失败！");
        }
    }

}