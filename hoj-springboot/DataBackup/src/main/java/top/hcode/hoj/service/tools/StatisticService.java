package top.hcode.hoj.service.tools;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.dto.StatisticRankDTO;
import top.hcode.hoj.pojo.entity.contest.StatisticContest;
import top.hcode.hoj.pojo.vo.*;

public interface StatisticService {

        public CommonResult<IPage<StatisticContest>> getStatisticList(Integer currentPage, Integer limit,
                        String keyword);

        public CommonResult<IPage<ACMContestRankVO>> getStatisticRank(StatisticRankDTO statisticRankDTO);

        public CommonResult<String> getStatisticRankCids(String scid);

        public CommonResult<IPage<StatisticContest>> getAdminStatisticList(Integer currentPage, Integer limit,
                        String keyword);

        public CommonResult<Void> addStatisticRank(StatisticRankDTO statisticRankDTO);

        public CommonResult<Void> updateStatisticRank(StatisticRankDTO statisticRankDTO);

        public CommonResult<Void> deleteStatisticRank(String scid);

        public CommonResult<Void> changeStatisticVisible(String scid, Boolean show);

        public CommonResult<IPage<ACMContestRankVO>> dealStatisticRankList(StatisticRankDTO statisticRankDTO);

}
