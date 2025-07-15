package com.ai.plug.common.constants;

import java.util.regex.Pattern;

/**
 * @author han
 * @time 2025/6/28 1:34
 */

public class MineTypeConstants {

    public static final String TEXT_MIME_TYPE = "text/*";
    public static final String TEXT_PLAIN_MIME_TYPE = "text/plain";



    public static final String IMAGE_MIME_TYPE = "image/*";
    public static final String IMAGE_JPEG_MIME_TYPE = "image/jpeg";
    public static final String IMAGE_PNG_MIME_TYPE = "image/png";
    public static final String IMAGE_GIF_MIME_TYPE = "image/gif";
    public static final String IMAGE_BMP_MIME_TYPE = "image/bmp";
    public static final String IMAGE_TIFF_MIME_TYPE = "image/tiff";
    public static final String IMAGE_WEBP_MIME_TYPE = "image/webp";
    public static final String IMAGE_SVG_MIME_TYPE = "image/svg+xml";



    public static final String AUDIO_MIME_TYPE = "audio/*";




    public static final String APPLICATION_MIME_TYPE = "application/*";
    public static final String ANY_MIME_TYPE = "*/*";
    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    // 正则匹配合法的 MIME 类型
    private static final Pattern MIME_TYPE_PATTERN = Pattern.compile(
            "^(?:[-\\w]+(?:\\.[-\\w]+)*)(?:/)(?:[-\\w]+(?:\\.[-\\w]+)*)(?:;\\s*(?:[-\\w]+)=(?:[-\\w]+))*?$"
    );
    /**
     * 判断是否是文本类型
     * @param mimeType
     * @return
     */
    public static boolean isTextMimeType(String mimeType) {
        return mimeType != null && mimeType.startsWith("text/") && isAnyMimeType(mimeType);
    }
    /**
     * 判断是否是图片类型
     * @param mimeType
     * @return
     */
    public static boolean isImageMimeType(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/") && isAnyMimeType(mimeType);
    }

    /**
     * 判断是否是音频类型
     * @param mimeType
     * @return
     */
    public static boolean isAudioMimeType(String mimeType) {
        return mimeType != null && mimeType.startsWith("audio/") && isAnyMimeType(mimeType);
    }



    /**
     * 判断给定字符串是否是一个合法的 MIME 类型
     *
     * @param mimeType 要检查的 MIME 类型字符串
     * @return 如果是合法的 MIME 类型返回 true，否则 false
     */
    public static boolean isAnyMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return false;
        }

        mimeType = mimeType.trim();

        // 排除通配符类型
        if (mimeType.equals("*/*") || mimeType.endsWith("/*") || mimeType.startsWith("*/")) {
            return false;
        }

        // 使用正则进行格式校验
        return MIME_TYPE_PATTERN.matcher(mimeType).matches();
    }
}
