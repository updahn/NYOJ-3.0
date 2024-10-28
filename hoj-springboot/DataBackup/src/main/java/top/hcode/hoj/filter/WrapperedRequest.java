package top.hcode.hoj.filter;

import org.apache.commons.lang.StringUtils;

import lombok.extern.slf4j.Slf4j;
import top.hcode.hoj.utils.AESUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * WrapperedRequest 用于包装 HttpServletRequest 并处理加密的请求体。
 * 解密请求体后，将解密后的内容替代原始请求体用于后续处理。
 */

@Slf4j(topic = "hoj")
public class WrapperedRequest extends HttpServletRequestWrapper {

    private String requestBody = ""; // 解密后的请求体
    private final HttpServletRequest req; // 原始请求
    private final Map<String, Object> parameterMap = new HashMap<>(); // 参数Map

    private static final String secretKey = "5A8F3C6B1D9E2F7A4B0C9D6E7F3B8A1C"; // 32字节密钥

    private static final ObjectMapper objectMapper = new ObjectMapper(); // JSON 解析器

    /**
     * 构造函数，解析并解密请求体和参数。
     *
     * @param request 原始的 HttpServletRequest
     * @throws IOException 读取请求体时发生异常
     */
    public WrapperedRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.req = request;

        // 读取并解密请求体
        String bodyString = getBodyString(request);
        if (bodyString != null && !bodyString.isEmpty()) {
            this.requestBody = decryptRequestBody(bodyString);
        }

        // 解析并解密请求参数
        parseAndDecryptParameters(request);

    }

    /**
     * 解密请求体，如果解密失败，返回原始请求体。
     *
     * @param bodyString 原始的请求体
     * @return 解密后的请求体
     */
    private String decryptRequestBody(String bodyString) {
        try {
            return AESUtils.decrypt(bodyString, secretKey);
        } catch (Exception e) {
            log.error("处理请求体时出错: {}", e.getMessage());
            return bodyString; // 如果解密出错，保留原始请求体
        }
    }

    /**
     * 解析请求参数并进行解密，存储到 parameterMap 中。
     *
     * @param request HttpServletRequest
     * @throws IOException 如果参数解析失败，抛出异常
     */
    private void parseAndDecryptParameters(HttpServletRequest request) throws IOException {
        Map<String, String[]> originalParameterMap = request.getParameterMap();

        // 清空原有的参数Map
        this.parameterMap.clear();

        for (Map.Entry<String, String[]> entry : originalParameterMap.entrySet()) {
            String[] values = entry.getValue();
            for (String value : values) {
                try {
                    // 解密每个参数值
                    String decryptedValue = AESUtils.decrypt(value, secretKey);

                    // 解析 JSON 字符串并添加到 parameterMap 中
                    Map<String, Object> parsedValue = objectMapper.readValue(decryptedValue, HashMap.class);

                    // 将 parsedValue 中的键值对添加到 parameterMap
                    this.parameterMap.putAll(parsedValue);
                } catch (Exception e) {
                    log.error("处理参数时出错: {}", e.getMessage());
                    throw new IOException("参数解密或解析失败", e); // 抛出异常以便上层处理
                }
            }
        }

    }

    /**
     * 读取请求体并返回字符串形式。
     *
     * @param request ServletRequest 请求对象
     * @return 请求体的字符串
     */
    public static String getBodyString(ServletRequest request) {
        StringBuilder sb = new StringBuilder();
        try (InputStream inputStream = request.getInputStream();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, Charset.forName("UTF-8")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /**
     * 返回一个 BufferedReader，用于读取解密后的请求体。
     *
     * @return BufferedReader 读取解密后的请求体
     * @throws IOException 如果读取失败，抛出异常
     */
    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new StringReader(this.requestBody));
    }

    /**
     * 返回 ServletInputStream，用于读取解密后的请求体。
     *
     * @return ServletInputStream 读取解密后的请求体
     * @throws IOException 如果读取失败，抛出异常
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream bais = new ByteArrayInputStream(this.requestBody.getBytes("UTF-8"));
        return new ServletInputStream() {
            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener listener) {
                // 未使用
            }

            @Override
            public int read() throws IOException {
                return bais.read();
            }
        };
    }

    /**
     * 获取指定参数名的值，如果没有则返回 null。
     * 接收一般变量，例如 text 类型。
     *
     * @param name 参数名
     * @return 参数值
     */
    @Override
    public String getParameter(String name) {
        Object result = parameterMap.get(name);
        if (result instanceof String) {
            return StringUtils.isBlank((String) result) ? null : (String) result;
        } else {
            return result == null ? null : result.toString(); // 转换为String返回
        }
    }

    /**
     * 获取指定参数名的所有值，返回数组形式。
     * 适用于接收数组类型变量，例如 checkbox 类型。
     *
     * @param name 参数名
     * @return 参数值数组
     */
    @Override
    public String[] getParameterValues(String name) {
        Object result = parameterMap.get(name);
        if (result instanceof String) {
            return ((String) result).split(",");
        } else if (result != null) {
            return new String[] { result.toString() }; // 如果是其他类型，转换为字符串数组
        }
        return null;
    }

    /**
     * 返回所有参数名的枚举。
     *
     * @return 参数名的枚举
     */
    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }

}
