package top.hcode.hoj.mapper;

import top.hcode.hoj.pojo.dto.ProblemResDTO;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.vo.ProblemVO;
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
public interface GroupProblemMapper extends BaseMapper<Problem> {

    List<ProblemVO> getProblemList(@Param("gid") Long gid);

    List<ProblemResDTO> getAdminProblemList(@Param("gid") Long gid);
}
