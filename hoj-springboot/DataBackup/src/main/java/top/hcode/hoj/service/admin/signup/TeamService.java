package top.hcode.hoj.service.admin.signup;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;

import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.vo.TeamSignVO;

public interface TeamService {

    public CommonResult<IPage<TeamSignVO>> getTeamSignList(Integer currentPage, Integer limit, Long cid,
            Integer type, Integer status, String keyword, Long signCid);

    public CommonResult<IPage<Contest>> getContestList(Integer limit, Integer currentPage, String status,
            String keyword);

    public CommonResult<TeamSignVO> getTeamSign(Long id);

    public CommonResult<Void> addTeamSign(TeamSignVO teamSignVo);

    public CommonResult<Void> updateTeamSign(TeamSignVO teamSignVo);

    public CommonResult<Void> removeTeamSign(Long id);

    public CommonResult<Void> updateTeamSignStatus(List<Long> ids, Long cid, Integer status, String msg);

    public CommonResult<Void> addTeamSignBatch(List<Long> ids, Long cid, Integer type);

}