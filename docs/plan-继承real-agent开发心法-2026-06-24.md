# 继承 real-agent 开发心法 · 实施方案

> 来源：董事长 2026-06-24 直接指示「全面在本项目中继承 ../real-agent 项目的开发心法」
> 性质：触及红线（项目 `CLAUDE.md` 修改 + `docs/specs/` 创建）→ 须事前请示后执行
> 状态：**已完成（2026-06-24）· 御史台 PASS WITH CONDITIONS · 4 项 🟡 必修全闭环 · 闭环简报见 docs/logs/2026-06-24_CEO_继承心法战役闭环简报.md**

---

## 〇、董事长决议（2026-06-24）

CEO 推荐方案 A（精炼但全面），**董事长行使最终决策权，拍板方案 B（全套基建搬迁）**：
- 连 `.claude/agents`（裁剪 82 → 保留约 10）、`agent-groups.json`、`scripts/agent-groups.mjs`、`hooks`、`task.json` 一并继承。
- `task.json` 降级为**轻量物理模块注册表**（common/core/autoconfigure/starter/test），不引入 real-agent 的 per-module `plans_dir/specs_dir` 意图锁复杂度。
- **文件头回填**（下方落地物 #5）作为**后续独立 Sprint**，本次仅立 `FILE_HEADER_SPEC` 规范。

> 故下方第三节「方案 A」的「不做」约束已被方案 B 覆盖——以本决议为准。

---

## 一、第一性原理：心法 vs 招式

**核心判断**：`real-agent` 是全栈 Web 项目（Vue3 + Spring DDD + Next.js），`api2mcp4j` 是**纯 Java 库**（Spring Boot Starter，把 Controller 自动暴露为 MCP 工具）。两者技术躯体完全不同，因此：

- **心法（继承）** = 与技术栈无关的工作方法论：授权治理、文档留痕、注册纪律、破坏性删除防护、TDD 节奏、文件头可读性、三方制衡。
- **招式（剥离）** = real-agent 特有、对 Java 库无意义的内容：前端设计令牌 / 品牌色板 `.impeccable.md` / SSES 生图 / 灵感库 / 设计笔 / VOLO 事件协议 / Vue/Pinia/GSAP 规则 / 82 个前端·设计·PM agent / pm-* 命令。

> 原则锚点：全局 Rule #9「适度设计——目前不需要就不引入」+ Rule #4「后端重构：功能/接口零变更、结构更规范」。

---

## 二、心法清单与去留裁定

| 心法载体（real-agent） | 裁定 | 适配说明 |
|---|---|---|
| `~/.claude/CLAUDE.md` 自决默认协议 / 三方制衡 / Briefback | **已全局生效** | 无需搬运，项目层只需引用 |
| `docs/rules/global/search-tool-parity.md` | 继承 | grep vs AST grep 序位，通用 |
| `docs/rules/global/destructive-deletion.md` | **重点继承** | 框架有公开 API（注解/Provider/解析器扩展点），删除防护极关键；适配为 Java/LSP/grep 三件套 |
| `docs/rules/global/session-continuity.md` | 继承 | 续接先验证 git 状态，通用 |
| `docs/rules/global/refactor-ordering.md` | 继承 | 契约提供者先行，通用 |
| `docs/rules/global/agent-capability-declaration.md` | 继承 | Agent 环境能力自检，通用 |
| `docs/rules/global/work-log.md` | 继承 | 工作留痕，通用 |
| `docs/specs/FILE_HEADER_SPEC.md` | **重点继承** | AI 可读性文件头 `@header-start/@end`，对 AI 协作高价值；适配 Java 注释模板 |
| `docs/specs/WORK_LOG_SPEC.md` | 继承 | 报告落 `docs/logs/`、Agent 五要素总结 |
| `docs/specs/REGISTRATION_DISCIPLINE_SPEC.md` | **极契合继承** | 本框架内核就是"注册"（parser/tool/resource/context 注册）；R1-R6 六维成熟度 Rubric 天然适配 |
| `docs/specs/TEST_SPEC.md` | 继承 | 测试金字塔 + TDD 双 commit（[RED]/[GREEN]）；适配 JUnit5 |
| `docs/specs/NATURAL_LANGUAGE_CODE_SPEC.md` | 可选继承 | 代码⇄自然语言双向绑定 |
| `docs/specs/VOLO_AI_PROTOCOL / UI_EVENT / SSES / 设计笔 / 灵感库` | **剥离** | 前端/AI 应用招式，与 Java 库无关 |
| `.impeccable.md` 品牌宪法 | **剥离** | 设计招式 |
| `.claude/agents/`（82 个）+ `agent-groups.json` | **剥离主体** | 前端/设计/PM agent 无意义；三方制衡用全局内置 agent（architect/code-reviewer/test-engineer/debugger 等）即可 |
| `.claude/commands/`（pm-*） | **剥离** | 产品经理命令，与库无关 |
| `.claude/task.json` 会话意图协议 | 剥离/降级 | 多模块意图锁对单一 Java 库过重，不引入 |

---

## 三、落地物清单（方案 A — 精炼但全面的心法层）

### 1. 文档骨架（`docs/`）
```
docs/
├── rules/global/          # 6 条通用心法（Java 适配版）
├── specs/                 # 4 核心规范（Java 适配版）
├── logs/                  # 工作留痕（.gitkeep）
├── todos/                 # TODO 追踪（.gitkeep）
├── plans/                 # 计划归档（本文件迁入）
└── reference/             # 架构/扩展点/onboarding 索引
```

### 2. 通用心法规则（`docs/rules/global/`，6 个）
按上表，逐一从 real-agent 取经、剥离前端语境、改写为 Maven/JUnit/Spring 语境。

### 3. 核心规范（`docs/specs/`，4 个）
`FILE_HEADER_SPEC` / `WORK_LOG_SPEC` / `REGISTRATION_DISCIPLINE_SPEC` / `TEST_SPEC`，全部 Java 库适配。

### 4. 项目 `CLAUDE.md` 升格【红线·须批准】
现有 7KB 已优秀（架构/处理链路/约定清晰），**仅增量补充**，不动现有结构：
- 顶部加「顶层授权框架」引用（指向 `~/.claude/CLAUDE.md` 自决协议）
- 新增「开发红线总表」——**本框架特有红线**（如：删注解/枚举/Provider 公开 API 前跑删除四件套；OutputSchema 改动需评估 SDK 兼容；SNAPSHOT 依赖破坏性变更预警；解析器 @Order 冲突）
- 新增「代码审查清单」（Java 库版）
- 新增「文档导航」表（指向上面 specs/rules）

### 5. 文件头回填（可选·第二阶段）
对 core 模块关键类按 `FILE_HEADER_SPEC` 回填 `@header` 头——工作量大，建议批准后作为独立 Sprint。

> **方案 A 原约束（已被董事长方案 B 决议覆盖，留档存照）**：~~不建 `.claude/agents` 大军、不建 task.json~~。
> **方案 B 现行**：建裁剪版 `.claude/agents`（约 10 个 Java 库适配 agent）+ 轻量 `task.json`，但**仍剥离全部前端/设计/PM 资产**（这一条两方案一致）。

---

## 四、执行方式

中等以上复杂度 + 涉及 ≥3 文件 → 按全局 Rule #6「三方独立制衡」组建团队：
- **架构师**：起草 specs/rules（适配 Java）
- **御史台**：独立审查每份产出是否忠于原心法、是否正确剥离招式、是否与本项目代码一致
- **CEO（我）**：裁定去留、闭环

---

## 五、红线与回滚

- 红线动作：项目 `CLAUDE.md` 增补、`docs/specs/` 新建 → 本方案即请示载体
- 回滚成本：极低——全为新增文档 + CLAUDE.md 增量，`git checkout` 即还原
- 不可逆风险：无（不删任何现有代码/接口）

---

## 六、董事长拍板结果（2026-06-24 · 已决）

1. ✅ 继承深度：**方案 B（全套基建搬迁）**——见第〇节决议。
2. ✅ 文件头回填：**作为后续独立 Sprint**，本次仅立 `FILE_HEADER_SPEC` 规范。
