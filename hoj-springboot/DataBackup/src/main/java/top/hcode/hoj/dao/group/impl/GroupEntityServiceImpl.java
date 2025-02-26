package top.hcode.hoj.dao.group.impl;

import top.hcode.hoj.common.result.Paginate;
import top.hcode.hoj.dao.group.GroupEntityService;
import top.hcode.hoj.mapper.GroupMapper;
import top.hcode.hoj.pojo.entity.group.Group;
import top.hcode.hoj.pojo.vo.GroupVO;
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
public class GroupEntityServiceImpl extends ServiceImpl<GroupMapper, Group> implements GroupEntityService {

    @Autowired
    private GroupMapper groupMapper;

    @Override
    public IPage<GroupVO> getGroupList(int limit,
            int currentPage,
            String keyword,
            Integer auth,
            String uid,
            Boolean onlyMine,
            Boolean isRoot) {
        List<GroupVO> groupList = groupMapper.getGroupList(keyword, auth, uid, onlyMine, isRoot);

        return Paginate.paginateListToIPage(groupList, currentPage, limit);
    }
}
