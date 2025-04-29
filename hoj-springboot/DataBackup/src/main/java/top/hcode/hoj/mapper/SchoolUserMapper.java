package top.hcode.hoj.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import top.hcode.hoj.pojo.entity.school.SchoolUser;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

@Mapper
@Repository
public interface SchoolUserMapper extends BaseMapper<SchoolUser> {

}
