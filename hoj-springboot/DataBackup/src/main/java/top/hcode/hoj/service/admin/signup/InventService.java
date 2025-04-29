package top.hcode.hoj.service.admin.signup;

import top.hcode.hoj.common.result.CommonResult;
import top.hcode.hoj.pojo.dto.InventDTO;
import top.hcode.hoj.pojo.dto.InventedDTO;
import top.hcode.hoj.pojo.vo.ReplyVO;
import top.hcode.hoj.pojo.vo.UserInventVO;

public interface InventService {

    public CommonResult<UserInventVO> addInvent(InventDTO inventDto);

    public CommonResult<Void> removeInvent(String username, String toUsername);

    public CommonResult<ReplyVO> handleInvent(InventedDTO inventedDto);

}
