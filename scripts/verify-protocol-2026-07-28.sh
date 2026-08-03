#!/usr/bin/env bash
#
# verify-protocol-2026-07-28.sh
#
# End-to-end smoke test for the framework's protocol-2026-07-28 wire
# compatibility. Run after `mvn spring-boot:run` in
# `server2mcp-test/` (or any consumer app) and the script will curl
# every public RPC + SSE endpoint, asserting each one carries the
# 2026-07-28 wire shape the matrix documents.
#
# Usage:   ./verify-protocol-2026-07-28.sh [BASE_URL]
# Default: http://localhost:8888
#
# Exit codes:
#   0 = every assertion passed
#   1 = at least one assertion failed (printed inline)
#
# @author han
# @time 2026/8/3

set -u
BASE="${1:-http://localhost:8888}"
JSONRPC="$BASE/mcp/jsonrpc"
SSE="$BASE/mcp/sse"
DISCOVER_HTTP="$BASE/mcp/discover"
NOTIFY_HTTP="$BASE/mcp/notifications"

PASS=0
FAIL=0

ok()   { echo "  \033[32m✓\033[0m $1"; PASS=$((PASS+1)); }
bad()  { echo "  \033[31m✗\033[0m $1"; FAIL=$((FAIL+1)); }
hdr()  { echo; echo "\033[1m== $1 ==\033[0m"; }

assert_contains() {
  local label="$1"; local body="$2"; local needle="$3"
  if echo "$body" | grep -q -- "$needle"; then ok "$label"
  else bad "$label  (missing: $needle)"; fi
}

# ----------------------------------------------------------------------------
hdr "0. liveness"
PING=$(curl -sf "$BASE/actuator/health" 2>/dev/null || true)
if [ -n "$PING" ]; then ok "actuator reachable"; else bad "actuator unreachable"; fi

# ----------------------------------------------------------------------------
hdr "1. server/discover (JSON-RPC)"
RESP=$(curl -sf -X POST "$JSONRPC" \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","method":"server/discover","params":{},"id":1}')
assert_contains "jsonrpc=2.0"             "$RESP" '"jsonrpc":"2.0"'
assert_contains "preferredVersion=2026"  "$RESP" '"preferredVersion":"2026-07-28"'
assert_contains "tools.listChanged"      "$RESP" '"listChanged"'
assert_contains "tools.subscription"     "$RESP" '"subscription"'
assert_contains "completions.listChanged" "$RESP" '"completions"'
assert_contains "experimental.tasks"     "$RESP" '"io.modelcontextprotocol/tasks"'
assert_contains "_meta.traceparent"      "$RESP" '"traceparent"'

# ----------------------------------------------------------------------------
hdr "2. tasks/* (JSON-RPC)"
CREATE=$(curl -sf -X POST "$JSONRPC" \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","method":"tasks/create","params":{"title":"verify-script"},"id":"t1"}')
TASK_ID=$(echo "$CREATE" | sed -n 's/.*"taskId":"\([^"]*\)".*/\1/p')
assert_contains "tasks/create returns taskId" "$CREATE" '"taskId"'
if [ -n "$TASK_ID" ]; then ok "captured taskId=$TASK_ID"; fi

GET=$(curl -sf -X POST "$JSONRPC" \
  -H 'Content-Type: application/json' \
  -d "{\"jsonrpc\":\"2.0\",\"method\":\"tasks/get\",\"params\":{\"taskId\":\"$TASK_ID\"},\"id\":\"t2\"}")
assert_contains "tasks/get returns status"   "$GET" '"status":"running"'

LIST=$(curl -sf -X POST "$JSONRPC" \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","method":"tasks/list","params":{},"id":"t3"}')
assert_contains "tasks/list returns array"  "$LIST" '"tasks"'

CANCEL=$(curl -sf -X POST "$JSONRPC" \
  -H 'Content-Type: application/json' \
  -d "{\"jsonrpc\":\"2.0\",\"method\":\"tasks/cancel\",\"params\":{\"taskId\":\"$TASK_ID\",\"reason\":\"verify\"},\"id\":\"t4\"}")
assert_contains "tasks/cancel returns cancelled" "$CANCEL" '"cancelled":true'

# ----------------------------------------------------------------------------
hdr "3. tasks/augmented-prompt (JSON-RPC)"
AP=$(curl -sf -X POST "$JSONRPC" \
  -H 'Content-Type: application/json' \
  -d "{\"jsonrpc\":\"2.0\",\"method\":\"tasks/augmented-prompt\",\"params\":{\"taskId\":\"$TASK_ID\"},\"id\":\"t5\"}")
assert_contains "augmented-prompt returns taskId" "$AP" "\"taskId\":\"$TASK_ID\""

# ----------------------------------------------------------------------------
hdr "4. subscriptions/listen (SSE long-poll)"
LISTEN=$(curl -sf -X POST "$JSONRPC" \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","method":"subscriptions/listen","params":{"since":-1},"id":"s1"}')
assert_contains "subscriptions/listen returns events" "$LISTEN" '"events"'

# Real SSE attempt — short timeout, just verify the headers + first frame
SSE_HEAD=$(curl -sf -D - "$SSE" --max-time 1 2>/dev/null | head -20 || true)
assert_contains "GET /mcp/sse serves text/event-stream" "$SSE_HEAD" 'text/event-stream'
assert_contains "GET /mcp/sse sends initial comment"    "$SSE_HEAD" 'connected'

# ----------------------------------------------------------------------------
hdr "5. input_required/respond (MRTR envelope)"
IRR=$(curl -sf -X POST "$JSONRPC" \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","method":"input_required/respond","params":{"requestState":"verify-rs-1","answers":{"street":"123 Main","city":"Springfield"}},"id":"r1"}')
assert_contains "input_required/respond accepted" "$IRR" '"status":"accepted"'
assert_contains "input_required/respond echoes state" "$IRR" '"requestState":"verify-rs-1"'

# ----------------------------------------------------------------------------
hdr "6. HTTP legacy endpoints (still available)"
DISC_HTTP=$(curl -sf "$DISCOVER_HTTP")
assert_contains "GET /mcp/discover preferredVersion" "$DISC_HTTP" '"preferredVersion":"2026-07-28"'
NOTIFY_HTTP_RESP=$(curl -sf "$NOTIFY_HTTP?since=-1")
assert_contains "GET /mcp/notifications events array" "$NOTIFY_HTTP_RESP" '"events"'

# ----------------------------------------------------------------------------
hdr "summary"
echo "  passed: $PASS"
echo "  failed: $FAIL"
if [ "$FAIL" -gt 0 ]; then
  echo
  echo "\033[31mFAILURE: $FAIL assertion(s) failed\033[0m"
  exit 1
fi
echo
echo "\033[32mALL ASSERTIONS PASSED — protocol 2026-07-28 wire verified\033[0m"
exit 0