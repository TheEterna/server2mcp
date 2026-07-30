package com.ai.plug.core.spec.callback.tool;

import com.ai.plug.common.constants.MineTypeConstants;
import com.ai.plug.common.utils.ConvertAudioUtils;
import com.ai.plug.common.utils.ConvertImageUtils;
import com.ai.plug.common.utils.JsonParser;
import com.ai.plug.core.utils.GenSchemaUtils.*;
import tools.jackson.core.JacksonException;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.springframework.ai.model.*;
import reactor.core.publisher.*;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;

import static com.ai.plug.common.constants.MineTypeConstants.*;

/**
 * Default implementation of McpCallToolResultConverter
 * @author han
 * @time 2025/6/27 14:05
 */

public class DefaultMcpCallToolResultConverter implements McpCallToolResultConverter {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DefaultMcpCallToolResultConverter.class);
    @Override
    public McpSchema.CallToolResult convertToCallToolResult(Object result, Type returnType, AbstractMcpToolMethodCallback callback) {
        // 首先, 无论返回的类型是什么, 即使是String 对应的 mineType 应该是text/* 但一切都要以方法中提供的mineType为准, 如果为空, 再以返回类型为准
        // 过滤
        // fixme: There are some doubts here, in fact, Springai's tool module does not support responsive programming
        // 这里有些疑问，实际上 springai的 tool模块是不支持响应式编程的

        // 兼容 响应式
        // Compatible Responsive
        if (result instanceof Mono<?>) {
            // If the result is already a Mono, map it to a GetPromptResult
            result = ((Mono<?>) result).block();
        } else if (result instanceof Flux<?>) {
            // If the result is already a Flux, map it to a GetPromptResult
            result = ((Flux<?>) result).collectList().block();
        }
        // 返回类就是最后结果
        if (result instanceof McpSchema.CallToolResult callToolResult) {
            return callToolResult;
        }
        else if (result instanceof McpSchema.Content content) {
            return McpSchema.CallToolResult.builder()
                    .addContent(content)
                    .isError(false)
                    .structuredContent(result)
                    .meta(null)
                    .build();
        }
        else if (result instanceof List<?> && !((List<?>) result).isEmpty() && ((List<?>) result).get(0) instanceof McpSchema.Content) {

            return McpSchema.CallToolResult.builder()
                    .content((List<McpSchema.Content>) result)
                    .isError(false)
                    .structuredContent(result)
                    .meta(null)
                    .build();
        }

        String mineType = callback.mineType;
        if (mineType == null || mineType.isBlank()) {
            // 没有mineType, 就完全按照返回类型来转换
            mineType = defaultConvertToCallToolResult(result, returnType);
        }
        if (mineType == null) {
            return McpSchema.CallToolResult.builder().addTextContent("Done").isError(false).build();
        } else if (isJsonMimeType(mineType) || isTextMimeType(mineType)) {
            String json = null;
            try {
                json = JsonParser.toJson(result);
            } catch (JacksonException e) {
                return McpSchema.CallToolResult.builder().addTextContent("find a incorrect text mineType of a Annotation from " + callback.method.getName()).isError(true).build();
            }
            // todo 感觉不太合理 因为文本不只有纯文本, 还有其他的文本类型, 比如markdown, html, xml, json等等
            return McpSchema.CallToolResult.builder().addTextContent(json).isError(false).build();
        } else if (isImageMimeType(mineType)) {
            // convert 图片为base64
            try {
                if (result instanceof Image image) {
                    String imgBase64 = ConvertImageUtils.imageToBase64ByClass(image, ConvertImageUtils.mapMimeTypeToFormatName(mineType));

                    return McpSchema.CallToolResult.builder()
                            .addContent(new McpSchema.ImageContent(new McpSchema.Annotations(List.of(McpSchema.Role.ASSISTANT), null), imgBase64, mineType))
                            .isError(false)
                            .structuredContent(result)
                            .meta(null)
                            .build();
                }
                else if (result instanceof byte[] || result instanceof InputStream || result instanceof File || result instanceof String
                        || result instanceof Path) {
                    //todo 该不该打破协议, 协议描述图片,音频的data部分都是base64
                    String base64 = ConvertImageUtils.imageToBase64(result, ConvertImageUtils.mapMimeTypeToFormatName(mineType));
                    return McpSchema.CallToolResult.builder()
                            .addContent(new McpSchema.ImageContent(new McpSchema.Annotations(List.of(McpSchema.Role.ASSISTANT), null), base64, mineType))
                            .isError(false)
                            .structuredContent(result)
                            .meta(null)
                            .build();
                } else {
                    //todo 该不该打破协议, 协议讲图片,音频的data部分都是base64
                    return McpSchema.CallToolResult.builder().addTextContent("sorry, 目前mineType为image类型只支持 byte[], Image, InputStream, File, Path(会当作本地路径解析), Url(会当作本地路径解析), String(会当作本地路径解析) ").isError(true).build();
                }

            } catch (Exception e){
                return McpSchema.CallToolResult.builder().addTextContent("Failed to convert tool result to a base64 image: " + e.getMessage()).isError(true).build();
            }

        } else if (isAudioMimeType(mineType)) {
            try {
                if (result instanceof byte[] || result instanceof InputStream || result instanceof File || result instanceof String
                    || result instanceof Path) {
                    String audioBase64 =  ConvertAudioUtils.audioToBase64(result);
                    return McpSchema.CallToolResult.builder().content(List.of(new McpSchema.AudioContent(
                                new McpSchema.Annotations(List.of(McpSchema.Role.ASSISTANT), null),
                                audioBase64,
                                mineType
                        ))).isError(false).build();
                    
                } else {
                    //todo 该不该打破协议, 协议讲图片,音频的data部分都是base64
                    return McpSchema.CallToolResult.builder().addTextContent("sorry, 目前mineType为audio类型只支持 byte[], InputStream, File, Path(会当作本地路径解析), Url(会当作本地路径解析), String(会当作本地路径解析) ").isError(true).build();
                }
             } catch (Exception e) {
                 return McpSchema.CallToolResult.builder().addTextContent("Failed to convert tool result to a base64 image: " + e.getMessage()).isError(true).build();
             }
        } else {
            return McpSchema.CallToolResult.builder().addTextContent("sorry, 目前不支持该mineType的返回类型, 要想返回该mineType, 请自行使用CallToolResult或ResourceLink或EmbeddedResource封装").isError(false).build();
        }


    }





    public String defaultConvertToCallToolResult(Object result, Type returnType) {
        if (returnType == Void.TYPE) {
            log.debug("The tool has no return type. Converting to conventional response.");
            return null;
        }


        // convert 图片为 base64
        if (result instanceof Image) {
            return IMAGE_MIME_TYPE;
        }
        // 因为java里没有具体的音频容器类, 就按 byte[] 等等来处理
        else if (result instanceof InputStream || result instanceof byte[]) {
            return AUDIO_MIME_TYPE;
        }

        return JSON_MIME_TYPE;


    }


}
