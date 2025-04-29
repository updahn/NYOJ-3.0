package top.hcode.hoj.controller.signup;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.dto.InventDTO;
import top.hcode.hoj.pojo.dto.InventedDTO;
import top.hcode.hoj.pojo.vo.ReplyVO;
import top.hcode.hoj.pojo.vo.UserInventVO;
import top.hcode.hoj.service.admin.signup.InventService;

@RestController
@RequestMapping("/api/invent")
@RequiresAuthentication
public class InventController {

    @Autowired
    private InventService inventService;

    @PostMapping
    public CommonResult<UserInventVO> addInvent(@RequestBody InventDTO inventDto) {

        return inventService.addInvent(inventDto);
    }

    @DeleteMapping
    public CommonResult<Void> removeInvent(
            @RequestParam("username") String username, @RequestParam("toUsername") String toUsername) {

        return inventService.removeInvent(username, toUsername);
    }

    @PostMapping("/handle")
    public CommonResult<ReplyVO> handleInvent(@RequestBody InventedDTO inventedDto) {

        return inventService.handleInvent(inventedDto);
    }

}
