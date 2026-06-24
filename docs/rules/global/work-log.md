# 工作留痕规则

> 触发条件：所有 agent 执行任务时、所有报告类产出时
> 强制度：[ENFORCED]
> 来源：继承自 ../real-agent docs/rules/global/work-log.md · 董事长 2026-06-24 批准方案B全套继承 · 董事长 2026-04-10 直接指示

---

## § 一、核心铁律

1. **报告类产出禁止直接输出到对话**，必须写入 `docs/logs/{YYYY-MM-DD}_{角色}_{主题}.md`
2. **每个 Agent 完成任务后必须写工作总结**到 `docs/logs/`
3. 对话中只输出**摘要 + 文件路径**，详情在文件中

> 本项目落点：日志目录为 `docs/logs/`；计划目录为 `docs/`（或 `docs/plans/`，沿用 CEO 现有 `docs/plan-*.md` 命名）。

---

## § 二、什么算报告类

- 审计报告（架构审计、代码审查产出）
- 研究分析（依赖分析、SDK 兼容性调研、竞品/上游 MCP SDK 调研）
- 代码审查（code-reviewer / security-reviewer / 御史台产出）
- 计划方案（实现计划、重构方案、迁移方案）
- 调试追踪（tracer / debugger 根因报告）
- 质量验证（verifier 验收报告）
- 删除 / 破坏性变更的四维证据归档（见 `destructive-deletion.md` §四）

---

## § 三、兜底规则

**超过 30 行的结构化产出，一律落文件。**

---

## § 四、文件命名

`docs/logs/{YYYY-MM-DD}_{角色}_{主题}.md`

- 角色：agent 类型名 / skill 名 / 职能名，kebab-case 或中文职能名
- 主题：中文简述，≤20 字
- 同日重复：追加 `_2`、`_3`

示例：`docs/logs/2026-06-24_心法规则官_global规则继承.md`

---

## § 五、文件模板

必须包含 YAML frontmatter：

```yaml
---
type: report | summary | review | plan | research | debug | audit
agent: {角色}
task: {一句话描述}
date: {YYYY-MM-DD HH:mm}
duration: {耗时}          # 可选
status: completed | partial | failed   # 仅工作总结需要
related: {关联文件}        # 可选
tags: {标签}              # 可选
---
```

---

## § 六、豁免场景

- 单行回答
- 简单确认
- 对话性讨论
- 代码 diff 展示

---

## § 七、Agent 工作总结必含五要素

1. **任务**（做了什么）
2. **过程**（关键决策点）
3. **结果**（产出物清单 · 绝对路径）
4. **发现**（意外 / 风险 / 与原描述不符之处）
5. **建议**（后续改进 / 需 CEO 复核点）

---

## § 八、与其他规则的关系

- 删除 / 破坏性变更的四维证据归档落点即本规则的 `docs/logs/` → `destructive-deletion.md` §四
- Agent 宣誓（Briefback）与工作总结首尾呼应 → `agent-capability-declaration.md`
- 续接时通过 `docs/logs/` 历史记录辅助核验（但 git / 文件系统仍是状态权威） → `session-continuity.md`

---

**立法者**：心法规则官（依据董事长 2026-06-24 批准方案B 起草；路径适配为本项目 docs/logs/ 与 docs/ 计划目录）
**颁布于**：2026-06-24
