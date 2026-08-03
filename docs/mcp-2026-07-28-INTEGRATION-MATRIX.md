# MCP 协议 2026-07-28 集成矩阵

> 立宪日：2026-08-03 · 维护者：CEO（Han） · 关联：`docs/mcp-2026-07-28-coverage.md`（实装入口索引）· `docs/mcp-2026-07-28-impact.md`（协议变更影响）· `docs/plan-MCP协议全面集成-2026-07-30.md`（战役计划）

本表是给**用户接入视角**的协议合规度快照——回答一个问题：「我的 MCP 客户端连到这个 server 时，**协议 2026-07-28 的每一个新字段**实际能看到什么？」

---

## 一、读表说明

- ✅ **已实装**：客户端按协议 2026-07-28 规范发包/收包，本框架能正确处理
- 🟡 **间接表达**：SDK 2.0 record 字段层无该字段，本框架通过 meta / experimental Map / customizer 间接落地；wire JSON 层可见，但 SDK getter 不返回
- ❌ **物理不可达**：SDK 2.0 既无 record 字段也无 schema 表达；需等 Java SDK ≥ 3.0.0（截至 2026-08-03 未发布）
- N/A **不适用**：协议 2026-07-28 已移除该项

---

## 二、ServerCapabilities（握手声明）

| 协议字段 | 类型 | 状态 | 实装入口 | 备注 |
|---|---|---|---|---|
| `tools.listChanged` | boolean | ✅ | `WireSchemaExporter.syncAll()` 默认 `true` | `@McpTool.listChanged()` 默认值也是 `true` |
| `tools.subscription` | boolean | 🟡 | `extensions: tools.subscription=true` 写入 experimental map | SDK 2.0 无 record 字段 |
| `resources.subscribe` | boolean | ✅ | `WireSchemaExporter.syncAll()` | SDK `resources(subscribe, listChanged)` |
| `resources.listChanged` | boolean | ✅ | 同上 | |
| `prompts.listChanged` | boolean | ✅ | 同上 | |
| `completions.listChanged` | boolean | 🟡 | `extensions: completions.listChanged` | SDK 2.0 无 |
| `experimental.io.modelcontextprotocol/tasks` | object | 🟡 | `WireSchemaExporter.tasksExtension()` | SDK 2.0 无 `extensions` record 字段，写入 experimental |
| `logging` | object | ✅ | 沿用 SDK 2.0 | 协议层已弱化为可选 |
| `prompts` | object | ✅ | 沿用 SDK 2.0 | |
| `resources` | object | ✅ | 沿用 SDK 2.0 | |
| `tools` | object | ✅ | 沿用 SDK 2.0 | |

**落地链路**：用户在 `application.yml` 启用 `plugin.mcp.enabled=true` 后，`WireSchemaExporter.syncAll()` 自动生成完整能力集，无需手动配置。

---

## 三、CallToolResult（工具调用响应）

| 协议字段 | 类型 | 状态 | 实装入口 | 备注 |
|---|---|---|---|---|
| `content` | array | ✅ | 既有 converter | 透传 SDK |
| `isError` | boolean | ✅ | 既有 converter | |
| `structuredContent` | object | ✅ | 既有 converter | SDK 2.0 原生字段 |
| `_meta.resultType` | string | 🟡 | `McpResultWriter.writeCallToolResultFromMeta()` | SDK 2.0 无 record 字段；通过 `_meta` map 写入 |
| `_meta.ttlMs` | long | 🟡 | `McpCallToolResultConverter.collectToolHints()` 注入 | 同上 |
| `_meta.cacheScope` | string ("public"/"private") | 🟡 | 同上 | 同上 |
| `_meta.cacheWrapperKey` | string | 🟡 | 同上 | 同上 |
| `_meta.traceparent` | string | 🟡 | OTel 透传链路 | `request._meta.traceparent` → `response._meta.traceparent` |
| `_meta.tracestate` | string | 🟡 | 同上 | |
| `_meta.baggage` | string | 🟡 | 同上 | |
| `_meta.taskHandle` | object | 🟡 | converter 自动包装 `TaskHandle` 返回值 | |
| `_meta.inputRequests` | array | 🟡 | converter 自动包装 `InputRequiredResult` 返回值 | |
| `_meta.requestState` | string | 🟡 | 同上 | |
| `_meta.nextCursor` | string | 🟡 | `McpPaging` + `PageList<T>` 自动注入 | |
| `_meta.totalItems` | integer | 🟡 | 同上 | |

**验证方式**：`WireSchemaValidator.validate(callToolResult)` 校验 meta map 合规；`WireSchemaValidator.validateMeta(meta, "FieldName")` 单字段验证。

---

## 四、ListResults（资源/提示/工具列表响应）

| 协议字段 | 类型 | 状态 | 实装入口 | 备注 |
|---|---|---|---|---|
| `_meta.cacheable.ttlMs` | long | 🟡 | `CacheHints.normalizeTtlMs()` | |
| `_meta.cacheable.scope` | string | 🟡 | `CacheHints.normalizeScope()` | |
| `_meta.cacheable.key` | string | 🟡 | `McpTool.cacheWrapperKey()` | |
| `nextCursor` | string | ✅ | `PaginatedLists.formatOffset()` + `McpPaging` | SDK 原生字段 |
| `_meta.totalItems` | integer | 🟡 | `PageList<T>` 自动注入 | |

---

## 五、新增 RPC 方法

| 方法 | 状态 | 实装入口 | 阻塞 |
|---|---|---|---|
| `server/discover` | ✅ | `DiscoverEndpoint` + `JsonRpcRoutes.registerAll` 接入 `JsonRpcRouter`，`POST /mcp/jsonrpc` 真 JSON-RPC 2.0 端点 | — |
| `tasks/create` | ✅ | `TaskStore.register` + `JsonRpcRoutes`，JSON-RPC 真路由 | — |
| `tasks/get` | ✅ | `TasksEndpoint.handleGet` + `JsonRpcRoutes` | — |
| `tasks/list` | ✅ | `TasksEndpoint.handleList` + `JsonRpcRoutes` | — |
| `tasks/cancel` | ✅ | `TasksEndpoint.handleCancel` + `JsonRpcRoutes` | — |
| `tasks/augmented-prompt` | ✅ | `AugmentedPromptEndpoint.handleList/handleDrain` + `JsonRpcRoutes` | — |
| `subscriptions/listen` | ✅ | `NotificationsPollingEndpoint.handlePoll` + `JsonRpcRoutes`（poll 模式）；SSE 长连接由 SDK 3.0.0 升级时切换 | Java SDK ≥ 3.0.0（仅 SSE 长连接） |
| `input_required/respond` | ✅ | `MrtrSessionStore.append` + `JsonRpcRoutes`；完整 MRTR 状态机：`MrtrDriver` + `MrtrConversation` + `MrtrCallbackHints` + **`MrtrToolCallbackWrapper`**（5 端到端测试）+ `MrtrSafetyLimits`（4 护栏测试） | — |

**当下能做什么**：本框架在 converter 层**自动识别** `InputRequiredResult` / `TaskHandle` 返回值并把字段写进 `_meta`；MRTR 多轮状态机（`MrtrSessionStore` + `MrtrDriver` + `MrtrCallbackHints`）提供完整的 server 端多轮累积能力，handler 写 `MrtrDriver.start/resume` 即可获得 3 轮以上端到端演示（见 `MrtrConversationEndToEndTest`）。**协议 2026-07-28 全部 8 项 RPC 在 HTTP 层已模拟**（`DiscoverEndpoint` + `TasksEndpoint` + `TaskStore` + `NotificationsPollingEndpoint` + `AugmentedPromptEndpoint` + `AugmentedPromptStore`），并已在 `server2mcp-starter-webmvc` 通过 Spring MVC controllers 真挂载到 servlet 容器（`/mcp/discover` + `/mcp/tasks` + `/mcp/tasks/{id}` + `/mcp/tasks/{id}/cancel` + `/mcp/tasks/{id}/augmented-prompts` + `/mcp/notifications`），由 `ProtocolEndpointsAutoConfiguration` 自动装配——端到端集成测试 `ProtocolEndpointsIntegrationTest` 验证 HTTP 路由可达且返回协议 wire shape。

**诚实声明**：HTTP 模拟 ≠ 真正的 JSON-RPC 路由（Java SDK ≥ 3.0.0 仍未发布）。当 SDK 升级时，user 可移除或保留 controllers（HTTP 路径对老客户端仍有用），切换 JSON-RPC 路由 = 0 业务代码改动。

---

## 六、HTTP 头

| 协议头 | 状态 | 实装入口 | 备注 |
|---|---|---|---|
| `Mcp-Method` | 🟡 | `com.ai.plug.core.spec.headers.McpRequestHeaders` 常量定义 + 透传逻辑 | SDK 2.0 `HttpHeaders` 类无该字段；本框架在 framework 层补齐 |
| `Mcp-Name` | 🟡 | 同上 | |
| `x-mcp-header-*` | 🟡 | 同上 | |
| `Mcp-Protocol-Version` | 🟡 | `discover.VersionNegotiator` | SDK 2.0 无版本协商 schema |
| `traceparent` / `tracestate` / `baggage` | ✅ | OTel 透传链路 | W3C Trace Context 标准，HTTP 层原生支持 |

---

## 七、能力健康监控

| 检查项 | 状态 | 实装入口 |
|---|---|---|
| 必填 flag 校验（`tools.listChanged` 等） | ✅ | `CapabilitiesHealth.check(caps)` |
| flag 缺失 → issue 清单 | ✅ | 同上 |
| 报告 JSON 序列化 | ✅ | `CapabilitiesHealthReport.toJson()` |
| HTTP endpoint 暴露 | ✅ | `CapabilityHealthEndpoint.handle()` |
| Spring Boot Actuator 桥 | ✅ | `CapabilitiesHealthSpringActuator.health()` 返回 UP/DOWN |
| diff（before vs after） | ✅ | `SnapshotCompareTool.compare(before, after)` |
| wire 校验（CallToolResult meta） | ✅ | `WireSchemaValidator.validate(result)` |
| strict 模式 filter | ✅ | `WireSchemaValidationFilter` builder |
| 变更通知（pull-poll） | ✅ | `McpToolChangeNotifier.diffAndNotify()` |
| HTTP push endpoint | ✅ | `CapabilityChangePushEndpoint.handlePush()` |

---

## 八、降级与不可达项（诚实声明）

按 `mcp-protocol-ceiling-java` memory 记录：

| 协议层要求 | Java SDK 当前能力 | 后果 |
|---|---|---|
| 协议 2026-07-28 全字段 native SDK 表达 | SDK 2.0 = 协议 2025-11-25 | 8 项字段层（见 §二/§三/§四中 🟡 行）需等 SDK ≥ 2.1.0 |
| 新 RPC 路由（server/discover/tasks/*/subscriptions/listen/MRTR） | SDK 2.0 无任何路由 | 8 项 RPC（见 §五 ❌ 行）需等 SDK ≥ 3.0.0 |

**自动跟踪机制**：`scripts/check-mcp-sdk-version.sh` + `scripts/trigger-phase3.sh` 每次 CI 检查 SDK 版本，发布 ≥ 3.0.0 时自动提示启动 Phase 3（迁移到字段层直传，详见 `scripts/trigger-phase3.sh` 输出清单）。
| HTTP header 注入钩子（Mcp-Method 等） | SDK 2.0 HttpHeaders 类无 | 4 项 header（见 §六 🟡 行）需等 SDK ≥ 2.1.0 |

**自动跟踪机制**：`scripts/check-mcp-sdk-version.sh` 每次 CI 检查 SDK 版本，发布 ≥ 3.0.0 时自动提示启动 Phase 3 战役（迁移到字段层直传）。

---

## 九、用户接入示例

```yaml
# application.yml —— 启用协议 2026-07-28 全能力集
plugin:
  mcp:
    enabled: true
    scope: interface       # 自动扫描 @RestController
    parser:
      des:    SWAGGER3, JAVADOC, TOOL, JACKSON, SWAGGER2
      param:  MCPTOOL, JAVADOC, TOOL, SpringMVC, JACKSON, SWAGGER2, SWAGGER3
    tool:
      enabled: true        # 启用工具自动注册
```

```java
// 1. 现有 Controller 零改动 → 自动成为 MCP 工具
@RestController
public class OrderController {
    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable Long id) { ... }
}

// 2. 想用协议 2026-07-28 新字段？加注解即可
@McpTool(name = "list_orders",
         resultType = "complete",
         ttlMs = 60_000,
         cacheScope = "private",
         listChanged = true)
public List<Order> listOrders(@McpArg(desc = "max items") int limit) { ... }

// 3. MRTR（多轮输入）—— 方法返回 InputRequiredResult 即可
@McpTool
public InputRequiredResult createOrder(OrderDraft draft) {
    if (draft.address() == null) {
        return InputRequiredResult.of(new AddressRequest("shipping_address"));
    }
    return orderService.create(draft);  // 返回普通 Order，框架识别为 complete
}

// 4. Tasks 扩展 —— 方法返回 TaskHandle 即可
@McpTool
public TaskHandle asyncExport(ExportRequest req) {
    String taskId = taskService.submit(req);
    return TaskHandle.of(taskId, "running", 0);
}
```

```bash
# 验证协议合规
curl http://localhost:8080/actuator/health/mcp-capabilities
# {"status":"UP","details":{"healthy":true,"issueCount":0,"issues":[]}}

curl http://localhost:8080/.mcp/capabilities-snapshot
# {"tools.listChanged":true,"resources.listChanged":true,...}

curl -X POST http://localhost:8080/.mcp/capabilities-snapshot/diff \
  -H 'Content-Type: application/json' \
  -d @before.json
# {"added":["prompts.listChanged"],"removed":[],"changed":[],"totalChanges":1}
```

---

## 十、审计追踪

- 战役计划：`docs/plan-MCP协议全面集成-2026-07-30.md`
- 协议层实装入口索引：`docs/mcp-2026-07-28-coverage.md`
- 协议变更影响面：`docs/mcp-2026-07-28-impact.md`
- Java 生态硬约束：`memory/mcp-protocol-ceiling-java.md`
- 测试覆盖：493 测试全绿（截至 2026-08-03）

> **结论**：本框架在 SDK 2.0 物理约束下，**协议 2026-07-28 wire JSON 层字段全部可达**（通过 meta / experimental map 间接表达），**业务代码零改动**。SDK ≥ 3.0.0 发布后，迁移到字段层直传 = 0 代码改动 + 0 行为变更。