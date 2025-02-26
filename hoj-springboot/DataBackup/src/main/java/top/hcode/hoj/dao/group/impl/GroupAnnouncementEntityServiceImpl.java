package top.hcode.hoj.dao.group.impl;

import top.hcode.hoj.common.result.Paginate;
import top.hcode.hoj.dao.group.GroupAnnouncementEntityService;
import top.hcode.hoj.mapper.GroupAnnouncementMapper;
import top.hcode.hoj.pojo.entity.common.Announcement;
import top.hcode.hoj.pojo.vo.AnnouncementVO;
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
public class GroupAnnouncementEntityServiceImpl extends ServiceImpl<GroupAnnouncementMapper, Announcement>
        implements GroupAnnouncementEntityService {

    @Autowired
    private GroupAnnouncementMapper groupAnnouncementMapper;

    @Override
    public IPage<AnnouncementVO> getAnnouncementList(int limit, int currentPage, Long gid) {

        List<AnnouncementVO> announcementList = groupAnnouncementMapper.getAnnouncementList(gid);

        return Paginate.paginateListToIPage(announcementList, currentPage, limit);
    }

    @Override
    public IPage<AnnouncementVO> getAdminAnnouncementList(int limit, int currentPage, Long gid) {

        List<AnnouncementVO> announcementList = groupAnnouncementMapper.getAdminAnnouncementList(gid);

        return Paginate.paginateListToIPage(announcementList, currentPage, limit);
    }

}
