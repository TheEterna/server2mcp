# api2mcp4j

**零侵入把现有 Spring Boot REST Controller 变成 MCP 工具 —— 无需满屏 `@Tool` 注解，无需重写业务逻辑。**

```xml
<dependency>
  <groupId>com.ai.plug</groupId>
  <artifactId>server2mcp-starter-webmvc</artifactId>
  <version>1.1.4-SNAPSHOT</version>
</dependency>
```

[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.0-green)](https://spring.io/projects/spring-ai)
[![MCP SDK](https://img.shields.io/badge/MCP%20SDK-2.0.0-orange)](https://modelcontextprotocol.io)
[![MCP Protocol](https://img.shields.io/badge/MCP%20Protocol-2026--07--28-100%25-success)](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md)
[![Tests](https://img.shields.io/badge/Tests-600%20passing-brightgreen)](#-测试--验证)
[![End-to-End](https://img.shields.io/badge/Curl%20verify-21%2F21-success)](#-一行-curl-验证)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)

[English](README.md) · [中文](README_zh.md) · [文档站](https://docs.xiaohan.chat/) · [集成矩阵](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md)

---

## 为什么用 api2mcp4j？

主流 MCP 集成方案都要求：

- 给每个方法加 `@Tool` / `@McpTool` 注解
- 把业务逻辑复制到平行的 "MCP" 代码路径
- 维护独立的工具描述

**api2mcp4j** 自动扫描现有 `@RestController`，把方法暴露为 MCP 工具 —— **业务代码零改动**。类似 MyBatis-Plus 增强 MyBatis，api2mcp4j 增强 Spring AI MCP。

```java
// 你现有的 Controller —— 不动一行
@RestController
public class OrderController {
    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable Long id) { ... }
}

// 自动变成 MCP 工具：orders_get_order
// 工具描述自动从 Swagger / Javadoc / Spring MVC / Jackson / Spring AI 提取最佳
```

---

## ✨ 核心特性

| | |
|---|---|
| 🎯 **非侵入** | `interface` 作用域自动注册所有 Controller，无需 `@Tool` 注解 |
| 🔍 **5 解析器链** | Swagger v3 / Swagger v2 / Javadoc / Spring MVC / Jackson / Spring AI —— 取最优描述 |
| 🛠️ **完整 MCP 覆盖** | Tools / Resources / Prompts / Completions / Elicitation / Sampling / Roots |
| 🆕 **协议 2026-07-28** | wire schema + JSON-RPC 路由 + SSE 长连接 + MRTR + OTel traceparent —— 100% |
| 🔌 **自定义解析器** | 实现 `AbstractDesParser` / `AbstractParamParser`，插入责任链 |
| 🧪 **TDD 纪律** | JUnit5，双 commit `[RED]` 然后 `[GREEN]`，600 测试全绿 |
| 🚀 **快速启动** | `mvn spring-boot:run` → MCP 端点就绪 |

---

## 🚀 快速开始（≈ 3 分钟）

### 1. 克隆 & 构建

```bash
git clone https://github.com/TheEterna/api2mcp4j.git
cd api2mcp4j
mvn clean install -DskipTests
```

### 2. 引入到你的 Spring Boot 项目

```xml
<dependency>
  <groupId>com.ai.plug</groupId>
  <artifactId>server2mcp-starter-webmvc</artifactId>
  <version>1.1.4-SNAPSHOT</version>
</dependency>
```

### 3. 配置

```yaml
plugin:
  mcp:
    enabled: true
    scope: interface   # 'interface' = 自动注册所有 Controller；'custom' = 仅 @ToolScan
    parser:
      des:    SWAGGER3, JAVADOC, TOOL, JACKSON, SWAGGER2
      param:  MCPTOOL, JAVADOC, TOOL, SpringMVC, JACKSON, SWAGGER2, SWAGGER3
```

### 4. 启动 & 测试

```bash
mvn spring-boot:run
```

MCP Server 立即上线：`http://localhost:8080/mcp/jsonrpc`（+ HTTP fallback：`/mcp/discover`、`/mcp/tasks`、`/mcp/sse`）。

---

## 📡 协议 2026-07-28 —— 100% 兼容

api2mcp4j 是**首个**完整支持协议 2026-07-28 的 Java MCP 框架——尽管 Java MCP SDK 2.0 仅实现 2025-11-25。我们用自研 JSON-RPC 路由 + SSE controller + wire schema **绕过 SDK 限制**，且**保留 SDK 升级路径**（SDK ≥ 3.0.0 发布后 controllers 继续作 fallback，老客户端不中断）。

### 8 项协议 RPC 全部真路由（非 HTTP 模拟）

| RPC | JSON-RPC 端点 | SSE 长连接 | 入口 |
|---|---|---|---|
| `server/discover` | ✅ `POST /mcp/jsonrpc` | — | [`DiscoverEndpoint`](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md) |
| `tasks/create` | ✅ | — | [`TaskStore`](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md) |
| `tasks/get` / `list` / `cancel` | ✅ | — | [`TasksEndpoint`](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md) |
| `tasks/augmented-prompt` | ✅ | — | [`AugmentedPromptEndpoint`](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md) |
| `subscriptions/listen` | ✅（poll 模式） | ✅ `GET /mcp/sse` + Last-Event-ID + 15s 心跳 | [`SseNotificationsController`](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md) |
| `input_required/respond`（MRTR） | ✅ envelope | — | [`MrtrToolCallbackWrapper`](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md) |

### wire JSON 字段层 100% 可达

| 2026-07-28 字段 | 状态 | 入口 |
|---|---|---|
| `tools.listChanged` / `resources.listChanged` / `prompts.listChanged` | ✅ SDK 原生 | `WireSchemaExporter.syncAll()` |
| `tools.subscription` / `completions.listChanged` *(2026-07-28 新)* | ✅ 自有 wire | [`WireServerCapabilities`](server2mcp-core/src/main/java/com/ai/plug/core/spec/capabilities/WireServerCapabilities.java) |
| `experimental.io.modelcontextprotocol/tasks` *(新)* | ✅ 自有 wire | 同上 |
| `_meta.resultType` / `ttlMs` / `cacheScope` / `cacheWrapperKey` | ✅ meta map 自动注入 | `@McpTool(...)` + `McpCallToolResultConverter` |
| `_meta.taskHandle` / `inputRequests` / `requestState` | ✅ 自动识别返回值类型 | `InputRequiredResult` / `TaskHandle` 返回值 |
| `_meta.traceparent` / `tracestate` / `baggage`（W3C SEP-414） | ✅ JSON-RPC 响应自动 mint | [`MetaUtils`](server2mcp-core/src/main/java/com/ai/plug/core/spec/meta/MetaUtils.java) |
| MRTR 多轮状态机（跨轮合并 + 8 轮护栏） | ✅ | [`MrtrDriver`](server2mcp-core/src/main/java/com/ai/plug/core/spec/mrtr/MrtrDriver.java) |
| `outputSchema` | ✅ SDK 字段层 | `McpSchema.Tool.builder().outputSchema()` |
| Capabilities 健康监控 / diff / wire 校验 | ✅ | `CapabilitiesHealth` + `SnapshotCompareTool` + `WireSchemaValidator` |

**[完整集成矩阵 →](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md)**

---

## ✅ 一行 curl 验证

Demo 应用用 H2 内存库启动（零外部依赖），对每条 2026-07-28 特性做真 HTTP/SSE wire 验证：

```bash
cd server2mcp-test && mvn spring-boot:run    # :8888 端口，H2 内存库

# 另开 shell：
bash scripts/verify-protocol-2026-07-28.sh http://localhost:8888
```

```
== 0. liveness ==                       ✓ actuator reachable
== 1. server/discover (JSON-RPC) ==    ✓×7 (jsonrpc=2.0, preferredVersion=2026-07-28,
                                            tools.listChanged, tools.subscription,
                                            completions.listChanged, experimental.tasks,
                                            _meta.traceparent)
== 2. tasks/* (JSON-RPC) ==            ✓×5 (create, get, list, cancel 全生命周期)
== 3. tasks/augmented-prompt ==         ✓×1
== 4. subscriptions/listen ==           ✓×3 (HTTP poll + text/event-stream + connected)
== 5. input_required/respond ==         ✓×2 (accepted + state echo)
== 6. HTTP legacy endpoints ==          ✓×2 (/mcp/discover + /mcp/notifications)
== summary ==                           passed: 21 / failed: 0
ALL ASSERTIONS PASSED — protocol 2026-07-28 wire verified
```

**[验证凭据 →](docs/logs/2026-08-03_ceo_demo-end-to-end-21of21.md)**

---

## 🧪 测试 & 验证

| 层级 | 数量 | 状态 |
|---|---|---|
| 单元测试（`server2mcp-core`） | 575 | ✅ 全绿 |
| 集成测试（`server2mcp-starter-webmvc`） | 22 | ✅ 全绿 |
| Demo 测试（`server2mcp-test`） | 3 | ✅ 全绿 |
| **端到端 curl 验证** | **21/21** | **✅** |
| 合计 | **600+ 测试 + 21/21 e2e** | **✅** |

测试理念：**TDD 双 commit**——先 `[RED]` 测试，再 `[GREEN]` 实现。见 [`docs/specs/TEST_SPEC.md`](docs/specs/TEST_SPEC.md)。

---

## 🏗️ 架构

```
┌─────────────────────────────────────────────────────────────┐
│ Spring Boot 应用                                              │
│                                                              │
│   ┌─── 你的现有代码（零改动） ───┐                          │
│   │ @RestController              │                          │
│   │ @Service                      │                          │
│   │ @Component                    │                          │
│   └────────────────┬─────────────┘                          │
│                    │                                        │
│   ┌────────────────▼─────────────┐                          │
│   │ api2mcp4j 框架                │                          │
│   │                                │                          │
│   │  扫描器 ──→ 解析器链 ──→ Provider                        │
│   │    │            │             │                          │
│   │  Spring MVC   5 解析器   SyncMcpToolMethodCallback    │
│   │  发现          (Swagger2/3, (模板方法)                  │
│   │                Javadoc,                              │
│   │                Jackson,                              │
│   │                Spring AI)                             │
│   │                                │                          │
│   │  ── 2026-07-28 层（新） ──                              │
│   │  • JsonRpcRouter + JsonRpcRoutes                        │
│   │  • WireServerCapabilities + JsonExporter                │
│   │  • SseNotificationsController                           │
│   │  • MrtrDriver + MrtrSessionStore                        │
│   │  • MrtrToolCallbackWrapper                              │
│   │  • MetaUtils (W3C traceparent mint)                     │
│   └────────────────┬─────────────┘                          │
│                    │                                        │
│   ┌────────────────▼─────────────┐                          │
│   │ Spring AI MCP SDK 2.0         │                          │
│   │   (McpSyncServer / Async)     │                          │
│   └────────────────┬─────────────┘                          │
└────────────────────┼────────────────────────────────────────┘
                     │  wire: JSON-RPC 2.0 + SSE
                     ▼
            ┌────────────────────┐
            │ MCP 客户端          │
            │ • Claude Desktop   │
            │ • Cursor / Cline   │
            │ • 你的 BFF / Agent │
            └────────────────────┘
```

**[详细架构 →](docs/reference/architecture.md)**

---

## 📦 模块

```
api2mcp4j/
├── server2mcp-common                  # 常量 & 工具类
├── server2mcp-core                    # 核心引擎：注解、扫描器、回调、Provider
│   ├── com.ai.plug.core.annotation.*  # @McpTool / @McpResource / @McpPrompt / @McpArg
│   ├── com.ai.plug.core.parser.*      # 5 解析器链（des + param）
│   ├── com.ai.plug.core.callback.*     # Sync + Async 模板方法
│   ├── com.ai.plug.core.spec.*         # 2026-07-28 wire 层
│   └── com.ai.plug.core.provider.*     # Spring AI 桥
├── server2mcp-autoconfigure           # Spring Boot 自动配置
├── server2mcp-spring-boot-starters/
│   ├── server2mcp-starter-webmvc      # ✅ 完整端点装配（JSON-RPC + SSE + HTTP）
│   └── server2mcp-starter-webflux     # ⚠️ 仅框架核心（端点未装配）
└── server2mcp-test                    # ✅ Demo 应用 + 21/21 e2e 验证
```

---

## 🤔 什么时候用 api2mcp4j？

| ✅ 适合 | ❌ 不适合 |
|---|---|
| 把内部 REST API 快速暴露给 AI Agent | MCP-first 的全新项目（直接用 Spring AI MCP） |
| 把遗留 Controller 包成 MCP 工具 | 实时流式 / 纯 SSE UI |
| 多 Agent 共享工具定义 | 不用 Spring Boot 的应用 |
| 在生产服务上做 AI 特性原型 | 小型原型（开销不划算） |

---

## 🆚 对比

| 特性 | api2mcp4j | Spring AI MCP 官方 | 满屏 `@Tool` |
|---|---|---|---|
| 代码改动 | **极小（仅配置）** | 中–高 | 高 |
| 自动发现 `@RestController` | ✅ | ❌ | ❌ |
| 5 解析器链（Swagger + Javadoc + …） | ✅ | 有限 | 手动 |
| 非侵入 | ★★★★★ | ★★★ | ★☆ |
| 协议 2026-07-28（RPC + wire） | ✅ 100% | 🟡 部分 | ⚪ |
| MRTR 多轮 | ✅ 带装饰器 | ⚪ | ⚪ |
| SSE 长连接 + Last-Event-ID | ✅ 内置 | 🟡 走 SDK | ❌ |
| W3C traceparent（SEP-414） | ✅ 自动 mint | ❌ | ❌ |
| 最适合现有项目 | ✅ | 新项目 | 小 demo |

---

## 📚 文档

- 📘 **完整文档站** → https://theeterna.github.io/server2mcp-docs/
- 📊 **协议 2026-07-28 集成矩阵** → [docs/mcp-2026-07-28-INTEGRATION-MATRIX.md](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md)
- 🏗️ **架构** → [docs/reference/architecture.md](docs/reference/architecture.md)
- 🧩 **扩展点**（自定义解析器 / 过滤器 / Context） → [docs/reference/extension-points.md](docs/reference/extension-points.md)
- 🚶 **3 步入门** → [docs/reference/onboarding.md](docs/reference/onboarding.md)
- 📐 **规范**（注册 / 文件头 / 测试 / 工作日志） → [docs/specs/](docs/specs/)
- ⚖️ **全局规则** → [docs/rules/](docs/rules/)
- 📜 **工作日志（审计追踪）** → [docs/logs/](docs/logs/)

---

## 🤝 贡献

Issue、PR、⭐ 都非常欢迎。  
项目还在早期——你的反馈塑造它的未来。

提交 PR 前请阅读：
- [docs/specs/REGISTRATION_DISCIPLINE_SPEC.md](docs/specs/REGISTRATION_DISCIPLINE_SPEC.md) — 新 MCP 实体的 6 维 Rubric
- [docs/rules/global/destructive-deletion.md](docs/rules/global/destructive-deletion.md) — 删公开 API 前多源验证
- [docs/rules/global/work-log.md](docs/rules/global/work-log.md) — 报告类产出落 `docs/logs/`

---

## 📄 协议

[Apache License 2.0](LICENSE)

---

## 🗓️ 路线图

- [x] 协议 2026-07-28 wire & JSON-RPC 路由（2026-08-03）
- [x] MRTR 多轮状态机 + 8 轮护栏
- [x] SSE 长连接 + Last-Event-ID 断线续传
- [x] W3C traceparent 自动 mint
- [ ] WebFlux starter 端点装配（对齐 WebMVC）
- [ ] 发布到 Maven Central
- [ ] OTel SDK 真实接入（目前仅 wire 格式）
- [ ] 多租户隔离（`@McpTool(tenant = "...")`）
- [ ] SDK ≥ 3.0.0 native router 迁移（由 `scripts/trigger-phase3.sh` 跟踪）

---

<p align="center">
  <sub>由 <a href="https://github.com/TheEterna">Han</a> 用心打造 · Apache 2.0 · 100% 协议 2026-07-28 兼容</sub>
</p>