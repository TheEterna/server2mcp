#!/usr/bin/env bash
#
# trigger-phase3.sh — Phase 3 战役启动检测器
#
# 用途：当 MCP Java SDK ≥ 3.0.0 发布后，触发 Phase 3 战役：
# 把当前通过 _meta / experimental Map / customizer 间接表达的协议 2026-07-28
# 字段迁移到 SDK record 字段层直传。
#
# 触发逻辑：
#   1. 调用 check-mcp-sdk-version.sh 检查 SDK 版本
#   2. 若 >= 3.0.0，输出 Phase 3 迁移清单
#   3. 否则，输出"等待中"提示
#
# 维护：详见 docs/mcp-2026-07-28-INTEGRATION-MATRIX.md 第八节

set -u

CHECK_SCRIPT="${BASH_SOURCE%/*}/check-mcp-sdk-version.sh"

if [[ ! -x "$CHECK_SCRIPT" ]]; then
    echo "[error] check-mcp-sdk-version.sh 不可执行或不存在"
    echo "        请执行: chmod +x $CHECK_SCRIPT"
    exit 2
fi

echo "===== Phase 3 战役启动检测 ====="
echo

# 调用 check-mcp-sdk-version.sh，若 SDK 已支持协议 2026-07-28 则继续
CHECK_OUTPUT=$(bash "$CHECK_SCRIPT" 2>&1)
CHECK_EXIT=$?

echo "$CHECK_OUTPUT"
echo

if [[ $CHECK_EXIT -eq 0 ]]; then
    echo "===== Phase 3 迁移清单 ====="
    echo
    echo "目标：把以下字段从 _meta / experimental Map / customizer 间接表达"
    echo "      迁移到 SDK record 字段层直传（业务代码 0 改动）"
    echo
    echo "ServerCapabilities:"
    echo "  - extensions 字段 (从 experimental: io.modelcontextprotocol/tasks)"
    echo "  - tools.subscription 字段 (从 extensions)"
    echo "  - completions.listChanged 字段 (从 extensions)"
    echo
    echo "CallToolResult:"
    echo "  - resultType 字段 (从 _meta.resultType)"
    echo "  - CacheableResult interface (从 _meta.cacheable.{ttlMs,scope,key})"
    echo
    echo "HTTP headers:"
    echo "  - Mcp-Method / Mcp-Name / x-mcp-header-* (从 customizer 注入)"
    echo
    echo "RPC 路由:"
    echo "  - server/discover (新增)"
    echo "  - tasks/create / tasks/get / tasks/list / tasks/cancel (新增)"
    echo "  - subscriptions/listen (新增)"
    echo "  - input_required/respond (新增)"
    echo
    echo "迁移策略:"
    echo "  1. 升级 spring-ai.version 到 SDK 3.0.0+ SNAPSHOT"
    echo "  2. 升级 mcp-sdk.version 到 3.0.0+"
    echo "  3. mvn clean install 全模块验证"
    echo "  4. 跑 500 测试，确保全绿（确认 wire JSON 行为不变）"
    echo "  5. 删除 McpResultWriter 的 wire JSON 重写逻辑（SDK 原生支持后）"
    echo "  6. 删除 WireSchemaExporter.tasksExtension() 等 experimental 注入"
    echo "  7. 删除 McpServerCustomizers.syncListChangedAll() 等 customizer 桥接"
    echo "  8. CHANGELOG 标注 MAJOR 版本升级"
    echo
    echo "风险:"
    echo "  - SDK 3.0.0 可能在 record 字段命名上与协议 2026-07-28 changelog 微调"
    echo "  - 需逐字段验证 wire JSON 兼容性"
    echo
    echo "→ 当前 SDK 已就位，建议启动 Phase 3 战役。"
    exit 0
elif [[ $CHECK_EXIT -eq 1 ]]; then
    echo "→ SDK 仍未发布 3.0.0，Phase 3 待命。"
    echo "→ 定期运行本脚本检测升级。"
    exit 1
else
    echo "[error] SDK 版本检测异常，详见上方输出"
    exit 2
fi