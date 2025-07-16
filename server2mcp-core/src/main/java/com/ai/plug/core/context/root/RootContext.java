package com.ai.plug.core.context.root;

import com.ai.plug.core.utils.CustomToolUtil;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * TODO 本想要使用服务器存储root的方式来减少 roots/list 请求 , 再配合 roots/list_changed, 完成对root的热更新, 服务端只需要通过简单的bean注入即可使用roots
 * 但由于 mcp-sdk 对 连接建立时, 并未开放接入点, 所以导致服务器无法第一时间不能获取客户端的roots, 只能通过resource, tool等接入点来获取 roots
 * 先通过 tool, resource, prompt 接入点来获取 roots, 为避免重复获取, 使用 null 判断 结合 changed 功能完成替代
 * 缺点: 代码嵌入性高, 对之前结构有极大侵入性
 *
 * en:
 * TODO The original intention was to use server-side storage of roots to reduce the need for repeated 'roots/list' requests and enable hot updates through 'roots/list_changed'. This would allow services to utilize roots simply via bean injection.
 * However, since the MCP SDK does not provide an entry point during connection establishment, the server is unable to obtain client roots immediately at initialization and can only retrieve them via resource or tool entry points.
 * As a current workaround, roots are fetched through the tool, resource, and prompt entry points. To avoid redundant fetching, null checks combined with the changed functionality are used as a substitute.
 * Drawback: High code entanglement and significant intrusion into the existing structure.
 * @author han
 * @time 2025/7/11 16:09
 */

public class RootContext implements IRootContext{

    RootContext() {}

    /**
     * 为了不破坏MCP协议, 暂不支持服务端自定义 root
     * In order not to violate the MCP protocol, custom root on the server is currently not supported
     */
    private final Map<McpServerSession, List<McpSchema.Root>> roots = new ConcurrentReferenceHashMap<>(16, ConcurrentReferenceHashMap.ReferenceType.WEAK);


    @Override
    public List<McpSchema.Root> getRoots(Object exchange) {
        McpServerSession session = null;
        try {
            session = CustomToolUtil.getSession(exchange);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        List<McpSchema.Root> result = roots.get(session);

        if (result == null || result.isEmpty()) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(result);
    }

    @Override
    public void updateRoots(Object exchange, List<McpSchema.Root> newRoots) {
        McpServerSession session = null;
        try {
            session = CustomToolUtil.getSession(exchange);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        if (newRoots == null) {
            newRoots = Collections.emptyList();
        }

        List<McpSchema.Root> replace = this.roots.replace(session, newRoots);
        if (replace == null) {
            throw new IllegalArgumentException("exchange is error, can not find the exchange");
        }
    }

    @Override
    public void setRoots(Object exchange, List<McpSchema.Root> newRoots) {

        McpServerSession session = null;
        try {
            session = CustomToolUtil.getSession(exchange);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        // 参数校验
        if (newRoots == null) {
            newRoots = Collections.emptyList();
        }
        this.roots.putIfAbsent(session, newRoots);

    }

}
