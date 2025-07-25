package top.hcode.hoj.dao.common.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.beans.factory.annotation.Autowired;
import top.hcode.hoj.pojo.entity.common.Announcement;
import top.hcode.hoj.mapper.AnnouncementMapper;
import top.hcode.hoj.pojo.vo.AnnouncementVO;
import top.hcode.hoj.common.result.Paginate;
import top.hcode.hoj.dao.common.AnnouncementEntityService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author Himit_ZH
 * @since 2020-10-23
 */
@Service
public class AnnouncementEntityServiceImpl extends ServiceImpl<AnnouncementMapper, Announcement>
        implements AnnouncementEntityService {

    @Autowired
    private AnnouncementMapper announcementMapper;

    @Override
    public IPage<AnnouncementVO> getAnnouncementList(int limit, int currentPage, Boolean notAdmin, Long id) {
        return Paginate.paginateListToIPage(announcementMapper.getAnnouncementList(notAdmin, id), currentPage,
                limit);
    }

    @Override
    public IPage<AnnouncementVO> getContestAnnouncement(Long cid, Boolean notAdmin, int limit, int currentPage,
            Long id) {
        return Paginate.paginateListToIPage(announcementMapper.getContestAnnouncement(cid, notAdmin, id),
                currentPage, limit);
    }
}
