package com.ai.plug.core.spec.callback.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.logaritex.mcp.common.MineTypeConstants;
import com.logaritex.mcp.utils.ConvertAudioUtils;
import com.logaritex.mcp.utils.ConvertImageUtils;
import com.logaritex.mcp.utils.JsonParser;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;

import static com.logaritex.mcp.common.MineTypeConstants.*;

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
        if (result instanceof Mono<?>) {
            result = ((Mono<?>) result).block();
        }
        // 返回类就是最后结果
        if (result instanceof McpSchema.CallToolResult callToolResult) {
            return callToolResult;
        }
        else if (result instanceof McpSchema.Content content) {
            return new McpSchema.CallToolResult(List.of(content), false);
        }
        else if (result instanceof List<?> && !((List<?>) result).isEmpty() && ((List<?>) result).get(0) instanceof McpSchema.Content) {
            return new McpSchema.CallToolResult((List<McpSchema.Content>) result, false);
        }

        String mineType = callback.mineType;
        if (mineType == null || mineType.isBlank()) {
            // 没有mineType, 就完全按照返回类型来转换
            return defaultConvertToCallToolResult(result, returnType);
        }

        if (isTextMimeType(mineType)) {
            String json = null;
            try {
                json = JsonParser.toJson(result);
            } catch (JsonProcessingException e) {
                return new McpSchema.CallToolResult("find a incorrect text mineType of a Annotation from " + callback.method.getName(), true);
            }
            // todo 感觉不太合理 因为文本不只有纯文本, 还有其他的文本类型, 比如markdown, html, xml, json等等
            return new McpSchema.CallToolResult(json, false);
        } else if (isImageMimeType(mineType)) {

            // convert 图片为base64

            try {
                if (result instanceof Image image) {
                    String imgBase64 = ConvertImageUtils.imageToBase64ByClass(image, "PNG");
                    return new McpSchema.CallToolResult(List.of(new McpSchema.ImageContent(new McpSchema.Annotations(List.of(McpSchema.Role.ASSISTANT), null), imgBase64, mineType)), false);
                }
                else if (result instanceof byte[] || result instanceof InputStream || result instanceof File || result instanceof String
                        || result instanceof Path) {
                    //todo 该不该打破协议, 协议描述图片,音频的data部分都是base64
                    String base64 = ConvertImageUtils.imageToBase64(result, ConvertImageUtils.mapMimeTypeToFormatName(mineType));
                    return new McpSchema.CallToolResult(List.of(new McpSchema.ImageContent(new McpSchema.Annotations(List.of(McpSchema.Role.ASSISTANT), null), base64, mineType)), false);
                } else {
                    //todo 该不该打破协议, 协议讲图片,音频的data部分都是base64
                    return new McpSchema.CallToolResult("sorry, 目前mineType为image类型只支持 byte[], Image, InputStream, File, Path(会当作本地路径解析), Url(会当作本地路径解析), String(会当作本地路径解析) ", true);
                }

            } catch (Exception e){
                return new McpSchema.CallToolResult("Failed to convert tool result to a base64 image: " + e.getMessage(), true);
            }

        } else if (isAudioMimeType(mineType)) {
            try {
                if (result instanceof byte[] || result instanceof InputStream || result instanceof File || result instanceof String
                    || result instanceof Path) {
                    String audioBase64 =  ConvertAudioUtils.audioToBase64(result);
                    return new McpSchema.CallToolResult(
                        List.of(new McpSchema.AudioContent(
                                new McpSchema.Annotations(List.of(McpSchema.Role.ASSISTANT), null),
                                audioBase64,
                                mineType
                        )),
                        false
                    );
                    
                } else {
                    //todo 该不该打破协议, 协议讲图片,音频的data部分都是base64
                    return new McpSchema.CallToolResult("sorry, 目前mineType为audio类型只支持 byte[], InputStream, File, Path(会当作本地路径解析), Url(会当作本地路径解析), String(会当作本地路径解析) ", true);
                }
             } catch (Exception e) {
                 return new McpSchema.CallToolResult("Failed to convert tool result to a base64 image: " + e.getMessage(), true);
             }
        } else {
            return new McpSchema.CallToolResult("sorry, 目前不支持该mineType的返回类型, 要想返回该mineType, 请自行使用CallToolResult或ResourceLink或EmbeddedResource封装", false);
        }

        /*

        // 接入 resource 无法通过returnType来推断
        else if (result instanceof McpSchema.ResourceLink) {

            // todo :sdk 少一个目前最新协议里title字段, 为了版本统一, 不再fork sdk, 后面需要补上
            new McpSchema.ResourceLink(McpSchema.Annotations, McpSchema.ResourceContents)
        }
        else if (result instanceof McpSchema.EmbeddedResource) {
            // todo :sdk 少一个目前最新协议里title字段, 为了版本统一, 不再fork sdk, 后面需要补上

            new McpSchema.EmbeddedResource(McpSchema.Annotations, McpSchema.ResourceContents)
        }
        */
    }





    public McpSchema.CallToolResult defaultConvertToCallToolResult(Object result, Type returnType) {
        if (returnType == Void.TYPE) {
            log.debug("The tool has no return type. Converting to conventional response.");
            return new McpSchema.CallToolResult("Done", false);
        }


        // convert 图片为 base64
        if (result instanceof Image image) {
            try {
                String imgBase64 = ConvertImageUtils.imageToBase64ByClass(image, "PNG");
                return new McpSchema.CallToolResult(List.of(new McpSchema.ImageContent(new McpSchema.Annotations(List.of(McpSchema.Role.ASSISTANT), null), imgBase64, IMAGE_MIME_TYPE)), false);
            } catch (InterruptedException e) {
                return new McpSchema.CallToolResult("Failed to convert tool result to a base64 image: " + e.getMessage(), true);
            }

        }
        // 因为java里没有具体的音频容器类, 就按 byte[] 等等来处理
        else if (result instanceof InputStream || result instanceof byte[]) {
            try {
                String audioBase64 =  ConvertAudioUtils.audioToBase64(result);

                return new McpSchema.CallToolResult(
                        List.of(new McpSchema.AudioContent(
                                new McpSchema.Annotations(List.of(McpSchema.Role.ASSISTANT), null),
                                audioBase64,
                                MineTypeConstants.AUDIO_MIME_TYPE
                        )),
                        false
                );
            } catch (Exception e) {
                return new McpSchema.CallToolResult(e.getMessage(), true);
            }

        } else {
            log.debug("Converting tool result to JSON.");
            String json = null;
            try {
                json = JsonParser.toJson(result);
            } catch (JsonProcessingException e) {
                return new McpSchema.CallToolResult("Conversion from Object to JSON failed", true);
            }
            return new McpSchema.CallToolResult(json, false);
        }


    }


}
