package top.hcode.hoj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;
import top.hcode.hoj.pojo.entity.discussion.DiscussionReport;
import top.hcode.hoj.pojo.vo.DiscussionReportVO;

@Mapper
@Repository
public interface DiscussionReportMapper extends BaseMapper<DiscussionReport> {

    List<DiscussionReportVO> getDiscussionReportList();
}
