package top.hcode.hoj.utils;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;

import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

@Slf4j(topic = "hoj")
public class SlideMatcherUtils {

    public static class MatchResult {
        public int targetX, targetY;
        public Rect matchRect;
        public double moveDistance;
        Point matchLoc;
        Mat background;
        Mat targetGray;

        public MatchResult(int targetX, int targetY, Rect matchRect, double moveDistance, Point matchLoc,
                Mat background, Mat targetGray) {
            this.targetX = targetX;
            this.targetY = targetY;
            this.matchRect = matchRect;
            this.moveDistance = moveDistance;
            this.matchLoc = matchLoc;
            this.background = background;
            this.targetGray = targetGray;
        }
    }

    public static class TargetInfo {
        public BufferedImage croppedImage;
        public int targetX, targetY;

        public TargetInfo(BufferedImage croppedImage, int targetX, int targetY) {
            this.croppedImage = croppedImage;
            this.targetX = targetX;
            this.targetY = targetY;
        }
    }

    public static MatchResult slideMatch(byte[] targetBytes, byte[] backgroundBytes, boolean simpleTarget, boolean flag)
            throws IOException {
        Mat target;
        int targetX = 0, targetY = 0;

        if (!simpleTarget) {
            try {
                TargetInfo targetInfo = getTarget(targetBytes);
                target = opencv_imgcodecs.imdecode(new Mat(targetBytes), opencv_imgcodecs.IMREAD_ANYCOLOR);
                targetX = targetInfo.targetX;
                targetY = targetInfo.targetY;
            } catch (Exception e) {
                if (flag) {
                    throw e;
                }
                return slideMatch(targetBytes, backgroundBytes, true, true);
            }
        } else {
            target = opencv_imgcodecs.imdecode(new Mat(targetBytes), opencv_imgcodecs.IMREAD_ANYCOLOR);
        }

        Mat background = opencv_imgcodecs.imdecode(new Mat(backgroundBytes), opencv_imgcodecs.IMREAD_ANYCOLOR);

        // 模板匹配
        Mat result = new Mat();
        opencv_imgproc.matchTemplate(background, target, result, opencv_imgproc.TM_CCOEFF_NORMED);
        DoublePointer minVal = new DoublePointer();
        DoublePointer maxVal = new DoublePointer();
        Point minLoc = new Point();
        Point maxLoc = new Point();
        opencv_core.minMaxLoc(result, minVal, maxVal, minLoc, maxLoc, null);
        Point matchLoc = maxLoc;

        int h = target.rows();
        int w = target.cols();
        Rect matchRect = new Rect(new Point((int) matchLoc.x(), (int) matchLoc.y()),
                new Point((int) matchLoc.x() + w, (int) matchLoc.y() + h));

        // 计算滑块需要移动的距离，并减去 移动按钮的宽度一半
        double moveDistance = matchLoc.x() - 30;

        return new MatchResult(targetX, targetY, matchRect, moveDistance, matchLoc, background, target);
    }

    public static TargetInfo getTarget(byte[] imgBytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imgBytes));
        int w = image.getWidth();
        int h = image.getHeight();

        int starttx = 0, startty = 0, end_x = 0, end_y = 0;

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int pixel = image.getRGB(x, y);
                int alpha = (pixel >> 24) & 0xff; // 获取 alpha 值（透明度）

                if (alpha == 0) {
                    if (startty != 0 && end_y == 0) {
                        end_y = y;
                    }
                    if (starttx != 0 && end_x == 0) {
                        end_x = x;
                    }
                } else {
                    if (startty == 0) {
                        startty = y;
                        end_y = 0;
                    } else if (y < startty) {
                        startty = y;
                        end_y = 0;
                    }
                }
            }
            if (starttx == 0 && startty != 0) {
                starttx = x;
            }
            if (end_y != 0) {
                end_x = x;
            }
        }

        BufferedImage croppedImage = image.getSubimage(starttx, startty, end_x - starttx, end_y - startty);
        return new TargetInfo(croppedImage, starttx, startty);
    }

    /**
     * 裁剪图片提取关键区域
     *
     * @param image 需要调整的图片
     * @param param 调整亮度的参数
     * @throws IOException
     */
    public static byte[][] cropImage(byte[] smallImageBytes, byte[] bigImageBytes) {
        int y = 0;
        int h_ = 0;
        byte[][] result = new byte[2][];

        try {
            // 将byte[]转为BufferedImage
            BufferedImage bigImage = ImageIO.read(new ByteArrayInputStream(bigImageBytes));
            BufferedImage smallImage = ImageIO.read(new ByteArrayInputStream(smallImageBytes));

            // 裁剪出滑块部分的起点和高度
            for (int h = 1; h < smallImage.getHeight(); h++) {
                for (int w = 1; w < smallImage.getWidth(); w++) {
                    int rgb = smallImage.getRGB(w, h);
                    int A = (rgb & 0xFF000000) >>> 24;
                    if (A > 0) {
                        if (y == 0)
                            y = h;
                        h_ = h - y;
                        break;
                    }
                }
            }

            // 使用subimage裁剪图像
            BufferedImage croppedSmallImage = smallImage.getSubimage(0, y, smallImage.getWidth(), h_);
            BufferedImage croppedBigImage = bigImage.getSubimage(0, y, bigImage.getWidth(), h_);

            // 将裁剪后的BufferedImage转换为byte[]
            ByteArrayOutputStream bigImageOutputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream smallImageOutputStream = new ByteArrayOutputStream();
            ImageIO.write(croppedBigImage, "png", bigImageOutputStream);
            ImageIO.write(croppedSmallImage, "png", smallImageOutputStream);

            // 将结果保存到byte数组中
            result[0] = smallImageOutputStream.toByteArray();
            result[1] = bigImageOutputStream.toByteArray();

        } catch (IOException e) {
        }

        return result;
    }

    /**
     * 获取滑块验证的移动距离
     *
     * @param targetBytes     滑块的图像
     * @param backgroundBytes 背景的图像
     * @return 识别结果
     */
    public static Double recognize(byte[] targetBytes, byte[] backgroundBytes, int i) {

        try {
            byte[][] resize_img = cropImage(targetBytes, backgroundBytes);

            targetBytes = resize_img[0];
            backgroundBytes = resize_img[1];

            // 调用 slideMatch 方法
            SlideMatcherUtils.MatchResult result = slideMatch(targetBytes, backgroundBytes, false, true);

            // 在背景图上标记出匹配的位置
            opencv_imgproc.rectangle(result.background, result.matchLoc,
                    new Point(result.matchLoc.x() + result.targetGray.cols(),
                            result.matchLoc.y() + result.targetGray.rows()),
                    new Scalar(0, 0, 255, 0));

            // String bgimg = "E:\\NYOJ\\NYOJ3.0\\yidun_bgimg" + i + ".jpg";

            // // 画出边框
            // opencv_imgcodecs.imwrite(bgimg, result.background);

            // 匹配结果
            return result.moveDistance;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从本地文件读取图像
     *
     * @param imageFile 图像文件路径
     * @return 图像对象
     * @throws IOException
     */
    // 辅助方法：将文件读取为字节数组
    private static byte[] imgFromFile(String filePath) throws IOException {
        File file = new File(filePath);
        return Files.readAllBytes(file.toPath());
    }

    /**
     * 从URL获取图像
     *
     * @param imageUrl 图像网址
     * @param cookies  请求的Cookies
     * @return 图像对象
     * @throws IOException
     */
    public static byte[] imgFromUrl(String imageUrl) {
        try {
            // 下载图片并返回字节数组
            try (InputStream in = new URL(imageUrl).openStream();
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

                byte[] dataBuffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(dataBuffer)) != -1) {
                    byteArrayOutputStream.write(dataBuffer, 0, bytesRead);
                }
                return byteArrayOutputStream.toByteArray(); // 返回图片的字节数组
            }
        } catch (Exception e) {
            log.warn("[SlideMatcher] Error loading image from URL: {} Error: {}", imageUrl, e);
            return null;
        }
    }

    // 测试方法1：从本地文件读取图像并进行滑块验证
    public void test() throws IOException {
        for (int i = 1; i <= 6; i++) {
            String path = "E:\\NYOJ\\NYOJ3.0";
            String suffix = i + ".jpg";
            String jig = path + "\\yidun_jigsaw" + suffix;
            String bg = path + "\\yidun_bgimg" + suffix;
            byte[] jigsaw = imgFromFile(jig);
            byte[] bgimg = imgFromFile(bg);

            Double predict = recognize(jigsaw, bgimg, i);

            System.out.println(predict);
        }
    }

    // 测试方法2：从URL获取图像并进行滑块验证
    public void test2(String url1, String url2) throws IOException {
        byte[] jigsaw = imgFromUrl(url1);
        byte[] bgimg = imgFromUrl(url2);

        Double predict = recognize(jigsaw, bgimg, 0);
        System.out.println(predict);
    }

}
