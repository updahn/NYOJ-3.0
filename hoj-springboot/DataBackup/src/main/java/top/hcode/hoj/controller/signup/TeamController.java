package top.hcode.hoj.controller.signup;

import java.util.List;
import java.util.Map;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.metadata.IPage;

import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.vo.TeamSignVO;
import top.hcode.hoj.service.admin.signup.TeamService;

@RestController
@RequestMapping("/api/team")
@RequiresAuthentication
public class TeamController {

    @Autowired
    private TeamService teamService;

    @GetMapping("/list")
    public CommonResult<IPage<TeamSignVO>> getTeamSignList(
            @RequestParam(value = "currentPage", required = false) Integer currentPage,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "cid", required = false) Long cid,
            @RequestParam(value = "type", required = false) Integer type,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "signCid", required = false) Long signCid) {

        return teamService.getTeamSignList(currentPage, limit, cid, type, status, keyword, signCid);
    }

    @GetMapping("/contest-list")
    @RequiresAuthentication
    public CommonResult<IPage<Contest>> getContestList(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "currentPage", required = false) Integer currentPage,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "keyword", required = false) String keyword) {

        return teamService.getContestList(limit, currentPage, status, keyword);
    }

    @GetMapping
    public CommonResult<TeamSignVO> getTeamSign(@RequestParam(value = "id", required = true) Long id) {

        return teamService.getTeamSign(id);
    }

    @PostMapping
    public CommonResult<Void> addTeamSign(@RequestBody TeamSignVO teamSignVo) {

        return teamService.addTeamSign(teamSignVo);
    }

    @PutMapping
    public CommonResult<Void> updateTeamSign(@RequestBody TeamSignVO teamSignVo) {

        return teamService.updateTeamSign(teamSignVo);
    }

    @DeleteMapping
    public CommonResult<Void> removeTeamSign(@RequestParam(value = "id", required = true) Long id) {

        return teamService.removeTeamSign(id);
    }

    @PostMapping("/status")
    public CommonResult<Void> updateTeamSignStatus(@RequestBody Map<String, Object> params,
            @RequestParam(value = "cid", required = true) Long cid,
            @RequestParam(value = "status", required = true) Integer status,
            @RequestParam(value = "msg", required = false) String msg) {

        return teamService.updateTeamSignStatus((List<Long>) params.get("ids"), cid, status, msg);
    }

    @PostMapping("/batch")
    public CommonResult<Void> addTeamSignBatch(@RequestBody Map<String, Object> params,
            @RequestParam(value = "cid", required = true) Long cid,
            @RequestParam(value = "type", required = true) Integer type) {

        return teamService.addTeamSignBatch((List<Long>) params.get("ids"), cid, type);
    }

}
