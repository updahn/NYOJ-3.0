package top.hcode.hoj.pojo.vo;

import java.util.HashMap;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import top.hcode.hoj.pojo.bo.Pair_;
import top.hcode.hoj.pojo.entity.contest.Contest;

@Data
@Accessors(chain = true)
public class StatisticVO {

    @ApiModelProperty(value = "查询的关键词")
    private String keyword;

    @ApiModelProperty(value = "包含比赛的比例")
    private List<Integer> percentList;

    @ApiModelProperty(value = "处理后的比赛列表")
    private List<Contest> contestList;

    @ApiModelProperty(value = "处理后的账号信息")
    private List<Pair_<String, String>> accountList;

    @ApiModelProperty(value = "用户字典")
    private HashMap<String, String> data;
}
