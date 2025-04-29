package top.hcode.hoj.dao.contest.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import top.hcode.hoj.mapper.TeamSignMapper;
import top.hcode.hoj.pojo.entity.contest.TeamSign;
import top.hcode.hoj.dao.contest.TeamSignEntityService;

@Service
public class TeamSignEntityServiceImpl extends ServiceImpl<TeamSignMapper, TeamSign>
        implements TeamSignEntityService {
}