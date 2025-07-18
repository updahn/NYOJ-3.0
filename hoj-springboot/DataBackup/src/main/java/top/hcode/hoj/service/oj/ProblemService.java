package top.hcode.hoj.service.oj;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.dto.LastAcceptedCodeVO;
import top.hcode.hoj.pojo.dto.PidListDTO;
import top.hcode.hoj.pojo.vo.ProblemFullScreenListVO;
import top.hcode.hoj.pojo.vo.ProblemInfoVO;
import top.hcode.hoj.pojo.vo.ProblemVO;
import top.hcode.hoj.pojo.vo.RandomProblemVO;
import top.hcode.hoj.pojo.vo.RemotejudgeVO;

import java.util.HashMap;
import java.util.List;

/**
 * @Author: Himit_ZH
 * @Date: 2022/3/11 11:04
 * @Description:
 */
public interface ProblemService {

    public CommonResult<Page<ProblemVO>> getProblemList(Integer limit, Integer currentPage,
            String keyword, List<Long> tagId, Integer difficulty, Integer type, String oj);

    public CommonResult<RandomProblemVO> getRandomProblem(String oj);

    public CommonResult<HashMap<Long, Object>> getUserProblemStatus(PidListDTO pidListDto);

    public CommonResult<ProblemInfoVO> getProblemInfo(String problemId, Long gid, Long tid, Long peid);

    public CommonResult<String> getProblemPdf(Long pid, Long peid, Long cid);

    public CommonResult<LastAcceptedCodeVO> getUserLastAcceptedCode(Long pid, Long cid);

    public CommonResult<List<ProblemFullScreenListVO>> getFullScreenProblemList(Long tid, Long cid);

    public CommonResult<List<RemotejudgeVO>> getRemoteJudgeStatusList(String remoteOj);
}