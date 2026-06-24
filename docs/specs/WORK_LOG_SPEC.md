# 工作留痕规范（Work Log Spec）[ENFORCED]

> 来源：继承自 ../real-agent docs/specs/WORK_LOG_SPEC.md · 董事长 2026-06-24 批准方案B全套继承
> 强制度：[ENFORCED]
>
> **定位**：工作产出存档制度——所有报告类产出和 Agent 工作总结必须落文件，禁止直接输出到对话流中消散。
>
> **哲学**：以痕为证。对话是流水，文件是磐石。流水冲刷后什么都不剩，磐石上的每一道刻痕都可追溯。

**版本**：1.0（继承自 real-agent v1.1，剥离前端业务示例，适配 Java 库语境）
**生效日期**：2026-06-24

---

## 一、为什么需要工作留痕

对话流存在三个致命缺陷：

| 缺陷 | 后果 | 留痕如何解决 |
|------|------|-------------|
| **上下文压缩** | 长对话中早期报告被压缩丢失 | 文件进 git，永不丢失 |
| **不可检索** | 无法跨会话搜索历史产出 | `grep`/`find` 秒级定位 |
| **不可复用** | 下次对话无法引用上次的审计结论 | 文件路径即引用地址 |

**与现有知识体系的关系**：

| 层级 | 机制 | 粒度 | 存储 | 核心问题 |
|------|------|------|------|---------|
| **L1 文件头** | `@header-start`（[FILE_HEADER_SPEC.md](./FILE_HEADER_SPEC.md)） | 单文件 | 源码内 | "这个类是什么" |
| **L2 规范** | `docs/specs/` | 跨模块规约 | 仓库内 | "怎么做才合规" |
| **L3 AI 记忆** | Memory | 话题/偏好级 | `~/.claude/.../memory/` | "上次聊了什么" |
| **L4 工作日志** | Work Log | 单次任务产出 | `docs/logs/` | "这次做了什么、结论是什么" |

L4 填补了 L1（太细）和 L2（太重）之间的空白——不是每次工作都值得立规范，但每次产出都值得留痕。

---

## 二、什么必须落文件

### 2.1 报告类产出（禁止直接输出到对话）

| 类型 | 触发场景（本项目语境） | 判定标准 |
|------|----------------------|---------|
| 审计报告 | 注册纪律审计、SDK 升级影响面审计 | 带评分/评级的结构化分析 |
| 研究分析 | MCP SDK / Spring AI API 破坏性变更调研、解析器优先级方案对比 | 多维度对比或深度调查 |
| 代码审查 | code-reviewer / security-reviewer 产出 | 带 severity 的条目化反馈 |
| 计划方案 | 实现计划、重构方案、新解析器/新 Context 设计方案 | 多步骤执行路径 |
| 调试追踪 | tracer / debugger 根因报告（如 OutputSchema 未发送问题排查） | 假设-证据-结论链 |
| 质量验证 | verifier 验收报告 | 通过/不通过的检查清单 |

### 2.2 Agent 工作总结（无条件强制）

**每个 Agent（subagent / team member）完成任务后，必须写一篇工作总结。**

无论任务大小、无论结果成败，一律留痕。这不是可选项。

### 2.3 兜底规则

**超过 30 行的结构化产出，一律落文件。**

不确定是否该落文件？落。宁可多一个文件，不可少一条记录。

---

## 三、存储结构

> **[ENFORCED]** 文件写入 `docs/logs/` 下的**模块子目录**，便于按模块检索。日志量小的早期阶段也可按"主题"分目录。

```
docs/logs/
├── core/              # server2mcp-core 相关（注册链路、解析器、回调、Provider）
├── autoconfigure/     # 自动配置、条件注册、双模式
├── starter/           # webmvc / webflux starter
├── common/            # 工具类、常量、Schema 生成
├── sdk-upgrade/       # MCP SDK / Spring AI 版本升级专题
├── specs/             # 规范起草/演进（本次 specs 继承即落此处）
└── {模块/主题}/       # 其他
```

**正确写法**：`docs/logs/{模块}/{YYYY-MM-DD}_{角色}_{主题}.md`

**示例**：`docs/logs/specs/2026-06-24_核心规范官_specs继承.md`

> 早期日志量少时，允许直接落 `docs/logs/{YYYY-MM-DD}_{角色}_{主题}.md`；积累到一定规模后按上表分目录归档。**与 real-agent 不同**：本项目目前无 hook 拦截根目录写入，靠 CR 与本规范约束。

### 3.1 文件命名

格式：`{YYYY-MM-DD}_{角色}_{主题}.md`

| 字段 | 规则 | 示例 |
|------|------|------|
| 日期 | ISO 8601 日期 | `2026-06-24` |
| 角色 | agent 类型名 / skill 名 / 自定义角色名 | `code-reviewer`、`architect`、`核心规范官`、`CEO` |
| 主题 | 中文简述，不超过 20 字，无空格用驼峰或连字 | `解析器链审计`、`SDK升级影响面` |

**同日同角色同主题**：追加序号 `_2`、`_3`。

### 3.2 CEO 自身产出

CEO（主智能体）的报告类产出同样适用此规范。角色字段填 `CEO`。

---

## 四、文件模板

### 4.1 报告类

```markdown
---
type: report | review | plan | research | debug | audit
agent: {执行者角色}
task: {一句话任务描述}
date: {YYYY-MM-DD HH:mm}
duration: {预估耗时，如 "~3min"}
related: {关联类/模块路径，可选，如 server2mcp-core/.../McpToolProvider.java}
tags: {关键词标签，逗号分隔}
---

# {标题}

## 摘要

{3-5 句话概括核心结论。董事长读这段就够了解全貌。}

## 详情

{完整分析/审计/研究内容}

## 结论与建议

{可操作的下一步建议，按优先级排列}
```

### 4.2 Agent 工作总结（五要素）

> **五要素**：任务 / 过程 / 结果 / 发现 / 建议。缺一不可。

```markdown
---
type: summary
agent: {agent 角色}
task: {一句话任务描述}
date: {YYYY-MM-DD HH:mm}
duration: {实际耗时}
status: completed | partial | failed
related: {关联类/文件路径}
---

# 工作总结：{任务名}

## 任务
{做了什么，为什么做}

## 过程
{关键决策点、执行路径}

## 结果
{产出物清单，修改/新增的文件列表（绝对路径）}

## 发现
{意外发现、风险点、技术债——如"OutputSchema 已解析但被注释未发送"}

## 建议
{后续可改进的地方}
```

---

## 五、对话行为规范

### 5.1 CEO 在对话中的行为

报告写入文件后，CEO 在对话中只输出**摘要引导**：

```
报告已写入 `docs/logs/core/2026-06-24_code-reviewer_解析器链审计.md`

核心发现：解析器链 R5 自动发现满分，但 R2 启动断言缺失（漏配解析器静默生效）。
建议优先补 ToolDefinitionBuilder 构造期断言。

需要我展开某项详情吗？
```

**关键原则**：
- 对话中提供足够的摘要让董事长决策
- 详情在文件中，董事长按需查阅
- 文件路径必须在对话中明确给出（绝对路径）

### 5.2 Agent 的行为

Agent 完成任务后：
1. **先写工作总结**到 `docs/logs/`
2. **再向调度者汇报**结果摘要 + 文件路径
3. 调度者（CEO / team lead）收到后，在对话中呈现摘要

### 5.3 豁免场景

以下场景**不需要**落文件：
- 单行回答（如"这个 bug 在 `McpToolProvider.java:87`"）
- 简单确认（如"已完成修改"）
- 对话性质的讨论和问答
- 代码 diff 展示（代码本身在 git 中）

---

## 六、日志保留策略

- **全量保留**：`docs/logs/` 中的文件随 git 永久保存
- **不做自动清理**：历史日志是项目资产，不是垃圾
- **可手动归档**：积累过多时，可按月归档到 `docs/logs/archive/YYYY-MM/`

---

## 七、规划兑现率 KPI（Plan Fulfillment Rate）[ENFORCED]

> 来源：继承自 real-agent · 解决"承诺的事是否真做到"。
> 与"工作留痕"同源——留痕解决"做了什么可追溯"，本铁律解决"承诺的事是否真兑现"。

### 7.1 核心铁律

**所有 Phase 级 / 战役级规划必须附 MUST-WIN 量化清单**，阶段结束后测量兑现率。**三源交叉验证**，任一源缺失即判该项兑现率为 0。

### 7.2 测量公式

```
规划兑现率 = (MUST-WIN 项在期限内通过验收的数量)
          ÷ (规划中声明的 MUST-WIN 项总数)
          × 100%
```

### 7.3 三源交叉验证（防自证 · 适配库开发）

| 源 | 数据来源 | 必备证据（本项目语境） |
|----|---------|----------------------|
| **规划源** | `docs/plan*.md` / `docs/specs/*.md` | 规划文档中显式标注 "MUST-WIN:" 的清单 |
| **交付源** | `docs/logs/*_summary.md` | Agent 工作总结声明 `status: completed` + 对应 MUST-WIN 引用 |
| **代码源** | `git log` + `grep` + **`mvn test` 通过** | 对应 MUST-WIN 的 commit hash / 测试类（`XxxTest`）/ 功能代码 file:line |

**三源一致才算真兑现**。任一源缺失或矛盾 → 该 MUST-WIN 项兑现率判 0。

> **库开发的代码源尤其严格**：声称"新增了 Swagger 解析器"，代码源必须能 grep 到 `@ConditionalOnParser` 注册 + 对应 `AbstractParamParser` 子类 + `mvn test` 绿。只建类不在 `McpConfig` 注册 = 半成品孤儿（参见 [REGISTRATION_DISCIPLINE_SPEC.md](./REGISTRATION_DISCIPLINE_SPEC.md) V 类违规）。

### 7.4 规划文档强制格式

所有 Phase 级 / 战役级规划必须包含：

```markdown
## MUST-WIN 清单

### MUST-WIN #1: <一句话功能名>
- **成功标准**（量化，任一不达即失败）：
  1. <可验证条件 1，如 "mvn test -Dtest=XxxParserTest 全绿">
  2. <可验证条件 2，如 "解析器在 McpConfig 以 @ConditionalOnParser 注册">
- **如何验证**：<具体命令/测试/demo 路径>
- **负责人**：<agent / 角色>
- **MUST-NOT**（≥2 条明示红线，违反任一即失败）：
  1. <如 "禁止散落 if-else 判类型绕过 @ConditionalOnParser">
  2. <如 "禁止改动 IToolContext 公开接口签名">
```

缺 MUST-WIN 清单的 Phase 规划 = **无效规划**，验收方有权拒收。

### 7.5 处理阈值

| 兑现率区间 | 处理 |
|----------|------|
| ≥ 85% | 正常推进 |
| 70% – 85% | **黄色预警** · 根因分析 + 下阶段 MUST-WIN 数量自动砍半 |
| 50% – 70% | **红色预警** · 暂停新规划 + 补课模式 + 向董事长面呈 |
| < 50% | **熔断** · 项目暂停 · 董事长裁决是否止损 |

---

## 八、偏离报告铁律（Deviation Report Protocol）[ENFORCED]

> 来源：继承自 real-agent · 与"工作留痕"同级——留痕解决"做了什么可追溯"，偏离报告铁律解决"做之前得批准"。

### 8.1 铁律

executor（subagent / team member）发出**偏离报告**后，必须**停止所有后续代码改动**，等 CEO / team-lead 明确批准再继续。

- 偏离报告本身的价值就是 **"停下来等批准"**
- CEO 的 scope 控制 **优先于** executor 的工程判断
- executor 如判断 CEO 指令有问题 → 正确做法是 `SendMessage` 质疑 + 等批准改指令
- 严禁"先斩后奏"——即使产出正确，也违反治理铁律

### 8.2 偏离递进模式

| 等级 | 模式 | 处置 |
|------|------|------|
| L1 | 未等批准直接执行（产出正确） | 赦免 + 自我反思 |
| L2 | 违反明确指令执行（产出可接受） | 批评 + 接受产出 + 记录 |
| L3 | 违反指令 + 交付声明与事实不符 | 不可接受 + 深度制度反思 |

### 8.3 executor 交付声明模板（强制）

发 "已交付" 类消息前必须附以下三项证据（本项目语境）：

```
1. ls -la {新建文件绝对路径}        ← 证明文件真实存在
2. git status                       ← 证明工作树与声明一致
3. cd server2mcp-core && mvn test   ← 证明编译 + 测试通过（或对应模块）
   （单测：mvn test -Dtest=XxxTest）
```

未附此三项证据的"已交付"声明，审查方**不予采信**。

---

## 九、验证方法

### 人工验证
- 审查对话：是否有超过 30 行的结构化产出直接输出在对话中？
- 审查 `docs/logs/`：Agent 工作是否都有对应的总结文件？
- 抽查工作总结：五要素是否齐全？`related` 路径是否物理存在（`ls -la` 核验）？

### 自动验证（未来可扩展）
- CI：检查 `docs/logs/` 中文件是否符合 frontmatter 格式
- Hook：Agent 返回结果时检查是否创建了 `docs/logs/` 文件

---

## 十、与其他规范的关系

| 规范 | 关系 |
|------|------|
| [FILE_HEADER_SPEC.md](./FILE_HEADER_SPEC.md) | 工作日志文件本身不需要 `@header-start`（frontmatter 已足够） |
| [REGISTRATION_DISCIPLINE_SPEC.md](./REGISTRATION_DISCIPLINE_SPEC.md) | 兑现率代码源验证依赖"注册即生效"——半成品孤儿不算兑现 |
| [TEST_SPEC.md](./TEST_SPEC.md) | 交付声明三件套含 `mvn test`；TDD 双 commit 提供代码源可追溯性 |

---

## 更新日志

| 日期 | 版本 | 变更内容 |
|------|------|---------|
| 2026-06-24 | 1.0 | 继承自 real-agent v1.1，剥离前端业务示例，三源交叉验证适配库开发（代码源含 mvn test + @ConditionalOnParser 注册验证） |

---

**维护者**：api2mcp4j Team
