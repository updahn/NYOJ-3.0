package top.hcode.hoj.crawler.cookie;

import org.springframework.util.CollectionUtils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;

import java.net.HttpCookie;
import java.util.List;

public abstract class CookieStrategy {

	public abstract List<HttpCookie> getCookiesByLogin(List<HttpCookie> cookies, String username, String password)
			throws Exception;

	public abstract Boolean checkLogin(List<HttpCookie> cookies, String username) throws Exception;

	public String getHtml(List<HttpCookie> cookies, String url) {
		// 清除当前线程的cookies缓存
		HttpRequest.getCookieManager().getCookieStore().removeAll();

		// 构造 HttpRequest
		HttpRequest request = HttpRequest.get(url)
				.header("Accept", "text/html, application/xhtml+xml, */*")
				.header("User-Agent",
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");

		// 设置 cookies
		if (!CollectionUtils.isEmpty(cookies)) {
			request.cookie(cookies);
		}

		// 发送请求并获取响应
		HttpResponse response = request.execute();

		// 获取 HTML 内容
		String html = response.body();

		return html;
	}

}
