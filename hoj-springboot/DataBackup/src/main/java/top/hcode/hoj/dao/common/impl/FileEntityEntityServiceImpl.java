package top.hcode.hoj.dao.common.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.hcode.hoj.mapper.FileMapper;
import top.hcode.hoj.pojo.entity.common.File;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.vo.ACMContestRankVO;
import top.hcode.hoj.pojo.vo.OIContestRankVO;
import top.hcode.hoj.dao.common.FileEntityService;
import top.hcode.hoj.dao.contest.ContestEntityService;
import top.hcode.hoj.utils.Constants;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * @Author: Himit_ZH
 * @Date: 2021/1/11 14:05
 * @Description:
 */
@Service
public class FileEntityEntityServiceImpl extends ServiceImpl<FileMapper, File> implements FileEntityService {
    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private ContestEntityService contestEntityService;

    @Override
    public int updateFileToDeleteByUidAndType(String uid, String type) {
        return fileMapper.updateFileToDeleteByUidAndType(uid, type);
    }

    @Override
    public int updateFileToDeleteByGidAndType(Long gid, String type) {
        return fileMapper.updateFileToDeleteByGidAndType(gid, type);
    }

    @Override
    public Boolean editHomeCarousel(Long id, String addLink, String addHint) {
        return fileMapper.editHomeCarousel(id, addLink, addHint);
    }

    @Override
    public Boolean editFileHint(Long id, String hint) {
        return fileMapper.editFileHint(id, hint);
    }

    @Override
    public List<File> queryDeleteAvatarList() {
        return fileMapper.queryDeleteAvatarList();
    }

    @Override
    public List<File> queryCarouselFileList() {
        return fileMapper.queryCarouselFileList();
    }

    @Override
    public List<File> queryBoxFileList() {
        return fileMapper.queryBoxFileList();
    }

    @Override
    public List<List<String>> getContestRankExcelHead(List<String> contestProblemDisplayIDList, Integer contestType) {
        List<List<String>> headList = new LinkedList<>();

        List<String> head0 = new LinkedList<>();
        head0.add("Rank");

        List<String> head1 = new LinkedList<>();
        head1.add("Username");
        List<String> head2 = new LinkedList<>();
        head2.add("ShowName");
        List<String> head3 = new LinkedList<>();
        head3.add("Real Name");
        List<String> head4 = new LinkedList<>();
        head4.add("School");
        List<String> head5 = new LinkedList<>();
        head5.add("Faculty");
        List<String> head6 = new LinkedList<>();
        head6.add("Course");

        headList.add(head0);
        headList.add(head1);
        headList.add(head2);
        headList.add(head3);
        headList.add(head4);
        headList.add(head5);
        headList.add(head6);

        List<String> head7 = new LinkedList<>();
        if (contestType == Constants.Contest.TYPE_ACM.getCode()) {
            head7.add("AC");
            List<String> head8 = new LinkedList<>();
            head8.add("Total Submission");
            List<String> head9 = new LinkedList<>();
            head9.add("Total Penalty Time");
            headList.add(head7);
            headList.add(head8);
            headList.add(head9);
        } else if (contestType == Constants.Contest.TYPE_EXAM.getCode()) {
            head7.add("AC");
            List<String> head8 = new LinkedList<>();
            head8.add("Total Submission");
            List<String> head9 = new LinkedList<>();
            head9.add("Total Score");
            headList.add(head7);
            headList.add(head8);
            headList.add(head9);
        } else {
            head7.add("Total Score");
            headList.add(head7);
        }

        // 添加题目头
        for (String displayID : contestProblemDisplayIDList) {
            List<String> tmp = new LinkedList<>();
            tmp.add(displayID);
            headList.add(tmp);
        }
        return headList;
    }

    @Override
    public List<List<String>> getStatisticRankExcelHead(List<String> cidList, Boolean isRoot) {
        List<List<String>> headList = new LinkedList<>();

        List<String> head0 = new LinkedList<>();
        head0.add("Rank");

        List<String> head1 = new LinkedList<>();
        head1.add("Username");
        List<String> head2 = new LinkedList<>();
        head2.add("School");

        headList.add(head0);
        headList.add(head1);

        if (isRoot) {
            List<String> head5 = new LinkedList<>();
            head5.add("Faculty");
            List<String> head6 = new LinkedList<>();
            head6.add("Course");
            List<String> head7 = new LinkedList<>();
            head7.add("Real Name");

            headList.add(head5);
            headList.add(head6);
            headList.add(head7);
        }

        headList.add(head2);

        List<String> head3 = new LinkedList<>();
        head3.add("AC");
        List<String> head4 = new LinkedList<>();
        head4.add("Total Penalty Time");
        headList.add(head3);
        headList.add(head4);

        // 添加题目头
        for (String cid : cidList) {
            List<String> tmp = new LinkedList<>();
            Contest contest = contestEntityService.getById(cid);
            tmp.add(contest != null ? cid + ":" + contest.getTitle() : cid);
            headList.add(tmp);
        }

        return headList;
    }

    @Override
    public List<List<Object>> changeACMContestRankToExcelRowList(List<ACMContestRankVO> acmContestRankVOList,
            List<String> contestProblemDisplayIDList,
            String rankShowName,
            Boolean isAcm) {
        List<List<Object>> allRowDataList = new LinkedList<>();
        for (ACMContestRankVO acmContestRankVo : acmContestRankVOList) {
            List<Object> rowData = new LinkedList<>();
            rowData.add(acmContestRankVo.getRank() == -1 ? "*" : acmContestRankVo.getRank().toString());
            rowData.add(acmContestRankVo.getUsername());
            if ("username".equals(rankShowName)) {
                rowData.add(acmContestRankVo.getUsername());
            } else if ("realname".equals(rankShowName)) {
                rowData.add(acmContestRankVo.getRealname());
            } else if ("nickname".equals(rankShowName)) {
                rowData.add(acmContestRankVo.getNickname());
            } else {
                rowData.add("");
            }
            rowData.add(acmContestRankVo.getRealname());
            rowData.add(acmContestRankVo.getSchool());
            rowData.add(acmContestRankVo.getFaculty());
            rowData.add(acmContestRankVo.getCourse());
            rowData.add(acmContestRankVo.getAc());
            rowData.add(acmContestRankVo.getTotal());
            if (isAcm) {
                rowData.add(acmContestRankVo.getTotalTime());
            } else {
                rowData.add(acmContestRankVo.getTotalScore());
            }
            HashMap<String, HashMap<String, Object>> submissionInfo = acmContestRankVo.getSubmissionInfo();
            for (String displayID : contestProblemDisplayIDList) {
                HashMap<String, Object> problemInfo = submissionInfo.getOrDefault(displayID, null);
                if (problemInfo != null) { // 如果是有提交记录的
                    boolean isAC = (boolean) problemInfo.getOrDefault("isAC", false);
                    boolean isAfterContest = (boolean) problemInfo.getOrDefault("isAfterContest", false);
                    String info = "";
                    int errorNum = (int) problemInfo.getOrDefault("errorNum", 0);
                    int tryNum = (int) problemInfo.getOrDefault("tryNum", 0);
                    if (isAC) {
                        if (errorNum == 0) {
                            info = "+(1)";
                        } else {
                            info = "+(" + (errorNum + 1) + ")";
                        }
                        if (isAfterContest) {
                            info += "*";
                        }
                    } else {
                        if (tryNum != 0 && errorNum != 0) {
                            info = "-(" + errorNum + "+" + tryNum + ")";
                        } else if (errorNum != 0) {
                            info = "-(" + errorNum + ")";
                        } else if (tryNum != 0) {
                            info = "?(" + tryNum + ")";
                        }
                    }
                    rowData.add(info);
                } else {
                    rowData.add("");
                }
            }
            allRowDataList.add(rowData);
        }
        return allRowDataList;
    }

    @Override
    public List<List<Object>> changOIContestRankToExcelRowList(List<OIContestRankVO> oiContestRankVOList,
            List<String> contestProblemDisplayIDList,
            String rankShowName) {
        List<List<Object>> allRowDataList = new LinkedList<>();
        for (OIContestRankVO oiContestRankVo : oiContestRankVOList) {
            List<Object> rowData = new LinkedList<>();
            rowData.add(oiContestRankVo.getRank() == -1 ? "*" : oiContestRankVo.getRank().toString());
            rowData.add(oiContestRankVo.getUsername());
            if ("username".equals(rankShowName)) {
                rowData.add(oiContestRankVo.getUsername());
            } else if ("realname".equals(rankShowName)) {
                rowData.add(oiContestRankVo.getRealname());
            } else if ("nickname".equals(rankShowName)) {
                rowData.add(oiContestRankVo.getNickname());
            } else {
                rowData.add("");
            }
            rowData.add(oiContestRankVo.getRealname());
            rowData.add(oiContestRankVo.getSchool());
            rowData.add(oiContestRankVo.getTotalScore());
            HashMap<String, Integer> submissionInfo = oiContestRankVo.getSubmissionInfo();
            for (String displayID : contestProblemDisplayIDList) {
                Integer score = submissionInfo.getOrDefault(displayID, null);
                if (score != null) { // 如果是有提交记录的就写最后一次提交的分数，没有的就写空
                    rowData.add(score);
                } else {
                    rowData.add("");
                }
            }
            allRowDataList.add(rowData);
        }
        return allRowDataList;
    }

    @Override
    public List<List<Object>> changeStatisticContestRankToExcelRowList(
            List<ACMContestRankVO> acmStatisticContestRankVOList,
            List<String> cidList, Boolean isRoot) {
        List<List<Object>> allRowDataList = new LinkedList<>();
        for (ACMContestRankVO acmContestRankVo : acmStatisticContestRankVOList) {
            List<Object> rowData = new LinkedList<>();
            rowData.add(acmContestRankVo.getRank() == -1 ? "*" : acmContestRankVo.getRank().toString());
            rowData.add(acmContestRankVo.getUsername());
            rowData.add(acmContestRankVo.getSchool());
            if (isRoot) {
                rowData.add(acmContestRankVo.getFaculty());
                rowData.add(acmContestRankVo.getCourse());
                rowData.add(acmContestRankVo.getRealname());
            }
            rowData.add(acmContestRankVo.getAc());
            rowData.add(acmContestRankVo.getTotalTime());
            HashMap<String, HashMap<String, Object>> contestInfos = acmContestRankVo.getSubmissionInfo();

            for (String cid : cidList) {
                HashMap<String, Object> contestInfo = contestInfos.getOrDefault(cid, null);
                if (contestInfo != null) {
                    String info = String.valueOf(contestInfo.getOrDefault("ac", 0));
                    rowData.add(info);
                } else {
                    rowData.add("");
                }
            }
            allRowDataList.add(rowData);
        }
        return allRowDataList;
    }
}