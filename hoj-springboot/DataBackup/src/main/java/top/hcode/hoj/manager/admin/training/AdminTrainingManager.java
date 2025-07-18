package top.hcode.hoj.manager.admin.training;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.result.Paginate;
import top.hcode.hoj.dao.training.MappingTrainingCategoryEntityService;
import top.hcode.hoj.dao.training.TrainingCategoryEntityService;
import top.hcode.hoj.dao.training.TrainingEntityService;
import top.hcode.hoj.dao.training.TrainingRegisterEntityService;
import top.hcode.hoj.mapper.TrainingMapper;
import top.hcode.hoj.pojo.dto.TrainingDTO;
import top.hcode.hoj.pojo.entity.training.MappingTrainingCategory;
import top.hcode.hoj.pojo.entity.training.Training;
import top.hcode.hoj.pojo.entity.training.TrainingCategory;
import top.hcode.hoj.pojo.entity.training.TrainingRegister;
import top.hcode.hoj.shiro.AccountProfile;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.validator.TrainingValidator;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.List;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/9 19:46
 * @Description:
 */

@Component
@Slf4j(topic = "hoj")
public class AdminTrainingManager {

    @Resource
    private TrainingEntityService trainingEntityService;

    @Resource
    private MappingTrainingCategoryEntityService mappingTrainingCategoryEntityService;

    @Resource
    private TrainingCategoryEntityService trainingCategoryEntityService;

    @Resource
    private TrainingRegisterEntityService trainingRegisterEntityService;

    @Resource
    private AdminTrainingRecordManager adminTrainingRecordManager;

    @Resource
    private TrainingValidator trainingValidator;

    @Resource
    private TrainingMapper trainingMapper;

    public IPage<Training> getTrainingList(Integer limit, Integer currentPage, String keyword, Long categoryId,
            String auth) {

        if (currentPage == null || currentPage < 1)
            currentPage = 1;
        if (limit == null || limit < 1)
            limit = 10;

        List<Training> trainingList = trainingMapper.getAdminTrainingList(categoryId, auth, keyword);

        return Paginate.paginateListToIPage(trainingList, currentPage, limit);
    }

    public TrainingDTO getTraining(Long tid) throws StatusFailException, StatusForbiddenException {
        // 获取本场训练的信息
        Training training = trainingEntityService.getById(tid);
        if (training == null) { // 查询不存在
            throw new StatusFailException("查询失败：该训练不存在,请检查参数tid是否准确！");
        }

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");
        // 只有超级管理员和训练拥有者才能操作
        if (!isRoot && !userRolesVo.getUsername().equals(training.getAuthor())) {
            throw new StatusForbiddenException("对不起，你无权限操作！");
        }

        TrainingDTO trainingDto = new TrainingDTO();
        trainingDto.setTraining(training);

        QueryWrapper<MappingTrainingCategory> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tid", tid);
        MappingTrainingCategory mappingTrainingCategory = mappingTrainingCategoryEntityService.getOne(queryWrapper,
                false);
        TrainingCategory trainingCategory = null;
        if (mappingTrainingCategory != null) {
            trainingCategory = trainingCategoryEntityService.getById(mappingTrainingCategory.getCid());
        }
        trainingDto.setTrainingCategory(trainingCategory);
        return trainingDto;
    }

    public void deleteTraining(Long tid) throws StatusFailException, StatusForbiddenException {
        // 获取本场训练的信息
        Training training = trainingEntityService.getById(tid);
        if (training == null) { // 查询不存在
            throw new StatusFailException("查询失败：该训练不存在,请检查参数tid是否准确！");
        }
        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");

        // 只有超级管理员和题目管理和训练拥有者才能操作
        if (!isRoot && !userRolesVo.getUsername().equals(training.getAuthor())) {
            throw new StatusForbiddenException("对不起，你无权限操作！");
        }
        boolean isOk = trainingEntityService.removeById(tid);
        /*
         * Training的id为其他表的外键的表中的对应数据都会被一起删除！
         */
        if (!isOk) {
            throw new StatusFailException("删除失败！");
        }
        log.info("[{}],[{}],tid:[{}],operatorUid:[{}],operatorUsername:[{}]",
                "Admin_Training", "Delete", tid, userRolesVo.getUid(), userRolesVo.getUsername());
    }

    @Transactional(rollbackFor = Exception.class)
    public void addTraining(TrainingDTO trainingDto) throws StatusFailException {
        Training training = trainingDto.getTraining();
        trainingValidator.validateTraining(training);
        trainingEntityService.save(training);
        TrainingCategory trainingCategory = trainingDto.getTrainingCategory();
        if (trainingCategory.getId() == null) {
            try {
                trainingCategoryEntityService.save(trainingCategory);
            } catch (Exception ignored) {
                QueryWrapper<TrainingCategory> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("name", trainingCategory.getName());
                trainingCategory = trainingCategoryEntityService.getOne(queryWrapper, false);
            }
        }

        boolean isOk = mappingTrainingCategoryEntityService.save(new MappingTrainingCategory()
                .setTid(training.getId())
                .setCid(trainingCategory.getId()));
        if (!isOk) {
            throw new StatusFailException("添加失败！");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateTraining(TrainingDTO trainingDto) throws StatusForbiddenException, StatusFailException {
        Training training = trainingDto.getTraining();
        trainingValidator.validateTraining(training);

        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");
        // 只有超级管理员和训练拥有者才能操作
        if (!isRoot && !userRolesVo.getUsername().equals(trainingDto.getTraining().getAuthor())) {
            throw new StatusForbiddenException("对不起，你无权限操作！");
        }
        Training oldTraining = trainingEntityService.getById(training.getId());
        trainingEntityService.updateById(training);

        // 私有训练 修改密码 需要清空之前注册训练的记录
        if (training.getAuth().equals(Constants.Training.AUTH_PRIVATE.getValue())) {
            if (!Objects.equals(training.getPrivatePwd(), oldTraining.getPrivatePwd())) {
                UpdateWrapper<TrainingRegister> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("tid", training.getId());
                trainingRegisterEntityService.remove(updateWrapper);
            }
        }

        TrainingCategory trainingCategory = trainingDto.getTrainingCategory();
        if (trainingCategory.getId() == null) {
            try {
                trainingCategoryEntityService.save(trainingCategory);
            } catch (Exception ignored) {
                QueryWrapper<TrainingCategory> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("name", trainingCategory.getName());
                trainingCategory = trainingCategoryEntityService.getOne(queryWrapper, false);
            }
        }

        MappingTrainingCategory mappingTrainingCategory = mappingTrainingCategoryEntityService
                .getOne(new QueryWrapper<MappingTrainingCategory>().eq("tid", training.getId()),
                        false);

        if (mappingTrainingCategory == null) {
            mappingTrainingCategoryEntityService.save(new MappingTrainingCategory()
                    .setTid(training.getId()).setCid(trainingCategory.getId()));
            adminTrainingRecordManager.checkSyncRecord(trainingDto.getTraining());
        } else {
            if (!mappingTrainingCategory.getCid().equals(trainingCategory.getId())) {
                UpdateWrapper<MappingTrainingCategory> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("tid", training.getId()).set("cid", trainingCategory.getId());
                boolean isOk = mappingTrainingCategoryEntityService.update(null, updateWrapper);
                if (isOk) {
                    adminTrainingRecordManager.checkSyncRecord(trainingDto.getTraining());
                } else {
                    throw new StatusFailException("修改失败");
                }
            }
        }

    }

    public void changeTrainingStatus(Long tid, String author, Boolean status)
            throws StatusForbiddenException, StatusFailException {
        // 获取当前登录的用户
        AccountProfile userRolesVo = (AccountProfile) SecurityUtils.getSubject().getPrincipal();

        boolean isRoot = SecurityUtils.getSubject().hasRole("root")
                || SecurityUtils.getSubject().hasRole("admin");
        // 只有超级管理员和训练拥有者才能操作
        if (!isRoot && !userRolesVo.getUsername().equals(author)) {
            throw new StatusForbiddenException("对不起，你无权限操作！");
        }

        boolean isOk = trainingEntityService.saveOrUpdate(new Training().setId(tid).setStatus(status));
        if (!isOk) {
            throw new StatusFailException("修改失败");
        }
    }

}