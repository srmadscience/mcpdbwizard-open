package com.mcpdbwizard.pub;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * How a generated MCP server exposes its Streamable HTTP transport: which network interface it
 * binds, and which browser origins it will answer.
 *
 * <p>Both are required by the MCP specification's transport security rules, and both are read from
 * the environment at run time rather than baked into generated source — the same pattern the bearer
 * token ({@code MCP_HTTP_TOKEN}) and the TLS keystore already use.
 *
 * <h2>Binding</h2>
 *
 * <p>The spec says a server running locally <em>SHOULD</em> bind only to loopback rather than every
 * interface. Jetty's default is every interface, so the default here is {@value #DEFAULT_BIND_HOST}
 * and exposing the server beyond the machine is a deliberate act: set {@value #BIND_HOST_VARIABLE}.
 * That matters because the other two protections are opt-in — a server generated with neither
 * {@code MCP_HTTP_TOKEN} nor {@code MCP_HTTPS} answers anyone who can reach the port.
 *
 * <h2>Origins</h2>
 *
 * <p>The spec requires that a server <em>MUST</em> validate the {@code Origin} header and answer 403
 * when it is present and invalid, to defeat DNS rebinding. Rebinding works by making the attacker's
 * page look <em>same-origin</em> to the browser, so the same-origin policy never engages and the
 * browser sends the request without a preflight; checking {@code Origin} server-side is what closes
 * it.
 *
 * <p>An absent {@code Origin} is allowed: non-browser MCP clients do not send one, and the
 * requirement is about rejecting a header that is present and wrong. A malformed one, or the literal
 * {@code null} that browsers send from sandboxed and {@code file:} contexts, is rejected.
 *
 * <p>With {@value #ALLOWED_ORIGINS_VARIABLE} unset, loopback origins are allowed and nothing else —
 * which pairs with the loopback default above. Setting it <em>replaces</em> that default rather than
 * adding to it, so the variable always states the whole allowlist; include the loopback forms
 * explicitly if a local browser client still needs them. The single value {@value #ALLOW_ANY_ORIGIN}
 * disables the check, which forfeits the protection and should be a last resort.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class McpHttpPolicy {

    /** Environment variable naming the interface to bind, e.g. {@code 0.0.0.0} for all of them. */
    public static final String BIND_HOST_VARIABLE = "MCP_HTTP_HOST";

    /** Environment variable holding a comma-separated allowlist of origins. */
    public static final String ALLOWED_ORIGINS_VARIABLE = "MCP_ALLOWED_ORIGINS";

    /** Loopback, so a generated server is not on the network until someone says so. */
    public static final String DEFAULT_BIND_HOST = "127.0.0.1";

    /** The one {@value #ALLOWED_ORIGINS_VARIABLE} value that turns the origin check off. */
    public static final String ALLOW_ANY_ORIGIN = "*";

    /** Set this to serve an unauthenticated server off loopback anyway. */
    public static final String ALLOW_UNAUTHENTICATED_VARIABLE = "MCP_ALLOW_UNAUTHENTICATED_EXPOSURE";

    /** Largest request body accepted on the MCP endpoint. Unset means no cap. */
    public static final String MAX_REQUEST_BYTES_VARIABLE = "MCP_MAX_REQUEST_BYTES";

    /** Normalised {@code scheme://host:port} entries; empty means "loopback only". */
    private final Set<String> theAllowedOrigins;

    private final boolean theAnyOriginFlag;

    /**
     * @param theSetting the raw {@value #ALLOWED_ORIGINS_VARIABLE} value, or null when unset
     */
    McpHttpPolicy(String theSetting) {
        String theTrimmedSetting = theSetting == null ? "" : theSetting.trim();
        this.theAnyOriginFlag = ALLOW_ANY_ORIGIN.equals(theTrimmedSetting);

        Set<String> theEntries = new LinkedHashSet<String>();
        if (!theAnyOriginFlag && theTrimmedSetting.length() > 0) {
            String[] theParts = theTrimmedSetting.split(",");
            for (int i = 0; i < theParts.length; i++) {
                String theNormalised = normalise(theParts[i].trim());
                // A junk entry is dropped rather than fatal: it must never widen the allowlist, and
                // failing the whole server over one malformed origin would be worse than ignoring it.
                if (theNormalised != null) {
                    theEntries.add(theNormalised);
                }
            }
        }
        this.theAllowedOrigins = Collections.unmodifiableSet(theEntries);
    }

    /** Build the policy from the process environment. */
    public static McpHttpPolicy fromEnvironment() {
        return new McpHttpPolicy(System.getenv(ALLOWED_ORIGINS_VARIABLE));
    }

    /**
     * The interface the HTTP transport should bind, defaulting to loopback.
     *
     * @return a host suitable for {@code InetSocketAddress} / {@code ServerConnector.setHost}
     */
    public static String bindHost() {
        return bindHost(System.getenv(BIND_HOST_VARIABLE));
    }

    /** Testable half of {@link #bindHost()}. */
    static String bindHost(String theSetting) {
        String theTrimmedSetting = theSetting == null ? "" : theSetting.trim();
        if (theTrimmedSetting.length() == 0) {
            return DEFAULT_BIND_HOST;
        }
        // "*" is the familiar shorthand for every interface; Jetty wants the address form.
        if (ALLOW_ANY_ORIGIN.equals(theTrimmedSetting)) {
            return "0.0.0.0";
        }
        return theTrimmedSetting;
    }

    /** True when this policy has been told to answer every origin. */
    public boolean isAnyOriginAllowed() {
        return theAnyOriginFlag;
    }

    /** Whether a bind address keeps the server on this machine. */
    public static boolean isLoopbackBindHost(String theHost) {
        if (theHost == null || theHost.trim().length() == 0) {
            return true;
        }
        String theTrimmedHost = theHost.trim().toLowerCase(Locale.ENGLISH);
        return "localhost".equals(theTrimmedHost)
                || "::1".equals(theTrimmedHost)
                || "[::1]".equals(theTrimmedHost)
                || isLoopbackIpv4(theTrimmedHost);
    }

    /**
     * Why this server must not start, or null if it may.
     *
     * <p>Binding off loopback is the single act that puts a generated server on a network, and until
     * now nothing tied it to the two features that protect one. Both of those are opt-in and both
     * fail closed, so a config can quite reasonably ship without them — which is fine on loopback and
     * not fine once the port is reachable. Publishing the container's MCP port needs
     * {@code MCP_HTTP_HOST}, so the exposing step is exactly where the question belongs.
     *
     * <p><b>Authentication, specifically — TLS does not count.</b> TLS encrypts the wire and
     * restricts nobody; a server with TLS and no token is an open server that is merely hard to
     * eavesdrop on. The thing that decides who may call is the bearer token.
     *
     * @param theBindHost                the interface about to be bound
     * @param theAuthenticationGenerated whether this server was generated with bearer-token auth
     */
    public static String exposureRefusalReason(String theBindHost, boolean theAuthenticationGenerated) {
        return exposureRefusalReason(theBindHost, theAuthenticationGenerated,
                System.getenv(ALLOW_UNAUTHENTICATED_VARIABLE));
    }

    /** Testable half of {@link #exposureRefusalReason(String, boolean)}. */
    static String exposureRefusalReason(String theBindHost, boolean theAuthenticationGenerated,
                                        String theOverride) {
        if (isLoopbackBindHost(theBindHost) || theAuthenticationGenerated || isAffirmative(theOverride)) {
            return null;
        }
        return "Refusing to start: this server would bind " + theBindHost
                + ", which is reachable from the network, but it was generated without bearer-token"
                + " authentication (MCP_HTTP_TOKEN), so anyone who can reach the port could call every"
                + " tool. Regenerate with MCP_HTTP_TOKEN=YES, or bind loopback by unsetting"
                + " " + BIND_HOST_VARIABLE + ", or set " + ALLOW_UNAUTHENTICATED_VARIABLE + "=YES if"
                + " something in front of this server is doing the authenticating.";
    }

    /** Whether the exposure guard has been deliberately disabled — worth saying out loud. */
    public static boolean isUnauthenticatedExposureOverridden() {
        return isAffirmative(System.getenv(ALLOW_UNAUTHENTICATED_VARIABLE));
    }

    /**
     * Largest request body to accept, or 0 for no cap.
     *
     * <p>Opt-in rather than defaulted. A cap low enough to be useful against heap exhaustion is also
     * low enough to reject a legitimate large argument — a base64 BLOB being written through a tool,
     * say — and only the deployment knows which of its tools carry bulk. Defaulting this on would
     * trade a rare failure for a routine one.
     */
    public static long maxRequestBytes() {
        return maxRequestBytes(System.getenv(MAX_REQUEST_BYTES_VARIABLE));
    }

    /** Testable half of {@link #maxRequestBytes()}. */
    static long maxRequestBytes(String theSetting) {
        if (theSetting == null || theSetting.trim().length() == 0) {
            return 0L;
        }
        long theValue;
        try {
            theValue = Long.parseLong(theSetting.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(MAX_REQUEST_BYTES_VARIABLE
                    + " must be a whole number of bytes, not '" + theSetting.trim() + "'");
        }
        if (theValue <= 0L) {
            throw new IllegalArgumentException(MAX_REQUEST_BYTES_VARIABLE
                    + " must be greater than zero, not '" + theSetting.trim() + "'");
        }
        return theValue;
    }

    private static boolean isAffirmative(String theValue) {
        if (theValue == null) {
            return false;
        }
        String theTrimmedValue = theValue.trim();
        return "YES".equalsIgnoreCase(theTrimmedValue)
                || "TRUE".equalsIgnoreCase(theTrimmedValue)
                || "1".equals(theTrimmedValue);
    }

    /**
     * Whether a request carrying this {@code Origin} may proceed.
     *
     * @param theOrigin the raw header value; null or blank when the client sent none
     * @return false only when the header is present and not allowed, which the caller answers 403
     */
    public boolean isOriginAllowed(String theOrigin) {
        if (theOrigin == null || theOrigin.trim().length() == 0) {
            // No header: not a browser. The spec's requirement is about a present, invalid one.
            return true;
        }
        if (theAnyOriginFlag) {
            return true;
        }

        String theNormalised = normalise(theOrigin.trim());
        if (theNormalised == null) {
            // Unparseable, or the literal "null" a sandboxed/file: context sends. Opaque origins
            // cannot be told apart from an attacker's, so they do not get in.
            return false;
        }
        if (!theAllowedOrigins.isEmpty()) {
            return theAllowedOrigins.contains(theNormalised);
        }
        return isLoopbackOrigin(theNormalised);
    }

    /**
     * Reduce an origin to {@code scheme://host:port} with the default port filled in, so that
     * {@code http://localhost} and {@code http://localhost:80} compare equal.
     *
     * @return null when the value is not a usable origin
     */
    private static String normalise(String theOrigin) {
        if (theOrigin.length() == 0 || "null".equalsIgnoreCase(theOrigin)) {
            return null;
        }

        URI theUri;
        try {
            theUri = new URI(theOrigin);
        } catch (Exception e) {
            return null;
        }

        String theScheme = theUri.getScheme();
        String theHost = theUri.getHost();
        if (theScheme == null || theHost == null || theHost.length() == 0) {
            return null;
        }
        // A real origin has no path, query or fragment. Tolerate a bare trailing slash, which some
        // clients send, and reject anything with more in it than an origin can carry.
        String thePath = theUri.getPath();
        if (thePath != null && thePath.length() > 0 && !"/".equals(thePath)) {
            return null;
        }
        if (theUri.getQuery() != null || theUri.getFragment() != null || theUri.getUserInfo() != null) {
            return null;
        }

        theScheme = theScheme.toLowerCase(Locale.ENGLISH);
        theHost = theHost.toLowerCase(Locale.ENGLISH);

        int thePort = theUri.getPort();
        if (thePort == -1) {
            if ("http".equals(theScheme)) {
                thePort = 80;
            } else if ("https".equals(theScheme)) {
                thePort = 443;
            } else {
                // No default port to assume for anything else, so the origin is not comparable.
                return null;
            }
        }
        return theScheme + "://" + theHost + ":" + thePort;
    }

    /** Whether a normalised origin names this machine. */
    private static boolean isLoopbackOrigin(String theNormalisedOrigin) {
        int theSchemeEnd = theNormalisedOrigin.indexOf("://");
        int thePortStart = theNormalisedOrigin.lastIndexOf(':');
        String theHost = theNormalisedOrigin.substring(theSchemeEnd + 3, thePortStart);

        return "localhost".equals(theHost)
                || "::1".equals(theHost)
                || "[::1]".equals(theHost)
                || isLoopbackIpv4(theHost);
    }

    /**
     * Whether a host is a literal address in 127.0.0.0/8.
     *
     * <p>Deliberately a full parse rather than a {@code startsWith("127.")}, which would also admit
     * {@code 127.0.0.1.evil.example.com} — a hostname an attacker can simply register.
     */
    private static boolean isLoopbackIpv4(String theHost) {
        String[] theOctets = theHost.split("\\.", -1);
        if (theOctets.length != 4) {
            return false;
        }
        for (int i = 0; i < theOctets.length; i++) {
            if (theOctets[i].length() == 0 || theOctets[i].length() > 3) {
                return false;
            }
            for (int c = 0; c < theOctets[i].length(); c++) {
                if (!Character.isDigit(theOctets[i].charAt(c))) {
                    return false;
                }
            }
            if (Integer.parseInt(theOctets[i]) > 255) {
                return false;
            }
        }
        return "127".equals(theOctets[0]);
    }
}
