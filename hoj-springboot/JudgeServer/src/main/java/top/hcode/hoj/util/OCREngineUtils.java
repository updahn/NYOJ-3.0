package top.hcode.hoj.util;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtSession;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;

import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

import org.springframework.util.CollectionUtils;

import java.awt.*;

@Slf4j(topic = "hoj")
public class OCREngineUtils {

    /** 字符集，用于OCR识别结果的字符映射 */
    private static JSONArray charsetArray;

    /** ONNX模型文件，用于OCR识别 */
    private static File modelFile;

    // 静态块，初始化模型和字符集
    static {
        try {
            // 通过 ResourceUtil 读取 common_old_charset.json 文件的内容
            String fileContent = ResourceUtil.readUtf8Str("common_old_charset.json");

            // 使用 Hutool 的 JSONUtil 解析文件内容为 JSON 数组
            charsetArray = JSONUtil.parseArray(fileContent);

            // 获取系统临时文件夹路径并创建d4ocr目录存储模型文件
            String javaTmpDir = System.getProperty("java.io.tmpdir", ".");
            File appTmpDir = new File(javaTmpDir, "d4ocr");
            appTmpDir.mkdirs();

            // 提取ONNX模型文件
            modelFile = new File(appTmpDir, "common_old.onnx");
            extractJarResource("common_old.onnx", modelFile);
        } catch (Exception e) {
            log.error("Error occurred while loading the OCR model configuration", e); // 加载模型时的错误处理
        }
    }

    /**
     * 识别图像中的文本
     *
     * @param image 输入的图像
     * @return 识别结果
     */
    public static String recognize(BufferedImage image) {
        if (image == null) {
            log.error("OCR input image cannot be null"); // 图像不能为空
            return null;
        }
        if (modelFile == null || !modelFile.exists() || charsetArray == null) {
            log.error("OCR model or charset is missing"); // 模型或字符集缺失
            return null;
        }

        // 预处理图像：调整尺寸和灰度化
        image = resize(image, 64 * image.getWidth() / image.getHeight(), 64);
        image = toGray(image);

        // 图像转换为ONNX模型需要的输入格式
        long[] shape = { 1, 1, image.getHeight(), image.getWidth() };
        float[] data = new float[(int) (shape[0] * shape[1] * shape[2] * shape[3])];
        image.getData().getPixels(0, 0, image.getWidth(), image.getHeight(), data);
        for (int i = 0; i < data.length; i++) {
            data[i] /= 255;
            data[i] = (float) ((data[i] - 0.5) / 0.5);
        }
        try (
                ONNXRuntimeUtils onnx = new ONNXRuntimeUtils();
                OnnxTensor inputTensor = onnx.createTensor(data, shape);
                OrtSession model = onnx.createSession(modelFile.getAbsolutePath());
                OrtSession.Result result = model.run(MapOf.of("input1", inputTensor))) {

            // 获取模型返回的识别结果
            OnnxTensor indexTensor = (OnnxTensor) result.get(0);
            long[][] index = (long[][]) indexTensor.getValue();

            // 将识别结果索引转换为字符
            StringBuilder words = new StringBuilder();
            for (long i : index[0]) {
                words.append(charsetArray.getStr((int) i));
            }
            return words.toString();
        } catch (Exception e) {
            log.error("Error during OCR recognition", e); // 识别过程中的错误处理
            return null;
        }
    }

    /**
     * 将图像转换为灰度图像
     *
     * @param image 原始图像
     * @return 灰度图像
     */
    public static BufferedImage toGray(BufferedImage image) {
        BufferedImage target = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = target.createGraphics();
        g2d.drawImage(image, 0, 0, null); // 绘制灰度图像
        g2d.dispose();
        return target;
    }

    /**
     * 调整图像大小
     *
     * @param image  原始图像
     * @param width  新宽度
     * @param height 新高度
     * @return 调整后的图像
     */
    public static BufferedImage resize(BufferedImage image, int width, int height) {
        BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Image tmp = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        Graphics2D g2d = newImage.createGraphics();
        g2d.drawImage(tmp, 0, 0, null); // 绘制调整大小后的图像
        g2d.dispose();
        return newImage;
    }

    /**
     * 从JAR包中提取资源文件
     *
     * @param source JAR包内资源路径
     * @param target 目标文件路径
     * @throws IOException
     */
    public static void extractJarResource(String source, File target) throws IOException {
        try (InputStream in = ResourceUtil.getStream(source);
                FileOutputStream out = new FileOutputStream(target)) {

            byte[] buffer = new byte[2048];
            int len;

            // 将资源文件的内容写入目标文件
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        }
    }

    /**
     * 从URL获取图像
     *
     * @param imageUrl 图像网址
     * @param cookies  请求的Cookies
     * @return 图像对象
     * @throws IOException
     */
    public static BufferedImage imgFromUrl(String imageUrl, java.util.List<HttpCookie> cookies) {
        // 清除当前线程的cookies缓存
        HttpRequest.getCookieManager().getCookieStore().removeAll();

        Map<String, String> headers = MapUtil
                .builder(new HashMap<String, String>())
                .put("Accept", "*/*")
                .put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .put("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/127.0.0.0 Safari/537.36")
                .put("X-Requested-With", "XMLHttpRequest")
                .map();

        BufferedImage image = null;

        try {
            // 使用 Hutool 构建请求
            HttpRequest request = HttpRequest.get(imageUrl)
                    .addHeaders(headers)
                    .timeout(5000);

            // 如果 cookies 不为空，添加 cookies 到请求中
            if (!CollectionUtils.isEmpty(cookies)) {
                request.cookie(cookies);
            }

            // 执行请求并获取响应
            HttpResponse response = request.execute();

            // 检查状态码是否为 200（OK）
            if (response.getStatus() == 200) {
                try (InputStream inputStream = new ByteArrayInputStream(response.bodyBytes())) {
                    // 使用 ImageIO 将响应转换为 BufferedImage
                    image = ImageIO.read(inputStream);
                }
            } else {
                log.warn("[OCR] Error loading image from URL: {} Status code: {}", imageUrl, response.getStatus());
            }
        } catch (IORuntimeException | cn.hutool.http.HttpException e) {
            log.warn("[OCR] Error loading image from URL: {} Exception: Connect timed out",
                    imageUrl); // 错误处理
        } catch (Exception e) {
            log.warn("[OCR] Error loading image from URL: {} Error: {}", imageUrl, e);
        }
        return image;
    }

    /**
     * 从本地文件读取图像
     *
     * @param imageFile 图像文件路径
     * @return 图像对象
     * @throws IOException
     */
    public static BufferedImage imgFromFile(String imageFile) throws IOException {
        return ImageIO.read(new File(imageFile));
    }

    // 测试方法1：从本地文件读取图像并进行OCR识别
    public void test() throws IOException {
        BufferedImage image = imgFromFile("E:/Project/ddddocr/capta/26.jpg");

        String predict = recognize(image);
        System.out.println(predict);
    }

    // 测试方法2：从URL获取图像并进行OCR识别
    public void test2(String url) throws IOException {
        BufferedImage image = imgFromUrl(url, null);

        String predict = recognize(image);
        System.out.println(predict);
    }

    // public static void main(String[] args) throws IOException {
    // String url = "https://vjudge.net/util/captcha";

    // OCREngineUtils ocrEngineUtils = new OCREngineUtils();
    // ocrEngineUtils.test2(url);
    // }
}
