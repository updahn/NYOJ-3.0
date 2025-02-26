package top.hcode.hoj.dao.group.impl;

import top.hcode.hoj.common.result.Paginate;
import top.hcode.hoj.dao.group.GroupContestEntityService;
import top.hcode.hoj.mapper.GroupContestMapper;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.vo.ContestVO;
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
public class GroupContestEntityServiceImpl extends ServiceImpl<GroupContestMapper, Contest>
        implements GroupContestEntityService {

    @Autowired
    private GroupContestMapper groupContestMapper;

    @Override
    public IPage<ContestVO> getContestList(int limit, int currentPage, Long gid, String keyword) {

        List<ContestVO> contestList = groupContestMapper.getContestList(gid, keyword);

        return Paginate.paginateListToIPage(contestList, currentPage, limit);
    }

    @Override
    public IPage<Contest> getAdminContestList(int limit, int currentPage, Long gid, String keyword) {

        List<Contest> contestList = groupContestMapper.getAdminContestList(gid, keyword);

        return Paginate.paginateListToIPage(contestList, currentPage, limit);
    }
}
