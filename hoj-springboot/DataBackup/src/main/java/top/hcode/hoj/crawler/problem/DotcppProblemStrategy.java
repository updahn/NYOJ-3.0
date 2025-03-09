package top.hcode.hoj.crawler.problem;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReUtil;
import lombok.extern.slf4j.Slf4j;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.problem.ProblemDescription;
import top.hcode.hoj.pojo.entity.problem.Tag;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.utils.CookiesUtils;
import top.hcode.hoj.utils.JsoupUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.HttpCookie;
import java.util.function.Consumer;
import java.util.Optional;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

@Slf4j(topic = "hoj")
public class DotcppProblemStrategy extends ProblemStrategy {

	public static final String JUDGE_NAME = "DOTCPP";
	public static final String HOST = "https://www.dotcpp.com";
	public static final String PROBLEM_API = "/oj/problem%s.html";

	public static Map<String, String> headers = MapUtil
			.builder(new HashMap<String, String>())
			.put("Accept", "*/*")
			.put("Connection", "keep-alive")
			.put("X-Requested-With", "XMLHttpRequest")
			.put("Content-Type",
					"application/x-www-form-urlencoded;application/json;charset=UTF-8")
			.put("User-Agent",
					"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36 Edg/129.0.0.0")
			.map();

	public static Map<String, String> languagesMap = new HashMap<>();

	public List<HttpCookie> cookies;

	@Override
	public RemoteProblemInfo getProblemInfoByCookie(String problemId, String author, List<HttpCookie> cookies)
			throws Exception {
		this.cookies = cookies;
		return getProblemInfo(problemId, author);
	}

	@Override
	public RemoteProblemInfo getProblemInfo(String problemId, String author) throws Exception {

		Problem info = new Problem();

		if (problemId.contains("contest")) {
			throw new IllegalArgumentException("[Dotcpp]: Do not support the contest!");
		}

		String url = String.format(HOST + PROBLEM_API, problemId);

		Connection connection = JsoupUtils.getConnectionFromUrl(url, null, headers, false);
		connection.cookies(CookiesUtils.convertHttpCookieListToMap(cookies));
		Connection.Response response = connection.execute();

		if (response.statusCode() == 404) {
			throw new IllegalArgumentException("[Dotcpp]: Incorrect problem id format!");
		}

		ProblemDescription problemDescription = new ProblemDescription().setPid(info.getId());

		// 解析页面内容
		Document document = Jsoup.parse(response.body());

		String html = document.html();

		problemId = JUDGE_NAME + "-" + problemId.replace("contest", "").replace("problem", "");

		info.setProblemId(problemId)
				.setTimeLimit(ReUtil.get("<span class=.*?>时间限制:\\s*(\\d*)\\s*s", html, 1) != null
						? Integer.parseInt(ReUtil.get("<span class=.*?>时间限制:\\s*(\\d*)\\s*s", html, 1)) * 1000
						: 0)
				.setMemoryLimit(ReUtil.get("<span class=.*?>内存限制:\\s*(\\d*)\\s*MB", html, 1) != null
						? Integer.parseInt(ReUtil.get("<span class=.*?>内存限制:\\s*(\\d*)\\s*MB", html, 1))
						: 0)
				.setIsRemote(true)
				.setType(0)
				.setAuth(1)
				.setAuthor(author)
				.setOpenCaseResult(false)
				.setIsRemoveEndBlank(false)
				.setIsGroup(false)
				.setDifficulty(1); // 默认为简单

		Element detailItem = document.selectFirst(".col-lg-9");

		String title = detailItem.selectFirst(".head_box_text_w") != null
				? detailItem.selectFirst(".head_box_text_w").text().split(":")[1].trim()
				: problemId;

		problemDescription.setTitle(title);

		// 提取样例数据并格式化
		List<String> sampleData = ReUtil.findAll("<pre class=\"sampledata\">([\\s\\S]*?)</pre>", html, 1);
		String input = sampleData.isEmpty() ? "" : sampleData.get(0);
		String output = sampleData.size() >= 2 ? sampleData.get(1) : "";

		problemDescription.setExamples("<input>" + input + "</input><output>" + output + "</output>");

		Elements listItems = detailItem.select(".panel_prob");

		// 使用Map映射替代switch语句
		Map<String, Consumer<String>> sectionHandlers = new HashMap<>();
		sectionHandlers.put("题目描述", problemDescription::setDescription);
		sectionHandlers.put("描述", problemDescription::setDescription);
		sectionHandlers.put("输入格式", problemDescription::setInput);
		sectionHandlers.put("输入", problemDescription::setInput);
		sectionHandlers.put("输出格式", problemDescription::setOutput);
		sectionHandlers.put("输出", problemDescription::setOutput);
		sectionHandlers.put("提示", problemDescription::setHint);

		for (Element item : listItems) {
			// 提取标题和内容
			String head = Optional.ofNullable(item.selectFirst(".panel_prob_head"))
					.map(element -> element.text().trim())
					.orElse("未知");

			String contentHtml = Optional.ofNullable(item.selectFirst(".panel_prob_body"))
					.map(element -> "<pp>" + element.html().replaceAll("src=\"[../]*", "src=\"" + HOST + "/")
							.replaceAll("(?<=\\>)\\s+(?=\\<)", ""))
					.orElse("");

			// 使用映射处理不同类型的内容
			Consumer<String> handler = sectionHandlers.get(head);
			if (handler != null) {
				handler.accept(contentHtml);
			}
		}

		problemDescription
				.setSource(String.format("<a style='color:#1A5CC8' href='%s'>%s</a>", url, problemId, problemId));

		List<Tag> tagList = new ArrayList<>();

		// 提取带有strong标签的按钮文本作为标签
		Elements strongElements = detailItem.select("button.btn strong");
		for (Element strongElement : strongElements) {
			String tag = strongElement.text().trim();
			if (!tag.isEmpty()) {
				tagList.add(new Tag().setName(tag));
			}
		}

		List<ProblemDescription> problemDescriptionList = Collections.singletonList(problemDescription);

		return new RemoteProblemInfo()
				.setProblem(info)
				.setProblemDescriptionList(problemDescriptionList)
				.setTagList(tagList)
				.setRemoteOJ(Constants.RemoteOJ.DOTCPP);

	}

}
