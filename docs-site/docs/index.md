# api2mcp4j 文档站

> **Zero-to-low code MCP integration for Spring Boot REST APIs**
> 把现有 `@RestController` 零代码变成 MCP tools。
> **100% 兼容协议 2026-07-28**（含 8 项 RPC + wire schema + SSE 长连接 + MRTR + OTel traceparent）

[English README](../README.md) · [中文 README](../README_zh.md) · [协议 2026-07-28 集成矩阵](mcp-2026-07-28-INTEGRATION-MATRIX.md)

---

## 快速导航

### 🚀 入门

| 文档 | 内容 |
|---|---|
| [Quick Start（英文）](../README.md#-quick-start-3-minutes) | 3 分钟上手 |
| [快速开始（中文）](../README_zh.md#-快速开始3-分钟) | 中文 3 分钟上手 |
| [onboarding.md](reference/onboarding.md) | 3 步走完整入门 |
| [architecture.md](reference/architecture.md) | 架构总览 + 6 大设计模式 |

### 📡 协议 2026-07-28

| 文档 | 内容 |
|---|---|
| [**mcp-2026-07-28-INTEGRATION-MATRIX.md**](mcp-2026-07-28-INTEGRATION-MATRIX.md) | **§十一节含 21/21 端到端验证凭据**（独立可复现） |
| [mcp-2026-07-28-coverage.md](mcp-2026-07-28-coverage.md) | 协议层实装入口索引 |
| [mcp-2026-07-28-impact.md](mcp-2026-07-28-impact.md) | 协议变更影响面 |
| [mcp-server-capabilities-field-mapping.md](mcp-server-capabilities-field-mapping.md) | ServerCapabilities 字段映射 |
| [wire-schema-samples.json](wire-schema-samples.json) | 完整 wire schema 样例 |

### 🏗️ 架构 & 扩展

| 文档 | 内容 |
|---|---|
| [reference/architecture.md](reference/architecture.md) | 架构总览 + 模块流向 + 6 大设计模式 + 数据流全过程 |
| [reference/extension-points.md](reference/extension-points.md) | 自定义解析器 / 转换器 / 过滤器 / Context |
| [reference/onboarding.md](reference/onboarding.md) | 3 步上手 + 必读清单 + 易踩坑 |

### 📐 规范（Spec）

| 文档 | 内容 |
|---|---|
| [specs/REGISTRATION_DISCIPLINE_SPEC.md](specs/REGISTRATION_DISCIPLINE_SPEC.md) | **注册纪律宪法**（[ENFORCED] 6 维 Rubric + 3 把利器） |
| [specs/FILE_HEADER_SPEC.md](specs/FILE_HEADER_SPEC.md) | 文件头规范（`@header-start` / `@header-end` 分级） |
| [specs/TEST_SPEC.md](specs/TEST_SPEC.md) | 测试规范（[ENFORCED] TDD 双 commit `[RED]` / `[GREEN]`） |
| [specs/WORK_LOG_SPEC.md](specs/WORK_LOG_SPEC.md) | 工作日志规范（[ENFORCED] 报告类产出落 `docs/logs/`） |

### ⚖️ 全局规则

| 规则 | 路径 | 核心 |
|---|---|---|
| 搜索工具序位 | [rules/global/search-tool-parity.md](rules/global/search-tool-parity.md) | grep / 结构 / LSP / 多模块 四维不互替 |
| 破坏性删除多源验证 | [rules/global/destructive-deletion.md](rules/global/destructive-deletion.md) | [ENFORCED] 删公开 API 前 grep + LSP + 跨模块评估 |
| Session 续接铁律 | [rules/global/session-continuity.md](rules/global/session-continuity.md) | [ENFORCED] 续接先 `git log/status` 验证 |
| 重构 commit 顺序 | [rules/global/refactor-ordering.md](rules/global/refactor-ordering.md) | [ENFORCED] 契约提供者先行 / 原子 commit |
| Agent 能力声明 | [rules/global/agent-capability-declaration.md](rules/global/agent-capability-declaration.md) | [ENFORCED] Briefback 第六项环境自检 |
| 工作留痕 | [rules/global/work-log.md](rules/global/work-log.md) | [ENFORCED] 报告类产出落 `docs/logs/` |

### 📜 工作日志（按时间）

| 日期 | 主题 |
|---|---|
| 2026-08-03 | [协议 2026-07-28 100% 兼容战役盘点](logs/2026-08-03_ceo_protocol-2026-07-28-100pct-compat.md) |
| 2026-08-03 | [Demo 端到端 21/21 验证凭据](logs/2026-08-03_ceo_demo-end-to-end-21of21.md) |
| 2026-08-03 | [MRTR 装饰器 + 安全护栏](logs/2026-08-03_ceo_mrtr-callback-wrapper-safety-limits.md) |
| 2026-06-24 | [继承心法战役闭环简报](logs/2026-06-24_CEO_继承心法战役闭环简报.md) |

### 📋 计划

| 文档 | 内容 |
|---|---|
| [plans/.gitkeep](plans/.gitkeep) | 历史战役计划（占位） |

---

## 🎯 一行 curl 验证

```bash
cd server2mcp-test && mvn spring-boot:run    # :8888，H2 内存库
bash scripts/verify-protocol-2026-07-28.sh http://localhost:8888
# 预期：passed: 21 / failed: 0
```

**21 项断言** 覆盖：server/discover · tasks.\{create,get,list,cancel\} · tasks/augmented-prompt · subscriptions/listen (HTTP + SSE) · input_required/respond (MRTR) · HTTP legacy endpoints · `_meta.traceparent` 自动 mint。

---

## 🧪 测试覆盖

| 模块 | 数量 | 状态 |
|---|---|---|
| `server2mcp-core` 单元测试 | 575 | ✅ |
| `server2mcp-starter-webmvc` 集成测试 | 22 | ✅ |
| `server2mcp-test` demo 测试 | 3 | ✅ |
| **端到端 curl 验证** | **21/21** | **✅** |

---

## 🔗 外部链接

- **GitHub 仓库**：https://github.com/TheEterna/api2mcp4j
- **MCP 协议官方**：https://modelcontextprotocol.io
- **Spring AI 文档**：https://docs.spring.io/spring-ai/reference/

---

## 📄 协议

[Apache License 2.0](../LICENSE)