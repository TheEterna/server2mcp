# 2026-08-03 MRTR 装饰器 + 安全护栏 + 构建修复 工作日志

> 作者：CEO（Han） · 关联：根 pom `fork=true` 修复、MRTR 战役第 5-6 commit

## 一、本轮交付（commit 顺序）

| commit | 内容 | 测试 |
|---|---|---|
| `b2588aa feat(starter)` | framework HTTP 契约真挂载到 Spring MVC（5 controller + auto-config + 集成测试） | +9 starter-webmvc |
| `7751ba7 feat(mrtr)` | MrtrToolCallbackWrapper 让任意 SyncMcpToolMethodCallback 即插即用 MRTR | +5 core |
| `60e73a3 docs(matrix)` | 矩阵 §五 input_required/respond 行加粗 MrtrToolCallbackWrapper 链接 | — |

外加本次第二批（待 commit）：
- `MrtrSafetyLimits` + `MrtrRoundLimitExceededException` + 4 端到端测试
- `<fork>true</fork>` 构建修复（lombok processor 在 JDK 21 独立 JVM 跑通）

## 二、SDK 硬约束（重申 · 必须诚实）

**Java MCP SDK 仍 2.0.0 = 协议 2025-11-25**，未发布 3.0.0 之前：
- 8 项协议 2026-07-28 新 RPC（`server/discover`、`tasks/*`、`subscriptions/listen`、`input_required/respond`）**仅 HTTP 模拟**，非真正 JSON-RPC 路由
- SDK 2.0 record 字段层缺失 `_meta.resultType` / `_meta.ttlMs` / `_meta.cacheScope` 等 13 项 meta 字段，全部走 `_meta` Map 间接表达
- 切换到字段层直传 = **0 业务代码改动**（controllers 保留作 fallback，老客户端继续 HTTP 调）

`scripts/trigger-phase3.sh` 已部署 CI 检查 SDK ≥ 3.0.0 自动触发迁移。

## 三、本轮深层修复

### 1. lombok 构建回归（系统级）

**病因**：JDK 21 + lombok 1.18.46 in-process processor 被模块访问拒绝（`jdk.compiler/com.sun.tools.javac.*`），导致 `@Slf4j` / `@Builder` 全部静默失效，错误表现 = "找不到符号 log" 散落在所有受影响类。

**修复**：maven-compiler-plugin 3.13.0 + `<fork>true</fork>`，让 javac fork 出独立 JVM 跑 lombok processor，`--add-opens` 才生效。

**影响**：不仅本轮新代码，所有既有 `@Slf4j` 类（ToolDefinitionBuilder、JavaDocDesParser、ScannableMethodToolCallbackProvider 等）都从"假性"恢复。

### 2. MRTR 装饰器（业务层）

把 MRTR 协议逻辑从"用户必须写"降到"框架自动接管"：

```java
// 用户代码（任何现有工具方法）
@McpTool
public Address submit(@MrtrInputResponses Map<String, Object> prior, String street, String city) { ... }

// 注册时一次性 wrap
BiFunction<McpSyncServerExchange, CallToolRequest, CallToolResult> wrapped =
    MrtrToolCallbackWrapper.wrap(inner, method, store);
```

- **零侵入**：用户方法签名无需任何 mrtr 知识
- **陈旧 token 自动失效**：wrapper 注入失败时 invalidate，store.start 自动 fallback 到新 UUID
- **5 测试覆盖**：null store / 首轮 / 携带 responses 重试 / 终端结果无副作用 / 未知 token

### 3. MRTR 安全护栏（系统稳定）

- `MrtrSafetyLimits.maxRounds` 默认 8（property: `plugin.mcp.mrtr.maxRounds`）
- `MrtrDriver.resume` 超限时自动 `store.abandon` + 抛 `MrtrRoundLimitExceededException`
- 防止恶意 / 错乱客户端无限重试膨胀会话
- 4 测试：within-limit / exceed / 默认值 / 非法 property 兜底

## 四、未做（坦白）

按董事长既往要求"不强行宣布收尾"：

| 项 | 阻塞 |
|---|---|
| 8 项 RPC 切到真 JSON-RPC 路由 | SDK ≥ 3.0.0 未发布（外部硬约束） |
| `server2mcp-test` demo 应用集成（curl 真能跑全套 endpoints） | 模块不在 root pom，时间成本高 |
| OTel `_meta.traceparent` 链路串联 | 已有 trace forwarding，缺端到端 OTel SDK 接入测试 |
| `outputSchema` 字段层启用（当前注释） | SDK 2.0 不支持，待 SDK ≥ 2.1.0 |

## 五、测试与构建

- **core**：556 全绿（+9 本轮 + 此前 547）
- **starter-webmvc**：9 全绿
- **mvn clean install**：全模块 BUILD SUCCESS（fork=true 已生效）

## 六、建议下一步（自决 · 不强推）

按 ROI 排序：

1. **`server2mcp-test` 真实 demo 集成**：把所有 endpoint 串成一个 `mvn spring-boot:run` 能直接 curl 看到的演示应用。董事会验收用。
2. **OTel trace forwarding 端到端测试**：用 OpenTelemetry SDK 测试 agent，验证 `_meta.traceparent` 链路真正贯穿。
3. **MRTR Driver JSON Schema 校验**：用 `networknt/json-schema-validator`（已在 classpath）校验 `inputRequests[].schema` 合法性。

待董事长指示。

---

> **维护者**：api2mcp4j Team · 本日志遵循 `docs/specs/WORK_LOG_SPEC.md`