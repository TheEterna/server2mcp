package com.ai.plug.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * @author han
 * @time 2025/6/28 1:13
 */

public class ConvertImageUtils {

    private final static Logger log = LoggerFactory.getLogger(ConvertImageUtils.class);
    /**
     * 将Image转换为Base64编码的字符串
     * @param image 输入的Image对象
     * @param format 图像格式，如"png"、"jpg"等
     * @return 转换后的Base64编码字符串
     * @throws InterruptedException
     */
    public static String imageToBase64ByClass(Image image, String format) throws InterruptedException {
        if (image == null) {
            return null;
        }

        RenderedImage renderedImage;
        if (image instanceof RenderedImage) {
            renderedImage = (RenderedImage) image;
        } else {
            renderedImage = toBufferedImage(image);
        }

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ImageIO.write(renderedImage, format, os);
            return Base64.getEncoder().encodeToString(os.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("图像转换失败", e);
        }
    }

    /**
     * 将Image转换为BufferedImage
     * @param image 输入的Image对象
     * @return 转换后的BufferedImage对象
     * @throws InterruptedException 线程中断异常
     */
    private static BufferedImage toBufferedImage(Image image) throws InterruptedException {
        if (image.getWidth(null) == -1 || image.getHeight(null) == -1) {
            MediaTracker tracker = new MediaTracker(new Panel());
            tracker.addImage(image, 0);
            tracker.waitForID(0);
        }

        int width = image.getWidth(null);
        int height = image.getHeight(null);

        BufferedImage bufferedImage = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g2d = bufferedImage.createGraphics();
        try {
            g2d.drawImage(image, 0, 0, null);
        } finally {
            g2d.dispose();
        }

        return bufferedImage;
    }





    /**
     * 将图片文件转换为 Base64 编码字符串，并指定输出格式（如 "png", "jpg"）。
     *
     * @param imageSource 图片来源
     * @param formatName  输出格式（null 表示自动探测）
     * @return Base64 编码的图片字符串
     * @throws IOException 如果读取失败或图片不存在
     */
    public static String imageToBase64(Object imageSource, String formatName) throws IOException {
        if (imageSource == null) {
            throw new IllegalArgumentException("图片源不能为空");
        }

        byte[] imageBytes = readImageBytes(imageSource, formatName);
        return Base64.getEncoder().encodeToString(imageBytes);
    }


    /**
     * 从各种类型的图片源中读取字节数据
     */
    public static byte[] readImageBytes(Object source, String formatName) throws IOException {
        BufferedImage bufferedImage = readBufferedImage(source);

        // 如果未指定格式，尝试从原始图像流中获取格式名
        if (formatName == null && source instanceof File file) {
            formatName = getFormatNameFromFileName(file.getName());
        } else if (formatName == null && source instanceof Path path) {
            formatName = getFormatNameFromFileName(path.getFileName().toString());
        }

        // 默认格式为 PNG
        if (formatName == null) {
            formatName = "png";
        }

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            boolean success = ImageIO.write(bufferedImage, formatName, os);
            if (!success) {
                throw new IOException("不支持的图片格式: " + formatName);
            }
            return os.toByteArray();
        }
    }

    /**
     * 从各种来源加载 BufferedImage
     */
    private static BufferedImage readBufferedImage(Object source) throws IOException {
        if (source instanceof File file && file.exists()) {
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new IOException("无法识别的图片文件: " + file);
            }
            return image;
        } else if (source instanceof Path path && Files.exists(path)) {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                throw new IOException("无法识别的图片文件: " + path);
            }
            return image;
        } else if (source instanceof String str) {
            File file = new File(str);
            if (file.exists()) {
                BufferedImage image = ImageIO.read(file);
                if (image == null) {
                    throw new IOException("无法识别的图片文件: " + str);
                }
                return image;
            }
            try {
                return ImageIO.read(new URL(str));
            } catch (Exception e) {
                throw new IOException("无法识别的图片路径或 URL: " + str, e);
            }
        } else if (source instanceof URL url) {
            BufferedImage image = ImageIO.read(url);
            if (image == null) {
                throw new IOException("无法识别的图片 URL: " + url);
            }
            return image;
        } else if (source instanceof InputStream is) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new IOException("无法识别的图片输入流");
            }
            return image;
        } else if (source instanceof byte[] bytes) {
            try (InputStream is = new ByteArrayInputStream(bytes)) {
                BufferedImage image = ImageIO.read(is);
                if (image == null) {
                    throw new IOException("无法识别的图片字节数组");
                }
                return image;
            }
        } else {
            throw new IllegalArgumentException("不支持的图片源类型: " + source.getClass());
        }
    }

    /**
     * 根据文件名猜测格式名（如 .png -> png）
     */
    private static String getFormatNameFromFileName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return null;
    }

    /**
     * 将 MIME 类型转换为 ImageIO 支持的 formatName
     */
    public static String mapMimeTypeToFormatName(String mimeType) {
        if (mimeType == null) return null;

        return switch (mimeType.toLowerCase()) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "image/bmp" -> "bmp";
            case "image/tiff", "image/x-tiff" -> "tiff";
            default -> null; // 不支持的类型返回 null
        };
    }

}
