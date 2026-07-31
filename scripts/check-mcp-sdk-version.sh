#!/usr/bin/env bash
#
# check-mcp-sdk-version.sh — MCP Java SDK 版本与协议版本对齐检测器
#
# 用途：扫描 ~/.m2 与 Maven Central，判定当前项目实际可用的 MCP SDK 是否已支持
# 协议 2026-07-28（无状态核心/MRTR/Tasks 扩展），并在启动期给出明确提示。
#
# 退出码：
#   0 = 当前 SDK 已支持 2026-07-28，建议启用 Phase 2 战役
#   1 = 当前 SDK 仍滞后（如 2.0.0 实现 2025-11-25），按计划继续推进 Phase 1
#   2 = 网络或环境异常，未能完成检测
#
# 维护：详见 docs/mcp-2026-07-28-impact.md 第四节

set -u

LOCAL_REPO="${HOME}/.m2/repository"
LOCAL_ARTIFACT="${LOCAL_REPO}/io/modelcontextprotocol/sdk/mcp-core"
CENTRAL_URL="https://repo.maven.apache.org/maven2/io/modelcontextprotocol/sdk/mcp-core/maven-metadata.xml"
IMPACT_DOC="${BASH_SOURCE%/*}/../docs/mcp-2026-07-28-impact.md"

# 已知支持 2026-07-28 的最低 SDK 版本（Phase 2 启动判据）
PHASE2_MIN_VERSION="3.0.0"

echo "===== MCP Java SDK 版本对齐检测 ====="
echo

# 1. 本地 ~/.m2 探测
if [[ -d "$LOCAL_ARTIFACT" ]]; then
    LOCAL_VERSION=$(ls "$LOCAL_ARTIFACT" | grep -v '\.lastUpdated\|maven-metadata\|resolver-status' | sort -V | tail -1)
    echo "[local] ~/.m2 实际版本：$LOCAL_VERSION"
else
    LOCAL_VERSION=""
    echo "[local] ~/.m2 未缓存 mcp-core"
fi

# 2. Maven Central 最新稳定版探测
echo
echo "[central] 查询 Maven Central 最新版本..."
CENTRAL_VERSION=$(curl -s --max-time 10 "$CENTRAL_URL" \
    | grep -oE '<latest>[^<]+' \
    | sed 's|<latest>||' \
    | head -1)

if [[ -z "$CENTRAL_VERSION" ]]; then
    echo "[central] 查询失败（网络异常或 metadata 格式变更）"
    echo "请检查：curl --max-time 10 '$CENTRAL_URL'"
    exit 2
fi
echo "[central] Maven Central latest：$CENTRAL_VERSION"

# 3. 版本对齐判据
echo
ACTUAL_VERSION="${LOCAL_VERSION:-$CENTRAL_VERSION}"

if [[ "$(printf '%s\n' "$ACTUAL_VERSION" "$PHASE2_MIN_VERSION" | sort -V | head -1)" == "$PHASE2_MIN_VERSION" ]]; then
    echo "✅ 检测通过：当前 SDK ($ACTUAL_VERSION) ≥ $PHASE2_MIN_VERSION"
    echo
    echo "→ 当前 MCP Java SDK 已支持协议 2026-07-28，应启动 Phase 2 战役。"
    echo "→ 目标清单见：$IMPACT_DOC 第二节。"
    echo "→ 战役计划原文：docs/plan-MCP协议全面集成-2026-07-30.md"
    exit 0
else
    echo "⚠️  当前 SDK ($ACTUAL_VERSION) < $PHASE2_MIN_VERSION —— 仍滞后于协议 2026-07-28"
    echo
    echo "→ 按 docs/plan-MCP协议全面集成-2026-07-30.md 继续推进 Phase 1 已完成项；"
    echo "  物理不可达项（MRTR / Tasks / subscriptions/listen 等）需等 Java SDK 跟进。"
    echo "→ 详见：$IMPACT_DOC"
    exit 1
fi