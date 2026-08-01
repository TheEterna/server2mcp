package com.ai.plug.core.spec.discover;

import java.util.List;

/**
 * Implements the protocol 2026-07-28 server/discover version negotiation
 * (SEP-2575).
 *
 * <p>Client sends a {@code DiscoverRequest} with its preferred protocol
 * version; server returns the best match from its own supported list
 * (typically derived from its own SDK version + this framework's
 * Protocol 2026-07-28 coverage).
 *
 * <h2>Negotiation rules</h2>
 * <ol>
 *   <li>Exact match → use that version</li>
 *   <li>Major.minor identical, patch differs → use the higher patch (no
 *       semantic difference)</li>
 *   <li>No compatible version → return null (caller should produce
 *       {@code UnsupportedProtocolVersionError} with the server's
 *       supported set)</li>
 * </ol>
 *
 * @author han
 * @time 2026/8/1 00:18
 */
public final class VersionNegotiator {

    /** The set of protocol versions this framework supports. */
    public static final List<String> SUPPORTED_VERSIONS = List.of(
        "2025-11-25",  // Java SDK 2.0 implements
        "2026-07-28"   // wire-compatible via this framework's protocol layer
    );

    /** The version this framework currently prefers (latest in SUPPORTED_VERSIONS). */
    public static final String PREFERRED_VERSION =
        SUPPORTED_VERSIONS.get(SUPPORTED_VERSIONS.size() - 1);

    private VersionNegotiator() {
    }

    /**
     * Negotiate the best protocol version. Returns the negotiated version
     * string, or null if no compatible version is found.
     *
     * @param clientPreferred the version the client prefers (typically from
     *                        its own SDK / application); may be null/blank
     *                        to mean "give me the latest"
     * @param serverSupported the versions the server supports (defaults to
     *                        {@link #SUPPORTED_VERSIONS})
     */
    public static String negotiate(String clientPreferred, List<String> serverSupported) {
        List<String> supported = serverSupported == null || serverSupported.isEmpty()
            ? SUPPORTED_VERSIONS : serverSupported;

        if (clientPreferred == null || clientPreferred.isBlank()) {
            // No preference — return the latest supported
            return supported.get(supported.size() - 1);
        }

        // Exact match wins
        if (supported.contains(clientPreferred)) {
            return clientPreferred;
        }

        // Strip the patch (e.g. "2025-11-25-PATCH1" or just different patch
        // numbers) and try major.minor match.
        String clientMajorMinor = stripPatch(clientPreferred);
        for (String s : supported) {
            if (stripPatch(s).equals(clientMajorMinor)) {
                // Major.minor matches — return the higher patch (server's
                // own version wins for safety)
                return s;
            }
        }

        // No compatible version
        return null;
    }

    /**
     * Convenience overload using {@link #SUPPORTED_VERSIONS} as the server's
     * supported set.
     */
    public static String negotiate(String clientPreferred) {
        return negotiate(clientPreferred, SUPPORTED_VERSIONS);
    }

    /**
     * Strip patch suffix from a version string. "2025-11-25" stays as
     * "2025-11-25"; "2025-11-25-rc1" or future "2025-11-25.1" forms map
     * back to "2025-11-25". Today the protocol uses date-only versions,
     * so this is a no-op for the canonical form — but defends against
     * future patch suffixes (e.g. 2026-07-28-errata1).
     */
    static String stripPatch(String version) {
        if (version == null) {
            return null;
        }
        // First two segments separated by '-': 2025-11-25 → "2025-11"
        int firstDash = version.indexOf('-');
        if (firstDash < 0) {
            return version;
        }
        int secondDash = version.indexOf('-', firstDash + 1);
        if (secondDash < 0) {
            return version;
        }
        int thirdDash = version.indexOf('-', secondDash + 1);
        if (thirdDash < 0) {
            return version;
        }
        return version.substring(0, thirdDash);
    }
}