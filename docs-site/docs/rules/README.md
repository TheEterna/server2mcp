# api2mcp4j · Rules 中央仓库

> 本项目（内部名 server2mcp）的通用开发心法层。
> 来源：继承自 ../real-agent docs/rules/ · 董事长 2026-06-24 批准方案B全套继承（仅继承"纯心法层"，剥离全栈/前端/设计招式）

---

## § 一、这是什么

`docs/rules/global/` 收录与技术栈无关的**通用开发心法**——从姊妹项目 `../real-agent` 沉淀、剥离前端/设计招式后，诚实适配到本项目 Java / Maven / Spring / MCP 语境的 6 条规则。

它们回答的不是"怎么写 Java"，而是"AI/团队怎么严谨地协作、删除、续接、留痕、声明能力"。

---

## § 二、6 条全局规则

| 文件 | 一句话 | 强制度 |
|------|--------|--------|
| `global/search-tool-parity.md` | 搜索四维（字面量/结构/语义/图谱）互不替代；本环境无 AST 工具时结构层用 grep + 人工代偿 | [ENFORCED] |
| `global/destructive-deletion.md` | 删公开 API（注解/Provider/解析器 SPI/Context 接口）前走四维检查 + 多模块下游兼容性评估 | [ENFORCED] |
| `global/session-continuity.md` | 跨 session 判断"X 是否完成"，第一动作是 git/文件验证，不凭记忆断言 | [ENFORCED] |
| `global/refactor-ordering.md` | 契约提供者先行，依赖者后置；跨模块按 common←core←autoconfigure←starters 流向 | [ENFORCED] |
| `global/agent-capability-declaration.md` | 工具不可用（如 AST/LSP）须在 Briefback 第六项事前声明，不装作已执行 | [ENFORCED] |
| `global/work-log.md` | 报告类产出落 `docs/logs/`，对话只给摘要+路径；Agent 五要素总结 | [ENFORCED] |

四条治理类规则（destructive-deletion / session-continuity / refactor-ordering / agent-capability-declaration）共同构成"破坏性变更治理"体系；search-tool-parity 是其搜索工具基座；work-log 是其留痕基座。

---

## § 三、加载机制（据实说明 · 本项目不引入意图协议）

> **与 real-agent 的关键差异**：real-agent 是全栈多模块项目，用 `.claude/task.json` + SessionStart hook 做"意图驱动加载"（按选中的业务模块只读对应 rules）。

**本项目是单一 Java 库，不引入 real-agent 那套「意图驱动加载协议」**——没有多业务模块需要"按选中意图只读对应 rules"的意图锁。

> 说明（方案 B 已建 task.json）：本项目的 `.claude/task.json` 确实存在，但**仅作静态物理模块注册表 + 授权提示清单**（common/core/autoconfigure/starter/test），由 `session-start.sh` 在会话启动时输出模块概览；它**不承担意图锁职责**，与 real-agent 的意图驱动加载是两回事。`docs/rules/global/` 的通用心法默认对所有会话生效，无需意图触发。

因此本项目的加载机制极简：

```
docs/rules/global/ 下的 6 条规则 = 默认对所有会话生效的通用心法层
```

- 这些是**通用**规则，与任何具体改动无关，AI/团队成员应视为常驻背景纪律
- 涉及删除 / 重构 / 续接 / 留痕 / 能力声明的场景，按对应规则的"触发条件"自查
- 路径在 `docs/` 下，不依赖 Claude Code 官方 globs 自动触发——靠规则本身的触发条件 + 团队纪律驱动

---

## § 四、维护规则

### 新增全局规则

1. 在 `docs/rules/global/` 新建 `{规则名}.md`
2. 文件顶部必须带：
   ```
   > 来源：... · 董事长 {日期} 批准
   > 强制度：[ENFORCED]
   ```
3. 在本 README §二 表格登记一行
4. 全局规则默认对所有会话生效，无需注册到任何 task.json（本项目无此机制）

### 修改 / 删除全局规则

- 修改 / 删除 `docs/rules/` 属**全局规范变更**，触及红线（见 `~/.claude/CLAUDE.md` 规范变更协议）→ **必须事前向董事长申请**，获批后执行

---

## § 五、与项目 CLAUDE.md 的关系

- 项目 `CLAUDE.md` 是架构宪法（模块/处理链路/约定/扩展点），由 CEO 亲自操刀
- 本目录是**通用心法层**，是 CLAUDE.md 的方法论补充，不重复架构内容
- 二者不冲突：CLAUDE.md 答"系统长什么样"，本目录答"怎么严谨地改它"

---

## § 六、历史

- **2026-06-24**：从 `../real-agent docs/rules/global/` 继承 6 条纯心法层规则，剥离前端/设计招式，诚实适配本项目 Java/Maven 工具表（无 AST 工具、LSP 需桥接、图谱层=多模块反向依赖）。不引入 real-agent 的 task.json 意图协议与 15 modules/12 layers 全栈目录。

---

**维护者**：心法规则官（董事长 2026-06-24 批准方案B）
