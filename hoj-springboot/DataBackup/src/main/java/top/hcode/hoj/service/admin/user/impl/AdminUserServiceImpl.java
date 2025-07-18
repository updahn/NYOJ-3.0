package top.hcode.hoj.service.admin.user.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.stereotype.Service;

import top.hcode.hoj.common.exception.StatusAccessDeniedException;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.common.result.ResultStatus;
import top.hcode.hoj.manager.admin.user.AdminUserManager;
import top.hcode.hoj.pojo.dto.AdminEditUserDTO;
import top.hcode.hoj.pojo.vo.UserRolesVO;
import top.hcode.hoj.service.admin.user.AdminUserService;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/9 21:34
 * @Description:
 */
@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Resource
    private AdminUserManager adminUserManager;

    @Override
    public CommonResult<IPage<UserRolesVO>> getUserList(Integer limit, Integer currentPage, Integer type,
            String keyword) {
        return CommonResult.successResponse(adminUserManager.getUserList(limit, currentPage, type, keyword));
    }

    @Override
    public CommonResult<Void> editUser(AdminEditUserDTO adminEditUserDto) {
        try {
            adminUserManager.editUser(adminEditUserDto);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<Void> deleteUser(List<String> deleteUserIdList) {
        try {
            adminUserManager.deleteUser(deleteUserIdList);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<Void> insertBatchUser(List<List<String>> users) {
        try {
            adminUserManager.insertBatchUser(users);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<Map<Object, Object>> generateUser(Map<String, Object> params) {
        try {
            return CommonResult.successResponse(adminUserManager.generateUser(params));
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<Void> applyUsersAccount(List<List<String>> users, String contestUrl, String contestTitle) {
        try {
            adminUserManager.applyUsersAccount(users, contestUrl, contestTitle);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusAccessDeniedException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.ACCESS_DENIED);
        }
    }

    @Override
    public CommonResult<Map<Object, Object>> resetUserPassword(List<List<String>> users) {
        try {
            return CommonResult.successResponse(adminUserManager.resetUserPassword(users));
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }
}