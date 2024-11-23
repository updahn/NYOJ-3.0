package top.hcode.hoj.service.oj;

import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;

import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.vo.AliveVO;

public interface CookieService {

    public CommonResult<IPage<AliveVO>> getAliveList(Integer limit, Integer currentPage, String scraper);

    public CommonResult<Map<String, String>> getCookieMap(String oj, String user);

}
