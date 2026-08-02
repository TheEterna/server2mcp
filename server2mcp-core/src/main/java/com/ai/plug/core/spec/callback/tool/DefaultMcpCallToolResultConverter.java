package com.ai.plug.core.spec.callback.tool;

import com.ai.plug.common.constants.MineTypeConstants;
import com.ai.plug.common.utils.ConvertAudioUtils;
import com.ai.plug.common.utils.ConvertImageUtils;
import com.ai.plug.common.utils.JsonParser;
import com.ai.plug.core.annotation.McpTool;
import com.ai.plug.core.spec.meta.MetaUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.ai.plug.common.constants.MineTypeConstants.*;

/**
 * Default implementation of McpCallToolResultConverter
 * @author han
 * @time 2025/6/27 14:05
 */

public class DefaultMcpCallToolResultConverter implements McpCallToolResultConverter {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DefaultMcpCallToolResultConverter.class);

    /**
     * System property to enable dev-mode schema validation. When set to
     * "true", the converter invokes {@link com.ai.plug.core.spec.resulttype.WireSchemaValidator}
     * on every produced {@code CallToolResult} and throws on issues.
     * Default "false" — zero overhead in production.
     */
    public static final String DEV_MODE_PROPERTY = "api2mcp4j.wireschema.validate";
    /**
     * Dev-mode schema validation hook. Returns a converted result, also
     * running {@link com.ai.plug.core.spec.resulttype.WireSchemaValidator}
     * when the {@link #DEV_MODE_PROPERTY} system property is "true".
     * Static utility so unit tests can call it directly without
     * going through the full converter flow.
     */
    static McpSchema.CallToolResult maybeValidateInDevMode(
            McpSchema.CallToolResult result, Type returnType) {
        if ("true".equalsIgnoreCase(System.getProperty(DEV_MODE_PROPERTY, "false"))) {
            var report = com.ai.plug.core.spec.resulttype.WireSchemaValidator.validate(result);
            if (!report.isOk()) {
                throw new IllegalStateException(
                    "WireSchema validation failed (dev-mode):\n" + report);
            }
        }
        return result;
    }

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
        // Collect wire-layer protocol 2026-07-28 hints from @McpTool annotation
        // (resultType / ttlMs / cacheScope). Merged into every CallToolResult
        // meta produced below. SDK 2.0 lacks the resultType field on
        // CallToolResult so these are exposed via meta for downstream
        // McpResultWriter consumption.
        java.util.Map<String, Object> toolHints = collectToolHints(callback);

        // Forward OpenTelemetry trace context (SEP-414) from the request's
        // _meta to the response's _meta so downstream clients / observability
        // tooling can stitch spans across the call boundary. Traceparent /
        // tracestate / baggage are extracted by MetaUtils.
        if (callback != null) {
            McpSchema.CallToolRequest req = callback.currentRequest();
            if (req != null) {
                Map<String, Object> trace = MetaUtils.forwardTraceContext(req.meta());
                if (!trace.isEmpty()) {
                    // Defensive merge — preserve any existing toolHints entries
                    java.util.Map<String, Object> merged = new HashMap<>(toolHints);
                    merged.putAll(trace);
                    toolHints = merged;
                }
            }
        }
        // 返回类就是最后结果
        // Auto-slice returned List<?> when a McpPaging was injected
        if (callback != null) {
            com.ai.plug.core.spec.pagination.McpPaging paging = callback.capturedPaging();
            if (paging != null && result instanceof java.util.List<?> list && !list.isEmpty()) {
                var page = com.ai.plug.core.spec.pagination.PaginatedLists.slice(
                    list, paging.offset(), paging.size());
                result = page.items();
                if (page.hasMore()) {
                    // Caller typically wraps this in a List*Result; we surface
                    // nextCursor via meta so downstream McpResultWriter picks it up
                    toolHints.put("nextCursor", page.nextCursor());
                }
            }
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
                    .meta(toolHints)
                    .build();
        }
        else if (result instanceof List<?> && !((List<?>) result).isEmpty() && ((List<?>) result).get(0) instanceof McpSchema.Content) {

            return McpSchema.CallToolResult.builder()
                    .content((List<McpSchema.Content>) result)
                    .isError(false)
                    .structuredContent(result)
                    .meta(toolHints)
                    .build();
        }
        // PageList 通用返回类型：自动计算 nextCursor + meta 注入
        else if (result instanceof com.ai.plug.core.spec.pagination.PageList<?> pageList) {
            String json;
            try {
                json = JsonParser.toJson(pageList.items());
            }
            catch (JacksonException e) {
                return McpSchema.CallToolResult.builder().addTextContent("page list serialization failed: " + e.getMessage()).isError(true).build();
            }
            com.ai.plug.core.spec.pagination.McpPaging paging = callback.capturedPaging();
            if (paging != null) {
                String nextCursor = pageList.nextCursor(paging);
                if (nextCursor != null) {
                    toolHints.put("nextCursor", nextCursor);
                }
                toolHints.put("totalItems", pageList.totalItems());
            }
            return McpSchema.CallToolResult.builder().addTextContent(json).isError(false).meta(toolHints).build();
        }
        // 时自动包装为 CallToolResult，isError=false（这是合法 interim result，
        // 不是错误）。注意 SDK 2.0 没有 resultType 字段，resultType 信息
        // 放在 meta 里供下游 McpResultWriter 序列化时读取。
        else if (result instanceof com.ai.plug.core.spec.mrtr.MrtrTypes.InputRequiredResult inputRequired) {
            java.util.Map<String, Object> meta = new java.util.HashMap<>(toolHints);
            // MRTR overrides any callback-level resultType hint
            meta.put("resultType", "input_required");
            meta.put("inputRequests", inputRequired.inputRequests());
            if (inputRequired.requestState() != null) {
                meta.put("requestState", inputRequired.requestState());
            }
            return McpSchema.CallToolResult.builder()
                    .addTextContent("input_required: " + inputRequired.inputRequests().size() + " request(s)")
                    .isError(false)
                    .structuredContent(inputRequired)
                    .meta(meta)
                    .build();
        }
        // Tasks 扩展（协议 2026-07-28 SEP-2663）：工具返回 TaskHandle
        // 时自动包装为 CallToolResult，isError=false。
        else if (result instanceof com.ai.plug.core.spec.tasks.TaskTypes.TaskHandle taskHandle) {
            java.util.Map<String, Object> meta = new java.util.HashMap<>(toolHints);
            meta.put("taskHandle", taskHandle.taskId());
            return McpSchema.CallToolResult.builder()
                    .addTextContent("task accepted: " + taskHandle.taskId())
                    .isError(false)
                    .structuredContent(taskHandle)
                    .meta(meta)
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
            return McpSchema.CallToolResult.builder().addTextContent(json).isError(false).meta(toolHints).build();
        } else if (isImageMimeType(mineType)) {
            // convert 图片为base64
            try {
                if (result instanceof Image image) {
                    String imgBase64 = ConvertImageUtils.imageToBase64ByClass(image, ConvertImageUtils.mapMimeTypeToFormatName(mineType));

                    return McpSchema.CallToolResult.builder()
                            .addContent(new McpSchema.ImageContent(new McpSchema.Annotations(List.of(McpSchema.Role.ASSISTANT), null), imgBase64, mineType))
                            .isError(false)
                            .structuredContent(result)
                            .meta(toolHints)
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
                            .meta(toolHints)
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
                        ))).isError(false).meta(toolHints).build();
                    
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


    /**
     * Collect wire-layer protocol 2026-07-28 hints from the {@link McpTool}
     * annotation that the callback carries. Returns a (possibly empty) map
     * suitable for {@code CallToolResult.Builder.meta(...)}.
     * <p>
     * Key set (each is independent — only present when set on the annotation):
     * <ul>
     *   <li>{@code resultType} — from {@link McpTool#resultType()}; default {@code "complete"}</li>
     *   <li>{@code ttlMs} — from {@link McpTool#ttlMs()}; omitted when &lt;= 0</li>
     *   <li>{@code cacheScope} — from {@link McpTool#cacheScope()}; omitted when blank</li>
     *   <li>{@code cacheWrapperKey} — from {@link McpTool#cacheWrapperKey()}; default {@code "_cacheable"}</li>
     * </ul>
     * Returns an empty map when callback is null or no hint fields are set,
     * which {@link McpSchema.CallToolResult.Builder#meta(Map)} accepts.
     */
    private java.util.Map<String, Object> collectToolHints(AbstractMcpToolMethodCallback callback) {
        java.util.Map<String, Object> hints = new java.util.HashMap<>();
        if (callback == null) {
            return hints;
        }
        McpTool ann = callback.toolAnnotation;
        if (ann == null) {
            return hints;
        }
        // resultType: default to "complete" when annotation is absent or empty
        String resultType = ann.resultType();
        if (resultType != null && !resultType.isBlank()) {
            hints.put("resultType", resultType);
        }
        // ttlMs: only emit when > 0 (0 means "don't cache")
        if (ann.ttlMs() > 0) {
            hints.put("ttlMs", ann.ttlMs());
        }
        // cacheScope: only emit when explicitly set
        if (ann.cacheScope() != null && !ann.cacheScope().isBlank()) {
            hints.put("cacheScope", ann.cacheScope());
        }
        // cacheWrapperKey: always emit (default "_cacheable") — gives
        // McpResultWriter a deterministic key to look up.
        String wrapperKey = ann.cacheWrapperKey();
        if (wrapperKey != null && !wrapperKey.isBlank()) {
            hints.put("cacheWrapperKey", wrapperKey);
        }
        return hints;
    }


}
