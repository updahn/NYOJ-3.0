package top.hcode.hoj.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.hcode.hoj.pojo.entity.common.Announcement;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.hcode.hoj.pojo.vo.AnnouncementVO;

/**
 * <p>
 * Mapper 接口
 * </p>
 *
 * @author Himit_ZH
 * @since 2020-10-23
 */
@Mapper
@Repository
public interface AnnouncementMapper extends BaseMapper<Announcement> {
    List<AnnouncementVO> getAnnouncementList(@Param("notAdmin") Boolean notAdmin,
            @Param("id") Long id);

    List<AnnouncementVO> getContestAnnouncement(@Param("cid") Long cid,
            @Param("notAdmin") Boolean notAdmin, @Param("id") Long id);
}
