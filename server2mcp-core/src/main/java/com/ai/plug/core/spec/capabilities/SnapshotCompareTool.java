package com.ai.plug.core.spec.capabilities;

import com.ai.plug.common.utils.JsonParser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for comparing two {@link CapabilitySnapshot}s and producing a
 * structured diff (added / changed / removed) suitable for CI / dev
 * audits. Builds on {@link CapabilitySnapshot#diff(CapabilitySnapshot)}
 * but adds JSON output and structured event access.
 *
 * <h2>用法</h2>
 * <pre>{@code
 *   var before = CapabilitySnapshot.from(capsBefore);
 *   var after = CapabilitySnapshot.from(capsAfter);
 *   var diff = SnapshotCompareTool.compare(before, after);
 *   log.info("changes: {}", diff.summary());
 *   String json = diff.toJson();
 * }</pre>
 */
public final class SnapshotCompareTool {

    private SnapshotCompareTool() {
    }

    /**
     * Compare two snapshots and produce a structured diff.
     */
    public static Diff compare(CapabilitySnapshot before, CapabilitySnapshot after) {
        java.util.Map<String, Boolean> beforeMap = before == null
            ? java.util.Map.of() : before.flags();
        java.util.Map<String, Boolean> afterMap = after == null
            ? java.util.Map.of() : after.flags();
        java.util.List<Change> added = new java.util.ArrayList<>();
        java.util.List<Change> removed = new java.util.ArrayList<>();
        java.util.List<Change> changed = new java.util.ArrayList<>();

        for (var e : new java.util.TreeMap<>(afterMap).entrySet()) {
            String key = e.getKey();
            Boolean newVal = e.getValue();
            if (!beforeMap.containsKey(key)) {
                added.add(new Change(key, null, newVal));
            }
            else if (!java.util.Objects.equals(beforeMap.get(key), newVal)) {
                changed.add(new Change(key, beforeMap.get(key), newVal));
            }
        }
        for (var e : new java.util.TreeMap<>(beforeMap).entrySet()) {
            if (!afterMap.containsKey(e.getKey())) {
                removed.add(new Change(e.getKey(), e.getValue(), null));
            }
        }
        return new Diff(List.copyOf(added), List.copyOf(removed), List.copyOf(changed));
    }

    /** A single change event. */
    public record Change(String key, @org.jspecify.annotations.Nullable Object before,
                         @org.jspecify.annotations.Nullable Object after) {
        public boolean isAddition() { return before == null; }
        public boolean isRemoval() { return after == null; }
    }

    /** Diff result. */
    public record Diff(List<Change> added, List<Change> removed, List<Change> changed) {

        public boolean isEmpty() {
            return added.isEmpty() && removed.isEmpty() && changed.isEmpty();
        }

        public int totalChanges() {
            return added.size() + removed.size() + changed.size();
        }

        /** Human-readable summary. */
        public String summary() {
            if (isEmpty()) return "Diff: no changes";
            return "Diff: +" + added.size() + " -" + removed.size()
                + " ~" + changed.size();
        }

        /** JSON-serializable representation for machine consumers. */
        public String toJson() throws java.io.IOException {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("isEmpty", isEmpty());
            body.put("totalChanges", totalChanges());
            body.put("added", added);
            body.put("removed", removed);
            body.put("changed", changed);
            return JsonParser.getObjectMapper().writeValueAsString(body);
        }
    }
}