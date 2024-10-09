package top.hcode.hoj.pojo.vo;

import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class StatisticRankVO {

    @ApiModelProperty(value = "系列比赛的标题")
    private String title;

    @ApiModelProperty(value = "包含cids")
    private String cids;

    @ApiModelProperty(value = "包含cids")
    private String percents;

    @ApiModelProperty(value = "榜单数据")
    private List<ACMContestRankVO> acmContestRankVoList;
}
