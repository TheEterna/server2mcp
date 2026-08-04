# Agent 能力声明机制

> 触发条件：任何 agent 接受任务时、工作协议宣誓（Briefback）阶段
> 强制度：[ENFORCED]
> 来源：继承自 ../real-agent docs/rules/global/agent-capability-declaration.md · 董事长 2026-06-24 批准方案B全套继承 · 赵匡胤·机制设计

---

## § 一、物理不可能性原则

**Physical Impossibility Principle**：

如果某项操作在当前环境下**物理上不可能执行**（工具未安装、桥接未暴露、权限不足、网络隔离、依赖缺失），agent 必须**在宣誓阶段**声明限制，由 CEO / 董事长裁决替代方案。

**严禁**事后补救或装作已执行。

"物理"不是字面意义，而是**环境约束层**——该约束 agent 自己无法解除，只能声明。

> 本项目典型实例：`ast-grep` 二进制未安装（结构层无 AST 工具）；`jdtls` 二进制虽装在 `/opt/homebrew/bin/jdtls`，但**会话工具表是否暴露 LSP MCP 桥接**才决定语义层能否真正调用——二者必须分开声明，不可混为一谈。

---

## § 二、Briefback 宣誓扩展（第六项 · 环境能力自检）

原工作协议宣誓五项（见全局 `~/.claude/CLAUDE.md` Briefback Protocol）：
1. 任务理解
2. 红线清单
3. 交付标准
4. 绝不会做的事（≥3 条）
5. 偏离处理预案

**第六项 · 环境能力自检**（本规则强制项）：

```markdown
## 六、我的环境能力自检

| 能力 | 依赖 | 状态 | 替代方案 |
|------|------|-----|---------|
| grep / Grep（字面量层） | Bash | ✅ 可用 | — |
| 结构层 AST 检索 | ast-grep / sg 二进制 | ❌/✅/❓ | 不可用 → grep 多形式 + 人工结构审查代偿 |
| LSP find_references（语义层） | jdtls + 会话 LSP MCP 桥接 | ❌/✅/❓ | 不可用 → grep 多形式 + mvn compile 类型兜底，CEO 裁决 |
| 多模块反向依赖分析（图谱层） | mvn dependency:tree + 人工 | ✅ 可用 | — |
| 构建 / 测试 | mvn 3.9.x + JDK 17 | ✅/❓ | — |
| ... | ... | ... | ... |

不可用项 → 提出替代方案或申请解除 → 等 CEO 裁决
```

---

## § 三、物理不可能性清单（本项目参考 · 已实测标注）

| 能力 | 依赖 | 验证命令 | 本环境实测（2026-06-24） |
|------|------|---------|------------------------|
| grep / Grep | Bash | 直接调用 | ✅ 可用 |
| 结构层 AST 检索 | ast-grep / sg | `which sg ast-grep` | ❌ 未安装 → 结构层靠 grep + 人工代偿 |
| LSP find_references (Java) | jdtls + LSP MCP 桥接 | `which jdtls` + 会话是否暴露 LSP 工具 | ⚠️ jdtls 二进制在 `/opt/homebrew/bin/jdtls`，但**会话 LSP 桥接需逐次确认**；未暴露则语义层降级 |
| 多模块反向依赖分析 | mvn + 人工 | `mvn dependency:tree` | ✅ 可用 |
| 构建 | mvn + JDK 17 | `mvn -version` / `java -version` | ✅ mvn 3.9.12 / OpenJDK 17 |
| 运行测试 | core 模块 test | `cd server2mcp-core && mvn test` | ✅ 可用（注意 server2mcp-test 需单独构建） |
| 启动 / 部署服务 | 权限 + 端口 | — | 红线禁止主动启停（默认按未运行处理，需启动先请示） |

> **严禁照抄** real-agent 的 `mcp__plugin_oh-my-claudecode_t__ast_grep_search` / `mcp__plugin_oh-my-claudecode_t__lsp_find_references` / `mcp__code-review-graph__get_impact_radius_tool` 等工具名——**本环境不存在这些工具**。声明时只能写本会话工具表里真实存在的能力。

---

## § 四、禁止行为

- ❌ 声称执行了不可能执行的操作（如声称跑了 AST grep，而本环境无 ast-grep）
- ❌ 捏造本会话不存在的工具名来"证明"做了某事
- ❌ 用"EXIT=0" / "通过"等模糊措辞掩盖"根本没执行"
- ❌ 在事后补救章节而非事前声明章节提及环境缺陷
- ❌ 用别的命令（如 `mvn compile`）装作执行了指定能力（如 LSP find_references）——维度不正交

---

## § 五、正确模式

### 事前声明（宣誓阶段）：

```
[能力核对] 结构层 AST 检索 — ❌ 不可用（ast-grep / sg 未安装）
[替代方案] grep 多形式 + 人工结构审查 + mvn clean install 编译兜底

[能力核对] LSP find_references — ❓ 本会话未暴露 LSP MCP 桥接（jdtls 二进制虽在）
[替代方案] grep 多形式 + 多模块反向依赖分析双源代偿，请 CEO 裁决
```

### CEO 裁决流程：

- 如 CEO 批准代偿 → agent 在工作总结中显式声明"LSP / AST 环节缺席，双源代偿通过"
- 如 CEO 否决代偿 → 暂停任务，请求安装 / 暴露环境
- 如 CEO 要求 "物理不可能性" 判定 → 请御史台使用 §一 原则裁决

### 事后记录（若 CEO 批准代偿）：

```
[证据] grep 零引用 + 多模块反向依赖分析（受影响模块 0）— 双源交叉验证
[缺陷] AST 结构层缺席（无工具），grep 多形式 + 人工审查代偿（CEO 批准）
[缺陷] LSP 语义层缺席（桥接未暴露），编译兜底代偿（CEO 批准）
```

---

## § 六、违规处理

事后补救型"装作已执行"：
- 御史台可判工作总结作废
- 责任方重做
- 连续违规 → 升级至董事长级诚信审查

---

## § 七、与其他规则的关系

- 本规则是 **赵匡胤·机制设计** 在 agent 宣誓层面的落地
- 与 `destructive-deletion.md` 的 §三 双源代偿表配套使用
- 与 `session-continuity.md` 同源——都要求"先核验状态，再做断言"
- 与 `search-tool-parity.md` 配套——四维工具的可用性正是本规则要声明的对象

---

**立法者**：心法规则官（依据董事长 2026-06-24 批准方案B 起草；能力清单按本环境 2026-06-24 实测填写，诚实标注 ast-grep 未装 / jdtls 需桥接）
**颁布于**：2026-06-24
