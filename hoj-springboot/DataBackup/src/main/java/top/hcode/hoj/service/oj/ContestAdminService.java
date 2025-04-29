package top.hcode.hoj.service.oj;

import java.util.Map;

import com.baomidou.mybatisplus.core.metadata.IPage;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.dto.CheckACDTO;
import top.hcode.hoj.pojo.entity.contest.ContestRecord;
import top.hcode.hoj.pojo.vo.ContestPrintVO;
import top.hcode.hoj.pojo.vo.SessionVO;

public interface ContestAdminService {

	public CommonResult<IPage<ContestRecord>> getContestACInfo(Long cid, Integer currentPage, Integer limit);

	public CommonResult<Void> checkContestACInfo(CheckACDTO checkACDto);

	public CommonResult<IPage<ContestPrintVO>> getContestPrint(Long cid, Integer currentPage, Integer limit);

	public CommonResult<Void> checkContestPrintStatus(Long id, Long cid);

	public CommonResult<IPage<SessionVO>> getContestSession(Long cid, Integer currentPage, Integer limit,
			String keyword, String unkeyword);

	public CommonResult<IPage<SessionVO>> getContestIp(Long cid, Integer currentPage, Integer limit);

	public CommonResult<Void> rejudgeContestIp(Long cid, String uid);

}
