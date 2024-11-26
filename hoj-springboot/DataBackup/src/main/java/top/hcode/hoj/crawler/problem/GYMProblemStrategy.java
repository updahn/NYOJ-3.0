package top.hcode.hoj.crawler.problem;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.http.HttpRequest;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.problem.ProblemDescription;
import top.hcode.hoj.utils.CodeForcesUtils;
import top.hcode.hoj.utils.Constants;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: Himit_ZH
 * @Date: 2021/11/6 11:35
 * @Description:
 */

public class GYMProblemStrategy extends CFProblemStrategy {

    public static final String IMAGE_HOST = "https://codeforces.com";

    @Override
    public String getJudgeName() {
        return "GYM";
    }

    @Override
    public String getProblemUrl(String contestId, String problemNum) {
        String problemUrl = "/gym/%s/problem/%s";
        return HOST + String.format(problemUrl, contestId, problemNum);
    }

    @Override
    public String getProblemSource(String html, String problemId, String contestNum, String problemNum) {
        return String.format(
                "<p>Problem：<a style='color:#1A5CC8' href='https://codeforces.com/gym/%s/problem/%s'>%s</a></p><p>" +
                        "Contest：" + ReUtil.get("(<a[^<>]+/gym/\\d+\">.+?</a>)", html, 1)
                                .replace("/gym", HOST + "/gym")
                                .replace("color: black", "color: #009688;")
                        + "</p>",
                contestNum, problemNum, getJudgeName() + "-" + problemId);
    }

    @Override
    public RemoteProblemInfo getProblemInfo(String problemId, String author) {
        try {
            return super.getProblemInfo(problemId, author);
        } catch (Exception ignored) {
            String contestNum = ReUtil.get("([0-9]+)[A-Z]{1}[0-9]{0,1}", problemId, 1);
            String problemNum = ReUtil.get("[0-9]+([A-Z]{1}[0-9]{0,1})", problemId, 1);
            return getPDFHtml(problemId, contestNum, problemNum, author);
        }
    }

    @Override
    public RemoteProblemInfo getProblemInfoByLogin(String problemId, String author, String username, String password)
            throws Exception {
        return null;
    }

    private RemoteProblemInfo getPDFHtml(String problemId, String contestNum, String problemNum, String author) {

        Problem problem = new Problem();
        ProblemDescription problemDescription = new ProblemDescription().setPid(problem.getId());

        String url = HOST + "/gym/" + contestNum;
        HttpRequest request = HttpRequest.get(url)
                .timeout(20000);

        if (cookies != null) {
            request.cookie(cookies);
        }
        String html = request.execute().body();

        String regex = "<a href=\"\\/gym\\/" + contestNum + "\\/problem\\/" + problemNum
                + "\"><!--\\s*-->([^<]+)(?:(?:.|\\s)*?<div){2}[^>]*>\\s*([^<]+)<\\/div>\\s*([\\d.]+)\\D*(\\d+)";

        Matcher matcher = Pattern.compile(regex).matcher(html);
        matcher.find();

        problem.setProblemId(getJudgeName() + "-" + problemId);
        problemDescription.setTitle(matcher.group(1));
        problem.setTimeLimit((int) (Double.parseDouble(matcher.group(3)) * 1000));
        problem.setMemoryLimit(Integer.parseInt(matcher.group(4)));

        problemDescription.setSource(String.format(
                "<p>Problem：<a style='color:#1A5CC8' href='https://codeforces.com/gym/%s/attachments'>%s</a></p><p>" +
                        "Contest：" + ReUtil.get("(<a[^<>]+/gym/\\d+\">.+?</a>)", html, 1)
                                .replace("/gym", HOST + "/gym")
                                .replace("color: black", "color: #009688;")
                        + "</p>",
                contestNum, getJudgeName() + "-" + problemId));

        regex = "\\/gym\\/" + contestNum + "\\/attachments\\/download\\S*?\\.pdf";

        matcher = Pattern.compile(regex).matcher(html);
        StringBuilder description = new StringBuilder();
        while (matcher.find()) {
            String pdfURI = "";
            try {
                String uri = matcher.group(0);
                if (uri.toLowerCase().contains("tutorials")) {
                    continue;
                }
                String fileName = IdUtil.fastSimpleUUID() + ".pdf";
                String filePath = Constants.File.PROBLEM_FILE_FOLDER.getPath() + File.separator + fileName;
                CodeForcesUtils.downloadPDF(IMAGE_HOST + uri, filePath, true);
                pdfURI = Constants.File.FILE_API.getPath() + fileName;

                String[] split = uri.split("/");
                String fileRealName = split[split.length - 1];
                description.append("<p><a href=\"")
                        .append(pdfURI).append("\">")
                        .append(fileRealName)
                        .append("</a></p>");
            } catch (Exception e1) {
                try {
                    String fileName = IdUtil.fastSimpleUUID() + ".pdf";
                    String filePath = Constants.File.PROBLEM_FILE_FOLDER.getPath() + File.separator + fileName;
                    CodeForcesUtils.downloadPDF(HOST + "/gym/" + contestNum + "/problem/" + problemNum, filePath, true);
                    pdfURI = Constants.File.FILE_API.getPath() + fileName;
                } catch (Exception e2) {
                    pdfURI = HOST + matcher.group(0);
                }
                description.append("<p><a href=\"")
                        .append(pdfURI).append("\">").append(problemId).append("</a></p>");
            }
        }
        problemDescription.setDescription(
                "<pp>" + HtmlUtil.unescape(description.toString().replaceAll("(?<=\\>)\\s+(?=\\<)", "")));
        problem.setType(0)
                .setIsRemote(true)
                .setAuth(1)
                .setAuthor(author)
                .setOpenCaseResult(true)
                .setIsRemoveEndBlank(false)
                .setIsGroup(false)
                .setDifficulty(1); // 默认为中等

        List<ProblemDescription> problemDescriptionList = Collections.singletonList(problemDescription);
        return new RemoteProblemInfo()
                .setProblem(problem)
                .setProblemDescriptionList(problemDescriptionList)
                .setTagList(null)
                .setRemoteOJ(Constants.RemoteOJ.GYM);
    }

}