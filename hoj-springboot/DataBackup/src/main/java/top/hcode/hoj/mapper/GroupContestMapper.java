package top.hcode.hoj.mapper;

import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.vo.ContestVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Author: LengYun
 * @Date: 2022/3/11 13:36
 * @Description:
 */
@Mapper
@Repository
public interface GroupContestMapper extends BaseMapper<Contest> {

    List<ContestVO> getContestList(@Param("gid") Long gid, @Param("keyword") String keyword);

    List<Contest> getAdminContestList(@Param("gid") Long gid, @Param("keyword") String keyword);

}
