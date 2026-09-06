/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.starter.webflux;

import com.ai.plug.core.spec.change.NotificationsPollingEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;

/**
 * Reactive SSE controller for protocol-2026-07-28
 * {@code subscriptions/listen}. Clients open {@code GET /mcp/sse} and
 * receive a non-blocking stream of {@code ServerSentEvent} frames.
 *
 * <h2>Why this is different from the WebMVC version</h2>
 *
 * <p>The MVC implementation kept a {@code ConcurrentHashMap<Long, SseEmitter>}
 * + a {@code ScheduledExecutorService} heartbeat. That's blocking-friendly
 * but unsafe on the reactive stack — under Netty we must not park worker
 * threads. So this version uses:
 * <ul>
 *   <li>{@link Sinks.Many#multicast()} — every SSE subscriber gets its
 *       own replay-from-cursor feed while the same upstream sink
 *       broadcasts new events.</li>
 *   <li>{@link Flux#interval(Duration)} — heartbeat comes from the
 *       Reactor scheduler, no JDK executor required.</li>
 *   <li>{@code Flux.merge(persistedEvents, liveSink.asFlux(), heartbeat)} —
 *       one merged stream per subscriber, fully back-pressured.</li>
 * </ul>
 *
 * <h2>Resume via {@code Last-Event-ID}</h2>
 *
 * <p>If the client supplies a {@code Last-Event-ID} header (per the SSE
 * spec), this controller replays every event whose id is greater than
 * that value before going live. The ring buffer capacity is the same
 * 1024-event window the polling endpoint exposes.
 *
 * @author han
 * @time 2026/8/3
 */
@RestController
public class SseNotificationsController {

    private static final Logger logger = LoggerFactory.getLogger(SseNotificationsController.class);

    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(15);

    private final NotificationsPollingEndpoint notifications;
    private final Sinks.Many<ServerSentEvent<Object>> liveSink =
        Sinks.many().multicast().directBestEffort();

    public SseNotificationsController(NotificationsPollingEndpoint notifications) {
        this.notifications = notifications;
    }

    @GetMapping(value = "/mcp/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> subscribe(
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {

        long since = parseCursor(lastEventId);
        Flux<ServerSentEvent<Object>> replay = since >= 0
            ? replayMissed(since)
            : Flux.just(ServerSentEvent.<Object>builder().comment("connected").build());

        // Heartbeat — Reactor-native; no ScheduledExecutorService.
        Flux<ServerSentEvent<Object>> heartbeat = Flux.interval(HEARTBEAT_INTERVAL)
            .map(tick -> ServerSentEvent.<Object>builder().comment("ping").build())
            .takeUntilOther(Mono.never()); // runs forever, cancelled when the client disconnects

        Flux<ServerSentEvent<Object>> live = liveSink.asFlux();

        logger.debug("SSE client subscribed: replaySince={}", since);

        return Flux.merge(replay, live, heartbeat)
            .doOnCancel(() -> logger.debug("SSE client disconnected"));
    }

    /**
     * Push a freshly recorded event to every live SSE client. Called by
     * the auto-configuration, which wires
     * {@link NotificationsPollingEndpoint#recordEvent} through this method.
     * If the sink rejects (slow consumer / full), we drop — the polling
     * endpoint still has the event for the next reconnect via
     * {@code Last-Event-ID}.
     */
    public void broadcast(long cursor, String kind, Map<String, Object> payload) {
        ServerSentEvent<Object> frame = ServerSentEvent.builder()
            .id(String.valueOf(cursor))
            .event(kind)
            .data(payload)
            .build();
        Sinks.EmitResult result = liveSink.tryEmitNext(frame);
        if (result.isFailure()) {
            logger.debug("SSE broadcast dropped (sink busy/full): cursor={} result={}", cursor, result);
        }
    }

    private Flux<ServerSentEvent<Object>> replayMissed(long since) {
        Map<String, Object> replay = notifications.handlePoll(since);
        Object eventsObj = replay.get("events");
        if (!(eventsObj instanceof java.util.List<?> list) || list.isEmpty()) {
            return Flux.just(ServerSentEvent.<Object>builder().comment("connected").build());
        }
        return Flux.fromIterable(list)
            .map(ev -> {
                if (ev instanceof NotificationsPollingEndpoint.NotificationEvent ne) {
                    return ServerSentEvent.<Object>builder()
                        .id(String.valueOf(ne.cursor()))
                        .event(ne.kind())
                        .data((Object) ne.payload())
                        .build();
                }
                return ServerSentEvent.<Object>builder().comment("malformed-event").build();
            })
            .concatWith(Flux.just(ServerSentEvent.<Object>builder().comment("connected").build()));
    }

    private static long parseCursor(String value) {
        if (value == null || value.isBlank()) return -1;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }
}
