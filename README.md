# api2mcp4j

[![MCP Toplist](https://mcptoplist.com/badge/mcp.so%2Fserver2mcp%2FTheEterna.svg)](https://mcptoplist.com/server/mcp.so%2Fserver2mcp%2FTheEterna)

**Turn your existing Spring Boot REST controllers into MCP (Model Context Protocol) tools — no rewrites, no `@Tool` everywhere.**

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
[![Tests](https://img.shields.io/badge/Tests-600%20passing-brightgreen)](#-testing--verification)
[![End-to-End](https://img.shields.io/badge/Curl%20verify-21%2F21-success)](#-one-line-verification)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue)](LICENSE)

[English](README.md) · [中文](README_zh.md) · [Docs](https://docs.xiaohan.chat/) · [Integration Matrix](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md)

---

## Why api2mcp4j?

Most MCP integrations force you to:

- Add `@Tool` / `@McpTool` to every method
- Duplicate business logic into a parallel "MCP" code path
- Maintain separate tool descriptions

**api2mcp4j** scans your existing `@RestController` beans and exposes their methods as MCP tools — **zero changes to business code**. Like MyBatis-Plus enhances MyBatis, api2mcp4j enhances Spring AI MCP.

```java
// Your existing controller — unchanged
@RestController
public class OrderController {
    @GetMapping("/orders/{id}")
    public Order getOrder(@PathVariable Long id) { ... }
}

// Becomes an MCP tool: orders_get_order
// With auto-generated description from Swagger / Javadoc / Spring MVC / Jackson / Spring AI
```

---

## ✨ Key Features

| | |
|---|---|
| 🎯 **Non-intrusive** | `interface` scope auto-registers all controllers; no `@Tool` annotation needed |
| 🔍 **5-parser chain** | Swagger v3 / Swagger v2 / Javadoc / Spring MVC / Jackson / Spring AI — best description wins |
| 🛠️ **Full MCP coverage** | Tools / Resources / Prompts / Completions / Elicitation / Sampling / Roots |
| 🆕 **Protocol 2026-07-28** | Wire schema + JSON-RPC routing + SSE long-poll + MRTR + OTel traceparent — 100% |
| 🔌 **Custom parsers** | Implement `AbstractDesParser` / `AbstractParamParser`, plug into the chain |
| 🧪 **TDD discipline** | JUnit5, double commit `[RED]` then `[GREEN]`, 600 tests all green |
| 🚀 **Quick start** | `mvn spring-boot:run` → MCP endpoint ready |

---

## 🚀 Quick Start (≈ 3 minutes)

### 1. Clone & build

```bash
git clone https://github.com/TheEterna/api2mcp4j.git
cd api2mcp4j
mvn clean install -DskipTests
```

### 2. Add to your Spring Boot project

```xml
<dependency>
  <groupId>com.ai.plug</groupId>
  <artifactId>server2mcp-starter-webmvc</artifactId>
  <version>1.1.4-SNAPSHOT</version>
</dependency>
```

### 3. Configure

```yaml
plugin:
  mcp:
    enabled: true
    scope: interface   # 'interface' = auto-register all controllers; 'custom' = @ToolScan only
    parser:
      des:    SWAGGER3, JAVADOC, TOOL, JACKSON, SWAGGER2
      param:  MCPTOOL, JAVADOC, TOOL, SpringMVC, JACKSON, SWAGGER2, SWAGGER3
```

### 4. Start & test

```bash
mvn spring-boot:run
```

Your MCP server is live on `http://localhost:8080/mcp/jsonrpc` (and HTTP fallbacks on `/mcp/discover`, `/mcp/tasks`, `/mcp/sse`).

---

## 📡 MCP Protocol 2026-07-28 — 100% Compatible

api2mcp4j is **the first Java MCP framework to ship full 2026-07-28 support**, even though Java MCP SDK 2.0 only implements the 2025-11-25 wire. We bypassed SDK limitations with a custom JSON-RPC router, SSE controller, and wire schema — **all without breaking SDK upgrade compatibility** (controllers stay as fallbacks when SDK ≥ 3.0.0 lands).

### 8 RPC routes, all real (not HTTP simulations)

| RPC | JSON-RPC endpoint | SSE long-poll | Source |
|---|---|---|---|
| `server/discover` | ✅ `POST /mcp/jsonrpc` | — | [`DiscoverEndpoint`](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md#二servercapabilities握手声明) |
| `tasks/create` | ✅ | — | [`TaskStore`](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md#五新增-rpc-方法) |
| `tasks/get` / `list` / `cancel` | ✅ | — | [`TasksEndpoint`](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md#五新增-rpc-方法) |
| `tasks/augmented-prompt` | ✅ | — | [`AugmentedPromptEndpoint`](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md#五新增-rpc-方法) |
| `subscriptions/listen` | ✅ (poll) | ✅ `GET /mcp/sse` + Last-Event-ID + 15s heartbeat | [`SseNotificationsController`](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md#五新增-rpc-方法) |
| `input_required/respond` (MRTR) | ✅ envelope | — | [`MrtrToolCallbackWrapper`](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md#五新增-rpc-方法) |

### Wire JSON fields — 100% reachable

| 2026-07-28 field | Status | Where |
|---|---|---|
| `tools.listChanged` / `resources.listChanged` / `prompts.listChanged` | ✅ SDK native | `WireSchemaExporter.syncAll()` |
| `tools.subscription` / `completions.listChanged` *(new in 2026-07-28)* | ✅ Custom wire | [`WireServerCapabilities`](server2mcp-core/src/main/java/com/ai/plug/core/spec/capabilities/WireServerCapabilities.java) |
| `experimental.io.modelcontextprotocol/tasks` *(new)* | ✅ Custom wire | Same |
| `_meta.resultType` / `ttlMs` / `cacheScope` / `cacheWrapperKey` | ✅ Auto-injected via meta map | `@McpTool(...)` + `McpCallToolResultConverter` |
| `_meta.taskHandle` / `inputRequests` / `requestState` | ✅ Auto-recognized | `InputRequiredResult` / `TaskHandle` return values |
| `_meta.traceparent` / `tracestate` / `baggage` (W3C SEP-414) | ✅ Auto-minted in JSON-RPC | [`MetaUtils`](server2mcp-core/src/main/java/com/ai/plug/core/spec/meta/MetaUtils.java) |
| MRTR state machine (cross-round merge + 8-round guard) | ✅ | [`MrtrDriver`](server2mcp-core/src/main/java/com/ai/plug/core/spec/mrtr/MrtrDriver.java) |
| `outputSchema` | ✅ SDK field | `McpSchema.Tool.builder().outputSchema()` |
| Capabilities health / diff / wire validation | ✅ | `CapabilitiesHealth` + `SnapshotCompareTool` + `WireSchemaValidator` |

**[Full integration matrix →](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md)**

---

## ✅ One-line verification

The demo app boots with H2 in-memory DB (zero external dependencies) and validates every 2026-07-28 feature against a real HTTP/SSE wire:

```bash
cd server2mcp-test && mvn spring-boot:run    # starts on :8888 (H2 in-memory)

# In another shell:
bash scripts/verify-protocol-2026-07-28.sh http://localhost:8888
```

```
== 0. liveness ==                       ✓ actuator reachable
== 1. server/discover (JSON-RPC) ==    ✓×7 (jsonrpc=2.0, preferredVersion=2026-07-28,
                                            tools.listChanged, tools.subscription,
                                            completions.listChanged, experimental.tasks,
                                            _meta.traceparent)
== 2. tasks/* (JSON-RPC) ==            ✓×5 (create, get, list, cancel full lifecycle)
== 3. tasks/augmented-prompt ==         ✓×1
== 4. subscriptions/listen ==           ✓×3 (HTTP poll + text/event-stream + connected)
== 5. input_required/respond ==         ✓×2 (accepted + state echo)
== 6. HTTP legacy endpoints ==          ✓×2 (/mcp/discover + /mcp/notifications)
== summary ==                           passed: 21 / failed: 0
ALL ASSERTIONS PASSED — protocol 2026-07-28 wire verified
```

**[Evidence log →](docs/logs/2026-08-03_ceo_demo-end-to-end-21of21.md)**

---

## 🧪 Testing & Verification

| Layer | Count | Status |
|---|---|---|
| Unit tests (`server2mcp-core`) | 575 | ✅ all green |
| Integration tests (`server2mcp-starter-webmvc`) | 22 | ✅ all green |
| Demo tests (`server2mcp-test`) | 3 | ✅ all green |
| **End-to-end curl verification** | **21/21** | **✅** |
| Total | **600+ tests, 21/21 e2e** | **✅** |

Test philosophy: **TDD double-commit** — `[RED]` test first, then `[GREEN]` implementation. See [`docs/specs/TEST_SPEC.md`](docs/specs/TEST_SPEC.md).

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│ Spring Boot Application                                     │
│                                                              │
│   ┌─── Your existing code (unchanged) ───┐                  │
│   │ @RestController                       │                  │
│   │ @Service                              │                  │
│   │ @Component                            │                  │
│   └────────────────┬─────────────────────┘                  │
│                    │                                        │
│   ┌────────────────▼─────────────────────┐                  │
│   │ api2mcp4j framework                   │                  │
│   │                                       │                  │
│   │  Scanner ──→ Parser chain ──→ Provider                 │
│   │    │            │              │                       │
│   │  Spring MVC   5 parsers    SyncMcpToolMethodCallback   │
│   │  discovery    (Swagger2/3,    (template method)        │
│   │                Javadoc,                              │
│   │                Jackson,                              │
│   │                Spring AI)                             │
│   │                                       │                  │
│   │  ── 2026-07-28 layer (new) ──                          │
│   │  • JsonRpcRouter + JsonRpcRoutes                       │
│   │  • WireServerCapabilities + JsonExporter               │
│   │  • SseNotificationsController                          │
│   │  • MrtrDriver + MrtrSessionStore                       │
│   │  • MrtrToolCallbackWrapper                             │
│   │  • MetaUtils (W3C traceparent mint)                    │
│   └────────────────┬─────────────────────┘                  │
│                    │                                        │
│   ┌────────────────▼─────────────────────┐                  │
│   │ Spring AI MCP SDK 2.0                 │                  │
│   │   (McpSyncServer / McpAsyncServer)    │                  │
│   └────────────────┬─────────────────────┘                  │
└────────────────────┼────────────────────────────────────────┘
                     │  wire: JSON-RPC 2.0 + SSE
                     ▼
            ┌────────────────────┐
            │ MCP Clients        │
            │ • Claude Desktop   │
            │ • Cursor / Cline   │
            │ • Your BFF / Agent │
            └────────────────────┘
```

**[Detailed architecture →](docs/reference/architecture.md)**

---

## 📦 Modules

```
api2mcp4j/
├── server2mcp-common                  # Constants & utilities
├── server2mcp-core                    # Core engine: annotations, scanners, callbacks, providers
│   ├── com.ai.plug.core.annotation.*  # @McpTool, @McpResource, @McpPrompt, @McpArg
│   ├── com.ai.plug.core.parser.*      # 5-parser chain (des + param)
│   ├── com.ai.plug.core.callback.*     # Sync + Async template methods
│   ├── com.ai.plug.core.spec.*         # 2026-07-28 wire layer
│   └── com.ai.plug.core.provider.*     # Spring AI bridge
├── server2mcp-autoconfigure           # Spring Boot auto-configuration
├── server2mcp-spring-boot-starters/
│   ├── server2mcp-starter-webmvc      # ✅ Full endpoint wiring (JSON-RPC + SSE + HTTP)
│   └── server2mcp-starter-webflux     # ⚠️ Framework core only (no endpoint wiring yet)
└── server2mcp-test                    # ✅ Demo app + 21/21 e2e verification
```

---

## 🤔 When to use api2mcp4j?

| ✅ Use it for | ❌ Don't use it for |
|---|---|
| Exposing internal REST APIs to AI agents quickly | Greenfield MCP-first projects (use Spring AI MCP directly) |
| Wrapping legacy controllers as MCP tools | Real-time streaming / SSE-only UIs |
| Multi-agent systems sharing tool definitions | Apps that don't already use Spring Boot |
| Prototyping AI features on production services | Tiny prototypes (overhead not worth it) |

---

## 🆚 Comparison

| Feature | api2mcp4j | Spring AI MCP Official | Manual `@Tool` everywhere |
|---|---|---|---|
| Code changes required | **Minimal (config only)** | Medium–High | High |
| Auto-discover from `@RestController` | ✅ | ❌ | ❌ |
| 5-parser chain (Swagger + Javadoc + …) | ✅ | Limited | Manual |
| Non-intrusive | ★★★★★ | ★★★ | ★☆ |
| Protocol 2026-07-28 (RPC + wire) | ✅ 100% | 🟡 partial | ⚪ |
| MRTR multi-round | ✅ with decorator | ⚪ | ⚪ |
| SSE long-poll + Last-Event-ID | ✅ built-in | 🟡 via SDK | ❌ |
| W3C traceparent (SEP-414) | ✅ auto-minted | ❌ | ❌ |
| Best for existing projects | ✅ | New apps | Tiny demos |

---

## 📚 Documentation

- 📘 **Full docs site** → https://theeterna.github.io/server2mcp-docs/
- 📊 **Protocol 2026-07-28 integration matrix** → [docs/mcp-2026-07-28-INTEGRATION-MATRIX.md](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md)
- 🏗️ **Architecture** → [docs/reference/architecture.md](docs/reference/architecture.md)
- 🧩 **Extension points** (custom parsers / filters / context) → [docs/reference/extension-points.md](docs/reference/extension-points.md)
- 🚶 **Onboarding (3-step)** → [docs/reference/onboarding.md](docs/reference/onboarding.md)
- 📐 **Specs (registration / file header / test / work log)** → [docs/specs/](docs/specs/)
- ⚖️ **Global rules** → [docs/rules/](docs/rules/)
- 📜 **Work logs (audit trail)** → [docs/logs/](docs/logs/)

---

## 🤝 Contributing

Issues, PRs, and ⭐ are very welcome.  
This is a young project — your feedback shapes its future.

Before submitting a PR, please read:
- [docs/specs/REGISTRATION_DISCIPLINE_SPEC.md](docs/specs/REGISTRATION_DISCIPLINE_SPEC.md) — the 6-dimension Rubric for new MCP entities
- [docs/rules/global/destructive-deletion.md](docs/rules/global/destructive-deletion.md) — multi-source verification before deleting public APIs
- [docs/rules/global/work-log.md](docs/rules/global/work-log.md) — reportable outputs land in `docs/logs/`

---

## 📄 License

[Apache License 2.0](LICENSE)

---

## 🗓️ Roadmap

- [x] Protocol 2026-07-28 wire & JSON-RPC routing (2026-08-03)
- [x] MRTR multi-round state machine + 8-round guard
- [x] SSE long-poll with Last-Event-ID resume
- [x] W3C traceparent auto-mint
- [ ] WebFlux starter endpoint wiring (parity with WebMVC)
- [ ] Publish to Maven Central
- [ ] OTel SDK real instrumentation (currently wire-format only)
- [ ] Multi-tenant isolation (`@McpTool(tenant = "...")`)
- [ ] SDK ≥ 3.0.0 native router migration (tracked by `scripts/trigger-phase3.sh`)

---

<p align="center">
  <sub>Built with care by <a href="https://github.com/TheEterna">Han</a> · Apache 2.0 · 100% protocol 2026-07-28 compatible</sub>
</p>
