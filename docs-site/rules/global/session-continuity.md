# Session 续接铁律

> 触发条件：所有 session 续接场景（compact summary 恢复、新对话延续既有任务、agent 重新承接工作、跨 session 汇报 "X 是否完成"）
> 强制度：[ENFORCED]
> 来源：继承自 ../real-agent docs/rules/global/session-continuity.md · 董事长 2026-06-24 批准方案B全套继承 · 康熙·多源验证
> 御史台随时可要求出示"续接三件套"执行记录

---

## § 一、核心铁律

**跨 session 涉及「X 是否完成 / X 是否存在 / X 是否已删除」的判断，第一动作必须是 git / 文件验证，严禁凭对话记忆断言。**

Session summary 是**意图快照**，不是**状态权威**。代码库的状态只存在于：
1. git 历史（commit 快照）
2. 文件系统当前状态（`ls` / `cat` / `find` / Read）
3. 构建 / 测试实际执行结果（`mvn clean install` / `cd server2mcp-core && mvn test`）

---

## § 二、续接第一动作清单（不可跳过）

接到续接上下文后，**在发出任何"状态断言"之前**，必须执行：

1. `git log --oneline -20` — 看够远（不要只看最近几条），确认 commit 是否与 summary 一致
2. `git status` — 看当前工作目录状态（注意：本仓存在未跟踪的 `.omc/` / `CLAUDE.md` 等，需区分"未跟踪"与"已删除"）
3. 关键文件存在性：`ls <关键文件>` 或 `find server2mcp-core -name "<文件>"` 或 Read

若涉及"X 已删除"判断（如某解析器类、某注解属性、某 Provider 方法），**必须** `find` 或 `grep` 确认文件系统 / 代码里真的不存在 X。

> 本项目特别提醒：`server2mcp-test` 模块**不在**根 pom 的 `<modules>` 中。续接时若要确认"测试/演示是否构建过"，不能只看根 `mvn clean install`，须单独 `cd server2mcp-test && mvn clean package` 核验。

---

## § 三、禁止行为

- ❌ 凭对话记忆说「X 解析器没删」、「`@McpTool.converter` 属性已删」、「Provider 改造已完成」
- ❌ 用 summary 文本作为唯一依据
- ❌ 在未 check git 前主动给 team-lead / CEO 发进度断言
- ❌ 仅回看最近 N 条 commit 就断言"某改动未做"（实际可能在第 N+1 条）
- ❌ 把"本地 `mvn clean install` 通过"等同于"功能正确"（SNAPSHOT 依赖 + 下游兼容性不在编译范围内）

---

## § 四、正确模式

```
续接收到
    ↓
git log --oneline -20（看够远，不要只看最近几条）
    ↓
git status + ls/find 关键文件（注意 server2mcp-test 不在根 modules）
    ↓
对比 summary
    ↓
发现偏差？
   ├─ 是 → 立即上报 team-lead / CEO，标记为"续接偏差事件"
   └─ 否 → 偏差消除后再发状态断言
```

---

## § 五、违规处理

- 第一次：口头纠偏，当事人自述违反条款
- 第二次：记入「跨 session 违规」档案
- 御史台巡查权：随时可要求出示"续接三件套"执行记录

---

## § 六、与其他规则的关系

- 本规则是 **康熙·多源验证** 在 session 层面的具体落地
- 与 `destructive-deletion.md` 的四维本质相同——都是"不单源采信"
- 与 `agent-capability-declaration.md` 配套——续接后若发现能力不可用（如 LSP 桥接、AST 工具），必须事前声明

---

**立法者**：心法规则官（依据董事长 2026-06-24 批准方案B 起草；命令适配为本项目 git + Maven 真实命令，补充 server2mcp-test 不在根 modules 的续接陷阱）
**颁布于**：2026-06-24
