/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 */

package com.ai.plug.starter.webmvc;

import com.ai.plug.core.spec.change.NotificationsPollingEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Server-Sent Events long-poll endpoint for protocol-2026-07-28
 * {@code subscriptions/listen}. The wire shape: clients open
 * {@code GET /mcp/sse} and receive a stream of {@code data: <json>}
 * lines, one per notification event the framework records via
 * {@link NotificationsPollingEndpoint#recordEvent}.
 *
 * <p>This replaces the SDK 2.0 polling fallback
 * ({@link NotificationsController}) for clients that prefer push. The
 * polling endpoint stays in place for clients that haven't upgraded —
 * both code paths coexist; SDK ≥ 3.0.0 will switch this controller to
 * the SDK's native SSE long-push handler when available.
 *
 * <h2>Resume via {@code Last-Event-ID}</h2>
 *
 * <p>If the client supplies a {@code Last-Event-ID} header (per the SSE
 * spec), this controller replays every event whose id is greater than
 * that value before going live. This makes reconnects lossless up to
 * the ring buffer's capacity ({@code DEFAULT_CAPACITY = 1024}).
 *
 * <h2>Heartbeat</h2>
 *
 * <p>An SSE comment line ({@code ": ping\n\n"}) is sent every 15 seconds
 * so intermediaries (proxies, browsers) don't close the connection on
 * idle timeout.
 *
 * @author han
 * @time 2026/8/3
 */
@RestController
public class SseNotificationsController {

    private static final Logger logger = LoggerFactory.getLogger(SseNotificationsController.class);

    private static final long HEARTBEAT_INTERVAL_MS = 15_000;

    private final NotificationsPollingEndpoint notifications;
    private final ConcurrentHashMap<Long, SseEmitter> liveClients = new ConcurrentHashMap<>();
    private final AtomicLong clientCursor = new AtomicLong(0);
    private final ScheduledExecutorService heartbeat =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sse-heartbeat");
            t.setDaemon(true);
            return t;
        });

    public SseNotificationsController(NotificationsPollingEndpoint notifications) {
        this.notifications = notifications;
    }

    @GetMapping(value = "/mcp/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {
        long emitterId = clientCursor.incrementAndGet();
        // 0L = no timeout; Spring's SseEmitter supports per-call timeout
        // if the client signals close — for SSE we let the heartbeat
        // and recorder drive liveness.
        SseEmitter emitter = new SseEmitter(0L);
        liveClients.put(emitterId, emitter);

        emitter.onCompletion(() -> liveClients.remove(emitterId));
        emitter.onTimeout(() -> liveClients.remove(emitterId));
        emitter.onError(t -> liveClients.remove(emitterId));

        // Replay missed events when Last-Event-ID present.
        long since = parseCursor(lastEventId);
        if (since >= 0) {
            Map<String, Object> replay = notifications.handlePoll(since);
            Object eventsObj = replay.get("events");
            if (eventsObj instanceof java.util.List<?> list) {
                for (Object ev : list) {
                    if (ev instanceof com.ai.plug.core.spec.change.NotificationsPollingEndpoint.NotificationEvent ne) {
                        try {
                            emitter.send(SseEmitter.event()
                                .id(String.valueOf(ne.cursor()))
                                .name(ne.kind())
                                .data(ne.payload()));
                        } catch (IOException ex) {
                            liveClients.remove(emitterId);
                            return emitter;
                        }
                    }
                }
            }
        }

        // Send an initial comment so the client knows the stream is live
        // before the first event arrives.
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException ignored) {
            liveClients.remove(emitterId);
            return emitter;
        }

        logger.debug("SSE client subscribed: id={} replaySince={}", emitterId, since);

        return emitter;
    }

    /**
     * Push a freshly recorded event to all live SSE clients. Called by
     * {@link NotificationsPollingEndpoint#recordEvent} via a small
     * publisher hook. We can't easily intercept recordEvent from here
     * without modifying it, so we expose this method to be invoked from
     * the auto-configuration's wiring (a thin adapter).
     */
    public void broadcast(long cursor, String kind, Map<String, Object> payload) {
        if (liveClients.isEmpty()) return;
        liveClients.forEach((id, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                    .id(String.valueOf(cursor))
                    .name(kind)
                    .data(payload));
            } catch (IOException | IllegalStateException ex) {
                // Emitter already completed (client disconnected); drop.
                liveClients.remove(id);
            }
        });
    }

    /** Start a heartbeat task that sends a comment line to every live
     *  client every {@link #HEARTBEAT_INTERVAL_MS} milliseconds. */
    public void startHeartbeat() {
        heartbeat.scheduleAtFixedRate(() -> {
            liveClients.forEach((id, emitter) -> {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (IOException ex) {
                    liveClients.remove(id);
                }
            });
        }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    /** @return number of currently connected SSE clients (operational
     *  metric). */
    public int activeClients() {
        return liveClients.size();
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