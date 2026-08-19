package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Database-free tests for the MCP HTTP transport's exposure rules.
 *
 * <p>These pin a security control, so the interesting cases are the ones that must be REFUSED. The
 * attack being defended against is DNS rebinding, where the attacker's page looks same-origin to the
 * browser and therefore reaches the server without a preflight — the server-side {@code Origin}
 * check is the only thing left in the way.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpHttpPolicyTest {

    /** The policy a server gets when nobody has configured anything. */
    private static McpHttpPolicy defaultPolicy() {
        return new McpHttpPolicy(null);
    }

    // ---- binding --------------------------------------------------------

    @Test
    void aGeneratedServerIsNotOnTheNetworkUntilSomeoneSaysSo() {
        // Jetty's own default is every interface. Ours must not be: token and TLS are both opt-in,
        // so an unconfigured server on 0.0.0.0 would answer anyone who can reach the port.
        assertEquals("127.0.0.1", McpHttpPolicy.bindHost(null));
        assertEquals("127.0.0.1", McpHttpPolicy.bindHost(""));
        assertEquals("127.0.0.1", McpHttpPolicy.bindHost("   "));
    }

    @Test
    void exposingItIsOneDeliberateVariable() {
        assertEquals("0.0.0.0", McpHttpPolicy.bindHost("0.0.0.0"));
        assertEquals("0.0.0.0", McpHttpPolicy.bindHost("*"), "* is the familiar shorthand");
        assertEquals("192.0.2.10", McpHttpPolicy.bindHost(" 192.0.2.10 "), "surrounding space is not a host");
    }

    // ---- origin: the absent case ----------------------------------------

    @Test
    void aClientThatSendsNoOriginIsNotABrowserAndIsAllowed() {
        // Every non-browser MCP client omits it; the spec's requirement is to reject a header that
        // is present and wrong, not to demand one.
        assertTrue(defaultPolicy().isOriginAllowed(null));
        assertTrue(defaultPolicy().isOriginAllowed(""));
        assertTrue(defaultPolicy().isOriginAllowed("  "));
    }

    // ---- origin: the default allowlist ----------------------------------

    @Test
    void loopbackOriginsAreAllowedByDefault() {
        McpHttpPolicy thePolicy = defaultPolicy();

        assertTrue(thePolicy.isOriginAllowed("http://localhost:8090"));
        assertTrue(thePolicy.isOriginAllowed("http://127.0.0.1:8090"));
        assertTrue(thePolicy.isOriginAllowed("https://localhost"));
        assertTrue(thePolicy.isOriginAllowed("http://[::1]:8090"));
        assertTrue(thePolicy.isOriginAllowed("http://127.0.0.53:3000"), "the whole 127/8 range is this machine");
    }

    @Test
    void anythingElseIsRefusedByDefault() {
        McpHttpPolicy thePolicy = defaultPolicy();

        assertFalse(thePolicy.isOriginAllowed("https://evil.example.com"));
        assertFalse(thePolicy.isOriginAllowed("http://192.0.2.10:8090"));
        assertFalse(thePolicy.isOriginAllowed("http://localhost.evil.example.com"),
                "a hostname that merely starts with localhost is a different host");
        assertFalse(thePolicy.isOriginAllowed("http://127.0.0.1.evil.example.com"),
                "and so is one that merely starts with the loopback address");
    }

    @Test
    void anOpaqueOriginIsRefused() {
        // Browsers send the literal "null" from sandboxed iframes and file: pages. It cannot be
        // told apart from an attacker's, so it does not get in.
        assertFalse(defaultPolicy().isOriginAllowed("null"));
        assertFalse(defaultPolicy().isOriginAllowed("NULL"));
    }

    @Test
    void aMalformedOriginIsRefusedRatherThanParsedLoosely() {
        McpHttpPolicy thePolicy = defaultPolicy();

        assertFalse(thePolicy.isOriginAllowed("localhost:8090"), "no scheme is not an origin");
        assertFalse(thePolicy.isOriginAllowed("http://"));
        assertFalse(thePolicy.isOriginAllowed("not a uri at all"));
        assertFalse(thePolicy.isOriginAllowed("http://localhost:8090/../.."), "an origin carries no path");
        assertFalse(thePolicy.isOriginAllowed("ftp://localhost"), "no default port to compare on");
        assertFalse(thePolicy.isOriginAllowed("http://user@localhost:8090"), "credentials are not part of an origin");
    }

    // ---- origin: an explicit allowlist -----------------------------------

    @Test
    void anExplicitListReplacesTheDefaultRatherThanExtendingIt() {
        // The variable states the WHOLE allowlist, so locking it down is possible. Anyone who still
        // wants a local browser client lists the loopback form too.
        McpHttpPolicy thePolicy = new McpHttpPolicy("https://app.example.com");

        assertTrue(thePolicy.isOriginAllowed("https://app.example.com"));
        assertFalse(thePolicy.isOriginAllowed("http://localhost:8090"),
                "loopback is no longer implied once an allowlist is given");
    }

    @Test
    void listedOriginsCompareOnSchemeHostAndPort() {
        McpHttpPolicy thePolicy = new McpHttpPolicy("https://app.example.com, http://localhost:8090");

        assertTrue(thePolicy.isOriginAllowed("https://app.example.com:443"), "the default port is filled in");
        assertTrue(thePolicy.isOriginAllowed("HTTPS://APP.EXAMPLE.COM"), "scheme and host are case-insensitive");
        assertTrue(thePolicy.isOriginAllowed("http://localhost:8090/"), "a bare trailing slash is tolerated");
        assertTrue(thePolicy.isOriginAllowed("http://localhost:8090"));

        assertFalse(thePolicy.isOriginAllowed("http://app.example.com"), "a different scheme is a different origin");
        assertFalse(thePolicy.isOriginAllowed("https://app.example.com:8443"), "and so is a different port");
        assertFalse(thePolicy.isOriginAllowed("https://evil.app.example.com"));
        assertFalse(thePolicy.isOriginAllowed("http://localhost:9999"), "a listed port does not admit its neighbours");
    }

    @Test
    void aJunkEntryIsDroppedWithoutWideningTheList() {
        McpHttpPolicy thePolicy = new McpHttpPolicy("https://app.example.com, ,rubbish, http://localhost:8090");

        assertTrue(thePolicy.isOriginAllowed("https://app.example.com"));
        assertTrue(thePolicy.isOriginAllowed("http://localhost:8090"));
        assertFalse(thePolicy.isOriginAllowed("https://evil.example.com"),
                "a malformed entry must never turn into a wildcard");
    }

    @Test
    void theCheckCanBeTurnedOffOnlyByAskingForExactlyThat() {
        McpHttpPolicy theOpenPolicy = new McpHttpPolicy("*");

        assertTrue(theOpenPolicy.isAnyOriginAllowed());
        assertTrue(theOpenPolicy.isOriginAllowed("https://evil.example.com"));
        assertTrue(theOpenPolicy.isOriginAllowed("null"));

        // Not a wildcard: a list that happens to contain a star among real entries.
        McpHttpPolicy theListPolicy = new McpHttpPolicy("https://app.example.com,*");
        assertFalse(theListPolicy.isAnyOriginAllowed());
        assertFalse(theListPolicy.isOriginAllowed("https://evil.example.com"));
    }

    @Test
    void theDefaultPolicyIsNotAWildcard() {
        assertFalse(defaultPolicy().isAnyOriginAllowed());
    }

    // ---- the exposure guard ----------------------------------------------

    @Test
    void loopbackNeedsNoAuthenticationBecauseItIsNotReachable() {
        // The common case: a server on the developer's own machine must stay frictionless.
        assertNull(McpHttpPolicy.exposureRefusalReason("127.0.0.1", false, null));
        assertNull(McpHttpPolicy.exposureRefusalReason("localhost", false, null));
        assertNull(McpHttpPolicy.exposureRefusalReason("::1", false, null));
        assertNull(McpHttpPolicy.exposureRefusalReason(null, false, null), "unset means the default, loopback");
    }

    @Test
    void bindingOffLoopbackWithoutAuthenticationIsRefused() {
        String theReason = McpHttpPolicy.exposureRefusalReason("0.0.0.0", false, null);

        assertNotNull(theReason, "an unauthenticated server must not reach the network by accident");
        assertTrue(theReason.contains("MCP_HTTP_TOKEN"), "the message must name the fix");
        assertTrue(theReason.contains(McpHttpPolicy.ALLOW_UNAUTHENTICATED_VARIABLE),
                "and the deliberate override, for whoever has a proxy in front");
    }

    @Test
    void authenticationSatisfiesTheGuard() {
        assertNull(McpHttpPolicy.exposureRefusalReason("0.0.0.0", true, null));
        assertNull(McpHttpPolicy.exposureRefusalReason("192.0.2.10", true, null));
    }

    @Test
    void theOverrideIsHonouredButOnlyWhenItSaysSo() {
        assertNull(McpHttpPolicy.exposureRefusalReason("0.0.0.0", false, "YES"));
        assertNull(McpHttpPolicy.exposureRefusalReason("0.0.0.0", false, "true"));
        assertNull(McpHttpPolicy.exposureRefusalReason("0.0.0.0", false, "1"));

        // Anything else is not consent — including the empty string an unset-but-exported var gives.
        assertNotNull(McpHttpPolicy.exposureRefusalReason("0.0.0.0", false, ""));
        assertNotNull(McpHttpPolicy.exposureRefusalReason("0.0.0.0", false, "no"));
        assertNotNull(McpHttpPolicy.exposureRefusalReason("0.0.0.0", false, "maybe"));
    }

    @Test
    void aHostnameThatMerelyLooksLocalIsNotLoopback() {
        // Same class of bug as the origin allowlist: a prefix match would admit a registrable name.
        assertFalse(McpHttpPolicy.isLoopbackBindHost("localhost.evil.example.com"));
        assertFalse(McpHttpPolicy.isLoopbackBindHost("127.0.0.1.evil.example.com"));
        assertNotNull(McpHttpPolicy.exposureRefusalReason("127.0.0.1.evil.example.com", false, null));

        assertTrue(McpHttpPolicy.isLoopbackBindHost("127.0.0.1"));
        assertTrue(McpHttpPolicy.isLoopbackBindHost("127.53.9.1"), "the whole 127/8 range");
    }
}
