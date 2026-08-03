
# api2mcp4j

**Zero-to-low code MCP integration for your Spring Boot REST APIs**  
Turn your existing Spring Boot controllers into an MCP (Model Context Protocol) server in minutes — **no @Tool annotations everywhere**, no rewriting business logic.

[![GitHub stars](https://img.shields.io/github/stars/TheEterna/api2mcp4j?style=social)](https://github.com/TheEterna/api2mcp4j)
[![License](https://img.shields.io/github/license/TheEterna/api2mcp4j)](https://github.com/TheEterna/api2mcp4j/blob/master/LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-blue)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)

## ✨ Why api2mcp4j?

Most MCP integrations require you to:
- Add `@Tool` / `@McpTool` annotations to every method
- Duplicate or heavily refactor code
- Maintain separate tool descriptions

**api2mcp4j changes that.**

It **automatically scans** your existing `@RestController` / `@Service` beans and **exposes them as MCP tools** with **minimal configuration** — leveraging parsers from:

- Swagger/OpenAPI (v2 & v3)
- Javadoc
- Spring MVC metadata
- Jackson
- Spring AI native descriptions
- Custom `@Tool` / `@McpTool` (optional)

Result: Your current REST APIs become AI-callable tools **almost for free**.

## 🔥 Key Features

- **Non-intrusive** — No need to change business code (like MyBatis-Plus enhances MyBatis)
- **Auto-discovery** — Scans controllers/services and registers methods as MCP tools
- **Multi-parser support** — Smartly combines descriptions & parameter info from Swagger, Javadoc, etc.
- **Custom annotations** — `@ToolScan`, `@ResourceScan`, `@PromptScan` for fine-grained control
- **Isolated MCP tool system** — Uses `McpTool` annotation (independent of Spring AI `@Tool`)
- **Full MCP SDK compatibility** — Supports latest MCP Java SDK features (callbacks, resources, prompts)
- **Easy debugging** — Works great with Cursor, Continue, or any MCP client

## Quick Start (≈ 5 minutes)

### 1. Add dependency

> **Note:** Project not yet in Maven Central — build from source for now.

```bash
# Clone & build
git clone https://github.com/TheEterna/api2mcp4j.git
cd api2mcp4j/server2mcp-starter-webmvc
mvn clean install
```

```xml
<!-- pom.xml -->
<dependency>
    <groupId>com.ai.plug</groupId>
    <artifactId>server2mcp-starter-webmvc</artifactId>
    <version>1.1.4-SNAPSHOT</version> <!-- or latest after build -->
</dependency>
```

### 2. Enable in application.yml

```yaml
plugin:
  mcp:
    enabled: true
    # Recommended: use multiple parsers for best description quality
    parser:
      params: SWAGGER3, SWAGGER2, SpringMVC, JACKSON, TOOL  # JAVADOC needs extra setup
      des:    SWAGGER3, SWAGGER2, JAVADOC, TOOL, JACKSON
    # 'interface' = auto-register all controller methods (skip @Deprecated)
    # 'custom'   = only register via @ToolScan / manual
    scope: interface
```

### 3. (Optional) Javadoc parser setup

To enable Javadoc parsing in production (bytecode doesn't contain comments):

```xml
<!-- pom.xml - package sources -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-resources-plugin</artifactId>
    <executions>
        <execution>
            <id>copy-java-sources</id>
            <phase>prepare-package</phase>
            <goals><goal>copy-resources</goal></goals>
            <configuration>
                <outputDirectory>${project.build.outputDirectory}</outputDirectory>
                <resources>
                    <resource>
                        <directory>src/main/java</directory>
                        <includes><include>**/*.java</include></includes>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 4. Start your app → Test

Your MCP endpoint is ready (default `/mcp` or configured via `spring.ai.mcp.*` properties).

Use any MCP client (Cursor, custom agent, etc.) to call your original REST methods as tools.

## Comparison with Alternatives

| Feature                     | api2mcp4j          | Spring AI MCP Official | Manual @Tool everywhere |
|-----------------------------|--------------------|------------------------|--------------------------|
| Code changes required       | Minimal (config only) | Medium–High           | High                    |
| Auto from existing REST     | Yes (controllers)  | No                     | No                      |
| Parser combinators          | Yes (Swagger+Javadoc+…) | Limited              | Manual                  |
| Non-intrusive               | ★★★★★              | ★★★                    | ★☆                      |
| Custom scan annotations     | Yes                | Partial                | No                      |
| Best for existing projects  | Yes                | New MCP-first apps     | Small prototypes        |

## Best Use Cases

- Expose internal management system APIs to AI agents quickly
- Build multi-agent systems by turning microservices into MCP tools
- Prototype AI features on top of production REST services
- Avoid duplicating logic between REST and MCP endpoints

## 📡 MCP Protocol 2026-07-28 Support

**本框架 100% 实装协议 2026-07-28——wire JSON 字段层 + JSON-RPC 路由层全部接管**（按董事长 2026-08-03 授权，与 SDK 2.0 record 抽象并行；SDK ≥ 3.0.0 发布后切换为 native router = 0 业务代码改动）。

### 8 项协议 RPC 全部已实装

| RPC | wire 路径 | JSON-RPC 端点 | SSE 长连接 |
|---|---|---|---|
| `server/discover` | `DiscoverEndpoint` + `WireServerCapabilities` (含 `tools.subscription` / `completions.listChanged` / `experimental.io.modelcontextprotocol/tasks`) | `POST /mcp/jsonrpc` ✅ | — |
| `tasks/create` | `TaskStore.register` | ✅ | — |
| `tasks/get` | `TasksEndpoint.handleGet` | ✅ | — |
| `tasks/list` | `TasksEndpoint.handleList` | ✅ | — |
| `tasks/cancel` | `TasksEndpoint.handleCancel` | ✅ | — |
| `tasks/augmented-prompt` | `AugmentedPromptEndpoint` (list / drain) | ✅ | — |
| `subscriptions/listen` | `NotificationsPollingEndpoint` (recordEvent + handlePoll) | ✅ (poll 模式) | ✅ `GET /mcp/sse` 真长连接，含 Last-Event-ID 断线续传 + 15s 心跳 |
| `input_required/respond` | `MrtrDriver` + `MrtrConversation` + `MrtrSafetyLimits` + `MrtrToolCallbackWrapper` | ✅ envelope | — |

### wire JSON 字段层 100% 可达

| 协议特性 | 实装状态 | 入口 |
|---|---|---|
| `tools.listChanged` / `resources.listChanged` / `prompts.listChanged` | ✅ SDK 原生 | `WireSchemaExporter.syncAll()` |
| `tools.subscription` / `completions.listChanged` (2026-07-28 新) | ✅ 自有 wire | `WireServerCapabilities` |
| `experimental.io.modelcontextprotocol/tasks` | ✅ 自有 wire | 同上 |
| `_meta.resultType` / `ttlMs` / `cacheScope` / `cacheWrapperKey` | ✅ meta map 自动注入 | `@McpTool(...)` + `McpCallToolResultConverter` |
| `_meta.taskHandle` / `_meta.inputRequests` / `_meta.requestState` | ✅ 自动识别返回值类型 | `InputRequiredResult` / `TaskHandle` |
| `_meta.traceparent` / `tracestate` / `baggage` (W3C SEP-414) | ✅ JSON-RPC 响应自动 mint | `MetaUtils.ensureTraceparent()` |
| MRTR 多轮状态机（合并 + maxRounds 护栏） | ✅ | `MrtrSessionStore` + `MrtrDriver` |
| `outputSchema` | ✅ SDK 字段层 | `McpSchema.Tool.builder().outputSchema()` |
| Capabilities 健康监控 / diff / wire 校验 | ✅ | `CapabilitiesHealth` + `SnapshotCompareTool` + `WireSchemaValidator` |

**完整字段矩阵 + 接入示例 + SDK 升级路径**：见 [`docs/mcp-2026-07-28-INTEGRATION-MATRIX.md`](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md)。

> **历史诚实声明**：截至 2026-08-03 13:16，协议 2026-07-28 的 8 项 RPC 因 Java SDK 2.0 record 抽象冻结在 2025-11-25 而不可达。董事长授权后，本框架直接接管 JSON-RPC 路由层 + SSE 长连接，绕过 SDK 限制。SDK ≥ 3.0.0 发布后，切换为 native router = 0 业务代码改动（controllers 保留作 fallback）。

## 🚀 一行 curl 验证

启动 demo 应用后：

```bash
bash scripts/verify-protocol-2026-07-28.sh http://localhost:8888
```

脚本会 curl 跑完 8 项 RPC + SSE 长连接 + MRTR envelope，全部输出协议 2026-07-28 字段。

## Documentation & Roadmap

- Full docs → [https://theeterna.github.io/server2mcp-docs/](https://theeterna.github.io/server2mcp-docs/)
- Protocol 2026-07-28 集成矩阵 → [docs/mcp-2026-07-28-INTEGRATION-MATRIX.md](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md)
- 验证脚本 → [scripts/verify-protocol-2026-07-28.sh](scripts/verify-protocol-2026-07-28.sh)
- Roadmap: Publish to Maven Central, stabilize SNAPSHOT deps, more parser plugins, SSE/Stream support

## Contributing

Issues, PRs, and stars are very welcome!  
The project is young — your feedback can shape its future.

## License

[Apache License 2.0](LICENSE)

---

**作者的话**  
这是一个我投入大量心血的个人项目，希望能真正帮助到更多 Spring Boot 开发者快速拥抱 MCP/AI Agent 时代。感谢每一位 star 和使用它的人！
```
- 语言更流畅、专业、吸引人

你可以直接发给作者，建议他 review 后合并（或作为 PR）。如果需要，我可以再调整语气、加 GIF/demo 图位置提示，或写中文版。
