# 破坏性删除 & 结构性变更前置检查清单

> 触发条件 A（删除）：任何 `rm` / `git rm` / `Delete` 操作对**公开 API**（框架注解 `@McpTool`/`@McpResource`/`@McpPrompt`/`@McpComplete`/`@McpArg`/`@ToolScan` 及其属性、Provider 公开类、解析器扩展点 `AbstractDesParser`/`AbstractParamParser`、`IXxxContext` 接口、回调抽象类、`McpCallToolResultConverter` SPI、public 类 / 方法 / 枚举值）
> 触发条件 B（结构性变更）：批量修改**被广泛使用的代码模式**（注解属性重命名 / 解析器 `@Order` 优先级重排 / `@ConditionalOnParser` value 变更 / Provider 过滤链改造 / 方法签名重构），必须在改之前先看清所有使用点的结构
> 强制度：[ENFORCED]
> 来源：继承自 ../real-agent docs/rules/global/destructive-deletion.md · 董事长 2026-06-24 批准方案B全套继承
> 御史台巡查强制项

---

## § 零、四个维度——为什么缺一不可（已诚实适配本项目工具表）

破坏性变更的影响面存在于**四个独立维度**，每个维度只有专属手段才能覆盖：

| 维度 | 手段（本项目真实可用） | 回答的问题 | 漏掉什么 |
|------|----------------------|-----------|---------|
| **字面量层** | `grep` 多形式 | 这个名字的字符串出现在哪里？ | 注释/字符串内的假匹配；反射/泛型间接引用 |
| **结构层** | 若有 AST 工具用之；否则 **grep 多形式 + 人工结构审查** | 这个符号以什么语法结构被使用？ | （其他三层都无法回答此问题） |
| **语义层** | LSP `find_references`（jdtls + LSP MCP 桥接，桥接未激活时降级） | 这个类型被谁在类型系统中依赖？ | 结构性使用模式；反射/SPI 动态加载 |
| **图谱层** | **多模块 Maven 反向依赖分析**（`mvn dependency:tree` + 依赖流向 + 下游兼容性） | 这个改动会波及哪些模块 / 下游用户？ | 同文件内的结构；运行期动态引用 |

**结构层是第四维，不是第一维的升级版。** 它回答"模式"，不回答"名字"。

> **图谱维度本项目特化说明**：本项目是 `common ← core ← autoconfigure ← starters` 多模块库（依赖流向见项目 CLAUDE.md），且**产物被下游用户依赖**。因此"图谱层"= **跨模块波及 + 语义化版本（API 兼容性）影响**。删一个 core 公开类，波及面不止本仓 autoconfigure/starters，还有所有 import 了它的下游应用——这是 real-agent `impact_radius` 在"被依赖的库"语境下的诚实落地。

---

## § 一、删除前必须执行（不可跳过）

### 1. grep 多形式（至少三条并行）

- 类名 / 注解名字面量：`grep -rn "McpToolDesParser" server2mcp-core/src/`
- 驼峰 / Bean 名变体：`grep -rn "mcpToolDesParser\|MCPTOOL" server2mcp-autoconfigure/src/`
- 路径 / 文件名片段：`grep -rn "McpToolDesParser.java" .`

### 2. LSP find_references（语义层）

- 工具：LSP `find_references`（依赖 jdtls + 会话暴露的 LSP MCP 桥接）
- **若桥接不可用**（本会话未暴露 LSP 工具，或 jdtls 未就绪），必须**事前声明不可用**（Briefback 第六项能力自检），由 CEO 裁决是否接受 §三 双源代偿方案
- 诚实边界：`/opt/homebrew/bin/jdtls` 二进制存在 ≠ 本会话能调用它；能否调用取决于会话工具表是否暴露 LSP 桥接

### 3. 多模块 / 下游兼容性评估（图谱层）

- `mvn dependency:tree` 看哪些模块依赖目标所在模块
- 逐条回答：
  - 目标在哪个模块？（common / core / autoconfigure / starter）
  - 本仓哪些下游模块 import 了它？（grep import + 模块依赖流向）
  - 它是否属**公开 API 表面**（注解 / Provider / SPI / Context 接口）？是 → 下游用户可能依赖 → 删除即**破坏性版本变更**（需 MAJOR 版本号 + 迁移说明）
- 读取并归档：受影响模块清单 / 受影响 import 点数 / 是否触及公开 API

---

## § 一·五、结构性变更：结构层与 grep 平级使用

> AST 检索工具在"结构性场景"下本应是首选搜索工具。**本环境无 AST 工具**，故结构层以"grep 多形式 + 人工结构审查 + 编译兜底"代偿；代偿不免除影响面量化义务。
> 详见 `search-tool-parity.md` 速查表。

### 触发信号（量化触发器，不依赖主观判断）

**满足以下任一条件 → 结构层审查必须做**：

1. **影响面阈值**：`grep -rln "<symbol>" server2mcp-core/src | wc -l` ≥ 3 个文件
2. **结构变更阈值**：commit 预计涉及 ≥ 5 处 `@Override` / `case` / `instanceof` / `@Order` / `@ConditionalOnParser` 行变化
3. **批量替换信号**：任何"把 A 模式替换为 B 模式"的重构

典型场景（全部 Java/MCP 语境）：
- **删除注解属性**（如从 `@McpTool` 删 `converter()` 属性 → 所有 `@McpTool(converter=...)` 使用点编译失败）
- **折叠枚举 / 常量**（如把 `MineTypeConstants` 某常量并入另一个）
- **解析器优先级重排**（`@Order(0..5)` 重新分配 → 解析结果优先级语义改变）
- **`@ConditionalOnParser` value 变更**（如把 `"SWAGGER3"` 改名 → `plugin.mcp.parser` 配置 key 同步失效）
- **Provider 过滤链改造**（`includeFilters` / `excludeFilters` / `includeToolFilters` 语义变更）
- **回调签名变更**（`AbstractMcpToolMethodCallback` 的 `doCall` 等模板方法签名变更 → 三个回调子类 + 下游自定义子类全受影响）
- **webflux starter 中 `Mono`/`Flux` 的 `.block()` 引入/移除**（响应式线程模型反模式）

### 4. 结构扫描（改前 + 改后）

- 有 AST 工具：改前列出所有使用点结构分布（改动地图），改后验证零残留
- 无 AST 工具（本环境）：grep 多形式 + 人工逐处判形态，改后再 grep + `mvn clean install` 兜底确认零残留
- 典型代偿模式示例：

```
# 找所有 @McpTool 的结构性使用（区分裸用 vs 带属性）
grep -rn "@McpTool" server2mcp-test/src/   # 命中后人工区分 @McpTool / @McpTool(name=..) / @McpTool(converter=..)

# 找所有 @Order 装配点，人工核对优先级 0-5 是否连续
grep -rn "@Order" server2mcp-autoconfigure/src/

# 找所有 @ConditionalOnParser 的 value，确认配置 key 一致性
grep -rn "@ConditionalOnParser" server2mcp-autoconfigure/src/

# 找 webflux starter 中的 .block() 反模式
grep -rn "\.block()" server2mcp-spring-boot-starters/server2mcp-starter-webflux/
```

### 工具序位心法

- **grep** = 字面量层（找名字、字符串、注释、import）
- **结构层** = 模式层（找注解组合、调用形态、`@Order` 分布）——本环境用 grep 多形式 + 人工审查代偿
- 两者**平级**，按问题类型选，不互相替代
- 凡涉及"模式"的搜索 → 结构层手段优先
- 凡涉及"名字"的搜索 → grep 优先

---

## § 二、不可替代项（明示禁令）

| 禁止替代方案 | 原因 |
|-------------|------|
| `mvn clean install` EXIT=0 | 类型安全 ≠ 影响面量化；且无法暴露**下游用户**（不在本仓）的引用 |
| `cd server2mcp-core && mvn test` EXIT=0 | 测试覆盖 ≠ 引用完整性 |
| IDE 自带 find references | 非可审计、非命令行可复现 |
| 单独的 grep | 字面量扫描漏反射 / 泛型 / SPI 动态加载 / 下游依赖 |
| 用 grep 替代结构层审查 | 字符串匹配会被注释 / 字符串内容污染；无法判注解组合等语法结构 |
| 用 `mvn compile` 装作执行了 LSP | 维度不正交，详见 §三 双源充分性原则 |

**核心命题**：四维目标是**影响面量化**，compile 目标是**类型安全**。两者目标不同、能力不交、不可互换。**尤其本项目是被依赖的库**——compile 只能证明本仓不报错，证明不了下游用户不报错。

---

## § 三、双源代偿决策表（适配多模块库 + 下游依赖）

当四维中某项不可用（如本会话 LSP 桥接未激活、本环境无 AST 工具），按以下表格决策：

| 删除对象类型 | 可否双源代偿 | 代偿方案 |
|-------------|-------------|---------|
| **公开 API**（框架注解及属性 / Provider 公开类 / `IXxxContext` 接口 / `AbstractDesParser`·`AbstractParamParser`·`McpCallToolResultConverter` SPI） | ❌ 不可 | 必须装齐四维（含下游兼容性评估）；删除 = MAJOR 版本破坏性变更 |
| **public Service / 工厂 / Builder 方法**（如 `ToolDefinitionBuilder` 公开方法） | ❌ 不可 | 必须装齐四维 |
| protected / package-private 方法 | ⚠️ CEO 裁决 | grep 多形式 + 多模块反向依赖分析 |
| private 方法（仅本类调用） | ✅ 可 | grep + 人工确认仅本类引用 |
| Legacy 死分支（grep 证明本仓零引用 + 非公开 API） | ✅ 可 | grep 单源 + 声明（须确认非下游可见符号） |
| 已删除符号的后验 | ✅ 可 | grep + 多模块扫描（LSP 对已删符号返回"不存在"，为空操作） |

**核心判据**（双源充分性原则）：
- 双源必须**能力维度正交**才充分
- grep（字面量层）+ 多模块反向依赖分析（图谱层）= 正交，对非反射 / 非 SPI 调用充分
- grep + `mvn compile`（都偏字面量 / 类型层，且都不覆盖下游）= 非正交，不充分

> **本项目特别警示**：解析器 `AbstractDesParser` / `AbstractParamParser` 与 `McpCallToolResultConverter` 是**通过 SPI / `@Bean` 动态装配**的扩展点。grep 可能找不到通过反射或下游 `@Bean` 注册的实现。涉及这类 SPI 的删除，**双源代偿不充分**，必须装齐四维 + 在 CHANGELOG 显式标注破坏性变更。

---

## § 四、四维输出必须归档

删除动作的工作总结必须附（写入 `docs/logs/`，见 `work-log.md`）：
- grep 原始输出（含零匹配的零输出）
- LSP 原始结果（或"桥接不可用"事前声明）
- 多模块 / 下游兼容性评估摘要（受影响模块清单 / 受影响 import 点数 / 是否触及公开 API / 是否需 MAJOR 版本号）
- 结构层审查记录（有 AST 工具的扫描结果，或无工具时的 grep 多形式 + 人工核对记录）

使用双源代偿的删除，工作总结必须包含：
- 删除对象类型归类（对应 §三 哪一行）
- CEO 裁决证据（若需裁决）
- 代偿源的正交性说明

---

## § 五、违规处理

- 四维证据缺失的删除 commit，御史台有一票否决权
- 删除公开 API 而未在 CHANGELOG / 版本号体现破坏性变更 → 御史台一票否决（下游用户会被静默破坏）
- 责任方须补写证据章节
- 连续两次违规 → 机制级排查（是工具问题还是纪律问题？）

---

## § 六、与其他规则的关系

- **事前声明**能力可用性（AST 工具缺席 / LSP 桥接未激活） → `agent-capability-declaration.md`
- **commit 顺序**避免契约真空期 → `refactor-ordering.md`
- **跨 session 续接**时的状态核验 → `session-continuity.md`
- **搜索工具序位** → `search-tool-parity.md`

这四条规则共同构成"破坏性变更治理"体系。

---

**立法者**：心法规则官（依据董事长 2026-06-24 批准方案B 起草；图谱维度诚实适配为多模块 Maven 反向依赖 + 下游语义化版本兼容性）
**颁布于**：2026-06-24
