package top.hcode.hoj.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import top.hcode.hoj.common.exception.StatusNotFoundException;
import top.hcode.hoj.config.NacosSwitchConfig;
import top.hcode.hoj.config.WebConfig;
import top.hcode.hoj.dao.contest.ContestEntityService;
import top.hcode.hoj.dao.contest.ContestProblemEntityService;
import top.hcode.hoj.dao.problem.ProblemDescriptionEntityService;
import top.hcode.hoj.dao.problem.ProblemEntityService;
import top.hcode.hoj.mapper.ContestProblemMapper;
import top.hcode.hoj.pojo.dto.ProblemRes;
import top.hcode.hoj.pojo.entity.contest.Contest;
import top.hcode.hoj.pojo.entity.contest.ContestProblem;
import top.hcode.hoj.pojo.entity.problem.ProblemDescription;

@Component
@RefreshScope
@Slf4j(topic = "hoj")
public class HtmlToPdfUtils {

    /**
     *
     * Pandoc 官方地址:
     * https://pandoc.org/
     *
     * 1. 查看系统中的字体：
     * fc-list :lang=zh
     *
     * 2. 检查系统中是否包含 Courier New 字体：
     * fc-list | grep "Courier New"
     *
     */

    @Autowired
    private NacosSwitchConfig nacosSwitchConfig;

    @Autowired
    private ProblemEntityService problemEntityService;

    @Autowired
    private ContestEntityService contestEntityService;

    @Autowired
    private ContestProblemMapper contestProblemMapper;

    @Autowired
    private ContestProblemEntityService contestProblemEntityService;

    @Autowired
    private ProblemDescriptionEntityService problemDescriptionEntityService;

    private String RootUrl;
    private String Host;
    private Boolean EC;

    @PostConstruct
    public void init() throws StatusNotFoundException {
        WebConfig webConfig = nacosSwitchConfig.getWebConfig();

        String host = webConfig.getHtmltopdfHost();
        Integer port = webConfig.getHtmltopdfPort();
        this.RootUrl = webConfig.getBaseUrl().replaceAll("/$", ""); // 去除末尾的 "/"
        this.Host = (host.startsWith("http") ? host : "https://" + host) + (port != null ? ":" + port : "");
        this.EC = webConfig.getHtmltopdfEc();
        if (StringUtils.isEmpty(webConfig.getHtmltopdfHost())) {
            throw new StatusNotFoundException("htmltopdf 服务未配置！");
        }
    }

    /**
     * 保存比赛题面
     *
     * @param contest    比赛信息
     * @param outputPath 保存位置
     */
    @Async
    public void updateContestPDF(Contest contest, String outputPath) {
        String outputName = getProblemDescriptionName(outputPath);
        Long cid = contest.getId();

        List<ContestProblem> contestProblemList = contestProblemMapper.getContestProblemList(cid);

        // 如果没有题目，直接返回
        if (CollectionUtils.isEmpty(contestProblemList)) {
            return;
        }

        // 提取 ContestProblem 列表中的 pid，并获取对应的 ProblemRes 列表
        List<ProblemRes> problemList = contestProblemList.stream()
                .map(contestProblem -> {
                    return problemEntityService.getProblemRes(
                            contestProblem.getPid(), contestProblem.getPeid(), null, contest.getGid(), cid);
                })
                .collect(Collectors.toList());

        // 遍历检查比赛题目列表中，没有生成pdf的题面，并处理 PDF 生成
        problemList.parallelStream().forEach(problem -> {
            if (StringUtils.isEmpty(problem.getPdfDescription())) {
                try {
                    String fileName = IdUtil.fastSimpleUUID();
                    convertPDFByHtml(problem, fileName);
                    problem.setPdfDescription(IdUtil.fastSimpleUUID());
                } catch (IOException e) {
                    // 捕获异常但不终止流处理
                    e.printStackTrace();
                }
            }
        });

        // 按照题面顺寻排序后的，对应合成的pdf路径
        List<String> fileNameList = problemList.stream()
                .map(problem -> getProblemDescriptionName(problem.getPdfDescription())) // 修复调用方式
                .filter(Objects::nonNull) // 过滤掉 null 值
                .collect(Collectors.toList());

        try {
            // 合并并保存 PDF
            savePDFDetails(fileNameList, outputName, problemList.get(0).getContestTime(),
                    problemList.get(0).getContestTitle());
        } catch (IOException e) {
            // 捕获异常但不终止流处理
            e.printStackTrace();
        }

        // 保存对应的比赛题面和题目题面
        contest.setPdfDescription(Constants.File.FILE_API.getPath() + outputName + ".pdf");
        contestEntityService.updateById(contest);
    }

    /**
     * 保存/更新题面
     *
     * @param problem 题目信息
     */
    @Async
    public void updateProblemPDF(ProblemRes problemRes) {
        List<ProblemRes> problemList = new ArrayList<>();

        problemList.add(problemRes);

        // 查询所有相关比赛题目并批量加入列表
        QueryWrapper<ContestProblem> contestProblemQuery = new QueryWrapper<>();
        contestProblemQuery.eq("pid", problemRes.getId());
        contestProblemEntityService.list(contestProblemQuery).forEach(cp -> {
            ProblemRes contestProblemRes = problemEntityService.getProblemRes(cp.getPid(), cp.getPeid(), null, null,
                    cp.getCid());
            contestProblemRes.setCid(cp.getCid());
            problemList.add(contestProblemRes);
        });

        // 使用并行流并发处理
        problemList.parallelStream().forEach(prob -> {
            Long probId = prob.getId();
            Long probPeid = prob.getPeid();

            String pdfName = Optional.ofNullable(getProblemDescriptionName(prob.getPdfDescription()))
                    .orElse(IdUtil.fastSimpleUUID());

            try {
                // 检查 PDF 文件
                Pattern pattern = Pattern.compile("/api/public/file/([a-fA-F0-9]+)\\.pdf");
                Matcher matcher = pattern.matcher(prob.getDescription());
                if (matcher.find()) {
                    pdfName = matcher.group(1);
                } else {
                    convertPDFByHtml(prob, pdfName);
                }

                // 更新数据库
                if (prob.getCid() != null && prob.getCid() != 0) {
                    contestProblemEntityService.update(new UpdateWrapper<ContestProblem>()
                            .eq("pid", probId).eq("cid", prob.getCid())
                            .set("pdf_description", Constants.File.FILE_API.getPath() + pdfName + ".pdf"));
                } else {
                    String htmlPath = Constants.File.PROBLEM_FILE_FOLDER.getPath() + File.separator + pdfName + ".html";
                    String html = FileUtil.readString(new File(htmlPath), StandardCharsets.UTF_8);

                    // 更新对应数据库
                    UpdateWrapper<ProblemDescription> updateWrapper = new UpdateWrapper<>();
                    updateWrapper.eq("pid", probId).eq(probPeid != null, "id", probPeid);
                    updateWrapper.set("pdf_description", Constants.File.FILE_API.getPath() + pdfName + ".pdf");
                    updateWrapper.set("html", html);

                    problemDescriptionEntityService.update(updateWrapper);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * html转化为pdf
     *
     * @param problem  题目信息
     * @param fileName 保存路径
     *
     */
    public void convertPDFByHtml(ProblemRes problem, String fileName) throws IOException {
        String html = problem.getHtml();

        // 对用的html为空则重新生成html
        if (StringUtils.isEmpty(html)) {
            // 生成保存 HTML 题面
            saveHtmlDetails(problem, fileName);
        } else {
            // 替换对应的html题面
            String workspace = Constants.File.PROBLEM_FILE_FOLDER.getPath() + File.separator + fileName + ".html";

            // 将html题面重新写入文件
            FileWriter fileWriter = new FileWriter(new File(workspace));
            fileWriter.write(html);
        }

        List<String> problemList = new ArrayList<>(Collections.singletonList(fileName));
        savePDFDetails(problemList, fileName, null, null);

    }

    /**
     * 保存 HTML 题面
     *
     * @param problem  题目
     * @param fileName 保存路径
     */
    public void saveHtmlDetails(ProblemRes problem, String fileName) throws IOException {
        // docker 对应的wkhtmltopdf默认目录
        String workspace = Constants.File.DOCKER_PROBLEM_FILE_FOLDER.getPath() + "/";

        HttpRequest httpRequest = HttpRequest.post(Host + "/html");

        // 将标题转化为比赛标题
        if (!StringUtils.isEmpty(problem.getContestTitle()))
            problem.setTitle("Problem " + problem.getDisplayId() + ". " + problem.getDisplayTitle());

        httpRequest.header("Accept", "*/*")
                .header("Connection", "keep-alive")
                .form("input_path", workspace + fileName + ".html")
                .form("timeLimit", problem.getTimeLimit())
                .form("memoryLimit", problem.getMemoryLimit())
                .form("title", problem.getTitle())
                .form("description", convertToMarkdown(problem.getDescription()))
                .form("input", convertToMarkdown(problem.getInput()))
                .form("output", convertToMarkdown(problem.getOutput()))
                .form("hint", convertToMarkdown(problem.getHint()))
                .form("EC", EC);

        List<Map<String, String>> examplesList = stringToExamples(problem.getExamples());

        // 根据类型添加表格样例和选项
        Integer type = problem.getType();
        if ((type == 0 || type == 1) && !CollectionUtils.isEmpty(examplesList)) {
            httpRequest.form("examples", JSONUtil.toJsonStr(examplesList));
        }
        if (type == 2 && !CollectionUtils.isEmpty(examplesList)) {
            httpRequest.form("selections", JSONUtil.toJsonStr(examplesList));
        }

        HttpResponse response = httpRequest.execute();

        if (!response.isOk()) {
            log.error("Problem: {}, Create HTML Error: {}", problem.getTitle(), response.body().toString());
            throw new IOException("Create HTML Error");
        } else {
            String contestInfo = !StringUtils.isEmpty(problem.getContestTitle())
                    ? "Contest: " + problem.getContestTitle() + " "
                    : "";
            log.info("{} Problem: {}, Create HTML Success", contestInfo, problem.getTitle());
        }
    }

    /**
     * 保存 PDF 题面
     *
     * @param inputNames pdf 文件名称
     * @param outputName 返回的 pdf 文件名称
     */
    public void savePDFDetails(List<String> inputNames, String outputName, Date contestTime,
            String contestTitle) throws IOException {
        String workspace = Constants.File.DOCKER_PROBLEM_FILE_FOLDER.getPath() + "/";

        String inputPaths = inputNames.stream()
                .map(path -> workspace + path + ".html") // 给每个元素加头部和尾部
                .collect(Collectors.joining(",")); // 用空格连接

        HttpRequest httpRequest = HttpRequest.post(Host + "/pdf")
                .header("Accept", "*/*")
                .header("Connection", "keep-alive")
                .form("input_path", inputPaths)
                .form("output_path", (workspace + outputName + ".pdf"))
                .form("EC", EC);

        if (contestTime != null) {
            // 根据 EC 决定日期格式
            String pattern = EC ? "yyyy 年 M 月 d 日" : "yyyy.M.d";
            SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);

            // 格式化并添加比赛的相关信息
            String formattedDate = dateFormat.format(contestTime);
            httpRequest.form("contest_data", formattedDate);
        }

        if (!StringUtils.isEmpty(contestTitle)) {
            httpRequest.form("contest_title", contestTitle);
        }

        HttpResponse response = httpRequest.execute();

        if (!response.isOk()) {
            log.error("InputPaths {} Create PDF Error: {}", inputPaths, response.body().toString());
        } else {
            log.info("InputPaths: {}, Create PDF Success", inputPaths);
        }
    }

    /**
     * 删除 PDF 题面
     *
     * @param problemDescription 题面信息
     */
    public Boolean removeProblemPDF(ProblemDescription problemDescription) {
        boolean problemDescriptionResult = true;

        if (problemDescription == null) {
            return problemDescriptionResult;
        }

        String pdf_description = problemDescription.getPdfDescription();

        if (!StringUtils.isEmpty(pdf_description)) {
            String file_name = getProblemDescriptionName(pdf_description);

            // 删除对应的文件
            String basePath = Constants.File.PROBLEM_FILE_FOLDER.getPath() + File.separator + file_name;
            FileUtil.del(new File(basePath + ".pdf"));
            FileUtil.del(new File(basePath + ".html"));

            // 将题面清空
            UpdateWrapper<ProblemDescription> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", problemDescription.getId()).set("pdf_description", null);
            problemDescriptionResult &= problemDescriptionEntityService.update(updateWrapper);
        }

        // 更新题面
        problemDescriptionResult &= problemDescriptionEntityService.updateById(problemDescription);

        return problemDescriptionResult;
    }

    // 以下是处理信息部分

    /**
     * 将该网站中的 Markdown 的特殊字符转化。
     *
     * @param reloadHtml Markdown 字符串
     * @return 转化后的字符串
     */
    private String convertToMarkdown(String reloadHtml) {

        if (StringUtils.isEmpty(reloadHtml)) {
            return null;
        }

        // 提取所有 $...$ 包围的部分，替换为占位符
        Map<String, String> placeholders = new HashMap<>();
        Matcher matcher = Pattern.compile("\\$(.*?)\\$").matcher(reloadHtml);
        int i = 0;
        while (matcher.find()) {
            String key = "__PLACEHOLDER_" + i++ + "__";
            placeholders.put(key, matcher.group(0)); // 保存 $...$ 部分
            reloadHtml = reloadHtml.replace(matcher.group(0), key); // 替换为占位符
        }

        // 处理加粗、斜体、标题、下划线、删除线、标记、上角标、下角标
        reloadHtml = reloadHtml
                .replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>") // 加粗
                .replaceAll("\\*(.*?)\\*", "<em>$1</em>") // 斜体
                .replaceAll("\\+\\+(.*?)\\+\\+", "<ins>$1</ins>") // 下划线
                .replaceAll("~\\~(.*?)~\\~", "<s>$1</s>") // 删除线
                .replaceAll("==([^=]*)==", "<mark>$1</mark>") // 标记
                .replaceAll("\\^(.*?)\\^", "<sup>$1</sup>") // 上角标
                .replaceAll("~(.*?)~", "<sub>$1</sub>"); // 下角标

        // 处理居左、居中、居右的容器
        reloadHtml = reloadHtml.replaceAll(
                ":::\\s*hljs-(left|center|right)[\\s\\S]*?(?:<br>\\s*){0,2}([\\s\\S]*?)(?:<br>\\s*){0,2}:::",
                "<div class=\"hljs-$1\"><p>$2</p></div>");

        // 处理引用、有序列表、无序列表
        reloadHtml = reloadHtml
                .replaceAll("^> (.*)$", "<blockquote><p>$1</p></blockquote>") // 引用
                .replaceAll("^(\\d+\\. .*)$", "<ol><li>$1</li></ol>") // 有序列表
                .replaceAll("^(- .*)$", "<ul><li>$1</li></ul>"); // 无序列表

        // 将本域图片加上域名
        reloadHtml = reloadHtml
                .replaceAll("/api/public/img", RootUrl + "/api/public/img") // 本域图片
                .replaceAll("\"//images\\.weserv\\.nl/\\?url=", "\""); // 跨域图片

        // 处理标题
        reloadHtml = convertHeading(reloadHtml);

        // 还原 $...$ 部分
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            reloadHtml = reloadHtml.replace(entry.getKey(), entry.getValue());
        }

        // 处理代码块，去除 ``` 后面的语言声明
        reloadHtml = reloadHtml.replaceAll("(?m)^```\\s*\\w*\\s*[\r\n]+", "```\n");

        // 将 HTML 实体转换为普通字符
        reloadHtml = HtmlUtil.unescape(reloadHtml);

        // 去除两端空白
        return reloadHtml.trim();
    }

    /**
     * 将字符串类型的题面样例转化
     *
     * @param examples 测试用例
     * @return 对应的题目样例列表
     */
    private List<Map<String, String>> stringToExamples(String examples) {
        if (StringUtils.isEmpty(examples)) {
            return null;
        }
        List<Map<String, String>> example_list = new ArrayList<>();

        // 正则表达式，用于匹配 <input> 和 <output> 之间的内容
        String reg = "<input>([\\s\\S]*?)</input><output>([\\s\\S]*?)</output>";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(examples);

        // 查找匹配的部分
        while (matcher.find()) {
            Map<String, String> map = new HashMap<>();

            String input = matcher.group(1) + "\n";
            String output = matcher.group(2) + "\n";

            if (!input.equals("undefined")) {
                map.put("input", input); // 获取 <input> 标签之间的内容
            }
            map.put("output", output);// 获取 <output> 标签之间的内容

            example_list.add(map);
        }

        return example_list;
    }

    private String convertHeading(String text) {
        StringBuilder result = new StringBuilder();
        Pattern pattern = Pattern.compile("^(#{1,5}) (.+)$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            // 在当前匹配之前附加文本
            result.append(text, lastEnd, matcher.start());
            int level = matcher.group(1).length();
            result.append(String.format("<h%d>%s</h%d>", level, matcher.group(2), level));
            lastEnd = matcher.end();
        }
        result.append(text.substring(lastEnd));
        return result.toString();
    }

    public static String getProblemDescriptionName(String pdfDescription) {
        if (StringUtils.isEmpty(pdfDescription)) {
            return null;
        }

        int lastSlash = pdfDescription.lastIndexOf('/');
        int lastDot = pdfDescription.lastIndexOf('.');

        return (lastDot > lastSlash && lastSlash != -1)
                ? pdfDescription.substring(lastSlash + 1, lastDot)
                : pdfDescription;
    }

}