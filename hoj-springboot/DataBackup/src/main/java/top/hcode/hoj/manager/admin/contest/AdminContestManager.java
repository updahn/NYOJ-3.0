package top.hcode.hoj.manager.admin.contest;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.exception.StatusSystemErrorException;
import top.hcode.hoj.dao.contest.ContestEntityService;
import top.hcode.hoj.dao.contest.ContestRegisterEntityService;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.entity.contest.ContestRegister;
import top.hcode.hoj.pojo.vo.AdminContestVO;
import top.hcode.hoj.pojo.vo.ContestAwardConfigVO;
import top.hcode.hoj.pojo.vo.ContestFileConfigVO;
import top.hcode.hoj.pojo.vo.ContestSynchronousConfigVO;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.utils.HtmlToPdfUtils;
import top.hcode.hoj.validator.ContestValidator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/9 11:20
 * @Description:
 */
@Component
@Slf4j(topic = "hoj")
public class AdminContestManager {

    @Autowired
    private ContestEntityService contestEntityService;

    @Autowired
    private ContestRegisterEntityService contestRegisterEntityService;

    @Autowired
    private ContestValidator contestValidator;

    @Autowired
    private HtmlToPdfUtils htmlToPdfUtils;

    public IPage<Contest> getContestList(Integer limit, Integer currentPage, Integer type,
            Integer auth, Integer status, String keyword) {

        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        if (limit == null || limit < 1)
            limit = 10;

        IPage<Contest> iPage = new Page<>(currentPage, limit);
        QueryWrapper<Contest> queryWrapper = new QueryWrapper<>();
        // 过滤密码
        queryWrapper.select(Contest.class, info -> !info.getColumn().equals("pwd"));

        if (type != null) {
            queryWrapper.eq("type", type);
        }
        if (auth != null) {
            queryWrapper.eq("auth", auth);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        if (!StringUtils.isEmpty(keyword)) {
            keyword = keyword.trim();
            queryWrapper
                    .like("title", keyword).or()
                    .like("id", keyword);
        }
        queryWrapper.eq("is_group", false).orderByDesc("start_time");
        return contestEntityService.page(iPage, queryWrapper);
    }

    public AdminContestVO getContest(Long cid) throws StatusFailException, StatusForbiddenException {
        Contest contest = contestEntityService.getById(cid);
        if (contest == null) { // 查询不存在
            throw new StatusFailException("查询失败：该比赛不存在,请检查参数cid是否准确！");
        }
        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");

        // 只有超级管理员和比赛拥有者才能操作
        if (!isRoot && !userRolesVo.getUid().equals(contest.getUid())) {
            throw new StatusForbiddenException("对不起，你无权限操作！");
        }
        AdminContestVO adminContestVo = BeanUtil.copyProperties(contest, AdminContestVO.class, "starAccount");
        if (StringUtils.isEmpty(contest.getStarAccount())) {
            adminContestVo.setStarAccount(new ArrayList<>());
        } else {
            try {
                JSONObject jsonObject = JSONUtil.parseObj(contest.getStarAccount());
                List<String> starAccount = jsonObject.get("star_account", List.class);
                adminContestVo.setStarAccount(starAccount);
            } catch (Exception e) {
                adminContestVo.setStarAccount(new ArrayList<>());
            }
        }

        if (contest.getAwardType() != null && contest.getAwardType() != 0) {
            try {
                JSONObject jsonObject = JSONUtil.parseObj(contest.getAwardConfig());
                List<ContestAwardConfigVO> awardConfigList = jsonObject.get("config", List.class);
                adminContestVo.setAwardConfigList(awardConfigList);
            } catch (Exception e) {
                adminContestVo.setAwardConfigList(new ArrayList<>());
            }
        } else {
            adminContestVo.setAwardConfigList(new ArrayList<>());
        }

        // 文件柜功能
        if (contest.getOpenFile() != null && contest.getOpenFile()) {
            try {
                JSONObject jsonObject = JSONUtil.parseObj(contest.getFileConfig());
                List<ContestFileConfigVO> fileConfigList = jsonObject.get("config", List.class);

                adminContestVo.setFileConfigList(fileConfigList);

            } catch (Exception e) {
                adminContestVo.setFileConfigList(new ArrayList<>());
            }
        } else {
            adminContestVo.setFileConfigList(new ArrayList<>());
        }

        // 同步赛
        if (contest.getAuth().intValue() == Constants.Contest.AUTH_SYNCHRONOUS.getCode()) {
            try {
                JSONObject jsonObject = JSONUtil.parseObj(contest.getSynchronousConfig());
                List<ContestSynchronousConfigVO> synchronousConfigList = jsonObject.get("config", List.class);

                adminContestVo.setSynchronousConfigList(synchronousConfigList);

            } catch (Exception e) {
                adminContestVo.setSynchronousConfigList(new ArrayList<>());
            }
        } else {
            adminContestVo.setSynchronousConfigList(new ArrayList<>());
        }

        return adminContestVo;
    }

    public void deleteContest(Long cid) throws StatusFailException, StatusForbiddenException {
        Contest contest = contestEntityService.getById(cid);
        if (contest == null) { // 查询不存在
            throw new StatusFailException("查询失败：该比赛不存在,请检查参数cid是否准确！");
        }
        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");

        // 只有超级管理员和题目管理和训练拥有者才能操作
        if (!isRoot && !userRolesVo.getUid().equals(contest.getUid())) {
            throw new StatusForbiddenException("对不起，你无权限操作！");
        }

        boolean isOk = contestEntityService.removeById(cid);
        /*
         * contest的id为其他表的外键的表中的对应数据都会被一起删除！
         */
        if (!isOk) { // 删除成功
            throw new StatusFailException("删除失败");
        }
        log.info("[{}],[{}],cid:[{}],operatorUid:[{}],operatorUsername:[{}]",
                "Admin_Contest", "Delete", cid, userRolesVo.getUid(), userRolesVo.getUsername());
    }

    public void addContest(AdminContestVO adminContestVo) throws StatusFailException {
        contestValidator.validateContest(adminContestVo);

        Contest contest = BeanUtil.copyProperties(adminContestVo, Contest.class, "starAccount");
        JSONObject accountJson = new JSONObject();
        if (adminContestVo.getStarAccount() == null) {
            accountJson.set("star_account", new ArrayList<>());
        } else {
            accountJson.set("star_account", adminContestVo.getStarAccount());
        }
        contest.setStarAccount(accountJson.toString());

        if (adminContestVo.getAwardType() != null && adminContestVo.getAwardType() != 0) {
            JSONObject awardConfigJson = new JSONObject();
            List<ContestAwardConfigVO> awardConfigList = adminContestVo.getAwardConfigList();
            awardConfigList.sort(Comparator.comparingInt(ContestAwardConfigVO::getPriority));
            awardConfigJson.set("config", awardConfigList);
            contest.setAwardConfig(awardConfigJson.toString());
        }

        // 文件柜
        if (adminContestVo.getOpenFile() != null && adminContestVo.getOpenFile()) {
            JSONObject fileConfigJson = new JSONObject();
            List<ContestFileConfigVO> fileConfigList = adminContestVo.getFileConfigList();
            fileConfigJson.set("config", fileConfigList);
            contest.setFileConfig(fileConfigJson.toString());
        }

        // 同步赛
        if (adminContestVo.getAuth().intValue() == Constants.Contest.AUTH_SYNCHRONOUS.getCode()) {
            List<ContestSynchronousConfigVO> synchronousConfigList = adminContestVo.getSynchronousConfigList();
            JSONObject awardConfigJson = new JSONObject();
            awardConfigJson.set("config", synchronousConfigList);
            contest.setSynchronousConfig(awardConfigJson.toString());
        }

        // 正式赛
        if (adminContestVo.getAuth().intValue() == Constants.Contest.AUTH_OFFICIAL.getCode()) {
            contest.setSignStartTime(adminContestVo.getSignStartTime());
            contest.setSignEndTime(adminContestVo.getSignEndTime());
            contest.setSignDuration(adminContestVo.getSignDuration());
            contest.setMaxParticipants(adminContestVo.getMaxParticipants());
            contest.setModifyEndTime(adminContestVo.getModifyEndTime());
        }

        boolean isOk = contestEntityService.save(contest);
        if (!isOk) { // 删除成功
            throw new StatusFailException("添加失败");
        }
    }

    public void cloneContest(Long cid) throws StatusSystemErrorException {
        Contest contest = contestEntityService.getById(cid);
        if (contest == null) {
            throw new StatusSystemErrorException("该比赛不存在，无法克隆！");
        }
        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        contest.setUid(userRolesVo.getUid())
                .setAuthor(userRolesVo.getUsername())
                .setSource(cid.intValue())
                .setId(null)
                .setGmtCreate(null)
                .setGmtModified(null);
        contest.setTitle(contest.getTitle() + " [Cloned]");
        contestEntityService.save(contest);
    }

    public void updateContest(AdminContestVO adminContestVo) throws StatusForbiddenException, StatusFailException {
        contestValidator.validateContest(adminContestVo);

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");
        // 只有超级管理员和比赛拥有者才能操作
        if (!isRoot && !userRolesVo.getUid().equals(adminContestVo.getUid())) {
            throw new StatusForbiddenException("对不起，你无权限操作！");
        }
        Contest contest = BeanUtil.copyProperties(adminContestVo, Contest.class, "starAccount");

        JSONObject accountJson = new JSONObject();
        accountJson.set("star_account", adminContestVo.getStarAccount());
        contest.setStarAccount(accountJson.toString());

        if (adminContestVo.getAwardType() != null && adminContestVo.getAwardType() != 0) {
            List<ContestAwardConfigVO> awardConfigList = adminContestVo.getAwardConfigList();
            awardConfigList.sort(Comparator.comparingInt(ContestAwardConfigVO::getPriority));
            JSONObject awardConfigJson = new JSONObject();
            awardConfigJson.set("config", awardConfigList);
            contest.setAwardConfig(awardConfigJson.toString());
        }

        // 文件柜
        if (adminContestVo.getOpenFile() != null && adminContestVo.getOpenFile()) {
            JSONObject fileConfigJson = new JSONObject();
            List<ContestFileConfigVO> fileConfigList = adminContestVo.getFileConfigList();
            fileConfigJson.set("config", fileConfigList);
            contest.setFileConfig(fileConfigJson.toString());
        }

        // 同步赛
        if (adminContestVo.getAuth().intValue() == Constants.Contest.AUTH_SYNCHRONOUS.getCode()) {
            List<ContestSynchronousConfigVO> synchronousConfigList = adminContestVo.getSynchronousConfigList();
            JSONObject awardConfigJson = new JSONObject();
            awardConfigJson.set("config", synchronousConfigList);
            contest.setSynchronousConfig(awardConfigJson.toString());
        }

        // 正式赛
        if (adminContestVo.getAuth().intValue() == Constants.Contest.AUTH_OFFICIAL.getCode()) {
            contest.setSignStartTime(adminContestVo.getSignStartTime());
            contest.setSignEndTime(adminContestVo.getSignEndTime());
            contest.setSignDuration(adminContestVo.getSignDuration());
            contest.setMaxParticipants(adminContestVo.getMaxParticipants());
            contest.setModifyEndTime(adminContestVo.getModifyEndTime());
        }

        Contest oldContest = contestEntityService.getById(contest.getId());
        boolean isOk = contestEntityService.saveOrUpdate(contest);
        if (isOk) {
            if (!contest.getAuth().equals(Constants.Contest.AUTH_PUBLIC.getCode())) {
                if (!Objects.equals(oldContest.getPwd(), contest.getPwd())) { // 改了比赛密码则需要删掉已有的注册比赛用户
                    UpdateWrapper<ContestRegister> updateWrapper = new UpdateWrapper<>();
                    updateWrapper.eq("cid", contest.getId());
                    contestRegisterEntityService.remove(updateWrapper);
                }
            }
        } else {
            throw new StatusFailException("修改失败");
        }

        String outputPath = contest.getPdfDescription();
        // 如果 outputName 为空，生成一个唯一 ID
        if (outputPath == null) {
            outputPath = IdUtil.fastSimpleUUID();
        }

        // 异步生成比赛题面
        htmlToPdfUtils.updateContestPDF(contest, outputPath);
    }

    public void changeContestVisible(Long cid, String uid, Boolean visible)
            throws StatusFailException, StatusForbiddenException {
        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");
        // 只有超级管理员和比赛拥有者才能操作
        if (!isRoot && !userRolesVo.getUid().equals(uid)) {
            throw new StatusForbiddenException("对不起，你无权限操作！");
        }

        boolean isOK = contestEntityService.saveOrUpdate(new Contest().setId(cid).setVisible(visible));

        if (!isOK) {
            throw new StatusFailException("修改失败");
        }
        log.info("[{}],[{}],value:[{}],cid:[{}],operatorUid:[{}],operatorUsername:[{}]",
                "Admin_Contest", "Change_Visible", visible, cid, userRolesVo.getUid(), userRolesVo.getUsername());
    }

}