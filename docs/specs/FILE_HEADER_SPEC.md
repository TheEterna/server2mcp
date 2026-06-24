# 文件头规范（AI 可读性增强）[ENFORCED]

> 来源：继承自 ../real-agent docs/specs/FILE_HEADER_SPEC.md · 董事长 2026-06-24 批准方案B全套继承
> 强制度：[ENFORCED]
>
> 目标：让 Claude Code / Cursor / Copilot 打开任何 `.java` 文件就能回答：
> "这是什么、属于哪一层、为什么存在、改它会怎样、和谁关联"

---

## 一、核心原则

| # | 原则 | 说明 |
|---|------|------|
| 1 | **统一边界标记** | 所有文件头用 `@header-start` / `@header-end` 包裹，人/脚本/AI 均可一行提取 |
| 2 | **修改时顺手更新** | `@updated` 精确到分钟，与文件修改时间差 ≤ 1 小时；修改代码时顺手更新文件头是常规操作 |
| 3 | **全量关键词索引** | `@keywords` 是文件的语义指纹，空格分隔、中英文混排，越多越好 |
| 4 | **分级而非一刀切** | 核心引擎类用完整文件头，简单 POJO/常量类用单行文件头，避免噪声 |

> 本项目（api2mcp4j / server2mcp）是纯 Java 库——Spring Boot Starter。本规范**只约束 `.java` 源文件**，不涉及任何前端/样式文件。

---

## 二、边界标记

所有 Java 文件头统一用 `@header-start` 和 `@header-end` 包裹，置于类声明上方的 Javadoc 块注释中。脚本提取逻辑：**从 `@header-start` 到 `@header-end` 之间的内容即为文件头。**

```
正则一行提取：/@header-start([\s\S]*?)@header-end/
```

> 边界标记是铁律：即使是单行 L1 文件头，也必须包含 `@header-start` 与 `@header-end`，否则审计脚本无法识别。

---

## 三、字段定义

| 字段 | 级别 | 说明 |
|------|------|------|
| `@module` | 必填 | 文件身份标识 + 一句话用途 |
| `@keywords` | 必填 | 全量关键词索引（空格分隔，中英文混排，越多越好） |
| `@updated` | 必填 | 最后更新时间 `YYYY-MM-DD HH:mm`（与文件修改时间差 ≤ 1h） |
| `@layer` | 必填 | 架构层级，取值见 §六 |
| `@purpose` | L2+ | 为什么存在（≤40 字） |
| `@affects` | L3 | 改动影响范围（牵一发动全身的连锁说明） |
| `@see` | 可选 | 关联类/文档（≤3 个） |
| `@constraint` | 可选 | 红线/冻结/对齐约束（如"SDK API 破坏性变更敏感"） |
| `@author` | 可选 | 作者 |

---

## 四、`@keywords` 全量关键词索引

`@keywords` 是文件的**语义指纹**——覆盖该文件触及的所有概念。目的是让 `grep @keywords XXX` 无论以什么角度搜索都能命中。

- **格式**：空格分隔，中英文混排
- **数量**：越多越好，不设上限
- **覆盖维度**：MCP 实体类型（Tool/Resource/Prompt/Complete）、设计模式名（责任链/工厂/模板方法/桥接）、技术概念（JSON Schema/Javadoc 解析/反射）、注册链路角色（Registrar/Scanner/Configurer/Context）、中文同义词、上下游类名
- **反例**：`java spring mcp`（太泛，无定位价值）
- **正例**：`工具描述解析 责任链 AbstractDesParser McpTool注解 description @Order order条件注册 ConditionalOnParser 解析器优先级`

---

## 五、分级策略

> 本项目按"该类在注册链路/处理链路中的角色"分级，不照搬前端的页面/组件分级。

| Level | 适用文件（本项目语境） | 必填字段 |
|-------|----------------------|---------|
| **L1 单行** | 注解定义（`@McpTool`/`@McpArg`/`@ToolScan`）、纯常量类（`ConfigConstants`）、DTO/VO/简单 POJO（`server2mcp-test` 的 `Dto`/`PoiData`）、`I{Type}Context` 这类极简接口 | `@module` + `@keywords` + `@updated` + `@layer` |
| **L2 标准** | 工具类（`ConvertUtil`/`JacksonUtils`/`GenSchemaUtils`）、具体解析器（`McpToolDesParser`/`MvcParamParser`）、具体 Context 实现、Builder | + `@purpose` |
| **L3 完整** | 核心引擎类——注册链路（`McpToolScanRegistrar`/`McpToolScanConfigurer`/`ClassPathToolScanner`）、解析器抽象基类（`AbstractDesParser`/`AbstractParamParser`）、回调抽象基类（`AbstractMcpToolMethodCallback`）、Provider（`McpToolProvider`）、自动配置（`Server2McpAutoConfiguration`/`McpConfig`）、双模式配置（`AsyncSpecMcpConfig`/`SyncSpecMcpConfig`） | + `@purpose` + `@affects` + `@see` / `@constraint` |

**判级速记**：
- 改它会牵动整条注册/解析链 → **L3**
- 它是链路中可替换的一环（一个具体解析器/转换器） → **L2**
- 它只是数据载体或纯声明 → **L1**

---

## 六、`@layer` 取值（本项目五层）

> 严格对应本项目的真实模块划分（来源：CLAUDE.md 模块架构），不引入前端层级。

| `@layer` 取值 | 对应模块 | 典型文件 |
|--------------|---------|---------|
| `common` | server2mcp-common | `ConvertUtil`、`JacksonUtils`、`GenSchemaUtils`、`ConfigConstants` |
| `core` | server2mcp-core | 注解、解析器、扫描器、Context、Provider、回调、Builder |
| `autoconfigure` | server2mcp-autoconfigure | `Server2McpAutoConfiguration`、`McpConfig`、`Conditions`、`@ConditionalOnParser` |
| `starter` | server2mcp-spring-boot-starters/* | webmvc / webflux starter 入口 |
| `test` | 任意模块的 `src/test` | `GenSchemaUtilsTest` 等测试类（见 [TEST_SPEC.md](./TEST_SPEC.md) §五） |

> **依赖流向铁律**：common ← core ← autoconfigure ← starter。`@layer` 标错会误导 AI 对依赖方向的判断，CR 必查。

---

## 七、Java 文件头模板

### L3 完整（核心引擎类）

```java
/**
 * @header-start
 * @module McpToolScanConfigurer
 * @keywords 工具扫描 注册链路 ClassPathToolScanner BeanDefinitionRegistryPostProcessor
 *           ToolScan 包扫描 includeFilters excludeFilters 两级过滤 IToolContext addTool
 *           ToolRegisterDefinition 注册定义 自动扫描 interface作用域
 * @updated 2026-06-24 11:10
 * @purpose 执行 @ToolScan 的包扫描，把扫描结果按过滤规则注册进 IToolContext
 * @layer core
 * @affects 修改影响所有 @ToolScan 与 interface 作用域下的工具注册结果
 * @see ClassPathToolScanner — 实际执行类路径扫描
 * @see IToolContext — 注册定义的容器
 * @constraint 注册链路核心，改动需评估对 Registrar→Scanner→Context 全链的连锁影响
 * @author han
 * @header-end
 */
```

### L2 标准（链路中的一环）

```java
/**
 * @header-start
 * @module McpToolDesParser
 * @keywords 工具描述解析 责任链 description @McpTool注解 AbstractDesParser order=0 解析器
 * @updated 2026-06-24 11:10
 * @purpose 从 @McpTool.description 提取工具描述，是描述解析责任链的最高优先级一环
 * @layer core
 * @header-end
 */
```

### L1 单行（注解/常量/POJO/简单接口）

```java
/** @header-start @module IToolContext @keywords 工具上下文 注册容器 addTool getRawTools 接口 @updated 2026-06-24 11:10 @layer core @header-end */
```

```java
/** @header-start @module ConfigConstants @keywords 配置常量 plugin.mcp 前缀 VARIABLE_PREFIX 解析器配置key @updated 2026-06-24 11:10 @layer common @header-end */
```

---

## 八、`@updated` 工程化双向绑定

### 格式
`YYYY-MM-DD HH:mm`（精确到分钟，取 `date '+%Y-%m-%d %H:%M'` 的真实输出）

### 校验规则
`@updated` 标记时间与文件实际修改时间（文件系统 mtime 或 git commit 时间）差距 **不超过 1 小时**。

### 常规流程（主路径）

修改文件时顺手更新文件头是常规操作：
1. 读 `@header-start` ... `@header-end` 块 → 理解职责/层级/约束
2. 修改代码
3. 顺手更新文件头字段 + `@updated` 为当前时间
4. 无文件头 → 顺手补加（按 §五 判级）

---

## 九、本项目落地现状与策略

> 来源：2026-06-24 实地勘察 `server2mcp-core/src`——**当前所有 `.java` 文件均无 `@header-start` 文件头**（`grep -rl "@header-start" --include="*.java"` 零命中）。

| 项 | 现状 | 策略 |
|----|------|------|
| 存量文件 | 多数仅有 `@author han` + `time:` + `des:` 形式的简易注释 | **渐进式补加**：不搞一次性全量改造（避免大面积无意义 diff）；谁改到谁补加，新增文件一律带文件头 |
| 新增文件 | — | **强制**带文件头，按 §五 判级、§七 套模板 |
| 核心引擎类（L3） | 注册链路 / 解析器基类 / Provider 等 | 优先补加（这些是 AI 最常需要理解的入口） |

> **不留文档债务铁律**（来源：~/.claude/CLAUDE.md 核心铁律 #14）：代码改动必须同步文件头。改了 `@purpose` 描述的行为却不更新 `@purpose` = 文档债务，CR 打回。

---

## 十、状态标记

> [ENFORCED] = 已有人工 CR 流程强制执行 · [PLANNED] = 已定义但尚无自动化保障

| 规则 | 状态 | 验证方式 |
|------|------|---------|
| 新增 `.java` 文件必须有 `@header-start`/`@header-end` 边界标记 | [ENFORCED] | CR 人工审查 + `grep -L "@header-start"` 抽查 |
| 必填字段：`@module` + `@keywords` + `@updated` + `@layer` | [ENFORCED] | CR 人工审查 |
| `@layer` 取值在五层枚举内且与所在模块一致 | [ENFORCED] | CR 人工审查（依赖流向铁律） |
| `@updated` 与文件修改时间差 ≤ 1 小时 | [PLANNED] | 暂无 hook，依赖修改时顺手更新 + CR 抽查 |
| L2+ 含 `@purpose`、L3 含 `@affects` | [PLANNED] | 依赖人工判级与审查 |
| 存量文件渐进式补加 | [PLANNED] | 谁改到谁补，无强制全量门禁 |

---

## 十一、如何验证

> 每条规则如果不能被验证，就等于不存在。

- [ ] 列出无文件头的 Java 文件：`grep -rL "@header-start" --include="*.java" server2mcp-core/src/main`
- [ ] 抽查 3 个核心引擎类（Registrar/Configurer/Provider），确认为 L3 且含 `@affects`
- [ ] 抽查 5 个近期改动文件，对比 `@updated` 与 `git log --format='%ai' -1 -- {file}`，差距应 ≤ 1 小时
- [ ] Grep 全部 `@layer`，确认取值均在 `common|core|autoconfigure|starter|test` 之内
- [ ] 抽查 `@layer` 与文件所在模块一致（如 `autoconfigure` 层文件不应出现在 `server2mcp-core`）

---

## 十二、与其他规范的关系

| 规范 | 关系 |
|------|------|
| [WORK_LOG_SPEC.md](./WORK_LOG_SPEC.md) | 工作日志文件本身**不需要** `@header-start` 文件头（YAML frontmatter 已足够） |
| [REGISTRATION_DISCIPLINE_SPEC.md](./REGISTRATION_DISCIPLINE_SPEC.md) | 注册链路核心类（L3）的 `@constraint` 应标注其注册纪律约束 |
| [TEST_SPEC.md](./TEST_SPEC.md) | 测试文件 `@layer` 统一标 `test`，模板见 TEST_SPEC §五 |

---

**版本**：1.0（继承自 real-agent v3.0，剥离 TS/Vue/SCSS，适配 Java 库五层）
**生效日期**：2026-06-24
**维护者**：api2mcp4j Team
