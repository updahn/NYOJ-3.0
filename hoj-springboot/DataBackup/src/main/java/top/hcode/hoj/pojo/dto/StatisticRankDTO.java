package top.hcode.hoj.pojo.dto;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import top.hcode.hoj.pojo.vo.ACMContestRankVO;

import java.util.HashMap;
import java.util.List;

import javax.validation.constraints.NotBlank;

@Data
@Accessors(chain = true)
public class StatisticRankDTO {

    private Integer currentPage;

    private Integer limit;

    private String keyword;

    private String scid;

    @NotBlank(message = "包含cids不能为空")
    private String cids;

    @ApiModelProperty(value = "包含比赛的比例")
    private String percents;

    @ApiModelProperty(value = "用户字典")
    private HashMap<String, String> data;

    @ApiModelProperty(value = "爬取使用账号")
    private HashMap<String, String> account;

    @ApiModelProperty(value = "系列比赛的标题")
    private String title;

    @ApiModelProperty(value = "榜单数据")
    private List<ACMContestRankVO> acmContestRankVoList;

}