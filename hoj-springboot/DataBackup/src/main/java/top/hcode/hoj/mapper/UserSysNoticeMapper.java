package top.hcode.hoj.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.hcode.hoj.pojo.entity.msg.UserSysNotice;
import top.hcode.hoj.pojo.vo.SysMsgVO;

import java.util.List;

@Mapper
@Repository
public interface UserSysNoticeMapper extends BaseMapper<UserSysNotice> {

    List<SysMsgVO> getSysOrMineNotice(@Param("uid") String uid, @Param("type") String type);
}
