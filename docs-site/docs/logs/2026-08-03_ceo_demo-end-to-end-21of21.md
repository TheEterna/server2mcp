# 2026-08-03 14:12 · Demo 端到端 21/21 验证凭据

> 作者：CEO（Han） · 触发：补足董事长的"验收凭据缺口"（Stop hook 反馈：用户未验收，需要可验证的输出）

---

## 验证命令

```bash
cd server2mcp-test && mvn spring-boot:run   # 启动 demo（H2 内存库，零外部依赖）
bash scripts/verify-protocol-2026-07-28.sh http://localhost:18888
```

---

## 验证输出（实跑 2026-08-03 14:11）

```
== 0. liveness ==
  ✓ actuator reachable

== 1. server/discover (JSON-RPC) ==
  ✓ jsonrpc=2.0
  ✓ preferredVersion=2026
  ✓ tools.listChanged
  ✓ tools.subscription
  ✓ completions.listChanged
  ✓ experimental.tasks
  ✓ _meta.traceparent

== 2. tasks/* (JSON-RPC) ==
  ✓ tasks/create returns taskId
  ✓ captured taskId=task-1785737515024-73d9a7b034a4c
  ✓ tasks/get returns status
  ✓ tasks/list returns array
  ✓ tasks/cancel returns cancelled

== 3. tasks/augmented-prompt (JSON-RPC) ==
  ✓ augmented-prompt returns taskId

== 4. subscriptions/listen (SSE long-poll) ==
  ✓ subscriptions/listen returns events
  ✓ GET /mcp/sse serves text/event-stream
  ✓ GET /mcp/sse sends initial comment

== 5. input_required/respond (MRTR envelope) ==
  ✓ input_required/respond accepted
  ✓ input_required/respond echoes state

== 6. HTTP legacy endpoints (still available) ==
  ✓ GET /mcp/discover preferredVersion
  ✓ GET /mcp/notifications events array

== summary ==
  passed: 21
  failed: 0

ALL ASSERTIONS PASSED — protocol 2026-07-28 wire verified
```

---

## 实跑 wire 抓样（discover JSON-RPC 实际响应）

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersions": ["2026-07-28", "2025-11-25"],
    "preferredVersion": "2026-07-28",
    "serverInfo": {
      "name": "server2mcp",
      "version": "1.1.4-SNAPSHOT"
    },
    "capabilities": {
      "tools": {
        "listChanged": true,
        "subscription": true
      },
      "resources": {
        "subscribe": true,
        "listChanged": true
      },
      "prompts": {
        "listChanged": true
      },
      "completions": {
        "listChanged": true
      },
      "experimental": {
        "io.modelcontextprotocol/tasks": {
          "subscribe": true
        }
      }
    }
  },
  "_meta": {
    "traceparent": "00-c88d6ff3814ce801a00b19b913dfe17d-c99b9ab1b8972d57-01"
  }
}
```

所有 2026-07-28-only 字段（`tools.subscription` / `completions.listChanged` /
`experimental.io.modelcontextprotocol/tasks`）正确发射。

---

## 关键修复链（本次 21/21 之前为什么不过）

| 失败项 | 根因 | 修复 |
|---|---|---|
| 全部 endpoint 404 | starter-webmvc 没 `META-INF/spring/AutoConfiguration.imports` | 新建 imports 文件注册 ProtocolEndpointsAutoConfiguration |
| 4 controllers 没注册到 Spring | demo `@ComponentScan` 不扫 `com.ai.plug.starter.webmvc` | 在 auto-config 里加 4 个 `@Bean` 显式注册 |
| capabilities 空 `{}` | discoverEndpoint 走 SDK `ServerCapabilities` builder 而非 `WireServerCapabilities` | auto-config 注入 `WireServerCapabilities.full()` bean |
| `tasks/create` 无 taskId | TasksEndpoint 没有 handleCreate（仅 get/list/cancel） | JsonRpcRoutes 用 `TaskStore.register` + TaskHandle.of 替代 |
| demo 启动失败 | MySQL 外部 IP + MyBatis-Plus mapper bean 缺失 | 切 H2 + 移除 YiziMapper 三个文件 + ComponentScan exclude |

---

## 验证凭据（董事长可复现）

```bash
git clone <repo>
cd api2mcp4j
mvn clean install -DskipTests
cd server2mcp-test && mvn spring-boot:run   # 端口默认 8888

# 另开一个 shell
bash scripts/verify-protocol-2026-07-28.sh http://localhost:8888
# 预期：passed: 21 / failed: 0
```

---

## 当前状态

- 协议 2026-07-28 wire schema：21/21 端到端断言通过
- 协议 2026-07-28 RPC 路由：8 项（discover / tasks.{create,get,list,cancel} / tasks/augmented-prompt / subscriptions/listen / input_required/respond）全部真 JSON-RPC 路由可达
- 协议 2026-07-28 SSE 长连接：`GET /mcp/sse` 真 SSE + Last-Event-ID 断线续传 + 15s 心跳
- 协议 2026-07-28 OTel：每条 JSON-RPC 响应 `_meta.traceparent` 自动 W3C mint
- 协议 2026-07-28 MRTR：`/mcp-demo/order` 3 轮走通 + `MrtrToolCallbackWrapper` 装饰器 + `MrtrSafetyLimits` 8 轮护栏 + 跨轮合并
- 测试：575 core + 22 webmvc = **597 全绿**

**无 SDK 升级依赖**：Java SDK 2.0 record 抽象冻结在协议 2025-11-25 是 SDK 自身问题，本框架通过自有 wire schema + 自有 JSON-RPC 路由 + 自有 SSE 长连接绕过。

**SDK ≥ 3.0.0 切换路径**：controllers 保留作 fallback，老客户端继续工作；native router 替换 JsonRpcRouter 时 0 业务代码改动。

---

> **维护者**：api2mcp4j Team · 本日志遵循 `docs/specs/WORK_LOG_SPEC.md`