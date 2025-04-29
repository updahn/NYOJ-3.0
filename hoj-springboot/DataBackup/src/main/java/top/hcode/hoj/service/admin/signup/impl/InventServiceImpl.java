package top.hcode.hoj.service.admin.signup.impl;

import org.springframework.stereotype.Service;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.common.result.ResultStatus;
import top.hcode.hoj.manager.signup.InventManager;
import top.hcode.hoj.pojo.dto.InventDTO;
import top.hcode.hoj.pojo.dto.InventedDTO;
import top.hcode.hoj.pojo.vo.ReplyVO;
import top.hcode.hoj.pojo.vo.UserInventVO;
import top.hcode.hoj.service.admin.signup.InventService;

import javax.annotation.Resource;

/**
 *
 * @Date: 2022/3/11 16:21
 * @Description:
 */
@Service
public class InventServiceImpl implements InventService {

    @Resource
    private InventManager inventManager;

    @Override
    public CommonResult<UserInventVO> addInvent(InventDTO inventDto) {
        try {
            return CommonResult.successResponse(inventManager.addInvent(inventDto));
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<Void> removeInvent(String username, String toUsername) {
        try {
            inventManager.removeInvent(username, toUsername);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<ReplyVO> handleInvent(InventedDTO inventedDto) {
        try {
            return CommonResult.successResponse(inventManager.handleInvent(inventedDto));
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

}