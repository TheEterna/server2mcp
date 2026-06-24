#!/bin/bash
# api2mcp4j · Session Start Hook（轻量版）
# 触发：SessionStart
# 继承自 ../real-agent · 董事长 2026-06-24 批准方案B（裁剪为纯 Java 库适配）
# 设计：JSON 协议输出 · systemMessage 给董事长看 · additionalContext 注入 AI 上下文
# 大幅简化：仅输出模块清单 + 自决协议 / 三方制衡 / 心法位置，不做意图锁、不强制询问任务类型

set +e

TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S" 2>/dev/null || echo "unknown")
BRANCH=$(git branch --show-current 2>/dev/null || echo "unknown")
LAST_COMMIT=$(git log -1 --format="%h · %s" 2>/dev/null | cut -c1-72 || echo "(no commits)")
CHANGED_COUNT=$(git diff --name-only 2>/dev/null | wc -l | tr -d ' ')
STAGED_COUNT=$(git diff --cached --name-only 2>/dev/null | wc -l | tr -d ' ')

# task.json 缺失兜底
if [ ! -f ".claude/task.json" ]; then
  python3 -c "
import json
print(json.dumps({
    'continue': True,
    'suppressOutput': False,
    'hookSpecificOutput': {
        'hookEventName': 'SessionStart',
        'additionalContext': '⚠️ .claude/task.json 未找到，模块注册表离线。'
    },
    'systemMessage': '✗ api2mcp4j·SessionStart · 模块注册表缺失（task.json）'
}, ensure_ascii=False))
"
  exit 0
fi

export A2M_TIMESTAMP="$TIMESTAMP"
export A2M_BRANCH="$BRANCH"
export A2M_LAST_COMMIT="$LAST_COMMIT"
export A2M_CHANGED="$CHANGED_COUNT"
export A2M_STAGED="$STAGED_COUNT"

python3 <<'PYEOF'
import json
import os
import sys

TIMESTAMP = os.environ.get('A2M_TIMESTAMP', 'unknown')
BRANCH = os.environ.get('A2M_BRANCH', 'unknown')
LAST_COMMIT = os.environ.get('A2M_LAST_COMMIT', '(no commits)')
CHANGED = os.environ.get('A2M_CHANGED', '0')
STAGED = os.environ.get('A2M_STAGED', '0')

try:
    with open('.claude/task.json') as f:
        data = json.load(f)
except Exception as e:
    print(json.dumps({
        'continue': True,
        'suppressOutput': False,
        'hookSpecificOutput': {
            'hookEventName': 'SessionStart',
            'additionalContext': f'⚠️ task.json 解析失败: {e}'
        },
        'systemMessage': '✗ api2mcp4j·SessionStart · task.json 解析失败'
    }, ensure_ascii=False))
    sys.exit(0)

modules = data.get('modules', {})
globals_ = data.get('global_rules', [])
specs = data.get('specs', [])
authority = data.get('authority', {})
active = [(k, v) for k, v in modules.items() if v.get('status') == 'active']

# ====== systemMessage · 给董事长看（精简）======
system_message = "\n".join([
    f"◈ api2mcp4j·SessionStart  {TIMESTAMP}",
    f"分支 {BRANCH} · 变更 暂存 {STAGED} / 未暂存 {CHANGED} · 最新 {LAST_COMMIT}",
    f"{len(active)} 个物理模块 · {len(globals_)} 条全局心法 · {len(specs)} 份核心规范 · 三方制衡待命",
])

# ====== additionalContext · 给 AI 看 ======
ctx = []
ctx.append("<api2mcp4j-session>")
ctx.append(f"## api2mcp4j 会话上下文（task.json v{data.get('version', '?')}）")
ctx.append("")
ctx.append(f"**项目**：{data.get('project', 'api2mcp4j')}")
ctx.append(f"**会话核身**：{TIMESTAMP} · 分支 `{BRANCH}` · 暂存 {STAGED} / 未暂存 {CHANGED}")
ctx.append("")

ctx.append("### ▣ 物理模块（依赖流向：common ← core ← autoconfigure ← starters）")
ctx.append("| key | module | description |")
ctx.append("|-----|--------|-------------|")
for k, v in active:
    desc = v.get('description', '').replace('|', '\\|')
    ctx.append(f"| `{k}` | {v['name']} | {desc} |")
ctx.append("")

ctx.append("### ▣ 授权与制衡（强制）")
ctx.append(f"- **自决默认协议**：{authority.get('self_decision', '非红线默认自决，事后简报')}")
ctx.append(f"- **三方制衡**：{authority.get('three_party_check', '中等以上任务组建架构师/御史台/CEO 三方团队')}")
ctx.append("- **红线（须事前请示）**：")
for r in authority.get('redlines', []):
    ctx.append(f"  - {r}")
ctx.append("")

ctx.append("### ▣ 心法规则位置")
ctx.append("- **全局心法** `docs/rules/global/`：")
for path in globals_:
    fname = path.split('/')[-1].replace('.md', '')
    ctx.append(f"  - `{fname}` → `{path}`")
ctx.append("- **核心规范** `docs/specs/`：")
for path in specs:
    fname = path.split('/')[-1].replace('.md', '')
    ctx.append(f"  - `{fname}` → `{path}`")
ctx.append("")

ctx.append("### ▣ Agent 团队")
ctx.append("- 名册与分组：`.claude/agents/agent-groups.json`（command / execution / quality 三组）")
ctx.append("- 查询：`node scripts/agent-groups.mjs list|find|verify|stats`")
ctx.append("</api2mcp4j-session>")

print(json.dumps({
    "continue": True,
    "suppressOutput": False,
    "hookSpecificOutput": {
        "hookEventName": "SessionStart",
        "additionalContext": "\n".join(ctx)
    },
    "systemMessage": system_message
}, ensure_ascii=False))
PYEOF
