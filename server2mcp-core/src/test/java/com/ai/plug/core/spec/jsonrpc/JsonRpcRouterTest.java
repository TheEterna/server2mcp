package com.ai.plug.core.spec.jsonrpc;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure-Java tests for {@link JsonRpcRouter}: register / dispatch /
 * error-translation semantics, with no Spring or SDK involvement.
 *
 * @author han
 * @time 2026/8/3
 */
class JsonRpcRouterTest {

    @Test
    void dispatch_routesByMethod() {
        JsonRpcRouter router = new JsonRpcRouter();
        router.register("echo", params -> params);
        JsonRpcResponse r = router.dispatch(
            JsonRpcRequest.of("echo", Map.of("hello", "world"), 1));
        assertThat(r.error()).isNull();
        assertThat(r.id()).isEqualTo(1);
        assertThat(r.result()).isInstanceOf(Map.class);
        assertThat((Map<String, Object>) r.result()).containsEntry("hello", "world");
    }

    @Test
    void dispatch_unknownMethod_returnsMethodNotFound() {
        JsonRpcRouter router = new JsonRpcRouter();
        JsonRpcResponse r = router.dispatch(
            JsonRpcRequest.of("ghost", Map.of(), 2));
        assertThat(r.result()).isNull();
        assertThat(r.error()).isNotNull();
        assertThat(r.error().code()).isEqualTo(-32601);
        assertThat(r.error().message()).contains("ghost");
        assertThat(r.id()).isEqualTo(2);
    }

    @Test
    void dispatch_handlerThrows_returnsInternalError() {
        JsonRpcRouter router = new JsonRpcRouter();
        router.register("boom", params -> { throw new IllegalStateException("kaboom"); });
        JsonRpcResponse r = router.dispatch(
            JsonRpcRequest.of("boom", Map.of(), 3));
        assertThat(r.result()).isNull();
        assertThat(r.error()).isNotNull();
        assertThat(r.error().code()).isEqualTo(-32603);
        assertThat(r.error().message()).contains("IllegalStateException", "kaboom");
    }

    @Test
    void dispatchRaw_parsesValidEnvelope() {
        JsonRpcRouter router = new JsonRpcRouter();
        router.register("ping", params -> "pong");
        JsonRpcResponse r = router.dispatchRaw(Map.of(
            "jsonrpc", "2.0",
            "method", "ping",
            "params", Map.of("x", 1),
            "id", "abc"
        ));
        assertThat(r.result()).isEqualTo("pong");
        assertThat(r.id()).isEqualTo("abc");
    }

    @Test
    void dispatchRaw_invalidJsonrpc_returnsParseError() {
        JsonRpcRouter router = new JsonRpcRouter();
        JsonRpcResponse r = router.dispatchRaw(Map.of(
            "jsonrpc", "1.0",
            "method", "ping",
            "id", 1
        ));
        assertThat(r.error()).isNotNull();
        assertThat(r.error().code()).isEqualTo(-32600);
    }

    @Test
    void dispatchRaw_nonMap_returnsNull() {
        JsonRpcRouter router = new JsonRpcRouter();
        assertThat(router.dispatchRaw("not a map")).isNull();
        assertThat(router.dispatchRaw(42)).isNull();
        assertThat(router.dispatchRaw(null)).isNull();
    }

    @Test
    void register_replacesPriorHandler() {
        JsonRpcRouter router = new JsonRpcRouter();
        router.register("x", params -> "first");
        router.register("x", params -> "second");
        assertThat(router.registeredCount()).isEqualTo(1);
        assertThat((String) router.dispatch(JsonRpcRequest.of("x", Map.of(), 1)).result())
            .isEqualTo("second");
    }

    @Test
    void unregister_removesHandler() {
        JsonRpcRouter router = new JsonRpcRouter();
        router.register("x", params -> "v");
        router.unregister("x");
        assertThat(router.registeredCount()).isZero();
        assertThat(router.dispatch(JsonRpcRequest.of("x", Map.of(), 1)).error())
            .isNotNull();
    }
}