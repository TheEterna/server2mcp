package com.ai.plug.core.spec.change;

import com.ai.plug.common.utils.JsonParser;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Framework-layer HTTP polling endpoint for change notifications —
 * the protocol-2026-07-28 {@code subscriptions/listen} replacement
 * when Java SDK 2.0 lacks the SSE long-push schema.
 *
 * <p>Contract:
 * <ul>
 *   <li>Server code calls {@link #recordEvent} whenever a capability /
 *       resource / prompt / tool changes — the event is appended to an
 *       in-memory ring buffer with a monotonically increasing cursor;</li>
 *   <li>Clients call {@code GET /mcp/notifications?since=&lt;cursor&gt;}
 *       to fetch all events that happened after that cursor, plus the
 *       new cursor. Calling with {@code since=-1} returns all events.</li>
 * </ul>
 *
 * <h2>SDK 升级路径</h2>
 * <p>When Java SDK ≥ 3.0.0 ships with native {@code subscriptions/listen},
 * integrators can keep {@link #recordEvent} (write-only) and stop calling
 * {@link #handlePoll} — clients that have upgraded to SDK 3.0.0 will
 * subscribe via SSE; clients that have not will continue to poll. Zero
 * business-code change required.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   var endpoint = new NotificationsPollingEndpoint();
 *   endpoint.recordEvent("tools", Map.of("added", List.of("getOrder")));
 *
 *   // Client polls:
 *   Map<String, Object> body = endpoint.handlePoll(-1);
 *   long newCursor = (long) body.get("nextCursor");
 * }</pre>
 *
 * @author han
 * @time 2026/8/3
 */
public final class NotificationsPollingEndpoint {

    /** Default ring buffer size — prevents unbounded memory growth. */
    public static final int DEFAULT_CAPACITY = 1024;

    /** Listener interface — fired after each event is recorded. Used by
     *  the SSE controller to fan-out to live clients without polling. */
    @FunctionalInterface
    public interface EventListener {
        void onEvent(long cursor, String kind, Map<String, Object> payload);
    }

    private final int capacity;
    private final AtomicLong cursor = new AtomicLong(0);
    private final Map<Long, NotificationEvent> events = new ConcurrentHashMap<>();
    private volatile EventListener listener;

    public NotificationsPollingEndpoint() {
        this(DEFAULT_CAPACITY);
    }

    public NotificationsPollingEndpoint(int capacity) {
        if (capacity < 16) {
            throw new IllegalArgumentException("capacity must be >= 16, got: " + capacity);
        }
        this.capacity = capacity;
    }

    /** Register a listener that will be invoked synchronously after each
     *  {@link #recordEvent}. Replaces any previous listener (last-write-wins). */
    public void setListener(EventListener listener) {
        this.listener = listener;
    }

    /**
     * Record a change event. Returns the assigned cursor (monotonically
     * increasing). Older events past {@link #capacity} are evicted (FIFO).
     */
    public long recordEvent(String kind, Map<String, Object> payload) {
        if (kind == null || kind.isBlank()) {
            throw new IllegalArgumentException("kind is required");
        }
        long c = cursor.incrementAndGet();
        events.put(c, new NotificationEvent(c, kind, payload, Instant.now()));
        // Evict oldest if over capacity
        if (events.size() > capacity) {
            long oldest = c - capacity;
            if (oldest > 0) events.remove(oldest);
        }
        EventListener l = this.listener;
        if (l != null) {
            try {
                l.onEvent(c, kind, payload);
            } catch (RuntimeException ex) {
                // Listener failures must not poison the ring buffer.
            }
        }
        return c;
    }

    /**
     * Poll all events whose cursor &gt; {@code sinceCursor}. Returns a map:
     * <pre>{@code
     *   {
     *     "nextCursor": 42,
     *     "count": 3,
     *     "events": [ {cursor, kind, payload, timestamp}, ... ]
     *   }
     * }</pre>
     * Pass {@code sinceCursor = -1} to fetch all events.
     */
    public Map<String, Object> handlePoll(long sinceCursor) {
        List<NotificationEvent> matching = new ArrayList<>();
        long maxSeen = 0L;
        for (NotificationEvent e : events.values()) {
            if (e.cursor() > sinceCursor) {
                matching.add(e);
            }
            if (e.cursor() > maxSeen) maxSeen = e.cursor();
        }
        matching.sort((a, b) -> Long.compare(a.cursor(), b.cursor()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("nextCursor", maxSeen);
        body.put("count", matching.size());
        body.put("events", matching);
        return body;
    }

    /** Convenience: serialize {@link #handlePoll} as JSON. */
    public String handlePollJson(long sinceCursor) throws java.io.IOException {
        return JsonParser.getObjectMapper().writeValueAsString(handlePoll(sinceCursor));
    }

    /** Number of events currently buffered. */
    public int bufferedCount() {
        return events.size();
    }

    /** Current cursor (max cursor issued so far). */
    public long currentCursor() {
        return cursor.get();
    }

    /** Clear all buffered events + reset cursor to 0. */
    public void clear() {
        events.clear();
        cursor.set(0);
    }

    /** A single change notification. */
    public record NotificationEvent(
        long cursor,
        String kind,
        Map<String, Object> payload,
        Instant timestamp
    ) {
        public NotificationEvent {
            if (kind == null) throw new IllegalArgumentException("kind is required");
            payload = payload == null ? Map.of() : Map.copyOf(payload);
        }
    }
}