package top.hcode.hoj.controller.signup;

import java.util.List;

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.metadata.IPage;

import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.dto.UserSignDTO;
import top.hcode.hoj.pojo.vo.UserInfoVO;
import top.hcode.hoj.pojo.vo.UserSignVO;
import top.hcode.hoj.service.admin.signup.UserService;

@RestController
@RequestMapping("/api/user")
@RequiresAuthentication
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/list")
    public CommonResult<IPage<UserSignVO>> getUserSignList(
            @RequestParam(value = "currentPage", required = false) Integer currentPage,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "startYear", required = false) String startYear,
            @RequestParam(value = "school", required = false) String school) {

        return userService.getUserSignList(currentPage, limit, keyword, startYear, school);
    }

    @GetMapping
    public CommonResult<UserSignVO> getUserSign(@RequestParam(value = "username", required = true) String username) {

        return userService.getUserSign(username);
    }

    @PostMapping
    public CommonResult<Void> addUserSign(@RequestBody UserSignDTO userSignDto) {

        return userService.addUserSign(userSignDto);
    }

    @PutMapping
    public CommonResult<UserInfoVO> updateUserSign(@RequestBody UserSignDTO userSignDto) {

        return userService.updateUserSign(userSignDto);
    }

    @DeleteMapping
    public CommonResult<Void> removeUserSign(@RequestParam("username") String username, @RequestParam("id") Long id) {

        return userService.removeUserSign(username, id);
    }

    @PostMapping("/batch")
    public CommonResult<Void> addUserSignBatch(@RequestBody List<UserSignDTO> userSignDtoList) {

        return userService.addUserSignBatch(userSignDtoList);
    }

}
