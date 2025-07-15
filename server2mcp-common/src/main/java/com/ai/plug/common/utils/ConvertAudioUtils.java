package com.ai.plug.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class ConvertAudioUtils {

    private static final Logger log = LoggerFactory.getLogger(ConvertAudioUtils.class);

    /**
     * 将音频文件转换为 Base64 编码字符串。
     *
     * @param audioSource 音频来源（可以是 File、Path、URL 或资源路径）
     * @return Base64 编码的音频字符串
     * @throws IOException 如果读取失败或音频不存在
     */
    public static String audioToBase64(Object audioSource) throws IOException {
        if (audioSource == null) {
            throw new IllegalArgumentException("音频源不能为空");
        }

        byte[] audioBytes = readAudioBytes(audioSource);
        return Base64.getEncoder().encodeToString(audioBytes);
    }


    /**
     * 从各种类型的音频源中读取字节数据
     */
    public static byte[] readAudioBytes(Object source) throws IOException {
        if (source instanceof File file && file.exists()) {
            return Files.readAllBytes(file.toPath());
        } else if (source instanceof Path path && Files.exists(path)) {
            return Files.readAllBytes(path);
        } else if (source instanceof String str) {
            // 尝试作为文件路径
            File file = new File(str);
            if (file.exists()) {
                return Files.readAllBytes(file.toPath());
            }
            // 尝试作为 URL
            try {
                return readFromURL(new URL(str));
            } catch (Exception ignored) {
                throw new IOException("无法识别的音频路径或 URL: " + str);
            }
        } else if (source instanceof URL url) {
            return readFromURL(url);
        } else if (source instanceof InputStream is) {
            return readStreamFully(is);
        } else if (source instanceof byte[]) {
            return (byte[]) source;
        } else {
            throw new IllegalArgumentException("不支持的音频源类型: " + source.getClass());
        }
    }

    private static byte[] readFromURL(URL url) throws IOException {
        try (InputStream is = url.openStream()) {
            return readStreamFully(is);
        }
    }

    private static byte[] readStreamFully(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}