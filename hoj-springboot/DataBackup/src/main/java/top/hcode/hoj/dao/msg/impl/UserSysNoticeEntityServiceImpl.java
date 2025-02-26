package top.hcode.hoj.dao.msg.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.hcode.hoj.mapper.UserSysNoticeMapper;
import top.hcode.hoj.pojo.entity.msg.UserSysNotice;
import top.hcode.hoj.pojo.vo.SysMsgVO;
import top.hcode.hoj.common.result.Paginate;
import top.hcode.hoj.dao.msg.UserSysNoticeEntityService;

import javax.annotation.Resource;

/**
 * @Author: Himit_ZH
 * @Date: 2021/10/1 20:35
 * @Description:
 */
@Service
public class UserSysNoticeEntityServiceImpl extends ServiceImpl<UserSysNoticeMapper, UserSysNotice>
        implements UserSysNoticeEntityService {

    @Resource
    private UserSysNoticeMapper userSysNoticeMapper;

    @Override
    public IPage<SysMsgVO> getSysNotice(int limit, int currentPage, String uid) {
        return Paginate.paginateListToIPage(userSysNoticeMapper.getSysOrMineNotice(uid, "Sys"), currentPage, limit);
    }

    @Override
    public IPage<SysMsgVO> getMineNotice(int limit, int currentPage, String uid) {
        return Paginate.paginateListToIPage(userSysNoticeMapper.getSysOrMineNotice(uid, "Mine"), currentPage, limit);
    }

}