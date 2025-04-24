package top.hcode.hoj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import top.hcode.hoj.pojo.entity.judge.RemoteJudge;

@Mapper
@Repository
public interface RemoteJudgeMapper extends BaseMapper<RemoteJudge> {
}
