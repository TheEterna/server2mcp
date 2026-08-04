# 注册纪律宪法（Registration Discipline）[ENFORCED]

> 来源：继承自 ../real-agent docs/specs/REGISTRATION_DISCIPLINE_SPEC.md · 董事长 2026-06-24 批准方案B全套继承
> 强制度：[ENFORCED]
>
> **版本**：1.0.0 · 立宪时间：2026-06-24
> **命题来源**：董事长命题——"**用什么必先向系统注册，超高复杂度下绝不遗漏**"在 server2mcp 框架上的工程翻译
> **范式铁证**：本规范所有范式引用本项目**真实 file:line**（2026-06-24 实地勘察确认）

---

## 一、为什么这条宪法对 server2mcp 是灵魂级的

> **本框架的灵魂就是"注册"。**

api2mcp4j（server2mcp）做的唯一一件事，就是把"散落在用户工程里的 Spring Controller 方法 / 自定义 MCP 实体"**收集、注册、暴露**为 MCP 的 Tool/Resource/Prompt/Complete。整个框架就是一台注册机：

```
@ToolScan → McpToolScanRegistrar → McpToolScanConfigurer → ClassPathToolScanner
         → IToolContext.addTool() → McpToolProvider（两级过滤）→ MCP SDK Specification
```

解析器、Context、Provider、自动配置——本框架几乎每一个核心类都在干"注册"或"被注册查询"。因此 real-agent 的注册纪律宪法在本项目**契合度极高**，不是生搬，是本命。

### 1.1 命题三段拆解（本框架对照）

| 子命题 | 工程含义 | 本框架现状 |
|--------|---------|-----------|
| **极高内聚** | 同类资源只有一个家——一张注册表、一个查询出口 | ✅ 每类 MCP 实体只有一个 `I{Type}Context`，工具只有 `IToolContext.getRawTools()` 单一出口 |
| **强制注册** | "用之前必先注册"是编译期/启动期的物理约束 | ⚠️ 解析器靠 `@ConditionalOnParser` + Spring `List<T>` 自动收集（强），但**无启动断言**（漏配静默生效） |
| **绝不遗漏** | 新增资源自动被收集，遗漏会主动报错拒绝运行 | ✅ R5 满分（`List<AbstractDesParser>` 自动注入）；⚠️ R2 弱（漏注册不报错） |

> **诚实声明**：本框架的 R5（自动发现）已是金标准范式，但 R2（启动断言）目前缺失——这是真实的可改进项，本宪法如实标注，不假装已有断言。详见 §五违规清单 V-2。

---

## 二、六维成熟度 Rubric [ENFORCED]

> 任何"MCP 运行时资源"（解析器 / Tool / Resource / Prompt / Complete / Context / 结果转换器）新增或重构时，CR 必须按本 Rubric 逐维打分（每维 0-5，满分 30）。
> **硬底线**：R1+R5+R6 三维（本框架 R2 暂为限期补齐项，见 §四）。

### 2.1 六维定义与判分标尺（本框架语境）

| 维度 | 一句话标准 | 5 分（满分） | 3 分（及格） | 0 分（FAIL） |
|------|-----------|-------------|-------------|-------------|
| **R1 强制注册入口** | 唯一注册门，同类资源从此进 | 单一注册方法/注解 + 类型签名约束 | 有注册方法但存在旁路 `new` | 散落多处 `new` / 多套并行注册面 |
| **R2 启动断言** | 缺注册则拒绝启动 / 硬告警 | 构造期/启动期抛异常拒启动 | 启动 WARN 日志但不拦截 | 静默缺失，运行时才暴雷 |
| **R3 单一查询 API** | 统一查询出口 | 全框架唯一查询出口，无旁路 | 主出口存在但有少量散查 | 各处散查 / 硬编码白名单 |
| **R4 禁硬编码绕过** | 编译期强约束 | 入参为枚举/类型对象，绕过即编译失败 | 入参为 string 但有运行时校验 | 裸 string 入口无校验 |
| **R5 自动发现** | 新增资源自动被收集 | Spring `List<T>` 自动注入 + `@Order` 排序 | 半自动（扫描 + 手动登记一处） | 手改枚举 / 手抄清单 |
| **R6 契约立宪** | 接口/抽象基类稳定、单一权威源 | 抽象基类 + 接口在 core，单一权威源 | 接口稳定但分散 | 散落实现层 / 多源手工对齐 |

### 2.2 评级映射

| 总分 | 评级 | 处置 |
|------|------|------|
| 28-30 | 🟢 金标准 | 直接 PASS，列为本域强制模板 |
| 22-27 | 🟢 达标 | PASS（硬底线三维须全部 ≥3） |
| 15-21 | 🟡 半成品 | PASS WITH CONDITIONS——限期补齐 |
| 0-14 | 🔴 不达标 | FAIL——CR 打回，禁止合并 |
| 任一硬底线维 = 0 | 🔴 直接 FAIL | 不论总分，CR 打回 |

### 2.3 决策树：新增 MCP 运行时资源时按此自检

```
Q1: 这是"MCP 运行时要用的资源"吗？（解析器/Tool/Resource/Prompt/Complete/Context/结果转换器）
 ├─ 否 → 本宪法不约束（如纯工具类 ConvertUtil 走普通 CR）
 └─ 是 ↓

Q2: 它有唯一注册入口吗？（R1）
 ├─ 否 → FAIL：先建唯一注册门（@ConditionalOnParser Bean / @ToolScan / IXxxContext.addXxx）
 └─ 是 ↓

Q3: 新增同类资源会自动被收集吗？（R5）
 ├─ 否（要手改枚举/手抄清单）→ FAIL：改为 List<T> 自动注入（解析器范式）或扫描器自动发现
 └─ 是 ↓

Q4: 注册接口在 core 层抽象基类/接口立宪、单一权威源吗？（R6）
 ├─ 否 → FAIL：上浮到 AbstractXxxParser / IXxxContext
 └─ 是 ↓

Q5: 漏注册时系统会拒绝运行/告警吗？（R2）
 ├─ 否 → 🟡 限期补启动断言（本框架当前普遍缺，挂 TODO）
 └─ 是 ↓

Q6: 注册入参能被硬编码字符串绕过吗？（R4）
 ├─ 是（如 @ConditionalOnParser 的 value 是裸 string）→ 🟡 限期改枚举
 └─ 否 → 🟢 PASS
```

---

## 三、三把利器（本框架强制代码模板）

> 凡需满足 R5（自动发现）+ R6（契约立宪）的注册域，**必须**套用以下利器之一，不得自创"散落 if-else 判类型 + 手抄清单"。
> 三把利器均引用本项目**真实 file:line**，可直接对照。

### 3.1 利器 A · 抽象基类 + List<T> 自动注入 + @Order 排序（解析器范式 · 本框架金标准）

**权威源**：
- 抽象基类：`server2mcp-core/.../parser/tool/des/AbstractDesParser.java`、`.../param/AbstractParamParser.java`
- 自动注入：`server2mcp-core/.../builder/ToolDefinitionBuilder.java:65`（构造注入 `List<AbstractDesParser>` + `List<AbstractParamParser>`）
- 条件注册 + 排序：`server2mcp-autoconfigure/.../McpConfig.java:225-260`

**心法**：每个解析器继承统一抽象基类（R6 契约立宪），用 `@ConditionalOnParser(value="XXX", type=AbstractDesParser.class)` 注册为 Bean，Spring 在构造 `ToolDefinitionBuilder` 时通过 `List<T>` **自动收集所有已激活的解析器 Bean**（R5 自动发现），`@Order(N)` 决定责任链优先级。新增解析器 = 加一个 `@ConditionalOnParser` Bean，自动进链，零处手抄。

**可复制范式**：

```java
// 1. 继承统一抽象基类（R6 契约立宪 · core 层单一权威源）
public class MyCustomDesParser extends AbstractDesParser {
    @Override
    public String doDesParse(Method method, Class<?> toolClass) {
        // 自定义描述解析逻辑；返回 null 表示交给责任链下一环
        return ...;
    }
}

// 2. 在 McpConfig 注册为条件 Bean（R1 唯一入口 + R5 自动发现 + R4 优先级显式）
@Bean
@ConditionalOnParser(value = "MYCUSTOM", type = AbstractDesParser.class)  // 配置 plugin.mcp.parser.des 含 MYCUSTOM 才激活
@Order(6)                                                                // 责任链顺序，越小越先
public AbstractDesParser myCustomDesParser() {
    return new MyCustomDesParser();
}
```

**为什么是金标准**（本框架现状打分）：
- R1 ✅ 唯一入口（`@ConditionalOnParser` Bean，无旁路 `new` 进链）
- R3 ✅ `ToolDefinitionBuilder` 是描述/参数解析的单一聚合出口
- R5 ✅ `ToolDefinitionBuilder(List<AbstractDesParser>, List<AbstractParamParser>, ...)` 构造注入自动收集（`ToolDefinitionBuilder.java:65`）
- R6 ✅ `AbstractDesParser` / `AbstractParamParser` 在 core 层单一权威源
- R4 🟡 `@ConditionalOnParser.value` 是裸 `String`（如 `"MYCUSTOM"`），拼错不报错（限期可改枚举）
- R2 🔴 漏配 / 拼错 parser 名，`Conditions.ParserCondition`（`autoconfigure/.../Conditions.java`）静默不激活，**无告警**（见 V-2）

**适用场景**：所有描述解析器（`AbstractDesParser` 子类）、参数解析器（`AbstractParamParser` 子类）、任何"一组同类策略按 @Order 组成责任链"的资源。**新增解析器一律走此范式，禁止散落判断。**

### 3.2 利器 B · ImportBeanDefinitionRegistrar 唯一扫描入口（注册链路范式）

**权威源**：
- 入口注解：`server2mcp-core/.../annotation/ToolScan.java`（`@Import(McpToolScanRegistrar.class)`）
- 注册器：`server2mcp-core/.../register/tool/McpToolScanRegistrar.java`（`implements ImportBeanDefinitionRegistrar`）
- 配置器：`server2mcp-core/.../register/tool/McpToolScanConfigurer.java`（`BeanDefinitionRegistryPostProcessor`）
- 扫描器：`server2mcp-core/.../register/tool/ClassPathToolScanner.java`
- 容器接口：`server2mcp-core/.../context/tool/IToolContext.java`（`addTool` / `getRawTools` 单一出入口）

**心法**：`@XxxScan` 注解通过 `@Import` 绑定唯一 `XxxScanRegistrar`（R1 强制入口），Registrar → Configurer → ClassPathScanner 链式把扫描结果统一写入 `IXxxContext`（R3 单一查询出口）。`@McpResourceScan`/`@McpPromptScan`/`@McpCompleteScan` 遵循**完全相同**的四件套模式。

**四件套铁律（新增一类 MCP 实体扫描时必须齐全）**：

| 角色 | 类名模式 | 职责 | 本项目实例 |
|------|---------|------|-----------|
| 入口注解 | `@XxxScan` + `@Import` | 唯一注册门 | `ToolScan` / `McpResourceScan` / `McpPromptScan` / `McpCompleteScan` |
| 注册器 | `McpXxxScanRegistrar implements ImportBeanDefinitionRegistrar` | 读注解属性，登记 Configurer BeanDefinition | `McpToolScanRegistrar` 等 4 个 |
| 配置器 | `McpXxxScanConfigurer implements BeanDefinitionRegistryPostProcessor` | 驱动 ClassPathScanner | `McpToolScanConfigurer` 等 4 个 |
| 容器接口 | `IXxxContext`（`addXxx`/`getRawXxx`） | 单一注册/查询出口 | `IToolContext` / `IResourceContext` / `IPromptContext` / `ICompleteContext` |

> **绝不可做**：新增一类 MCP 实体却只建注解不建 Registrar，或绕过 `IXxxContext` 直接往别处塞——这破坏 R1/R3，是"两份户口本"型违规（V-3）。

**适用场景**：新增需要"扫描用户工程类路径并注册"的 MCP 实体类型（极少发生，但发生时必须四件套齐全）。

### 3.3 利器 C · I{Type}Context 工厂 + 单一查询出口（容器范式）

**权威源**：每类 MCP 实体的 `context/{type}/` 三件套——
- 接口：`IToolContext` / `IResourceContext` / `IPromptContext` / `ICompleteContext`
- 工厂：`ToolContextFactory` / `ResourceContextFactory` / `PromptContextFactory` / `CompleteContextFactory`
- 实现：`ToolContext` / `ResourceContext` / `PromptContext` / `CompleteContext`

**心法**：每类资源的注册定义只活在一个 `IXxxContext` Spring Bean 里（R1 极高内聚 + R3 单一查询出口）。`IToolContext` 的 `Map<String, ToolRegisterDefinition> getRawTools()` 是工具注册定义的**唯一权威源**，Provider 只从这里读，不另开一张表。

**可复制范式**：

```java
// R3 单一查询出口 + R6 接口立宪（core 层）
public interface IXxxContext {
    Map<String, XxxRegisterDefinition> getRawXxx();   // 唯一查询出口
    void addXxx(String name, XxxRegisterDefinition def); // 唯一注册入口
}
```

**为什么是利器**：选用面（扫描注册）与运行面（Provider 暴露）同源——Provider（`McpToolProvider`）只从 `IToolContext.getRawTools()` 取数，杜绝"注册的工具和实际暴露的工具是两套清单"的脱钩（real-agent V-2 病根）。

**适用场景**：所有 MCP 实体的注册定义容器。**禁止**为同一类资源再开第二张 Map / 第二个 VO。

### 3.4 三把利器选型决策树

```
Q1: 这是"一组同类策略按优先级组链"吗？（解析器/转换器）
 ├─ 是 → 利器 A（AbstractXxxParser + List<T> 注入 + @Order）★ 首选
 └─ 否 ↓

Q2: 这是"需要扫描用户类路径并注册的新 MCP 实体类型"吗？
 ├─ 是 → 利器 B（@XxxScan 四件套：注解+Registrar+Configurer+IXxxContext）
 └─ 否 ↓

Q3: 这是"某类资源的注册定义需要一个家"吗？
 └─ 是 → 利器 C（IXxxContext 单一查询出口 + 工厂模式）
```

---

## 四、红线条款 [ENFORCED]

> 列入 CR 必查项。

### 4.1 核心红线

| 禁止行为 | 原因 | 正确做法 |
|---------|------|---------|
| 新增解析器用散落的 `if (type == X) ... else if` 判类型，绕过 `@ConditionalOnParser` + `@Order` 责任链 | 违反 R1/R5，新增类型必改散落判断、必漏 | 套用利器 A：继承 `AbstractXxxParser` + `@ConditionalOnParser` Bean + `@Order` |
| 新增解析器/转换器靠 `new` 手动塞进链，不走 Spring `List<T>` 自动注入 | 违反 R5，漏 `new` 一行静默不生效 | 注册为 Bean，由 `ToolDefinitionBuilder(List<T>)` 自动收集 |
| 同类 MCP 资源散落多个 Context / 多张 Map | 违反 R1 极高内聚；选用面与运行面脱钩 | 收口为单一 `IXxxContext` + 单一 `getRawXxx()` 出口 |
| 新增一类 MCP 实体扫描，注解/Registrar/Configurer/IXxxContext 四件套不齐 | 破坏注册链路完整性，半成品孤儿 | 四件套齐全（利器 B），缺一不合并 |
| Provider 绕过 `IXxxContext` 另开数据源读注册定义 | 注册面与暴露面脱钩，注册了却不暴露 / 暴露了未注册的 | Provider 只从 `IXxxContext.getRawXxx()` 取数 |
| 建完整注册-发现链路却零调用 / 资源既非 Bean 也无人引用（僵尸引擎） | 死代码 + 误导后人 | 注册即接线：标 `@Component`/`@Bean` 被自动收集，或删除僵尸链路 |
| 自定义 `@McpTool(converter=...)` 结果转换器只声明不实现 `McpCallToolResultConverter` | 运行时才暴雷 | 实现接口契约（R6），CR 验证 |

### 4.2 硬底线三维速记

> 凡 MCP 运行时资源新增，CR 第一眼查这三维，任一缺失直接 FAIL：

- **R1 唯一入口**——同类只有一个家（一个 `@ConditionalOnParser` 域 / 一个 `IXxxContext`）
- **R5 自动发现**——新增的会被 `List<T>` 自动收（解析器范式）或扫描器自动发现
- **R6 契约立宪**——接口/抽象基类在 core 层，单一权威源

R2（启动断言）/ R4（禁硬编码 string）为 🟡 限期补齐项，**本框架当前普遍缺 R2，不阻断合并但须挂 TODO**。

---

## 五、违规识别清单（本框架真实风险点）

> 每条对照本框架真实机制。CR 时逐条比对。标 ⚠️ 者为本框架**当前真实存在的成熟度缺口**（诚实标注，非理论）。

| 编号 | 违规反例 | file:line / 机制 | 违反维度 | 正确做法 |
|------|---------|------------------|---------|---------|
| **V-1** | 新增描述解析器时写 `switch(parserType)` 散落判断，不继承 `AbstractDesParser`、不走 `@ConditionalOnParser` | 应有范式 `McpConfig.java:225-260` | R1 ❌ R5 ❌ | 利器 A：继承基类 + `@ConditionalOnParser` Bean + `@Order` |
| **V-2** ⚠️当前缺口 | `plugin.mcp.parser.des` 配错/拼错 parser 名（如把 `JAVADOC` 拼成 `JAVADOCS`），`Conditions.ParserCondition` 静默不激活该解析器，启动零告警，运行时描述缺失才发现 | `autoconfigure/.../Conditions.java`（`ParserCondition.matches` 仅遍历比对，无"已知名集合"校验） | R2 ❌（无启动断言） | 🟡 限期补：对配置的 parser 名做"是否在已知集合内"启动校验，未知名 WARN/抛异常 |
| **V-3** | 新增一类 MCP 实体扫描（假设 `@McpXxxScan`），只建注解不建 `McpXxxScanRegistrar` / 不建 `IXxxContext`，或绕过 Context 往别处塞注册定义 | 应有四件套 `ToolScan.java` + `McpToolScanRegistrar` + `IToolContext` | R1 ❌ R3 ❌ | 利器 B 四件套齐全，注册统一入 `IXxxContext` |
| **V-4** | 同一类工具注册定义散落在 `IToolContext` 之外的第二张 Map，Provider 从那张读 | 应唯一源 `IToolContext.getRawTools()` | R1 ❌ R3 ❌ | 收口单一 Context；Provider 只读 `getRawTools()` |
| **V-5** | 自定义 `McpCallToolResultConverter` 在 `@McpTool(converter=X.class)` 引用，但 X 是空壳/未实现接口契约 | `McpToolProvider` 转换链 | R6 ❌ | 实现接口契约，加测试覆盖 |
| **V-6** | 新增 starter（如未来某 web 框架）复制粘贴 webmvc 的注册逻辑而非复用 core，造成两套并行注册实现 | starter 应只做装配，注册逻辑在 core | R1 ❌ R6 ❌ | starter 只引 core + autoconfigure，注册逻辑单一权威源在 core |
| **V-7** ⚠️当前缺口 | `@ConditionalOnParser(value="...")` 收裸 `String`，拼错的 parser 名编译器零报错 | `ConditionalOnParser.java`（`String value()`） | R4 ❌ | 🟡 限期：`value` 由枚举替代裸 string（破坏性变更，需董事长批准协议变更） |

### 5.1 通用违规模式（举一反三）

CR 时凡见以下模式即按违规处理：

- ❌ "新增解析器我手动 `new` 塞进 list 就行"——漏一行静默不生效（V-1 同型）
- ❌ "先建 Context 类，回头再接 Provider"——半成品孤儿（V-3/V-4 同型）
- ❌ "配置项拼写靠开发者细心"——无启动断言必漂移（V-2 同型）
- ❌ "starter 里复制一份注册逻辑快一点"——双源必偏离（V-6 同型）
- ❌ "`@ConditionalOnParser` 的 value 留 string 灵活点"——君子协定（V-7 同型）

---

## 六、CR 检查项

> 提交/合并涉及"MCP 运行时资源"新增或重构时，AI 自查 + 御史台 Code Review 逐项确认。

### 6.1 解析器（AbstractDesParser / AbstractParamParser 子类）

- [ ] 是否继承统一抽象基类（`AbstractDesParser` / `AbstractParamParser`），而非另起炉灶？（R6）
- [ ] 是否用 `@ConditionalOnParser(value=..., type=...)` 注册为 Bean，由 `ToolDefinitionBuilder(List<T>)` 自动收集？（R1+R5）
- [ ] 是否用 `@Order(N)` 显式声明责任链优先级，且与现有解析器（des 0-5 / param 0-6）不冲突？（R4 优先级）
- [ ] parser 名是否同步登记进默认集合 + 文档（防 V-2 漏激活）？
- [ ] 是否新增对应 `XxxParserTest`（见 [TEST_SPEC.md](./TEST_SPEC.md)）？

### 6.2 扫描类 MCP 实体（新增 @XxxScan）

- [ ] 注解 + Registrar + Configurer + IXxxContext **四件套是否齐全**？（R1）
- [ ] 注解是否 `@Import` 唯一 Registrar？（R1 唯一入口）
- [ ] 注册定义是否统一写入 `IXxxContext`，无第二张表？（R3）
- [ ] 是否遵循与 Tool 完全相同的四件套模式（一致性）？

### 6.3 Context / Provider / 转换器

- [ ] 同类资源是否只有**单一 `IXxxContext` + 单一 `getRawXxx()` 查询出口**？（R1+R3）
- [ ] Provider 是否只从 `IXxxContext` 取数，无旁路数据源？（防 V-4）
- [ ] 自定义结果转换器是否真实实现 `McpCallToolResultConverter` 契约？（R6，防 V-5）
- [ ] 注册接口/抽象基类是否在 core 层、单一权威源？（R6）

### 6.4 starter（新增/修改 starter）

- [ ] starter 是否只做装配（引 core + autoconfigure），注册逻辑单一权威源在 core？（防 V-6）

### 6.5 启动断言（限期补齐项 · 当前挂 TODO）

- [ ] 漏配/拼错 parser 名时，是否有启动校验告警？（R2，当前缺，记 TODO）

---

## 七、与既有体系的关系

| 层级 | 内容 | 与本宪法关系 |
|------|------|-------------|
| 顶层心法 | "绝不遗漏"（董事长命题）/ 让错误不可能 | 本宪法是其在 server2mcp"注册域"的执行接口 |
| 框架文档 | `CLAUDE.md`（处理链路、扩展点） | CLAUDE.md 描述"长什么样"，本宪法规定"如何强制注册不遗漏" |
| 底层范式 | `AbstractDesParser`+`ToolDefinitionBuilder` / `ToolScan` 四件套 / `IToolContext` | 本宪法把三者从"局部实现"升格为"全域强制模板" |
| 平级规范 | [FILE_HEADER_SPEC.md](./FILE_HEADER_SPEC.md) / [TEST_SPEC.md](./TEST_SPEC.md) / [WORK_LOG_SPEC.md](./WORK_LOG_SPEC.md) | 文件头标注注册约束；测试覆盖注册行为；留痕记录注册审计 |

---

## 八、落地动作

1. ✅ **本宪法立宪 [ENFORCED]**（董事长 2026-06-24 批准）
2. ✅ **解析器域已达金标准**（利器 A：`ToolDefinitionBuilder.java:65` List 自动注入 + `McpConfig.java:225-260` `@ConditionalOnParser`+`@Order`）——列为强制模板，新增解析器一律遵循
3. 🚧 **R2 启动断言补齐**（限期项，非红线）：对 `plugin.mcp.parser.param/des` 配置的 parser 名做启动校验，未知名告警/抛异常（消灭 V-2）。属"全局规范/协议层改动"，动工前向董事长申请
4. 🚧 **R4 枚举化**（限期项）：`@ConditionalOnParser.value` 裸 string → 枚举（消灭 V-7）。属 SDK 公开 API 破坏性变更，须董事长批准协议变更

---

## 九、相关文档与范式锚点

- `server2mcp-core/.../parser/tool/des/AbstractDesParser.java` · 利器 A 抽象基类（R6）
- `server2mcp-core/.../builder/ToolDefinitionBuilder.java:65` · 利器 A `List<T>` 自动注入（R5 金标准）
- `server2mcp-autoconfigure/.../McpConfig.java:225-260` · 利器 A `@ConditionalOnParser`+`@Order` 注册
- `server2mcp-autoconfigure/.../conditional/Conditions.java` · `ParserCondition` 配置驱动激活（R2 缺口 V-2 现场）
- `server2mcp-core/.../annotation/ToolScan.java` · 利器 B 唯一入口（`@Import`）
- `server2mcp-core/.../register/tool/McpToolScanRegistrar.java` · 利器 B 注册器
- `server2mcp-core/.../context/tool/IToolContext.java` · 利器 C 单一查询出口（R1+R3）

---

**起草人**：核心规范官 · 2026-06-24 起草立宪
**状态**：**[ENFORCED] · 董事长 2026-06-24 批准** · 版本 1.0.0
