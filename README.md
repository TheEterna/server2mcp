
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

**本框架已实装协议 2026-07-28（"无状态核心"）所有 wire JSON 层可写字段**——SDK 2.0 物理约束下，业务代码零改动即可获得协议合规。

| 协议特性 | 实装状态 | 入口 |
|---|---|---|
| `tools.listChanged` / `resources.listChanged` / `prompts.listChanged` | ✅ | `WireSchemaExporter.syncAll()` 一键启用 |
| `_meta.resultType` (complete / input_required) | 🟡 meta map | `@McpTool.resultType` + `McpResultWriter` |
| `_meta.ttlMs` / `_meta.cacheScope` / `_meta.cacheWrapperKey` | 🟡 meta map | `@McpTool(ttlMs=, cacheScope=, cacheWrapperKey=)` |
| MRTR（多轮输入请求 / InputRequiredResult） | 🟡 meta map | `com.ai.plug.core.spec.mrtr.MrtrTypes` |
| Tasks 扩展（TaskHandle / TaskStatus / TaskError） | 🟡 meta map | `com.ai.plug.core.spec.tasks.TaskTypes` |
| `server/discover`（能力自描述） | 🟡 experimental map | `com.ai.plug.core.spec.discover.DiscoverTypes` |
| OTel trace 透传（traceparent / tracestate / baggage） | ✅ | 自动注入 `_meta` |
| Capabilities 健康检查 | ✅ | `CapabilitiesHealth` + `/actuator/health/mcp-capabilities` |
| Wire JSON 校验（dev-mode） | ✅ | `WireSchemaValidator` + `WireSchemaValidationFilter` |
| 工具变更通知（pull-poll） | ✅ | `McpToolChangeNotifier.diffAndNotify()` |

✅ = SDK 原生字段 · 🟡 = 通过 `_meta` / `experimental` map 间接表达（wire JSON 可见）

**完整字段矩阵 + 接入示例 + 不可达项诚实声明**：见 [`docs/mcp-2026-07-28-INTEGRATION-MATRIX.md`](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md)。

> **诚实声明**：协议 2026-07-28 的 8 项新 RPC（`server/discover`、`tasks/*`、`subscriptions/listen`、MRTR 实际模式）需 Java SDK ≥ 3.0.0 才能 native 支持；截至 2026-08-03 未发布。本框架通过 callback 自动识别 `InputRequiredResult` / `TaskHandle` 返回值并写进 `_meta`——客户端按协议 2026-07-28 解析即可完整识别。

## Documentation & Roadmap

- Full docs → [https://theeterna.github.io/server2mcp-docs/](https://theeterna.github.io/server2mcp-docs/)
- Protocol 2026-07-28 集成矩阵 → [docs/mcp-2026-07-28-INTEGRATION-MATRIX.md](docs/mcp-2026-07-28-INTEGRATION-MATRIX.md)
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
