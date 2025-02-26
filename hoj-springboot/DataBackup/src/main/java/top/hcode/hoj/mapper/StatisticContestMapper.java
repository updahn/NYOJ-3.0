package top.hcode.hoj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.hcode.hoj.pojo.entity.contest.StatisticContest;

@Mapper
@Repository
public interface StatisticContestMapper extends BaseMapper<StatisticContest> {

    List<StatisticContest> getAdminStatisticContestList(@Param("keyword") String keyword);

    List<StatisticContest> getStatisticContestList(@Param("keyword") String keyword);
}
