package top.hcode.hoj.pojo.vo;

import java.util.List;
import java.util.Date;

import lombok.Data;

@Data
public class RemotejudgeVO {

    private String oj;

    private Integer percent;

    private List<Integer> percentList;

    private List<Date> createTimeList;

    public RemotejudgeVO(String oj, List<Integer> percentList, List<Date> createTimeList) {
        this.oj = oj;
        this.percentList = percentList;
        this.createTimeList = createTimeList;
    }
}
