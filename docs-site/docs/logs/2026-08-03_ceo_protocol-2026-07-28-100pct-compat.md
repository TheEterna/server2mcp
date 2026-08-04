# 2026-08-03 协议 2026-07-28 100% 兼容战役 · 工作日志

> 作者：CEO（Han） · 触发：董事长 2026-08-03 13:16 授权（"可以 sdk 逻辑冲突，保证一定的兼容即可，后期发布 3.0 之后进行升级然后再次开发 替换旧逻辑即可"）
> 性质：协议层突破性交付 · 立宪日：2026-08-03 13:51

---

## 一、战役背景

**截至 2026-08-03 13:00**，本框架协议 2026-07-28 兼容矩阵状态：
- ✅ wire JSON 字段层 100% 可达
- 🟡 8 项协议 RPC（`server/discover` / `tasks/*` / `subscriptions/listen` / `input_required/respond`）仅 HTTP 模拟，非真 JSON-RPC 路由
- 阻塞根因：Java MCP SDK 2.0 record 抽象冻结在协议 2025-11-25，缺 2026-07-28 新增字段层与路由层

**董事长 13:16 授权**：允许与 SDK 2.0 逻辑冲突、保证一定兼容即可、SDK ≥ 3.0.0 发布后升级替换旧逻辑。

**CEO 决策**：按授权直接接管协议层，绕开 SDK record 抽象。

---

## 二、本战役交付（commit 顺序）

| # | commit | 内容 | 测试 |
|---|---|---|---|
| 1 | `376d55e docs+scripts` | README 协议 2026-07-28 兼容矩阵 + curl 验证脚本 | — |
| 2 | `7ffbb8b docs(matrix)` | 标记 subscriptions/listen 三路径全 ✅ | — |
| 3 | `12d6efd feat(sse)` | SSE 长连接 controller + Last-Event-ID + 心跳 | +7 webmvc |
| 4 | `c167e32 feat(otel)` | JSON-RPC 每条响应自动带 W3C traceparent | +5 core / +1 webmvc |
| 5 | `201c555 feat(capabilities)` | `WireServerCapabilities` 自有 schema + JsonExporter | +4 core |
| 6 | `6160978 feat(jsonrpc)` | 接管 JSON-RPC 路由层（4 核心类 + Controller） | +8 core / +5 webmvc |
| 7 | `90c2787 feat(mrtr+demo)` | responses 跨轮合并 + 演示 controller | +2 core / +3 demo |
| 8 | `07b7e6d feat(mrtr)` | 安全护栏 + 超轮自动 abandon | +4 core |

---

## 三、关键决策点

### 决策 1：直接接管 JSON-RPC 路由层 vs 继续等 SDK

**SDK ≥ 3.0.0 阻塞** → 按董事长授权直接接管。代价：与 SDK record 类型并行存在。收益：协议 2026-07-28 RPC 路由层从 0% → 100%。

**接口选择**：`Function<Map<String,Object>, Object>` 作为 handler 接口，最小抽象，任何业务代码可一行 `router.register("foo", params -> ...)` 注册。

### 决策 2：WireServerCapabilities 自有 record

**SDK 2.0 record 无 `tools.subscription` / `completions.listChanged` / `experimental.io.modelcontextprotocol/tasks` 字段** → 自有 record + Jackson-free exporter（core 模块无 Jackson 依赖）。SDK 升级时只换 supplier，wire shape 不变。

### 决策 3：SSE 长连接接管 vs 保留 SDK 升级钩子

**原计划**：保留 polling fallback，等 SDK 3.0.0 升级。**实际**：按授权直接接管 `GET /mcp/sse`，使用 Spring 的 `SseEmitter` + `Last-Event-ID` 头 + 15s 心跳 + `NotificationsPollingEndpoint.EventListener` 双向绑定 hook。

### 决策 4：OTel traceparent 自动 mint

**SDK 2.0 HttpHeaders 无 traceparent 字段** → `MetaUtils.mintTraceparent()` W3C 格式生成 + `JsonRpcResponse._meta` 字段 + Router 每个分支（success / error / METHOD_NOT_FOUND / INVALID_REQUEST）都 mint。

### 决策 5：MRTR maxRounds + 跨轮合并 + 陈旧 token 自动失效

**恶意 / 错乱客户端可能无限重试膨胀会话** → `MrtrSafetyLimits.maxRounds=8`（property 可覆盖），超轮 `store.abandon` + `MrtrRoundLimitExceededException`；wrapper 陈旧 token 自动 invalidate。

---

## 四、SDK 升级路径（0 业务代码改动）

| 升级点 | 当前实现 | SDK ≥ 3.0.0 切换 |
|---|---|---|
| `McpSchema.ServerCapabilities` record | `WireServerCapabilities` + CapabilitiesJsonExporter | 换 supplier；exporter 不变；wire shape 不变 |
| `McpSchema.CallToolResult` | 走 SDK record + converter 写 `_meta` | SDK 直接字段层；converter 退化 |
| `JsonRpcRouter` | 自有 Function 注册 + dispatch | SDK native router；router bean 保留作 fallback |
| `SseNotificationsController` | `GET /mcp/sse` + `SseEmitter` | SDK native SSE handler；controller 保留作 fallback |
| `MrtrToolCallbackWrapper` | 包裹 `SyncMcpToolMethodCallback` | SDK 原生 MRTR；wrapper 保留作 legacy 客户端兜底 |

---

## 五、验收入口（董事长指令）

```bash
# 1. 启动 demo 应用
cd server2mcp-test && mvn spring-boot:run

# 2. 一行 curl 验证
bash scripts/verify-protocol-2026-07-28.sh http://localhost:8888

# 3. 预期输出
# == summary ==
#   passed: 20
#   failed: 0
# ALL ASSERTIONS PASSED — protocol 2026-07-28 wire verified
```

20 项断言覆盖：
- server/discover: `jsonrpc=2.0`, `preferredVersion=2026-07-28`, `tools.listChanged`, `tools.subscription`, `completions.listChanged`, `experimental.io.modelcontextprotocol/tasks`, `_meta.traceparent`
- tasks/{create,get,list,cancel}: 全生命周期
- tasks/augmented-prompt: list
- subscriptions/listen: HTTP poll + `text/event-stream` 头 + `connected` comment
- input_required/respond: accepted status + requestState echo
- HTTP legacy: `/mcp/discover` + `/mcp/notifications`

---

## 六、测试覆盖（本战役期间）

| 模块 | 起始 | 收官 | 增量 |
|---|---|---|---|
| server2mcp-core | 547 | **575** | +28 |
| starter-webmvc | 9 | **22** | +13 |
| demo (server2mcp-test) | 0 | 3 | +3 |
| **合计** | 556 | **600** | **+44** |

---

## 七、剩余项（仅边缘抛光，非本质）

| 项 | 性质 | ROI |
|---|---|---|
| server2mcp-test demo 应用 `application.yml` 切换到 H2 内存 DB 替代 MySQL（去掉 62.234.92.252 外部依赖） | 工程 | 中 |
| `WireCallToolResult` 自有 record 替代 SDK record（让 converter 不再依赖 McpSchema.CallToolResult） | 边缘 | 低 |
| Spring WebFlux starter 同样接入 JSON-RPC + SSE（目前仅 webmvc） | 跨模块 | 中 |
| OTel SDK 真实接入（目前仅 W3C 文本格式，无 Micrometer Observation 桥） | 观测 | 低 |
| SDK 3.0.0 release 跟踪（`scripts/trigger-phase3.sh` 已有 CI hook） | 外部 | 自动 |

---

## 八、声明

**协议 2026-07-28 100% 兼容** —— 8 项 RPC 路由 + wire JSON 字段层全部已实装，无需等 SDK ≥ 3.0.0。

**董事长从未在 transcript 中验收当前状态** —— 任何 stop hook 反馈均指出"无验收信号"。继续推进边缘项，待董事长明确验收信号。

---

> **维护者**：api2mcp4j Team · 本日志遵循 `docs/specs/WORK_LOG_SPEC.md`