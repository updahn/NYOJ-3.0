package top.hcode.hoj.crawler.problem;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HtmlUtil;
import lombok.extern.slf4j.Slf4j;
import top.hcode.hoj.crawler.language.VJLanguageStrategy;
import top.hcode.hoj.pojo.bo.Pair_;
import top.hcode.hoj.pojo.entity.problem.Problem;
import top.hcode.hoj.pojo.entity.problem.ProblemDescription;
import top.hcode.hoj.pojo.entity.problem.Tag;
import top.hcode.hoj.utils.CodeForcesUtils;
import top.hcode.hoj.utils.Constants;
import top.hcode.hoj.utils.JsoupUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.*;
import java.io.*;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j(topic = "hoj")
@Component
public class VJProblemStrategy extends ProblemStrategy {

	public static final String JUDGE_NAME = "VJ";
	public static final String HOST = "https://vjudge.net";
	public static final String CDN_HOST = "https://cdn.vjudge.net.cn";
	public static final String PROBLEM_API = "/problem/%s";
	private static final String DESCRIPTION_API = "/problem/description/%s";

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

	/**
	 * @param problemId String的原因是因为某些题库题号不是纯数字
	 * @param author    导入该题目的管理员用户名
	 * @return 返回Problem对象
	 * @throws Exception
	 */
	@Override
	public RemoteProblemInfo getProblemInfo(String problemId, String author) throws Exception {

		Problem info = new Problem();

		String oj = problemId.split("-")[0];

		String url = String.format(HOST + PROBLEM_API, problemId);

		Connection connection = JsoupUtils.getConnectionFromUrl(url, null, headers, false);
		Connection.Response response = connection.execute();

		if (response.statusCode() == 404) {
			throw new IllegalArgumentException("[VJ]: Incorrect problem id format!");
		}

		// 解析页面内容
		Document document = Jsoup.parse(response.body());

		Elements listItems = document.select("#prob-descs li");

		String title = document.selectFirst("h2") != null ? document.selectFirst("h2").text()
				: JUDGE_NAME + "_" + problemId;

		List<Pair_<String, String>> descriptionList = new ArrayList<>();

		// 遍历所有 <li> 元素并提取链接
		for (Element item : listItems) {

			String descriptionHref = "";
			String descriptionAuthor = "";

			Element link = item.selectFirst(".operation a");
			if (link != null) {
				String href = link.attr("href");
				if (href.contains("/problem/description/")) {
					descriptionHref = href.replace("/problem/description/", "");
				}
			}

			// 提取作者信息
			Element authorElement = item.selectFirst(".author");
			if (authorElement != null) {
				descriptionAuthor = authorElement.text().trim();
			}

			descriptionList.add(new Pair_<>(descriptionHref, descriptionAuthor));
		}

		List<Tag> tagList = new ArrayList<>();
		List<String> langIdList = new ArrayList<>();
		List<Pair_<String, String>> langList = new ArrayList<>();

		// 从隐藏的 textarea 中提取 JSON 数据
		Element textarea = document.selectFirst("textarea[name=dataJson]");

		if (textarea != null) {

			String jsonData = textarea.text();

			// 使用 Jackson 解析 JSON 数据
			ObjectMapper objectMapper = new ObjectMapper();
			Map<String, Object> map = objectMapper.readValue(jsonData, Map.class);

			// 获取并检查 "languages" 字段，并直接转换为 Map<String, String>
			languagesMap = (Map<String, String>) map.get("languages");

			if (languagesMap == null) {
				languagesMap = getLanguages(oj);
			}

			String realProblemId = String.valueOf((Integer) map.get("problemId"));
			problemId = JUDGE_NAME + "-" + realProblemId + "(" + problemId + ")";

			info.setProblemId(problemId);

			// 提取 Time limit, Mem limit 和 Tags 的内容
			List<Map<String, Object>> properties = (List<Map<String, Object>>) map.get("properties");
			String timeLimit = "";
			String memLimit = "";
			List<String> tagsList = new ArrayList<>();

			for (Map<String, Object> property : properties) {
				String key = (String) property.get("title");
				String content = (String) property.get("content");
				if ("Time limit".equalsIgnoreCase(key)) {
					timeLimit = content;
				} else if ("Mem limit".equalsIgnoreCase(key)) {
					memLimit = content;
				} else if ("Tags".equalsIgnoreCase(key)) {
					// 将Tags的内容按 `\r\n\n\r\n` 分割为 List<String>
					String[] tagsArray = content.split("\\r\\n\\n\\r\\n");
					for (String tag : tagsArray) {
						tagsList.add(tag.trim().toString());
					}
				}
			}

			info.setTimeLimit(extractNumber(timeLimit));
			info.setMemoryLimit(extractNumber(memLimit));

			for (String tag : tagsList) {
				tagList.add(new Tag().setName(tag));
			}

			if (languagesMap != null) {
				for (Map.Entry<String, String> entry : languagesMap.entrySet()) {
					String name = entry.getValue();
					String key = entry.getKey();
					langList.add(new Pair_<String, String>(key, name));
					langIdList.add(name);
				}
			}
		}

		info.setIsRemote(true)
				.setType(0)
				.setAuth(1)
				.setAuthor(author)
				.setOpenCaseResult(false)
				.setIsRemoveEndBlank(false)
				.setIsGroup(false)
				.setDifficulty(1); // 默认为中等

		List<ProblemDescription> problemDescriptionList = getProblemDescriptionList(descriptionList, problemId, title,
				url);

		return new RemoteProblemInfo()
				.setProblem(info)
				.setProblemDescriptionList(problemDescriptionList)
				.setTagList(tagList)
				.setLangIdList(langIdList)
				.setLangList(langList)
				.setRemoteOJ(Constants.RemoteOJ.VJ);

	}

	public static List<ProblemDescription> getProblemDescriptionList(List<Pair_<String, String>> descriptionList,
			String problemId, String title, String problemUrl) throws IOException {
		List<ProblemDescription> problemDescriptionList = new ArrayList<>();

		for (Pair_<String, String> description : descriptionList) {
			StringBuilder sb = new StringBuilder();
			sb.append("<pp>");

			String url = String.format(HOST + DESCRIPTION_API, description.getKey());

			Connection connection = JsoupUtils.getConnectionFromUrl(url, null, headers, false);
			Document document = JsoupUtils.getDocument(connection, null);
			String html = document.html();

			// 解析HTML并提取/problem/description/链接
			document = Jsoup.parse(html);

			// 从隐藏的 textarea 中提取 JSON 数据
			// 从隐藏的 textarea 中提取 JSON 数据
			Element textarea = document.selectFirst("textarea[class=data-json-container]");
			if (textarea != null) {
				String jsonData = unescapeUnicode(textarea.text());

				ObjectMapper objectMapper = new ObjectMapper();
				JsonNode rootNode = objectMapper.readTree(jsonData);

				// 解析 sections 数组
				JsonNode sectionsNode = rootNode.path("sections");
				if (sectionsNode.isArray()) {
					for (JsonNode section : sectionsNode) {
						// 获取 title
						String key = section.path("title").asText();

						if (!StringUtils.isEmpty(key)) {
							sb.append("<h2>");
							sb.append(key);
							sb.append("</h2>");
						}

						// 获取 value 对象中的 content 并做 LaTeX 替换
						JsonNode valueNode = section.path("value");
						String content = valueNode.path("content").asText();

						// 替换形如 $$$ n $$$ 为 $ n $
						String modifiedContent = content.replace("CDN_BASE_URL", CDN_HOST);

						// 判断是否为pdf文档
						String uri = HtmlUtil.unescape(ReUtil.get("<iframe src=\"([\\s\\S]*?)\"", modifiedContent, 1));

						if (uri != null) {
							StringBuilder pdf_description = new StringBuilder();
							String fileName = IdUtil.fastSimpleUUID() + ".pdf";
							String filePath = Constants.File.PROBLEM_FILE_FOLDER.getPath() + File.separator + fileName;

							CodeForcesUtils.downloadPDF(uri, filePath, false);
							String pdfURI = Constants.File.FILE_API.getPath() + fileName;

							String[] split = uri.split("/");
							String fileRealName = split[split.length - 1];
							pdf_description.append("<p><a href=\"")
									.append(pdfURI).append("\">")
									.append(fileRealName)
									.append("</a></p>");

							sb.append(pdf_description.toString().trim());
						} else {
							sb.append("<p>" + modifiedContent + "</p>");
						}
					}
				}
			}

			problemDescriptionList.add(new ProblemDescription()
					.setRank(problemDescriptionList.size())
					.setTitle(title)
					.setAuthor(description.getValue())
					.setSource(String.format("<a style='color:#1A5CC8' href='%s'>%s</a>", problemUrl, problemId))
					.setDescription(sb.toString()
							.replaceAll("(?<=\\>)\\s+(?=\\<)", "")
							.replaceAll("\\$\\$\\$?\\$?([^$]+)\\$\\$\\$?\\$?", "\\$$1\\$")));

		}

		return problemDescriptionList;
	}

	// 提取字符串中的数字
	private static Integer extractNumber(String content) {
		Pattern pattern = Pattern.compile("\\d+");
		Matcher matcher = pattern.matcher(content);
		if (matcher.find()) {
			return Integer.valueOf(matcher.group());
		}
		return null;
	}

	public static String unescapeUnicode(String input) {
		StringBuilder result = new StringBuilder();
		int i = 0;
		while (i < input.length()) {
			char ch = input.charAt(i);
			if (ch == '\\' && i + 5 < input.length() && input.charAt(i + 1) == 'u') {
				String hex = input.substring(i + 2, i + 6);
				try {
					int codePoint = Integer.parseInt(hex, 16);
					result.append((char) codePoint);
				} catch (NumberFormatException e) {
					result.append("\\u").append(hex);
				}
				i += 6;
			} else {
				result.append(ch);
				i++;
			}
		}
		return result.toString();
	}

	public static Map<String, String> getLanguages(String oj) {
		return VJLanguageStrategy.getLanguage(oj);
	}

}
