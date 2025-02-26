package top.hcode.hoj.dao.group.impl;

import top.hcode.hoj.common.result.Paginate;
import top.hcode.hoj.dao.group.GroupProblemEntityService;
import top.hcode.hoj.mapper.GroupProblemMapper;
import top.hcode.hoj.pojo.dto.ProblemResDTO;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.vo.ProblemVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: LengYun
 * @Date: 2022/3/11 13:36
 * @Description:
 */
@Service
public class GroupProblemEntityServiceImpl extends ServiceImpl<GroupProblemMapper, Problem>
        implements GroupProblemEntityService {

    @Autowired
    private GroupProblemMapper groupProblemMapper;

    @Override
    public IPage<ProblemVO> getProblemList(int limit, int currentPage, Long gid) {

        List<ProblemVO> problemList = groupProblemMapper.getProblemList(gid);

        return Paginate.paginateListToIPage(problemList, currentPage, limit);
    }

    @Override
    public IPage<ProblemResDTO> getAdminProblemList(int limit, int currentPage, Long gid) {

        List<ProblemResDTO> problemList = groupProblemMapper.getAdminProblemList(gid);

        return Paginate.paginateListToIPage(problemList, currentPage, limit);
    }

}
