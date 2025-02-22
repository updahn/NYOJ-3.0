package top.hcode.hoj.service.oj;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.vo.UserClocVO;

public interface RankService {

    public CommonResult<IPage> getRankList(Integer limit, Integer currentPage, String searchUser, Integer type);

    public CommonResult<List<UserClocVO>> getUserCodeRecord(List<String> uidList, String startTime, String endTime);

}
