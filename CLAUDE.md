# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

**api2mcp4j**（内部名称 server2mcp）是一个 Spring Boot Starter 框架，能自动将 Java Spring Controller 接口暴露为 MCP（Model Context Protocol）的 Tool、Resource、Prompt 和 Complete。设计理念是非侵入式、纯增强——类似 MyBatis-Plus 与 MyBatis 的关系。

**核心思想**：在 `interface` 作用域模式下，现有 `@RestController` 方法无需改动即可成为 MCP 工具。自定义 MCP 实体则使用专用注解（`@McpTool`、`@McpResource`、`@McpPrompt`、`@McpComplete`）。

---

## 顶层授权框架（必读）

> 本项目 2026-06-24 全面继承姊妹项目 `../real-agent` 的「开发心法」（董事长批准·方案 B）。本节及下方红线、审查、导航均在「自决默认协议」框架下解读。
> 协议正本：`~/.claude/CLAUDE.md` 顶部 · [ENFORCED] · 优先级高于本文件以下所有具体规范。

**核心契约**：
- **红线场景**（不可逆操作 / 跨系统协议变更 / 全局规范修改 / 战役级启停 / 生产部署 / 删公开 API / 升级 SNAPSHOT 依赖的破坏性变更）→ **必须事前请示董事长**，按下方「开发红线总表」执行多源验证等慢档流程。
- **红线之外** → AI 自决，不推送选项给董事长，事后简报（`[DONE] 做了 X · 因 Y · 风险 Z · 回滚成本 W`）。

**三方独立制衡**（中等以上复杂度任务，全局 Rule #6）：架构师（开发，不自写测试）/ 御史台（独立测试 + Code Review，不参与开发）/ CEO（协调验证闭环）。本项目质检 agent 见 `.claude/agents/`（御史台 `imperial-censor`）。

**Briefback 宣誓**：所有 agent 执行前须输出工作协议宣誓六项（任务理解 / 红线清单 / 交付标准 / 绝不会做的事 / 偏离预案 / **环境能力自检**），未宣誓 = 产出作废。

## 开发心法与规范导航

> 心法继承自 `../real-agent`，已剥离前端/设计招式、适配本 Java 库语境。**单一权威源原则**：下方「开发红线总表」是本项目红线的唯一权威源，各 spec 只引用不重复。

### 通用心法（`docs/rules/global/`，所有会话默认生效）

| 规则 | 路径 | 核心 |
|------|------|------|
| 搜索工具序位 | `docs/rules/global/search-tool-parity.md` | grep（字面量）/ 结构层 / LSP（语义）/ 多模块（图谱）四维不互替 |
| 破坏性删除多源验证 | `docs/rules/global/destructive-deletion.md` | [ENFORCED] 删公开 API 前 grep 多形式 + LSP + 跨模块/下游兼容性评估 |
| Session 续接铁律 | `docs/rules/global/session-continuity.md` | [ENFORCED] 续接先 `git log/status` 验证，禁凭记忆断言状态 |
| 重构 commit 顺序 | `docs/rules/global/refactor-ordering.md` | [ENFORCED] 契约提供者先行 / 原子 commit，避免契约真空期 |
| Agent 能力声明 | `docs/rules/global/agent-capability-declaration.md` | [ENFORCED] 物理不可能性原则，Briefback 第六项环境自检 |
| 工作留痕 | `docs/rules/global/work-log.md` | [ENFORCED] 报告类产出 / >30 行结构化产出落 `docs/logs/` |

### 核心规范（`docs/specs/`）

| 规范 | 路径 | 核心 |
|------|------|------|
| 注册纪律宪法 | `docs/specs/REGISTRATION_DISCIPLINE_SPEC.md` | [ENFORCED] 六维成熟度 Rubric + 三把利器（本框架灵魂：注册不遗漏） |
| 文件头规范 | `docs/specs/FILE_HEADER_SPEC.md` | [ENFORCED] AI 可读性文件头 `@header-start`/`@header-end` 分级模板 |
| 测试规范 | `docs/specs/TEST_SPEC.md` | [ENFORCED] 测试金字塔 + TDD 双 commit（`[RED]`/`[GREEN]`） |
| 工作留痕规范 | `docs/specs/WORK_LOG_SPEC.md` | [ENFORCED] 落文件模板 + Agent 五要素总结 + MUST-WIN 三源验证 |

### 参考索引（`docs/reference/`）

| 文档 | 路径 | 核心 |
|------|------|------|
| 架构总览 | `docs/reference/architecture.md` | 模块流向 + 6 大设计模式 + 数据流全过程 |
| 扩展点手册 | `docs/reference/extension-points.md` | 自定义解析器 / 转换器 / 过滤器 / Context |
| 新人入门 | `docs/reference/onboarding.md` | 3 步上手 + 必读清单 + 易踩坑 |

### 团队基建（`.claude/`）

- `.claude/agents/` — 10 个 Java 库适配 agent（三分组：指挥层 / 执行层 / 质检层），注册表 `.claude/agents/agent-groups.json`，验证 `node scripts/agent-groups.mjs verify`。
- `.claude/task.json` — 物理模块注册表 · `.claude/hooks/session-start.sh` — 会话启动提示。

## 构建与测试命令

```bash
# 构建全部模块（尚未推送至 Maven Central，必须本地安装）
mvn clean install

# 仅构建 WebMVC Starter
cd server2mcp-spring-boot-starters/server2mcp-starter-webmvc && mvn clean install

# 运行测试（测试位于 server2mcp-core）
cd server2mcp-core && mvn test

# 运行单个测试类
cd server2mcp-core && mvn test -Dtest=GenSchemaUtilsTest

# 打包测试/演示应用
cd server2mcp-test && mvn clean package
```

**注意**：要求 Java 17。根 pom 全局启用了 `-parameters` 编译参数，用于反射获取方法参数名。

## 模块架构

```
server2mcp-parent (根 pom, v1.1.4-SNAPSHOT)
├── server2mcp-common          → 常量、工具类（ConvertUtil, JacksonUtils, GenSchemaUtils）
├── server2mcp-core            → 核心引擎：注解、解析器、扫描器、回调、Provider
├── server2mcp-autoconfigure   → Spring Boot 自动配置（Server2McpAutoConfiguration）
├── server2mcp-spring-boot-starters/
│   ├── server2mcp-starter-webmvc    → Spring MVC 应用的 Starter
│   └── server2mcp-starter-webflux   → WebFlux 应用的 Starter
└── server2mcp-test            → 演示应用（不在根 pom 的 modules 中，需单独构建）
```

**依赖流向**：common ← core ← autoconfigure ← starters

## 关键依赖

| 依赖 | 版本 | 用途 |
|---|---|---|
| Spring Boot | 3.4.4 | 基础框架 |
| Spring AI | 1.1.0-SNAPSHOT | MCP 集成层 |
| MCP Java SDK | 0.14.0-SNAPSHOT | MCP 协议实现 |
| JavaParser | 3.25.5 | Javadoc 注释解析 |
| VicTools JsonSchema | 4.37.0 | JSON Schema 生成（Swagger 支持） |

全部使用 SNAPSHOT 版本，来源于 Spring 仓库，必须本地 `mvn clean install`。

## 核心设计模式与处理链路

### 1. 注解驱动注册（ImportBeanDefinitionRegistrar 模式）

```
@ToolScan → McpToolScanRegistrar → McpToolScanConfigurer (BeanDefinitionRegistryPostProcessor)
    → ClassPathToolScanner → ToolBeanNameGenerator → IToolContext.addTool()
```

`@McpResourceScan`、`@McpPromptScan`、`@McpCompleteScan` 遵循完全相同的模式。

### 2. 双层解析器链（责任链模式）

**描述解析器**（`AbstractDesParser`，按 order 0-5 排列）：
`McpToolDesParser → ToolDesParser → JacksonDesParser → JavaDocDesParser → Swagger3DesParser → Swagger2DesParser`

**参数解析器**（`AbstractParamParser`，按 order 0-6 排列）：
`McpToolParamParser → ToolParamParser → MvcParamParser → JacksonParamParser → JavaDocParamParser → Swagger3ParamParser → Swagger2ParamParser`

解析器通过 `@ConditionalOnParser` 条件注册，由 `plugin.mcp.parser.param` / `plugin.mcp.parser.des` 配置控制。

### 3. 上下文容器模式（工厂模式）

每种 MCP 实体类型均有：`I{Type}Context` 接口 → `{Type}ContextFactory` 工厂 → `{Type}Context` 实现。上下文作为 Spring Bean 持有注册定义。

### 4. 回调架构（模板方法模式）

```
AbstractMcpToolMethodCallback
├── SyncMcpToolMethodCallback
└── AsyncMcpToolMethodCallback
```

回调处理流程：参数提取 → 特殊参数注入（Exchange, Logger, Elicitation, Sampling, Root）→ 方法调用 → 通过 `McpCallToolResultConverter` 转换结果。

Resource、Prompt、Complete 均有相同的回调层次结构。

### 5. Provider 层（桥接模式）

`McpToolProvider` / `McpAnnotationProvider`（Sync 与 Async 变体）在 Spring Bean 和 MCP SDK Specification 之间桥接。应用两级过滤：
- **类级别**：`includeFilters` / `excludeFilters`（如包含 `@Controller`，排除 `@Deprecated`）
- **方法级别**：`includeToolFilters` / `excludeToolFilters`（如包含 `@RequestMapping` 元注解）

### 6. 双模式执行

`AsyncSpecMcpConfig` vs `SyncSpecMcpConfig`——由 `spring.ai.mcp.server.type`（ASYNC/SYNC）条件激活。

## 配置参考

```yaml
plugin:
  mcp:
    enabled: true                    # 总开关
    scope: interface                 # interface（自动扫描 Controller）| custom（仅显式 @ToolScan）
    parser:
      param: MCPTOOL, JAVADOC, TOOL, SpringMVC, JACKSON, SWAGGER2, SWAGGER3   # 注意：键为单数 param（对应 PluginProperties.Parser.param 字段）
      des: MCPTOOL, JAVADOC, TOOL, JACKSON, SWAGGER3, SWAGGER2
    tool:
      enabled: true
    resource:
      enabled: true
    prompt:
      enabled: true
    complete:
      enabled: true
    root:
      enabled: true
```

自动配置入口：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` → `Server2McpAutoConfiguration`

## 关键约定

- **包根路径**：`com.ai.plug`——common 在 `.common`，core 在 `.core`，autoconfig 在 `.autoconfigure`
- **注解命名**：框架注解统一以 `Mcp` 为前缀（如 `@McpTool`、`@McpArg`、`@McpResource`），与 Spring AI 的 `@Tool` 区分
- **工具命名**：自动生成为 `className_methodName`，可通过 `@McpTool.name` 覆盖
- **特殊可注入类型**：方法参数类型为 `McpSyncServerExchange`、`McpAsyncServerExchange`、`McpLogger`、`McpElicitation`、`McpSampling`、`McpRoot` 时自动注入，不从 MCP 参数映射
- **Javadoc 解析器限制**：需要通过 `maven-resources-plugin` 将 `.java` 源文件复制到 classpath（字节码不含 Javadoc）
- **作用域模式**：`interface` 自动注册 `@Controller` 类的 `@RequestMapping` 方法；`custom` 需显式 `@ToolScan`
- **排除机制**：`@Deprecated` 和 `@ToolNotScanForAuto` 注解可将类/方法排除出自动扫描

## 扩展点

1. **自定义解析器**：继承 `AbstractDesParser` 或 `AbstractParamParser`，注册为 Spring Bean 并用 `@Order` 指定优先级
2. **自定义结果转换器**：实现 `McpCallToolResultConverter`，在 `@McpTool(converter = ...)` 中指定
3. **自定义工具过滤器**：使用 `@ToolScan(includeToolFilters = ..., excludeToolFilters = ...)`，支持 ANNOTATION 和 META_ANNOTATION 过滤类型
4. **自定义上下文**：实现 `IRootContext` 用于 Root 生命周期管理

## 架构注意事项

- `OutputSchema` 已解析但**当前未发送**至 MCP（在 `McpToolProvider` 中被注释掉）——MCP SDK 可能尚未支持
- `server2mcp-test` 模块**不在**根 pom 的 `<modules>` 中，必须单独构建
- Spring AI 和 MCP SDK 均为 SNAPSHOT 版本，预期会有 API 破坏性变更
- `ToolRegisterDefinition` 在注册链路中携带过滤器元数据，使得在 Specification 创建时可按扫描组进行过滤

---

## 开发红线总表

> **单一权威源原则**：本表是本项目红线的唯一权威来源；`docs/specs/` 与 `docs/rules/` 只引用、不重复。
> 每条先判定是否属「红线场景」（须事前请示董事长）；非红线但列于此表者 → 自决执行 + 事后简报。
> 来源：继承自 `../real-agent` 红线总表心法 · 董事长 2026-06-24 批准 · 适配 server2mcp 框架。

### 架构与注册红线

| 禁止行为 | 原因 | 正确做法 |
|---------|------|---------|
| 删除/重命名**公开 API**（框架注解 `@McpTool`/`@McpResource`/`@McpPrompt`/`@McpComplete`/`@McpArg`/`@ToolScan` 及其属性、Provider 公开类、`IXxxContext` 接口、`AbstractDesParser`/`AbstractParamParser`/`McpCallToolResultConverter` SPI、public 方法/枚举值）未跑「破坏性删除多源验证」 | 本项目是**被下游依赖的库**，`mvn install` EXIT=0 只证明本仓不报错，证明不了下游用户不报错 | 按 `docs/rules/global/destructive-deletion.md`：grep 多形式 + LSP find_references + 跨模块/下游兼容性评估；公开 API 破坏 = **MAJOR 版本号 + CHANGELOG**（红线·须事前请示） |
| 新增解析器用散落 `if/else`/`switch` 判类型，绕过 `@ConditionalOnParser` + `@Order` 责任链 | 违反注册纪律 R1/R5，新增类型必改散落判断、必漏 | 套用「利器 A」：继承 `AbstractXxxParser` + `@ConditionalOnParser` Bean + `@Order`，见 `docs/specs/REGISTRATION_DISCIPLINE_SPEC.md` |
| 同类 MCP 资源散落多个 Context / 多张 Map；或 Provider 绕过 `IXxxContext` 另开数据源读注册定义 | 违反 R1 极高内聚；注册面与暴露面脱钩（注册了却不暴露 / 暴露了未注册） | 收口单一 `IXxxContext` + 单一 `getRawXxx()` 出口，Provider 只从此取数 |
| 新增一类 MCP 实体扫描，注解/Registrar/Configurer/`IXxxContext` 四件套不齐 | 破坏注册链路完整性，半成品孤儿 | 「利器 B」四件套齐全，缺一不合并 |
| 启用/修改 `OutputSchema` 发送逻辑（`McpToolProvider` 中当前被注释）未确认 MCP SDK 版本支持 | MCP SDK 为 SNAPSHOT，可能尚未支持 | 先验证 SDK 当前版本支持，再启用并加测试 |
| 升级 Spring AI / MCP SDK SNAPSHOT 版本未评估 API 破坏性变更 | SNAPSHOT 预期破坏性变更，牵一发动全身 | 升级前全模块 `mvn clean install` + 跑测试，记录破坏点（红线·跨系统协议变更，须事前请示） |
| webflux starter 响应式链路中使用 `.block()` | 阻塞 Reactor 线程模型 | 用 `flatMap` / `flatMapMany` |
| `@ConditionalOnParser.value` / parser 名（`plugin.mcp.parser.param`·`.des`）拼写无启动校验即依赖 | R2 启动断言缺失，漏配/拼错静默不激活、零告警（真实缺口 V-2） | parser 名须与默认集合核对；启动断言补齐为限期项（改协议层须董事长批准） |
| 逆转模块依赖流向（`common ← core ← autoconfigure ← starters`） | 破坏分层，starter 应只做装配 | 注册逻辑单一权威源在 core，starter 仅引 core + autoconfigure |
| 破坏「非侵入式」原则——要求用户改动现有 `@RestController` 才能被扫描 | 违背框架核心卖点（类比 MyBatis-Plus 之于 MyBatis） | `interface` 作用域下现有 Controller 零改动即成 MCP 工具 |

### 工程纪律红线（继承自心法层）

| 禁止行为 | 原因 | 正确做法 |
|---------|------|---------|
| 新建/修改文件无文件头（`@header-start`/`@header-end` 边界 + `@module`/`@layer`/`@updated`） | AI 可读性、定位困难 | 按 `docs/specs/FILE_HEADER_SPEC.md` 分级添加（回填存量类为后续独立 Sprint） |
| 修改文件逻辑后不更新文件头 `@updated` | 过时文件头误导 AI | 与代码强关联，同步更新为当前真实时间 |
| 报告类产出（审计/研究/审查/计划/调试/评审）或 >30 行结构化产出直接输出对话 | 随上下文压缩消散，不可追溯 | 写入 `docs/logs/{date}_{role}_{topic}.md`，对话只留摘要+路径，见 `docs/specs/WORK_LOG_SPEC.md` |
| TDD 的 `[RED]` 测试与 `[GREEN]` 实现合并为单 commit | 审计者无法 `git show` 看到失败断言、无法 checkout 复现 RED | 双 commit 签名，见 `docs/specs/TEST_SPEC.md` |
| 重构先删依赖者、后改契约提供者（留契约真空期） | 中间 commit 若部署即事故 | 契约提供者先行 / 原子 commit，见 `docs/rules/global/refactor-ordering.md` |
| 跨 session 续接凭对话记忆断言「X 已完成/已删除/还在」 | summary 是意图快照非状态权威 | 续接第一动作 `git log --oneline -10` + `git status` + 关键文件验证，见 `docs/rules/global/session-continuity.md` |
| Agent 宣誓阶段不声明环境能力自检 | 工具不可用时事后"装作已执行"破坏可信度 | Briefback 第六项环境能力自检，不可用项事前声明，见 `docs/rules/global/agent-capability-declaration.md` |
| 把 `server2mcp-test` 当作根 pom 模块构建 | 它**不在**根 pom `<modules>` 中 | 单独 `cd server2mcp-test && mvn ...` |
| 修改全局规范（本 `CLAUDE.md` / `docs/specs/` / `docs/rules/`）的实质内容未经董事长批准 | 宪法层稳定性 | 红线·须事前请示（文档级 typo/残骸清理属授权内，自决） |

## 代码审查清单

> 提交/合并前 AI 自查 + 御史台 Code Review 逐项确认。模块实现细节见 `docs/reference/`。

### 注册与架构

- [ ] 新增解析器走 `@ConditionalOnParser` + `@Order` 责任链，未散落判类型？（注册纪律 R1/R5）
- [ ] `@Order` 优先级与现有 des（0-5）/ param（0-6）不冲突？
- [ ] 同类资源仅单一 `IXxxContext` + 单一 `getRawXxx()` 出口；Provider 只从此取数？
- [ ] 新增扫描类 MCP 实体四件套（注解/Registrar/Configurer/IXxxContext）齐全？
- [ ] 模块依赖流向未逆（common ← core ← autoconfigure ← starters）；starter 仅做装配？
- [ ] 非侵入性保持——`interface` 作用域下用户 Controller 零改动？

### 破坏性变更与兼容性

- [ ] 删除/重命名公开 API 跑了破坏性删除多源验证（grep 多形式 + LSP + 跨模块/下游评估）？
- [ ] 公开 API 破坏在版本号（MAJOR）+ CHANGELOG 中体现？
- [ ] SNAPSHOT 依赖升级评估了破坏性变更并跑全模块构建？
- [ ] webflux starter 响应式链路无 `.block()`？

### 工程纪律

- [ ] 新建/修改文件有文件头（`@header-start`…`@header-end` + `@module`/`@layer`/`@updated`）？
- [ ] `@updated` 为当前真实时间（`YYYY-MM-DD HH:mm`）？
- [ ] 报告类产出落 `docs/logs/`（非直接输出对话）？
- [ ] 新增/变更行为有对应测试（`XxxTest`），TDD 双 commit？
- [ ] `mvn clean install` 全模块通过（含 `cd server2mcp-test` 单独构建演示应用）？

---

> **维护者**：api2mcp4j Team · **心法继承**：2026-06-24 全面继承 `../real-agent` 开发心法（方案 B，董事长批准）· 详见 `docs/plan-继承real-agent开发心法-2026-06-24.md` 与 `docs/logs/2026-06-24_*`
