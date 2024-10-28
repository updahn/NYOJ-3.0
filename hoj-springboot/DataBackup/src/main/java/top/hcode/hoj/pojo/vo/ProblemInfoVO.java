package top.hcode.hoj.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import top.hcode.hoj.pojo.dto.ProblemRes;
import top.hcode.hoj.pojo.entity.problem.ProblemDescription;
import top.hcode.hoj.pojo.entity.problem.Tag;
import java.util.HashMap;
import java.util.List;

/**
 * @Author: Himit_ZH
 * @Date: 2020/12/30 21:37
 * @Description:
 */
@Data
@AllArgsConstructor
public class ProblemInfoVO {
    /**
     * 题目内容
     */
    private ProblemRes problem;
    /**
     * 题目内容
     */
    private List<ProblemDescription> problemDescriptionList;
    /**
     * 题目标签
     */
    private List<Tag> tags;
    /**
     * 题目可用编程语言
     */
    private List<String> languages;
    /**
     * 题目提交统计情况
     */
    private ProblemCountVO problemCount;
    /**
     * 题目默认模板
     */
    private HashMap<String, String> codeTemplate;
    /**
     * 题目编程语言对应的判题key
     */
    private HashMap<String, String> languageKey;
}