package top.hcode.hoj.service.oj.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;

import lombok.extern.slf4j.Slf4j;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.manager.oj.CookieManager;
import top.hcode.hoj.pojo.vo.AliveVO;
import top.hcode.hoj.service.oj.CookieService;

import java.util.Map;

import javax.annotation.Resource;

@Service
@Slf4j(topic = "hoj")

public class CookieServiceImpl implements CookieService {

    @Resource
    private CookieManager cookieManager;

    @Override
    public CommonResult<IPage<AliveVO>> getAliveList(Integer limit, Integer currentPage, String scraper) {
        try {
            return CommonResult.successResponse(cookieManager.getCoursePage(limit, currentPage, scraper));
        } catch (Exception e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

    @Override
    public CommonResult<Map<String, String>> getCookieMap(String oj, String user) {
        try {
            return CommonResult.successResponse(cookieManager.getCookieMap(oj, user));
        } catch (Exception e) {
            return CommonResult.errorResponse(e.getMessage());
        }
    }

}