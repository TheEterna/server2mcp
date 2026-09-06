# SDK ≥ 3.0.0 Migration Plan

> **Status: pending upstream.** Spring AI MCP SDK 3.0.0 is not yet
> released (as of 2026-09). This page is a forward-looking
> migration plan; revisit when the SDK ships.

## Why this matters

Today (SDK 2.0) the framework implements 8 JSON-RPC routes
**on top of** the SDK, because SDK 2.0 only ships schemas for the
2025-11-25 protocol. Once SDK 3.0.0 lands, those routes will be
native — and the framework's own `JsonRpcRouter` becomes
redundant-but-backward-compatible.

The current code already supports both code paths:

```java
@AutoConfiguration
@ConditionalOnWebApplication(type = SERVLET)
public class ProtocolEndpointsAutoConfiguration { ... }
```

When SDK 3.0.0 ships, the controllers stay registered as fallbacks
for older clients — the `mcp.jsonrpc.dispatch` endpoint continues to
work, just delegates to the SDK router when available.

## What we expect to change

| Today (SDK 2.0) | Tomorrow (SDK 3.0.0) | Migration effort |
|-----------------|-----------------------|------------------|
| Custom `JsonRpcRouter` with 8 hand-rolled routes | SDK native router | **Delete** `JsonRpcRouter` + `JsonRpcRoutes`; redirect `JsonRpcController` to `McpRouter` bean |
| `McpServerFeatures.Async/SyncToolSpecification` constructed manually | SDK exposes a builder | Replace builder calls; keep our `SyncMcpToolMethodCallback` / `AsyncMcpToolMethodCallback` (they implement `BiFunction` — SDK 3.0 still uses this shape) |
| `NotificationsPollingEndpoint` for `subscriptions/listen` polling fallback | SDK native SSE | Keep our polling as fallback; SDK 3.0 SSE primary |
| W3C traceparent minted by `MetaUtils.ensureTraceparent` | SDK 3.0 uses OTel natively | **Drop** our minting if SDK does it; keep our `McpTracer` SPI for spans |
| Hand-written `WireSchemaExporter` for 2026-07-28 wire fields | SDK 3.0 native | Delete `WireSchemaExporter`; trust SDK |

## Migration trigger

The repo contains `scripts/trigger-phase3.sh` (referenced in
README). The intended workflow:

```bash
./scripts/trigger-phase3.sh
# This should:
#   1. Bump dep version in pom.xml
#   2. Re-run mvn verify
#   3. Open a "Phase 3: SDK 3.0" tracking issue
```

If the script doesn't exist yet, that's fine — write it as part
of the SDK-3.0 PR.

## What we will keep, even after SDK 3.0

These are net-new features the framework provides on top of the SDK
and should remain:

- **Auto-registration from `@RestController`** (the whole point of
  `api2mcp4j` — SDK doesn't do this).
- **5-parser chain** (Swagger v3 / v2 / Javadoc / Spring MVC /
  Jackson / Spring AI). SDK has no concept of a description
  fallback chain.
- **Multi-tenant isolation** (`@McpTool.tenants()` / `denyAll()`,
  the new `tenant` package).
- **`McpTracer` SPI + observability extensions** — orthogonal to
  the SDK.
- **MRTR (multi-round tool response) decorator** —
  `MrtrToolCallbackWrapper` works at the callback level, not the
  protocol level, so it's SDK-version-agnostic.

## How to test the migration (when SDK 3.0 ships)

1. Bump `mcp-sdk.version` in `pom.xml`.
2. `mvn clean verify`. The framework's tests should pass because
   they don't depend on SDK internals — they wire the router
   end-to-end.
3. Run the demo app's 21/21 e2e suite (see
   `docs/logs/2026-08-03_ceo_demo-end-to-end-21of21.md`).
4. If a public API in the SDK has changed shape, audit our
   callbacks per the rules in
   `docs/rules/global/destructive-deletion.md` before changing
   our public API. Don't break downstream users in a minor
   release.
