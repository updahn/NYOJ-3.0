package top.hcode.hoj.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;
import top.hcode.hoj.pojo.entity.discussion.Comment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.hcode.hoj.pojo.vo.CommentVO;

import java.util.List;

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
public interface CommentMapper extends BaseMapper<Comment> {

    List<CommentVO> getCommentList(
            @Param("cid") Long cid,
            @Param("did") Integer did,
            @Param("onlyMineAndAdmin") Boolean onlyMineAndAdmin,
            @Param("myAndAdminUidList") List<String> myAndAdminUidList);
}
