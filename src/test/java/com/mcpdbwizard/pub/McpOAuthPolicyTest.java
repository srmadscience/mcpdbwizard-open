package com.mcpdbwizard.pub;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the OAuth resource-server behaviour, against a real JWKS endpoint and real signatures.
 *
 * <p>A local {@link HttpServer} publishes a generated RSA key, and each case mints a token with it —
 * so the signature path being exercised is the genuine one rather than a stub. The cases that matter
 * are the refusals, and in particular the two that turn this control into a no-op if they regress:
 * an {@code alg:none} token, and an HMAC token offered where an asymmetric key is expected
 * (algorithm confusion). Both must be rejected before any claim is looked at.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpOAuthPolicyTest {

    private static final String ISSUER = "https://as.example.com";
    private static final String RESOURCE = "https://mcp.example.com/mcp";

    private static HttpServer theJwksServer;
    private static RSAKey theSigningKey;
    private static String theJwksUri;

    @BeforeAll
    static void publishAKeySet() throws Exception {
        theSigningKey = new RSAKeyGenerator(2048).keyID("test-key").generate();
        String theJwks = "{\"keys\":[" + theSigningKey.toPublicJWK().toJSONString() + "]}";

        theJwksServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        theJwksServer.createContext("/jwks", theExchange -> {
            byte[] theBody = theJwks.getBytes(StandardCharsets.UTF_8);
            theExchange.getResponseHeaders().add("Content-Type", "application/json");
            theExchange.sendResponseHeaders(200, theBody.length);
            theExchange.getResponseBody().write(theBody);
            theExchange.close();
        });
        theJwksServer.start();
        theJwksUri = "http://127.0.0.1:" + theJwksServer.getAddress().getPort() + "/jwks";
    }

    @AfterAll
    static void stopServing() {
        if (theJwksServer != null) {
            theJwksServer.stop(0);
        }
    }

    private static McpOAuthPolicy policy(String theScopes) throws Exception {
        return new McpOAuthPolicy(ISSUER, RESOURCE, theJwksUri, theScopes);
    }

    /** A token signed with the published key. */
    private static String token(JWTClaimsSet theClaims) throws Exception {
        SignedJWT theJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(theSigningKey.getKeyID()).build(), theClaims);
        theJwt.sign(new RSASSASigner(theSigningKey));
        return "Bearer " + theJwt.serialize();
    }

    private static JWTClaimsSet.Builder goodClaims() {
        return new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .audience(RESOURCE)
                .subject("agent-1")
                .expirationTime(new Date(System.currentTimeMillis() + 300_000));
    }

    // ---- the accepting case ----------------------------------------------

    @Test
    void aProperlySignedTokenForThisServerIsAccepted() throws Exception {
        McpOAuthPolicy.Decision theDecision = policy(null).authorize(token(goodClaims().build()));

        assertTrue(theDecision.isAllowed(), "reason: " + theDecision.reason);
        assertEquals(0, theDecision.status);
    }

    // ---- audience binding, the requirement that stops replay --------------

    @Test
    void aTokenMintedForSomeoneElseIsRefused() throws Exception {
        // The MUST that matters most: a valid token for a different API must not work here, or any
        // service sharing this authorization server becomes a way in.
        McpOAuthPolicy.Decision theDecision = policy(null).authorize(
                token(goodClaims().audience("https://other-api.example.com").build()));

        assertEquals(401, theDecision.status);
    }

    @Test
    void aTokenFromADifferentIssuerIsRefused() throws Exception {
        McpOAuthPolicy.Decision theDecision = policy(null).authorize(
                token(goodClaims().issuer("https://evil.example.com").build()));

        assertEquals(401, theDecision.status);
    }

    @Test
    void anExpiredTokenIsRefused() throws Exception {
        McpOAuthPolicy.Decision theDecision = policy(null).authorize(token(goodClaims()
                .expirationTime(new Date(System.currentTimeMillis() - 60_000)).build()));

        assertEquals(401, theDecision.status);
    }

    @Test
    void aTokenWithNoExpiryIsRefused() throws Exception {
        // Otherwise it would be valid forever, which is indistinguishable from no expiry policy.
        JWTClaimsSet theClaims = new JWTClaimsSet.Builder()
                .issuer(ISSUER).audience(RESOURCE).subject("agent-1").build();

        assertEquals(401, policy(null).authorize(token(theClaims)).status);
    }

    // ---- the two classic forgeries ---------------------------------------

    @Test
    void anAlgNoneTokenIsRefused() throws Exception {
        // The unsigned-token attack: a well-formed JWT whose header claims no signature is needed.
        String theHeader = base64Url("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String thePayload = base64Url("{\"iss\":\"" + ISSUER + "\",\"aud\":\"" + RESOURCE
                + "\",\"sub\":\"agent-1\",\"exp\":" + ((System.currentTimeMillis() / 1000) + 300) + "}");

        McpOAuthPolicy.Decision theDecision = policy(null).authorize(
                "Bearer " + theHeader + "." + thePayload + ".");

        assertEquals(401, theDecision.status, "an unsigned token must never be accepted");
    }

    @Test
    void anHmacTokenIsRefusedRatherThanVerifiedAgainstThePublicKey() throws Exception {
        // Algorithm confusion: the attacker signs with HMAC using the public key as the secret,
        // hoping the server picks the algorithm from the token rather than from its own policy.
        byte[] theSecret = new byte[32];
        System.arraycopy(theSigningKey.toPublicJWK().toJSONString().getBytes(StandardCharsets.UTF_8),
                0, theSecret, 0, 32);
        SignedJWT theJwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).build(), goodClaims().build());
        theJwt.sign(new MACSigner(theSecret));

        assertEquals(401, policy(null).authorize("Bearer " + theJwt.serialize()).status);
    }

    @Test
    void aTamperedSignatureIsRefused() throws Exception {
        String theGood = token(goodClaims().build());
        String theTampered = theGood.substring(0, theGood.length() - 4) + "AAAA";

        assertEquals(401, policy(null).authorize(theTampered).status);
    }

    // ---- absent and malformed --------------------------------------------

    @Test
    void aRequestWithNoTokenGetsAChallengePointingAtTheMetadata() throws Exception {
        McpOAuthPolicy.Decision theDecision = policy(null).authorize(null);

        assertEquals(401, theDecision.status);
        assertNotNull(theDecision.challenge);
        assertTrue(theDecision.challenge.contains("resource_metadata="),
                "without this a client cannot discover where to get a token: " + theDecision.challenge);
        assertTrue(theDecision.challenge.contains(McpOAuthPolicy.METADATA_PATH));
    }

    @Test
    void aNonBearerAuthorizationHeaderIsRefused() throws Exception {
        assertEquals(401, policy(null).authorize("Basic dXNlcjpwYXNz").status);
        assertEquals(401, policy(null).authorize("").status);
        assertEquals(401, policy(null).authorize("Bearer not.a.jwt").status);
    }

    // ---- scopes -----------------------------------------------------------

    @Test
    void aMissingScopeIs403WithTheScopesNeeded() throws Exception {
        McpOAuthPolicy.Decision theDecision = policy("orders:read orders:write").authorize(
                token(goodClaims().claim("scope", "orders:read").build()));

        assertEquals(403, theDecision.status, "a valid token that is merely too weak is 403, not 401");
        assertTrue(theDecision.challenge.contains("error=\"insufficient_scope\""));
        assertTrue(theDecision.challenge.contains("orders:write"), theDecision.challenge);
    }

    @Test
    void allRequiredScopesPresentIsAccepted() throws Exception {
        assertTrue(policy("orders:read").authorize(
                token(goodClaims().claim("scope", "orders:read profile").build())).isAllowed());
    }

    @Test
    void theScpArrayFormIsUnderstoodToo() throws Exception {
        // Entra and friends emit an array under "scp" rather than a space-delimited "scope".
        assertTrue(policy("orders:read").authorize(token(goodClaims()
                .claim("scp", java.util.Arrays.asList("orders:read", "profile")).build())).isAllowed());
    }

    // ---- metadata document -------------------------------------------------

    @Test
    void theMetadataNamesThisResourceAndItsAuthorizationServer() throws Exception {
        String theJson = policy("orders:read").protectedResourceMetadataJson();

        assertTrue(theJson.contains("\"resource\":\"" + RESOURCE + "\""), theJson);
        assertTrue(theJson.contains("\"authorization_servers\":[\"" + ISSUER + "\"]"), theJson);
        assertTrue(theJson.contains("\"scopes_supported\":[\"orders:read\"]"), theJson);
        assertTrue(theJson.contains("\"bearer_methods_supported\":[\"header\"]"), theJson);
    }

    @Test
    void theMetadataUrlHangsOffTheResourceIdentifier() throws Exception {
        assertEquals(RESOURCE + McpOAuthPolicy.METADATA_PATH, policy(null).metadataUrl());
    }

    // ---- configuration -----------------------------------------------------

    @Test
    void anIncompleteConfigurationFailsClosed() {
        // A server that cannot validate must not start, rather than start and accept everything.
        assertThrows(CSException.class, () -> new McpOAuthPolicy(null, RESOURCE, theJwksUri, null));
        assertThrows(CSException.class, () -> new McpOAuthPolicy(ISSUER, "  ", theJwksUri, null));
    }

    @Test
    void theJwksUriIsDerivedFromTheIssuerWhenNotGiven() {
        assertEquals("https://as.example.com/.well-known/jwks.json",
                McpOAuthPolicy.defaultJwksUri("https://as.example.com"));
        assertEquals("https://as.example.com/.well-known/jwks.json",
                McpOAuthPolicy.defaultJwksUri("https://as.example.com/"), "a trailing slash must not double up");
    }

    @Test
    void aDecisionCarriesItsReasonForTheLogButNotForTheCaller() throws Exception {
        McpOAuthPolicy.Decision theDecision = policy(null).authorize(
                token(goodClaims().audience("https://other-api.example.com").build()));

        assertNotNull(theDecision.reason, "the operator needs to know why");
        assertFalse(theDecision.challenge.contains(theDecision.reason),
                "but the caller must not be told which check failed");
    }

    private static String base64Url(String theText) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(theText.getBytes(StandardCharsets.UTF_8));
    }
}
