# Observability — bridging `McpTracer` to OpenTelemetry

`api2mcp4j` ships with **wire-format tracing for free** — every JSON-RPC response carries a
W3C `traceparent` (SEP-414), so any downstream collector that understands the W3C spec can
stitch calls into a trace without further help.

For **real OpenTelemetry SDK spans** (with attributes, exceptions, parent-child relationships),
you provide a `McpTracer` bean that delegates to OTel. This page shows how.

## Why an SPI instead of a hard `opentelemetry-api` dep?

- One-size-fits-all deps lock every downstream to one vendor.
- OTel users pay nothing if they don't use it.
- Brave / Zipkin / homegrown can implement the same `McpTracer` SPI.

## 1. Add OTel to your app

```xml
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-api</artifactId>
</dependency>
<dependency>
  <groupId>io.opentelemetry</groupId>
  <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
<dependency>
  <groupId>io.opentelemetry.instrumentation</groupId>
  <artifactId>opentelemetry-spring-boot-starter</artifactId>
</dependency>
```

## 2. Define your `OpenTelemetry` bean

Use OTel's SDK auto-config (or roll your own):

```java
@Bean
public OpenTelemetry openTelemetry() {
    Resource resource = Resource.getDefault().merge(
        Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "my-mcp-server")));
    SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(
            OtlpGrpcSpanExporter.builder().setEndpoint("http://otel-collector:4317").build())
            .build())
        .setResource(resource)
        .build();
    return OpenTelemetrySdk.builder()
        .setTracerProvider(tracerProvider)
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .buildAndRegisterGlobal();
}
```

## 3. Bridge `McpTracer` to OTel

Copy-paste this adapter (about 30 lines) into your project:

```java
package com.yourorg.mcp;

import com.ai.plug.core.observability.McpTracer;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OpenTelemetryMcpTracer implements McpTracer {

    private final Tracer otel;

    public OpenTelemetryMcpTracer(OpenTelemetry openTelemetry) {
        this.otel = openTelemetry.getTracer("api2mcp4j", "1.1.4-SNAPSHOT");
    }

    @Override
    public McpTracer.Span startSpan(String name) {
        return new OtelSpan(otel.spanBuilder(name).startSpan());
    }

    @Override
    public McpTracer.Span startSpan(String name, Map<String, Object> attributes) {
        var b = otel.spanBuilder(name);
        attributes.forEach((k, v) -> b.setAttribute(AttributeKey.stringKey(k), String.valueOf(v)));
        return new OtelSpan(b.startSpan());
    }

    private static final class OtelSpan implements McpTracer.Span {
        private final Span span;
        private final Scope scope;
        OtelSpan(Span span) {
            this.span = span;
            this.scope = span.makeCurrent();
        }
        @Override public void setAttribute(String key, Object value) {
            span.setAttribute(AttributeKey.stringKey(key), String.valueOf(value));
        }
        @Override public void recordException(Throwable t) {
            span.recordException(t);
        }
        @Override public void end(Throwable error) {
            if (error != null) {
                span.setStatus(StatusCode.ERROR, error.getClass().getSimpleName());
                span.recordException(error);
            }
            scope.close();
            span.end();
        }
        @Override public void end() {
            scope.close();
            span.end();
        }
    }
}
```

## 4. Verify

Start the app, hit any MCP endpoint with `curl`, and you should see spans
named `mcp.jsonrpc.dispatch` (every JSON-RPC envelope) and
`mcp.tool.call` (every tool invocation — emitted from the tool callback,
not the router).

The auto-configuration logs a one-line warning if it detects OTel on the
classpath but no `McpTracer` bean — that's the signal to add the adapter above.

## Span attributes

| Span | Attribute | Value |
|------|-----------|-------|
| `mcp.jsonrpc.dispatch` | `mcp.jsonrpc.method` | e.g. `tools/call` |
| `mcp.jsonrpc.dispatch` | `mcp.jsonrpc.id`     | the JSON-RPC id (as string) |
| `mcp.jsonrpc.dispatch` | `error`              | `true` on handler exception |
| `mcp.tool.call`        | `mcp.tool.name`      | e.g. `orders_get_order` |
| `mcp.tool.call`        | `mcp.tenant.id`      | the active tenant (or empty) |

## What about WebFlux / Netty?

OTel context propagates over Reactor's `Context` automatically when
`opentelemetry-instrumentation-reactor-netty` is on the classpath — no
extra wiring required. The framework's `McpTracer` calls run on the
right thread.
