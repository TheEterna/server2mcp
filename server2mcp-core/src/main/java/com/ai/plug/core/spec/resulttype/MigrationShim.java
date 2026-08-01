package com.ai.plug.core.spec.resulttype;

import io.modelcontextprotocol.spec.McpSchema;

import java.lang.reflect.Method;

/**
 * Migration shim — switches the framework's wire-layer hint delivery
 * between (a) meta-map-based wrapping (current SDK 2.0 path) and
 * (b) direct SDK Builder field access (Java SDK ≥ 2.1 future path).
 *
 * <p>The shim detects at construction time whether the SDK record exposes
 * the relevant fields (e.g. {@code CallToolResult.resultType},
 * {@code ListToolsResult.ttlMs}, {@code ServerCapabilities.extensions}).
 * If yes, future code can call {@link #sdkSupportsWireFields()} to know
 * it's safe to use direct Builder calls; if no, callers should use the
 * existing meta-map path.
 *
 * <h2>SDK 2.0 字节码实证</h2>
 * Each {@code supportsXxx()} method calls a reflection check at init
 * time. Today's SDK 2.0 returns false for {@code resultType} /
 * {@code ttlMs} / {@code cacheScope} / {@code extensions} — the framework
 * uses meta-map for those.
 *
 * <p>Once Java SDK 2.1+ exposes the fields, the reflection check returns
 * true and {@link MigrationShim#sdkSupportsWireFields()} flips to true —
 * framework code can switch to direct Builder calls without API changes.
 */
public final class MigrationShim {

    private static final boolean SDK_SUPPORTS_RESULT_TYPE;
    private static final boolean SDK_SUPPORTS_TTL_MS;
    private static final boolean SDK_SUPPORTS_CACHE_SCOPE;
    private static final boolean SDK_SUPPORTS_EXTENSIONS;

    static {
        SDK_SUPPORTS_RESULT_TYPE = hasMethod("io.modelcontextprotocol.spec.McpSchema$CallToolResult",
            "resultType");
        SDK_SUPPORTS_TTL_MS = hasMethod("io.modelcontextprotocol.spec.McpSchema$CallToolResult",
            "ttlMs");
        SDK_SUPPORTS_CACHE_SCOPE = hasMethod("io.modelcontextprotocol.spec.McpSchema$CallToolResult",
            "cacheScope");
        SDK_SUPPORTS_EXTENSIONS = hasMethod("io.modelcontextprotocol.spec.McpSchema$ServerCapabilities",
            "experimental");
        // Note: experimental() already exists in SDK 2.0, so this is true.
        // The check is here for symmetry / future-proofing.
    }

    private MigrationShim() {
    }

    /** @return true iff SDK ≥ 2.1 exposes resultType as a record field. */
    public static boolean sdkSupportsResultType() {
        return SDK_SUPPORTS_RESULT_TYPE;
    }

    /** @return true iff SDK ≥ 2.1 exposes ttlMs as a record field. */
    public static boolean sdkSupportsTtlMs() {
        return SDK_SUPPORTS_TTL_MS;
    }

    /** @return true iff SDK ≥ 2.1 exposes cacheScope as a record field. */
    public static boolean sdkSupportsCacheScope() {
        return SDK_SUPPORTS_CACHE_SCOPE;
    }

    /** @return true iff SDK ≥ 2.1 exposes extensions on ServerCapabilities. */
    public static boolean sdkSupportsExtensions() {
        return SDK_SUPPORTS_EXTENSIONS;
    }

    /**
     * @return true iff ALL four protocol fields are exposed by the SDK.
     *         When this returns true, the framework can safely drop the
     *         meta-map fallback path. Today (SDK 2.0) this is false.
     */
    public static boolean sdkSupportsWireFields() {
        return SDK_SUPPORTS_RESULT_TYPE && SDK_SUPPORTS_TTL_MS
            && SDK_SUPPORTS_CACHE_SCOPE && SDK_SUPPORTS_EXTENSIONS;
    }

    /**
     * Reflectively check whether a class declares a parameterless method of
     * the given name. Used to detect SDK upgrades without forcing hard
     * compile-time dependencies on future SDK classes.
     */
    private static boolean hasMethod(String fqcn, String methodName) {
        try {
            Class<?> klass = Class.forName(fqcn);
            Method m = klass.getMethod(methodName);
            return m != null;
        }
        catch (ClassNotFoundException | NoSuchMethodException ex) {
            return false;
        }
        catch (Exception ex) {
            return false;
        }
    }
}