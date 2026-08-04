> 维护者：api2mcp4j Team · 创建：2026-06-24（继承 ../real-agent 文档体系心法）
> 📖 参考文档 — 系统架构总览，按需查阅。事实源：本项目 `CLAUDE.md` + `server2mcp-core` 真实代码（已逐类核对）。

# 系统架构总览

## 一、项目本质

**api2mcp4j**（内部代号 server2mcp）是一个 **Spring Boot Starter 框架**，把现有的 Spring `@RestController` 接口**零改动**地暴露为 MCP（Model Context Protocol）的 **Tool / Resource / Prompt / Complete**。

设计哲学一句话：**非侵入、纯增强**——就像 MyBatis-Plus 之于 MyBatis。

- `interface` 作用域：现有 `@RestController` 方法**无需任何改动**即成为 MCP 工具。
- `custom` 作用域：用专用注解（`@McpTool`、`@McpResource`、`@McpPrompt`、`@McpComplete`）+ 扫描注解（`@ToolScan` 等）显式声明。

---

## 二、模块依赖流向

```
server2mcp-parent (根 pom, v1.1.4-SNAPSHOT)
│
├── server2mcp-common          常量、工具类（ConvertUtil / JacksonUtils / GenSchemaUtils / JsonParser）
│        ▲
├── server2mcp-core            核心引擎：注解 / 解析器 / 扫描器 / 回调 / Context / Provider
│        ▲
├── server2mcp-autoconfigure   Spring Boot 自动配置（Server2McpAutoConfiguration / McpConfig / PluginProperties / Conditions）
│        ▲
├── server2mcp-spring-boot-starters/
│   ├── server2mcp-starter-webmvc     Spring MVC 应用 Starter
│   └── server2mcp-starter-webflux    WebFlux 应用 Starter
│
└── server2mcp-test            演示应用（⚠️ 不在根 pom modules 中，需单独构建）
```

**依赖流向（单向，不可逆）**：`common ← core ← autoconfigure ← starters`

> 包根路径统一为 `com.ai.plug`：common 在 `.common`，core 在 `.core`，autoconfig 在 `.autoconfigure`。

---

## 三、六大核心设计模式（落到真实类名）

### 模式 1 · 注解驱动注册（ImportBeanDefinitionRegistrar）

`@ToolScan` 标注后，链路如下：

```
@ToolScan                        com.ai.plug.core.annotation.ToolScan
   │  @Import(McpToolScanRegistrar.class)
   ▼
McpToolScanRegistrar             implements ImportBeanDefinitionRegistrar, BeanFactoryAware
   │  registerBeanDefinitions() → 注册 McpToolScanConfigurer 的 BeanDefinition
   ▼
McpToolScanConfigurer            implements BeanDefinitionRegistryPostProcessor, BeanFactoryPostProcessor
   │  postProcessBeanDefinitionRegistry() → 创建并驱动扫描器
   ▼
ClassPathToolScanner             extends ClassPathBeanDefinitionScanner
   │  scan() → 命中类交给内部命名生成器
   ▼
ToolBeanNameGenerator (内部类)    → 调用 IToolContext.addTool(name, ToolRegisterDefinition)
```

> ⚠️ **与 CLAUDE.md 链路图的偏差（已核实）**：CLAUDE.md 写 `McpToolScanConfigurer (InitializingBean)`，但代码实际实现的是 `BeanDefinitionRegistryPostProcessor` + `BeanFactoryPostProcessor`，扫描动作发生在 `postProcessBeanDefinitionRegistry()` 而非 `afterPropertiesSet()`。本文档以代码为准。
> 来源：`server2mcp-core/.../register/tool/McpToolScanConfigurer.java`

`@McpResourceScan`、`@McpPromptScan`、`@McpCompleteScan` 遵循**完全相同**的四段式：

| 实体 | 注解 | Registrar | Configurer | Scanner |
|---|---|---|---|---|
| Tool | `@ToolScan` | `McpToolScanRegistrar` | `McpToolScanConfigurer` | `ClassPathToolScanner` |
| Resource | `@McpResourceScan` | `McpResourceScanRegistrar` | `McpResourceScanConfigurer` | `ClassPathResourceScanner` |
| Prompt | `@McpPromptScan` | `McpPromptScanRegistrar` | `McpPromptScanConfigurer` | `ClassPathPromptScanner` |
| Complete | `@McpCompleteScan` | `McpCompleteScanRegistrar` | `McpCompleteScanConfigurer` | `ClassPathCompleteScanner` |

### 模式 2 · 双层解析器链（责任链 + 条件注册）

工具的「描述」和「参数」分别由两条按 `@Order` 排列的解析器链处理。高优先级（order 小）先尝试，解析不到再降级到下一个。

**描述解析器链**（`AbstractDesParser`，抽象方法 `doDesParse(Method, Class<?>)`）：

| order | 类名 | 解析来源 |
|---|---|---|
| 0 | `McpToolDesParser` | `@McpTool` 注解 |
| 1 | `ToolDesParser` | Spring AI `@Tool` 注解 |
| 2 | `JacksonDesParser` | Jackson 注解 |
| 3 | `JavaDocDesParser` | Javadoc 源码注释 |
| 4 | `Swagger3DesParser` | Swagger3（OpenAPI）注解 |
| 5 | `Swagger2DesParser` | Swagger2 注解 |

**参数解析器链**（`AbstractParamParser`）：

| order | 类名 | 解析来源 |
|---|---|---|
| 0 | `McpToolParamParser` | `@McpArg` / `@McpTool` |
| 1 | `ToolParamParser` | Spring AI `@ToolParam` |
| 2 | `MvcParamParser` | Spring MVC 参数注解（部分逻辑） |
| 3 | `JacksonParamParser` | Jackson 注解 |
| 4 | `JavaDocParamParser` | Javadoc `@param` |
| 5 | `Swagger3ParamParser` | Swagger3 注解 |
| 6 | `Swagger2ParamParser` | Swagger2 注解 |

**条件注册机制**：每个解析器在 `McpConfig` 中用 `@ConditionalOnParser` 注册（定义于 autoconfigure 模块 `conditional/ConditionalOnParser.java`），由 `Conditions.ParserCondition` 读取配置决定是否生效：

- 描述链开关：`plugin.mcp.parser.des`
- 参数链开关：`plugin.mcp.parser.param`

> 默认值（来源 `PluginProperties` 枚举 + `Conditions`）：
> - Des = `MCPTOOL, TOOL, JACKSON, JAVADOC, SWAGGER2, SWAGGER3`
> - Param = `MCPTOOL, TOOL, JACKSON, SPRINGMVC, JAVADOC, SWAGGER2, SWAGGER3`
> ⚠️ 配置键是单数 `parser.param`（非 `params`），与代码 `PluginProperties.Parser.param` 字段一致。

### 模式 3 · 上下文容器（工厂模式）

每种 MCP 实体都有「接口 → 工厂 → 实现」三件套，作为 Spring Bean 持有注册定义：

```
I{Type}Context (接口)  ──创建──  {Type}ContextFactory (工厂)  ──产出──  {Type}Context (实现, Spring Bean)
```

| 实体 | 接口 | 工厂 | 实现 |
|---|---|---|---|
| Tool | `IToolContext` | `ToolContextFactory` | `ToolContext` |
| Resource | `IResourceContext` | `ResourceContextFactory` | `ResourceContext` |
| Prompt | `IPromptContext` | `PromptContextFactory` | `PromptContext` |
| Complete | `ICompleteContext` | `CompleteContextFactory` | `CompleteContext` |
| Root | `IRootContext` | `RootContextFactory` | `RootContext` |

`IToolContext` 核心方法：`addTool(String name, ToolRegisterDefinition tool)`、`getRawTools()`。
`ToolContext` 由 `ToolContextFactory.createToolContext()` 创建，在 `McpConfig` 中注册为 Bean。

### 模式 4 · 回调架构（模板方法模式）

```
AbstractMcpToolMethodCallback           （模板：定义 buildArgs 流程 + 抽象 isExchangeType）
├── SyncMcpToolMethodCallback           （覆写 isExchangeType → McpSyncServerExchange）
└── AsyncMcpToolMethodCallback          （覆写 isExchangeType → McpAsyncServerExchange）
```

**`buildArgs()` 模板流程**（`AbstractMcpToolMethodCallback`）：

```
遍历方法每个 Parameter:
  ├─ isExchangeType?    → 注入 exchange（子类决定 Sync/Async 类型）
  ├─ isLoggerType?      → McpLoggerFactory.getLogger(...)
  ├─ isElicitationType? → McpElicitationFactory.getElicitation(exchange)
  ├─ isSamplingType?    → McpSamplingFactory.getSampling(exchange)
  ├─ isRootType?        → McpRootFactory.getRoot(exchange)
  └─ 普通参数           → 读 @McpArg.name（或反射参数名）→ 从 arguments Map 取值 → JsonParser.toTypedObject 转型
最终结果由 McpCallToolResultConverter.convertToCallToolResult(...) 转换为 CallToolResult
```

Resource / Prompt / Complete 均有同构的 `Abstract* / Sync* / Async*` 三层回调。

> **特殊可注入类型**（不从 MCP 参数映射，由框架自动注入）：
> `McpSyncServerExchange`、`McpAsyncServerExchange`、`McpLogger`、`McpElicitation`、`McpSampling`、`McpRoot`。
> 识别逻辑见 `AbstractMcpToolMethodCallback` 的 `isLoggerType / isElicitationType / isSamplingType / isRootType`（用 `isAssignableFrom` 判定），`isExchangeType` 为抽象方法交由子类区分 Sync/Async。

### 模式 5 · Provider 层（桥接模式）

`McpToolProvider` 等 Provider 在 Spring Bean 与 MCP SDK Specification 之间桥接，应用**两级过滤**：

```
┌─ 类级别过滤（扫描阶段，来自 ToolRegisterDefinition）
│    includeFilters / excludeFilters   例：包含 @Controller，排除 @Deprecated
└─ 方法级别过滤（Provider 内 doToolFilter）
     includeToolFilters / excludeToolFilters   例：只暴露 @RequestMapping 元注解的方法
```

Provider 家族：

| Provider | 职责 |
|---|---|
| `McpToolProvider` | Tool 桥接（Sync/Async 两套构建逻辑） |
| `McpResourceProvider` | Resource 桥接 |
| `McpPromptProvider` | Prompt 桥接 |
| `McpCompletionProvider` | Complete 桥接 |
| `SyncMcpAnnotationProvider` / `AsyncMcpAnnotationProvider` | 与 Spring AI 注解 Provider 体系对接 |

> ⚠️ **OutputSchema 已解析但当前未发送**（已核实）：`McpToolProvider` 中构建工具规格时，`outputSchema` 的传入被注释掉、改传 `null`（Sync 与 Async 构建路径均如此）。原因推测为当时 MCP SDK 尚未支持。`AbstractMcpToolMethodCallback` 仍保留 `outputSchema` 字段，待 SDK 支持后放开即可。
> 来源：`server2mcp-core/.../provider/McpToolProvider.java`（被注释的 outputSchema 传参）

### 模式 6 · 双模式执行（Sync vs Async）

由 `spring.ai.mcp.server.type`（`SYNC` / `ASYNC`，默认 `SYNC`）条件激活：

```
spring.ai.mcp.server.type = SYNC   →  Conditions.IsSyncCondition  →  SyncSpecMcpConfig 生效
spring.ai.mcp.server.type = ASYNC  →  Conditions.IsAsyncCondition →  AsyncSpecMcpConfig 生效
```

两套 Config（`autoconfigure/spec/SyncSpecMcpConfig`、`AsyncSpecMcpConfig`）分别用 `@Conditional(...)` 装配对应的 Sync / Async 回调与 Provider。整条链路（回调、Provider、Exchange 注入类型）都有 Sync/Async 双实现，互不干扰。

---

## 四、数据流：一个 Controller 方法如何变成 MCP 工具

以 `interface` 作用域、`SYNC` 模式为例，追踪一个普通 `@RestController` 方法的完整生命周期：

```
【启动期 · 注册阶段】
1. Spring Boot 启动
   ↓ 触发 META-INF/.../AutoConfiguration.imports
2. Server2McpAutoConfiguration → McpConfig
   ↓ plugin.mcp.enabled=true 且 scope=interface
3. 扫描启动类路径下所有 @Controller / @RestController
   ↓ 排除带 @Deprecated / @ToolNotScanForAuto 的类与方法
4. ClassPathToolScanner.scan() 命中类
   ↓ ToolBeanNameGenerator
5. IToolContext.addTool(name, ToolRegisterDefinition)
   ↓ 工具名 = className_methodName（可被 @McpTool.name 覆盖）
   ↓ ToolRegisterDefinition 携带 include/exclude 过滤器元数据

【启动期 · 解析阶段】
6. 描述解析链（McpToolDesParser→...→Swagger2DesParser）按 order 解析出 description
7. 参数解析链（McpToolParamParser→...→Swagger2ParamParser）按 order 解析出 inputSchema
   ↓ inputSchema 由 GenSchemaUtils 生成 JSON Schema

【启动期 · 桥接阶段】
8. McpToolProvider 读取 ToolContext
   ↓ 类级 includeFilters/excludeFilters → 方法级 doToolFilter
   ↓ 为每个工具构建 MCP SDK 的 Tool Specification（outputSchema 当前传 null）
9. 绑定 SyncMcpToolMethodCallback 作为执行体

【运行期 · 调用阶段】
10. MCP 客户端（如 Cursor）发起 tools/call
    ↓ 命中对应 Specification
11. SyncMcpToolMethodCallback.buildArgs()
    ↓ 特殊类型注入（Exchange/Logger/Elicitation/Sampling/Root）
    ↓ 普通参数：arguments Map → JsonParser.toTypedObject 转型
12. 反射调用原始 Controller 方法（bean.method(args)）
13. McpCallToolResultConverter.convertToCallToolResult()
    ↓ 返回值 → CallToolResult
14. MCP 客户端收到结果
```

**关键洞察**：原始 Controller 方法在第 12 步被**原样反射调用**，业务代码对 MCP 的存在毫无感知——这正是「非侵入、纯增强」哲学的落地点。

---

## 五、关键约定速查

| 约定 | 内容 |
|---|---|
| 注解前缀 | 框架注解统一 `Mcp` 前缀（`@McpTool`、`@McpArg`...），与 Spring AI `@Tool` 区分 |
| 工具命名 | `className_methodName`，可用 `@McpTool.name` 覆盖（来源 `ToolDefinitionBuilder.getToolName()`） |
| 作用域 | `interface`（自动扫描 Controller）/ `custom`（仅显式 `@ToolScan`） |
| 自动排除 | `@Deprecated`、`@ToolNotScanForAuto` 注解的类/方法不参与自动扫描 |
| Javadoc 解析限制 | 字节码不含 Javadoc，需用 `maven-resources-plugin` 把 `.java` 源拷进 classpath |
| 编译参数 | 根 pom 全局启用 `-parameters`，用于反射获取参数名（要求 Java 17） |

---

## 六、延伸阅读

- 扩展点（自定义解析器 / 转换器 / 过滤器 / Context）：见 `docs/reference/extension-points.md`
- 新人/新 AI 上手：见 `docs/reference/onboarding.md`
- 架构权威事实源：项目根 `CLAUDE.md`
