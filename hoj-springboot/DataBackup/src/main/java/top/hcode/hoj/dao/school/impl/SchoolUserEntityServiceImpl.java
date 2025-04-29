package top.hcode.hoj.dao.school.impl;

import top.hcode.hoj.pojo.entity.school.SchoolUser;
import top.hcode.hoj.dao.school.SchoolUserEntityService;
import top.hcode.hoj.mapper.SchoolUserMapper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SchoolUserEntityServiceImpl extends ServiceImpl<SchoolUserMapper, SchoolUser>
        implements SchoolUserEntityService {

}
