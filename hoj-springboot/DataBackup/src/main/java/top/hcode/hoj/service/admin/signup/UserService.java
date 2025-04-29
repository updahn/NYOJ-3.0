package top.hcode.hoj.service.admin.signup;

import java.util.List;

import com.baomidou.mybatisplus.core.metadata.IPage;

import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.dto.UserSignDTO;
import top.hcode.hoj.pojo.vo.UserInfoVO;
import top.hcode.hoj.pojo.vo.UserSignVO;

public interface UserService {

    public CommonResult<IPage<UserSignVO>> getUserSignList(Integer currentPage, Integer limit, String keyword,
            String startYear, String school);

    public CommonResult<UserSignVO> getUserSign(String username);

    public CommonResult<Void> addUserSign(UserSignDTO userSignDto);

    public CommonResult<UserInfoVO> updateUserSign(UserSignDTO userSignDto);

    public CommonResult<Void> removeUserSign(String username, Long id);

    public CommonResult<Void> addUserSignBatch(List<UserSignDTO> userSignDtoList);

}