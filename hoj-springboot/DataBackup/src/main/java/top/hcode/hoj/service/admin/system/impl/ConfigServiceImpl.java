package top.hcode.hoj.service.admin.system.impl;

import cn.hutool.json.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.hcode.hoj.common.exception.StatusFailException;
import top.hcode.hoj.common.exception.StatusForbiddenException;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.common.result.ResultStatus;
import top.hcode.hoj.judge.Dispatcher;
import top.hcode.hoj.manager.admin.system.ConfigManager;
import top.hcode.hoj.pojo.dto.*;
import top.hcode.hoj.service.admin.system.ConfigService;
import top.hcode.hoj.utils.Constants;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/9 22:23
 * @Description:
 */
@Service
public class ConfigServiceImpl implements ConfigService {

    @Resource
    private ConfigManager configManager;

    @Autowired
    private Dispatcher dispatcher;

    @Override
    public CommonResult<JSONObject> getServiceInfo() {
        return CommonResult.successResponse(configManager.getServiceInfo());
    }

    @Override
    public CommonResult<List<JSONObject>> getJudgeServiceInfo() {
        return CommonResult.successResponse(configManager.getJudgeServiceInfo());
    }

    @Override
    public CommonResult<List<JSONObject>> getDockerServiceInfo() {
        try {
            return CommonResult.successResponse(configManager.getDockerServiceInfo());
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult setDockerServer(DockerConfigDTO config) {
        Boolean isJudge = config.getIsJudge();

        if (isJudge) {
            // 调用判题服务,运行管理容器命令
            return dispatcher.dispatch(Constants.TaskType.DOCKER, config);
        }

        try {
            configManager.setDockerServer(config);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<Void> deleteHomeCarousel(Long id) {
        try {
            configManager.deleteHomeCarousel(id);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<Void> editHomeCarousel(Long id, String addLink, String addHint) {
        try {
            configManager.editHomeCarousel(id, addLink, addHint);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<Void> editFileHint(Long id, String hint) {
        try {
            configManager.editFileHint(id, hint);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        } catch (StatusForbiddenException e) {
            return CommonResult.errorResponse(e.getMessage(), ResultStatus.FORBIDDEN);
        }
    }

    @Override
    public CommonResult<WebConfigDTO> getWebConfig() {
        return CommonResult.successResponse(configManager.getWebConfig());
    }

    @Override
    public CommonResult<Void> setWebConfig(WebConfigDTO config) {
        try {
            configManager.setWebConfig(config);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<EmailConfigDTO> getEmailConfig() {
        return CommonResult.successResponse(configManager.getEmailConfig());
    }

    @Override
    public CommonResult<HtmltopdfDTO> getHtmltopdfConfig() {
        return CommonResult.successResponse(configManager.getHtmltopdfConfig());
    }

    @Override
    public CommonResult<ClocDTO> getClocConfig() {
        return CommonResult.successResponse(configManager.getClocConfig());
    }

    @Override
    public CommonResult<Void> setEmailConfig(EmailConfigDTO config) {
        try {
            configManager.setEmailConfig(config);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<Void> setHtmltopdfConfig(HtmltopdfDTO config) {
        try {
            configManager.setHtmltopdfConfig(config);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<Void> setClocConfig(ClocDTO config) {
        try {
            configManager.setClocConfig(config);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<Void> testEmail(TestEmailDTO testEmailDto) {
        try {
            configManager.testEmail(testEmailDto);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<DBAndRedisConfigDTO> getDBAndRedisConfig() {
        return CommonResult.successResponse(configManager.getDBAndRedisConfig());
    }

    @Override
    public CommonResult<Void> setDBAndRedisConfig(DBAndRedisConfigDTO config) {
        try {
            configManager.setDBAndRedisConfig(config);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<SwitchConfigDTO> getSwitchConfig() {
        return CommonResult.successResponse(configManager.getSwitchConfig());
    }

    @Override
    public CommonResult<Void> setSwitchConfig(SwitchConfigDTO config) {
        try {
            configManager.setSwitchConfig(config);
            return CommonResult.successResponse();
        } catch (StatusFailException e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }
}