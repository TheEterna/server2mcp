# 搜索工具序位 · 速查（Search Tool Parity）

> 触发条件：任何"我想找 / 我想看 / 我想知道哪里用到了 X"的场景
> 强制度：[ENFORCED] —— 字面量 / 结构 / 语义 / 图谱四维互不替代，不存在"高级技巧"心智隔阂
> 来源：继承自 ../real-agent docs/rules/global/search-tool-parity.md · 董事长 2026-06-24 批准方案B全套继承
> 关联：本目录 `destructive-deletion.md` 删除四件套 + `agent-capability-declaration.md` 能力声明

---

## § 一、四维全景（按维度排列 · 已诚实适配本项目工具表）

| 维度 | 工具（本项目真实可用） | 回答的问题 | 本项目可用性 |
|------|----------------------|-----------|-------------|
| **字面量层** | `grep` / `Grep` 工具（Bash） | 这个名字 / 字符串 / 注释 / `@Order` 值 在哪些文件出现？ | ✅ 始终可用 |
| **结构层** | 若具备 AST 检索工具则用之；否则 **grep 多形式 + 人工结构审查代偿** | 这个符号以什么**语法结构**被使用（注解组合 / 方法签名 / 链式调用）？ | ⚠️ 本环境**无** AST 检索工具，结构层靠代偿 |
| **语义层** | LSP `find_references`（依赖 jdtls + LSP MCP 桥接） | 这个类型在**类型系统**中被谁依赖（含泛型 / 继承 / 重写）？ | ⚠️ jdtls 二进制已装，但需会话暴露 LSP 工具桥接；桥接未激活时降级 |
| **图谱层** | **多模块 Maven 反向依赖分析**（`mvn dependency:tree` + 模块依赖流向 + 下游兼容性评估） | 这个改动会**波及哪些模块 / 下游用户**？ | ✅ 可用（人工 + Maven 命令） |

**核心心法（一字不可丢）**：四个维度**互不替代**。grep 不能答结构，结构层不能答语义依赖，语义层不能答跨模块 / 下游波及。每一维有自己的盲区，靠其他维度补。

> **诚实声明**：本项目无 real-agent 的 `ast_grep_search` / `get_impact_radius_tool` 等具体 MCP 工具。结构层与图谱层在本环境分别以"代偿"和"Maven 反向依赖分析"落地。**严禁捏造本会话不存在的工具名**。

---

## § 二、grep vs 结构层视野 分工速查

### 用 grep 的场景（字面量视野）

| 场景 | 示例命令 |
|------|---------|
| 找一个符号在哪些文件出现 | `grep -rln "McpToolDesParser" server2mcp-core/src/` |
| 找 import | `grep -rn "import com.ai.plug.core.annotation.McpTool" server2mcp-core/src/` |
| 找注解使用点 | `grep -rn "@McpTool\|@ToolScan" server2mcp-test/src/` |
| 找配置 key、注释、文档关键字 | `grep -rn "plugin.mcp.parser" .` |
| 粗略统计符号被引用次数 | `grep -rln "AbstractDesParser" server2mcp-core/src \| wc -l` |
| 找 `@Order` / `@ConditionalOnParser` 装配点 | `grep -rn "@ConditionalOnParser" server2mcp-autoconfigure/src/` |

**判断准则**：你要找的是**字符**，不在意它的语法位置——用 grep。

### 需要结构层视野的场景（本环境用 grep 多形式 + 人工结构审查代偿）

| 场景 | 代偿手法（无 AST 工具时） |
|------|------------------------|
| 找一个注解的所有使用形态 | `grep -rn "@McpTool"` 后**逐处人工读**，区分 `@McpTool` / `@McpTool(name=...)` / `@McpTool(converter=...)` 形态 |
| 找解析器链所有 `@Order(n)` 分布 | `grep -rn "@Order" server2mcp-autoconfigure/src/` 后人工排序核对优先级 0-5 是否连续 |
| 找 `.block()` 在 webflux starter 的反模式 | `grep -rn "\.block()" server2mcp-spring-boot-starters/server2mcp-starter-webflux/` 后人工确认是否阻塞响应式线程 |
| 找回调层次中重写 `doCall` / `doDesParse` 的子类 | `grep -rln "extends AbstractMcpToolMethodCallback\|@Override" ...` 后人工读签名 |
| 找特定注解组合（如 `@Bean` + `@ConditionalOnParser` + `@Order`） | grep 三者各一遍，人工取交集 |

**判断准则**：你要找的是**结构 / 模式 / 形态**——若有 AST 工具用之；本环境无，则 grep 多形式后**必须人工结构审查**，不可只凭单条 grep 字符匹配下结论。

---

## § 三、结构层代偿的元方法（无 AST 工具时的纪律）

| 元方法 | 说明 | 本项目示例 |
|--------|------|-----------|
| **多形式 grep** | 一个语义概念用多个字面量形式各搜一遍 | 找 McpTool：`@McpTool` + `McpTool.class` + `import .*McpTool` |
| **交集人工取** | 多个 grep 结果人工取交集而非机器 AST 匹配 | `@Bean` ∩ `@ConditionalOnParser` ∩ `@Order` = 解析器工厂方法 |
| **逐处肉眼判形态** | grep 命中后逐行读上下文，区分真实结构 vs 字符串/注释内假匹配 | `grep "outputSchema"` 命中后区分"被注释掉"vs"真实调用" |
| **mvn compile 类型兜底** | 改动后 `mvn clean install` 让编译器替你做一次全量类型校验 | 删枚举值后编译失败 = 暴露遗漏引用 |

> AST 工具的价值在于"机器精确匹配语法结构"。本环境无此工具时，**人工结构审查 + 编译兜底**是诚实代偿，但代偿不等于免责——影响面大的改动（§四）仍须按 `destructive-deletion.md` 走完整流程。

---

## § 四、自动触发器（量化判断 · 避免主观漏掉）

满足以下任一条件，**改动前必须做结构层审查**（有 AST 工具则跑之，无则 grep 多形式 + 人工核对）：

1. **影响面阈值**：`grep -rln "<symbol>" server2mcp-core/src \| wc -l` ≥ 3 个文件
2. **结构变更阈值**：commit 预计涉及 ≥ 5 处 `@Override` / `case` / `instanceof` / `@Order` / `@ConditionalOnParser` 行变化
3. **批量替换信号**：任何"把 A 模式替换为 B 模式"的重构（注解属性重命名 / 解析器优先级重排 / Provider 过滤链改造 / `flatMap` → `switchMap` 等）

满足触发条件未做结构层审查 → 御史台一票否决。

---

## § 五、典型场景示例（四维联动 · 全部 Java/MCP 语境）

### 场景 1：删除一个解析器（如 `Swagger2DesParser`）

```
1. grep      —— 找所有出现位置（字符串层）
                grep -rn "Swagger2DesParser\|swagger2DesParser" .
2. 结构层    —— 找装配点与 @Order 链（无 AST 工具 → grep + 人工）
                grep -rn "@ConditionalOnParser.*SWAGGER2\|@Order(5)" server2mcp-autoconfigure/src/
                人工确认删除后 @Order 0-4 是否仍连续、SWAGGER2 配置 key 是否需同步清理
3. 语义层    —— LSP find_references（桥接可用时）找类型系统依赖；不可用则声明降级
4. 图谱层    —— 评估：Swagger2DesParser 属 core 公开扩展点吗？下游用户是否可能继承它？
                mvn dependency:tree 看依赖此类的模块
```

四维全部确认零调用 / 已处理 → 才可动手删。

### 场景 2：把解析器优先级从 `@Order` 数字改为新机制（批量重构）

```
1. 结构层  —— grep -rn "@Order" server2mcp-autoconfigure/src/  改前扫描，列出所有 @Order 标注点
2. grep    —— 验证是否有代码硬依赖具体 order 数值
3. 改动
4. 结构层  —— 再次 grep + 人工核对，确认零残留旧机制、优先级语义不变
5. mvn clean install —— 编译 + 测试兜底
```

### 场景 3：找一个常量在哪定义

```
grep -rn "JSON_MIME_TYPE" server2mcp-common/src/   —— 一行解决
```

不需要结构层大材小用。

---

## § 六、与其他规则的关系

- **破坏性删除四件套** → `destructive-deletion.md`
- **agent 能力声明**（搜索能力不可用须事前声明，尤其结构层 AST 工具与语义层 LSP 桥接） → `agent-capability-declaration.md`
- **跨 session 续接验证** → `session-continuity.md`

---

**立法者**：心法规则官（依据董事长 2026-06-24「全套继承 ../real-agent 开发心法」起草，诚实适配本项目无 AST 工具、LSP 需桥接的真实工具表）
**颁布于**：2026-06-24
