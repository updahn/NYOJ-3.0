package top.hcode.hoj.crawler.problem;

import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.http.HttpUtil;
import org.apache.commons.lang.Validate;
import org.jsoup.Jsoup;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.problem.ProblemDescription;
import top.hcode.hoj.utils.Constants;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: Himit_ZH
 * @Date: 2022/1/28 21:23
 * @Description:
 */
public class AtCoderProblemStrategy extends ProblemStrategy {

        public static final String JUDGE_NAME = "AC";
        public static final String HOST = "https://atcoder.jp";
        public static final String PROBLEM_URL = "/contests/%s/tasks/%s";

        public String getJudgeName() {
                return JUDGE_NAME;
        }

        public String getProblemUrl(String problemId, String contestId) {
                return HOST + String.format(PROBLEM_URL, contestId, problemId);
        }

        public String getProblemSource(String problemId, String contestId) {
                return String.format(
                                "<a style='color:#1A5CC8' href='" + getProblemUrl(problemId, contestId) + "'>%s</a>",
                                "AtCoder-" + problemId);
        }

        @Override
        public RemoteProblemInfo getProblemInfo(String problemId, String author) throws Exception {

                problemId = problemId.toLowerCase();
                boolean isMatch = ReUtil.isMatch("[a-z]+[0-9]+_[a-z]*[0-9]*", problemId);
                if (!isMatch && !problemId.contains("_")) {
                        throw new IllegalArgumentException(
                                        "AtCoder: Incorrect problem id format! Must be like `abc110_a`");
                }

                String contestId = problemId.split("_")[0];

                String body = HttpUtil.get(getProblemUrl(problemId, contestId));
                Pattern pattern = Pattern.compile("Time Limit: (\\d+) sec / Memory Limit: (\\d+) MB");
                Matcher matcher = pattern.matcher(body);
                Validate.isTrue(matcher.find());
                String timeLimit = matcher.group(1).trim();
                String memoryLimit = matcher.group(2).trim();
                String title = ReUtil.get("<title>[\\s\\S]*? - ([\\s\\S]*?)</title>", body, 1);

                Problem problem = new Problem();
                ProblemDescription problemDescription = new ProblemDescription().setPid(problem.getId());

                problem.setProblemId(getJudgeName() + "-" + problemId)
                                .setAuthor(author)
                                .setType(0)
                                .setTimeLimit(Integer.parseInt(timeLimit) * 1000)
                                .setMemoryLimit(Integer.parseInt(memoryLimit))
                                .setIsRemote(true)
                                .setAuth(1)
                                .setOpenCaseResult(false)
                                .setIsRemoveEndBlank(false)
                                .setIsGroup(false)
                                .setDifficulty(1); // 默认为中等

                problemDescription.setTitle(title)
                                .setSource(getProblemSource(problemId, contestId));

                if (body.contains("Problem Statement")) {
                        String desc = ReUtil.get("<h3>Problem Statement</h3>([\\s\\S]*?)</section>[\\s\\S]*?</div>",
                                        body, 1);

                        desc = desc.replaceAll("<var>", "\\$").replaceAll("</var>", "\\$");
                        desc = desc.replaceAll("<pre>",
                                        "<pre style=\"padding:9px!important;background-color: #f5f5f5!important\">");
                        desc = desc.replaceAll("src=\"/img", "src=\"" + HOST + "/img");

                        StringBuilder sb = new StringBuilder();
                        String rawInput = ReUtil.get("<h3>Input</h3>([\\s\\S]*?)</section>[\\s\\S]*?</div>", body, 1);
                        sb.append(rawInput);
                        String constrains = ReUtil.get("<h3>Constraints</h3>([\\s\\S]*?)</section>[\\s\\S]*?</div>",
                                        body, 1);
                        sb.append(constrains);
                        String input = sb.toString().replaceAll("<var>", "\\$").replaceAll("</var>", "\\$");
                        input = input.replaceAll("<pre>",
                                        "<pre style=\"padding:9px!important;background-color: #f5f5f5!important\">");

                        String rawOutput = ReUtil.get("<h3>Output</h3>([\\s\\S]*?)</section>[\\s\\S]*?</div>", body, 1);
                        String output = rawOutput.replaceAll("<var>", "\\$").replaceAll("</var>", "\\$");
                        output = output.replaceAll("<pre>",
                                        "<pre style=\"padding:9px!important;background-color: #f5f5f5!important\">");

                        List<String> sampleInput = ReUtil.findAll(
                                        "<h3>Sample Input \\d+</h3><pre>([\\s\\S]*?)</pre>[\\s\\S]*?</section>[\\s\\S]*?</div>",
                                        body, 1);
                        List<String> sampleOutput = ReUtil.findAll(
                                        "<h3>Sample Output \\d+</h3><pre>([\\s\\S]*?)</pre>[\\s\\S]*?</section>[\\s\\S]*?</div>",
                                        body, 1);

                        StringBuilder examples = new StringBuilder();

                        for (int i = 0; i < sampleInput.size() && i < sampleOutput.size(); i++) {
                                examples.append("<input>");
                                String exampleInput = sampleInput.get(i).trim();
                                examples.append(exampleInput).append("</input>");
                                examples.append("<output>");
                                String exampleOutput = sampleOutput.get(i).trim();
                                examples.append(exampleOutput).append("</output>");
                        }

                        problemDescription
                                        .setInput("<pp>" + HtmlUtil
                                                        .unescape(input.trim().replaceAll("(?<=\\>)\\s+(?=\\<)", "")))
                                        .setOutput("<pp>" + HtmlUtil
                                                        .unescape(output.trim().replaceAll("(?<=\\>)\\s+(?=\\<)", "")))
                                        .setDescription("<pp>" + HtmlUtil
                                                        .unescape(desc.trim().replaceAll("(?<=\\>)\\s+(?=\\<)", "")))
                                        .setExamples(examples.toString());

                } else {
                        org.jsoup.nodes.Element element = Jsoup.parse(body).getElementById("task-statement");
                        String desc = element.html();
                        desc = desc.replaceAll("src=\"/img", "src=\"https://atcoder.jp/img");
                        desc = desc.replaceAll("<pre>",
                                        "<pre style=\"padding:9px!important;background-color: #f5f5f5!important\">");
                        desc = desc.replaceAll("<var>", "\\$").replaceAll("</var>", "\\$");
                        desc = desc.replaceAll("<hr>", "");
                        problemDescription.setDescription(desc);
                }

                List<ProblemDescription> problemDescriptionList = Collections.singletonList(problemDescription);
                return new RemoteProblemInfo()
                                .setProblem(problem)
                                .setProblemDescriptionList(problemDescriptionList)
                                .setTagList(null)
                                .setLangIdList(null)
                                .setRemoteOJ(Constants.RemoteOJ.ATCODER);
        }
}