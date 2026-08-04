---
date: 2026-06-24
author: 索引官（资深架构师）
role: docs/reference 与骨架搭建
task: 继承 ../real-agent 文档体系心法，搭建 api2mcp4j 文档骨架与 reference 索引
status: 已完成
---

# 工作总结 · reference 文档与骨架搭建

## 一、做了什么（What）

为纯 Java 库 api2mcp4j（Spring Boot Starter，把 `@RestController` 自动暴露为 MCP Tool/Resource/Prompt/Complete）搭建 `docs/` 文档骨架，并撰写 3 篇 reference 索引文档。借鉴 `../real-agent` reference 文档的**结构与文风**，内容 100% 来自本项目代码与 CLAUDE.md。

**产出清单**：
- `docs/reference/architecture.md` — 系统架构总览（模块依赖流向、6 大设计模式落到真实类名、数据流全过程、双模式执行）
- `docs/reference/extension-points.md` — 扩展点手册（4 类扩展点，每个含真实接口签名 + 何时用 + 怎么用）
- `docs/reference/onboarding.md` — 新人/新 AI 入门（3 步上手 + 必读清单 + 6 个易踩坑 + 最小可用配置）
- 骨架目录：`docs/logs/`、`docs/todos/`、`docs/plans/`（各放 `.gitkeep`）

## 二、为什么这么做（Why）

团队（人与 AI）需要一份「不空中楼阁」的事实索引。real-agent 的 reference 文档证明了「顶部提示 + ASCII 链路图 + 编号数据流 + 表格 + Q&A」这套结构的可读性，但其内容是 Vue/全栈/SSE，与本纯 Java 库无关——故只继承骨架文风，事实全部重写。

## 三、怎么验证的（How）

- 派 explore agent + 亲自 Read，逐类核对了注册链、双层解析器 @Order、回调 buildArgs 注入逻辑、Provider 过滤、双模式 Conditions、PluginProperties 绑定。
- 核对了根 pom（`server2mcp-test` 确实不在 modules 中）、core 测试类清单、docs 子目录现状。

## 四、发现的问题（Findings）

**⚠️ CLAUDE.md 与代码不符（1 处，重要）**：
- CLAUDE.md 处理链路图写 `McpToolScanConfigurer (InitializingBean)`，但代码实际实现 `BeanDefinitionRegistryPostProcessor` + `BeanFactoryPostProcessor`，扫描发生在 `postProcessBeanDefinitionRegistry()` 而非 `afterPropertiesSet()`。
- 处理：未擅自改 CLAUDE.md（红线，CEO 亲自操刀）。在 architecture.md 中以代码为准并显式标注偏差，已上报 CEO。

**已坐实无偏差的点**：解析器 order（Des 0-5 / Param 0-6）、工具命名 `className_methodName`、OutputSchema 被注释未发送、依赖流向 common←core←autoconfigure←starters。

**需 CEO 复核的点**：
1. CLAUDE.md 的 `InitializingBean` 描述是否需要 CEO 订正（红线，不敢动）。
2. CLAUDE.md 配置参考用 `parser.params`（复数），但代码字段是 `param`（单数）。README_zh.md 也写 `param`。文档已按代码单数为准并加坑位提示，请 CEO 定夺 CLAUDE.md 是否订正。

## 五、未尽事项 / 下一步（Next）

- 本任务范围内无遗留。
- `docs/rules/global/`、`docs/specs/` 由兄弟架构师负责，已确认存在并在 onboarding 必读清单中正确指向，未触碰其文件。
