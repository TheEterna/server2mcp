# 重构 commit 顺序铁律

> 触发条件：多 commit 重构，后续 commit 依赖前面 commit 的契约变更
> 强制度：[ENFORCED]
> 来源：继承自 ../real-agent docs/rules/global/refactor-ordering.md · 董事长 2026-06-24 批准方案B全套继承 · 赵匡胤·机制设计

---

## § 一、核心原则

**契约提供者先行，契约依赖者后置。**

或 **原子化单 commit**。

契约真空期 = 潜在生产事故。即使未触发，也是**可证伪的风险**。Git 历史上存在"某 commit 的代码在装配/运行时会抛异常"的快照，审计时就是问题。

> 本项目语境：契约 = 解析器/Provider 的 `@Bean` 注册关系、`@ConditionalOnParser` value 与 `plugin.mcp.parser` 配置 key 的对应、回调抽象类与子类的方法签名约定、注解属性与其使用点的对应。

---

## § 二、示例（全部 Java/Spring/MCP 语境）

### ❌ 错误顺序（契约真空期）：

```
commit 1: [依赖者] McpConfig 改用新 @ConditionalOnParser value "SWAGGER_V3"（依赖配置 key 已改名）
commit 2: [提供者] 文档/默认配置把 plugin.mcp.parser.des 的 "SWAGGER3" 改名为 "SWAGGER_V3"
```

→ commit 1 与 commit 2 之间是**契约真空期**：该窗口内用旧配置 key 启动，解析器条件不满足 → 该层解析器静默不注册 → 工具描述解析降级。本次未触发属**偶然性安全**，不是**必然性安全**。

### ✅ 正确顺序：

```
commit 1: [提供者] 让 ToolDefinitionBuilder 兼容新旧两种解析器装配方式（提供契约）
commit 2: [依赖者] McpConfig 切换到新装配方式（消费契约）
```

### ✅ 原子化：

```
commit 1: [原子] 解析器优先级机制切换（@Order 重排 + ToolDefinitionBuilder 适配，单 commit）
```

---

## § 三、决策判据（适配多模块库）

| 重构规模 | 推荐模式 |
|---------|---------|
| 同模块契约变更（< 3 文件，如仅改 core 内部） | 原子化单 commit |
| **跨模块契约变更**（如 core 改抽象类签名 → autoconfigure / starter 需跟随） | 提供者先行（core 先，autoconfigure/starter 后） |
| **公开 API 契约变更**（注解属性 / SPI 接口 / Provider 公开方法） | 提供者先行 + 兼容期（保留旧 API 一个版本，加 `@Deprecated`） |
| **跨仓库契约变更**（影响下游用户依赖） | 提供者先行 + 版本发布（MAJOR 版本号 + 迁移说明 + CHANGELOG） |

> 依赖流向铁律：本项目模块依赖为 `common ← core ← autoconfigure ← starters`。契约**总是从被依赖方（common/core）向依赖方（autoconfigure/starters）流动**。因此跨模块重构，**底层模块的契约必须先就位**，上层才能消费——这与"提供者先行"天然一致。

---

## § 四、违规处理

- 工作总结必须**明示 commit 顺序是否有契约真空期**
- 若有，须：
  - 量化风险窗口期（如 "若在该窗口用旧配置 key 启动，解析器层静默降级"）
  - 说明为何可接受（如 "未发布 SNAPSHOT" / "本地单机 session"）
- 未说明的，御史台可判为"偶然性安全"，要求补声明
- 严重违规（已发布破坏下游的版本）→ Post-mortem 分析 + 机制升级

---

## § 五、commit message 标注规范

涉及契约依赖的 commit 应在 message 中显式标注（沿用本项目现有 `fix:` / `refactor[core]:` 风格）：

```
[提供者] refactor[core]: AbstractDesParser 新增兼容方法（提供新装配契约）
[依赖者] refactor[autoconfigure]: McpConfig 切换到新装配方式（依赖 commit a1b2c3d）
[原子]   refactor[core]: 解析器优先级机制统一（@Order 重排 + Builder 适配）
```

便于 `git log --grep="\[提供者\]"` 审计契约流向。

---

## § 六、反向捆绑补声明模式

如果顺序已经错了且无法 rebase 修改（如已 push 到共享分支 / 已发布 SNAPSHOT）：

必须在工作总结中补反向声明：
```
commit A（依赖者）⟺ commit B（提供者）构成双向捆绑合入关系。
未来回滚操作必须同步。
窗口期风险量化：[X 小时 / Y 小时 / 未发布]
```

---

## § 七、与其他规则的关系

- 本规则是 **赵匡胤·机制设计** 在 commit 层面的落地（让错误的顺序通过 commit message 前缀被及时发现）
- 与 TDD 的 `[RED]` / `[GREEN]` 双 commit 签名模式互补（御史台先写测试 → RED → 架构师 conform → GREEN，见全局 Rule #6 三方制衡）
- 与 `destructive-deletion.md` 的捆绑合入纪律配套

---

**立法者**：心法规则官（依据董事长 2026-06-24 批准方案B 起草；示例适配为解析器装配 / 配置 key / 模块依赖流向语境，契约真空期落到 SNAPSHOT 发布与下游兼容）
**颁布于**：2026-06-24
