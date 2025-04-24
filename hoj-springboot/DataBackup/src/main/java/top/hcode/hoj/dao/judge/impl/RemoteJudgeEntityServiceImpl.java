package top.hcode.hoj.dao.judge.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.hcode.hoj.mapper.RemoteJudgeMapper;
import top.hcode.hoj.pojo.entity.judge.RemoteJudge;
import top.hcode.hoj.dao.judge.RemoteJudgeEntityService;

@Service
public class RemoteJudgeEntityServiceImpl extends ServiceImpl<RemoteJudgeMapper, RemoteJudge>
        implements RemoteJudgeEntityService {
}