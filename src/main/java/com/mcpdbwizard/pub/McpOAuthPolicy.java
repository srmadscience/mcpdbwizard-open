package com.mcpdbwizard.pub;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.JWKSourceBuilder;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * OAuth 2.1 resource-server behaviour for a generated MCP server: validating access tokens, and
 * telling clients where to get one.
 *
 * <p>MCP's authorization spec makes a protected server a <em>resource server</em> and nothing more —
 * the authorization server is somebody else's (Entra, Okta, Keycloak, Auth0). So this does the four
 * things the spec requires of that role, and deliberately none of the things it requires of an
 * authorization server:
 *
 * <ol>
 *   <li>publish Protected Resource Metadata (RFC 9728) naming the authorization server;</li>
 *   <li>validate the access token's signature against the authorization server's JWKS;</li>
 *   <li><b>validate that the token was issued for this server</b> (RFC 8707 audience binding) — the
 *       requirement that stops a token minted for some other API being replayed here;</li>
 *   <li>answer 401 with a {@code WWW-Authenticate} challenge pointing at the metadata, and 403 with
 *       {@code error="insufficient_scope"} when the token is valid but too weak.</li>
 * </ol>
 *
 * <h2>Why a library does the signature check</h2>
 *
 * <p>Verification is delegated to Nimbus rather than hand-rolled. The classic JWT failures — an
 * {@code "alg":"none"} token accepted, an HMAC token verified against the public key, expiry never
 * checked, {@code kid} ignored — all turn the control silently into a no-op. The accepted algorithms
 * here are declared up front and are asymmetric only, which is what closes the confusion cases.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class McpOAuthPolicy {

    /** Issuer URL of the authorization server that mints tokens for this server. */
    public static final String ISSUER_VARIABLE = "MCP_OAUTH_ISSUER";

    /** This server's canonical URI — the audience a token must carry to be accepted here. */
    public static final String RESOURCE_VARIABLE = "MCP_OAUTH_RESOURCE";

    /** Optional explicit JWKS URI; derived from the issuer when unset. */
    public static final String JWKS_URI_VARIABLE = "MCP_OAUTH_JWKS_URI";

    /** Optional space- or comma-separated scopes a token must carry. */
    public static final String SCOPES_VARIABLE = "MCP_OAUTH_SCOPES";

    /** Where the metadata document is served, per RFC 9728. */
    public static final String METADATA_PATH = "/.well-known/oauth-protected-resource";

    /** Asymmetric only, and listed explicitly: this is what rules out "none" and HMAC confusion. */
    private static final Set<JWSAlgorithm> ACCEPTED_ALGORITHMS = Collections.unmodifiableSet(
            new LinkedHashSet<JWSAlgorithm>(Arrays.asList(
                    JWSAlgorithm.RS256, JWSAlgorithm.RS384, JWSAlgorithm.RS512,
                    JWSAlgorithm.PS256, JWSAlgorithm.PS384, JWSAlgorithm.PS512,
                    JWSAlgorithm.ES256, JWSAlgorithm.ES384, JWSAlgorithm.ES512)));

    /** What the caller should do with a request. */
    public static final class Decision {

        /** 0 when the request may proceed, else the HTTP status to send. */
        public final int status;

        /** The {@code WWW-Authenticate} value to send, or null. */
        public final String challenge;

        /** Why, for the log — never sent to the caller. */
        public final String reason;

        Decision(int theStatus, String theChallenge, String theReason) {
            this.status = theStatus;
            this.challenge = theChallenge;
            this.reason = theReason;
        }

        public boolean isAllowed() {
            return status == 0;
        }
    }

    private final String theIssuer;
    private final String theResource;
    private final Set<String> theRequiredScopes;
    private final DefaultJWTProcessor<SecurityContext> theProcessor;

    McpOAuthPolicy(String theIssuerValue, String theResourceValue, String theJwksUriValue,
                   String theScopesValue) throws CSException {
        this.theIssuer = requireValue(ISSUER_VARIABLE, theIssuerValue);
        this.theResource = requireValue(RESOURCE_VARIABLE, theResourceValue);
        this.theRequiredScopes = parseScopes(theScopesValue);

        URL theJwksUrl;
        try {
            theJwksUrl = new URI(theJwksUriValue == null || theJwksUriValue.trim().length() == 0
                    ? defaultJwksUri(this.theIssuer)
                    : theJwksUriValue.trim()).toURL();
        } catch (Exception e) {
            throw new CSException("Cannot build the JWKS URL for OAuth validation: " + e.getMessage()
                    + ". Set " + JWKS_URI_VARIABLE + " explicitly if the authorization server does not"
                    + " publish keys at the conventional location.");
        }

        // Cached and refreshed by the builder, so a running server is not fetching keys per request
        // and survives a key rotation without a restart.
        JWKSource<SecurityContext> theKeySource = JWKSourceBuilder.<SecurityContext>create(theJwksUrl).build();

        this.theProcessor = new DefaultJWTProcessor<SecurityContext>();
        this.theProcessor.setJWSKeySelector(
                new JWSVerificationKeySelector<SecurityContext>(ACCEPTED_ALGORITHMS, theKeySource));
        // Issuer must match exactly, the audience must contain this server, and exp must be present -
        // a token without an expiry would otherwise validate forever.
        this.theProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<SecurityContext>(
                this.theResource,
                new JWTClaimsSet.Builder().issuer(this.theIssuer).build(),
                new LinkedHashSet<String>(Arrays.asList("sub", "exp"))));
    }

    /** Build from the environment; fails closed if the required variables are absent. */
    public static McpOAuthPolicy fromEnvironment() throws CSException {
        return new McpOAuthPolicy(System.getenv(ISSUER_VARIABLE), System.getenv(RESOURCE_VARIABLE),
                System.getenv(JWKS_URI_VARIABLE), System.getenv(SCOPES_VARIABLE));
    }

    /**
     * Whether this request carries an acceptable access token.
     *
     * @param theAuthorizationHeader the raw {@code Authorization} header, or null
     */
    public Decision authorize(String theAuthorizationHeader) {
        if (theAuthorizationHeader == null || !theAuthorizationHeader.regionMatches(
                true, 0, "Bearer ", 0, "Bearer ".length())) {
            return new Decision(401, challenge(null, null), "no bearer token presented");
        }

        String theToken = theAuthorizationHeader.substring("Bearer ".length()).trim();
        JWTClaimsSet theClaims;
        try {
            theClaims = theProcessor.process(theToken, null);
        } catch (Exception e) {
            // Covers a bad signature, an unaccepted algorithm, a wrong issuer, a wrong audience and
            // an expired token alike. The caller is told nothing beyond "invalid_token".
            return new Decision(401, challenge("invalid_token", null), e.getMessage());
        }

        if (theRequiredScopes.isEmpty()) {
            return new Decision(0, null, null);
        }

        Set<String> theGranted = grantedScopes(theClaims);
        for (String theRequired : theRequiredScopes) {
            if (!theGranted.contains(theRequired)) {
                return new Decision(403, challenge("insufficient_scope", requiredScopeList()),
                        "token lacks scope " + theRequired);
            }
        }
        return new Decision(0, null, null);
    }

    /** The RFC 9728 document, served at {@link #METADATA_PATH}. */
    public String protectedResourceMetadataJson() {
        StringBuilder theJson = new StringBuilder();
        theJson.append("{\"resource\":\"").append(escape(theResource));
        theJson.append("\",\"authorization_servers\":[\"").append(escape(theIssuer)).append("\"]");
        theJson.append(",\"bearer_methods_supported\":[\"header\"]");
        if (!theRequiredScopes.isEmpty()) {
            theJson.append(",\"scopes_supported\":[");
            boolean theFirstFlag = true;
            for (String theScope : theRequiredScopes) {
                if (!theFirstFlag) {
                    theJson.append(',');
                }
                theJson.append('"').append(escape(theScope)).append('"');
                theFirstFlag = false;
            }
            theJson.append(']');
        }
        return theJson.append('}').toString();
    }

    /** Absolute URL of the metadata document, for the challenge header. */
    public String metadataUrl() {
        String theBase = theResource.endsWith("/")
                ? theResource.substring(0, theResource.length() - 1)
                : theResource;
        return theBase + METADATA_PATH;
    }

    /**
     * Build a {@code WWW-Authenticate} value.
     *
     * <p>{@code resource_metadata} is always present: it is how a client discovers where to get a
     * token, and without it a 401 tells an agent nothing actionable.
     */
    private String challenge(String theError, String theScopeList) {
        StringBuilder theChallenge = new StringBuilder("Bearer resource_metadata=\"");
        theChallenge.append(metadataUrl()).append('"');
        if (theError != null) {
            theChallenge.append(", error=\"").append(theError).append('"');
        }
        if (theScopeList != null) {
            theChallenge.append(", scope=\"").append(theScopeList).append('"');
        } else if (!theRequiredScopes.isEmpty()) {
            theChallenge.append(", scope=\"").append(requiredScopeList()).append('"');
        }
        return theChallenge.toString();
    }

    private String requiredScopeList() {
        StringBuilder theList = new StringBuilder();
        for (String theScope : theRequiredScopes) {
            if (theList.length() > 0) {
                theList.append(' ');
            }
            theList.append(theScope);
        }
        return theList.toString();
    }

    /** Read granted scopes from either the standard {@code scope} string or an array claim. */
    private static Set<String> grantedScopes(JWTClaimsSet theClaims) {
        Set<String> theGranted = new LinkedHashSet<String>();

        Object theScopeClaim = theClaims.getClaim("scope");
        if (theScopeClaim instanceof String) {
            theGranted.addAll(splitScopes((String) theScopeClaim));
        }
        // Some authorization servers (Entra among them) emit an array under "scp" instead.
        Object theScpClaim = theClaims.getClaim("scp");
        if (theScpClaim instanceof String) {
            theGranted.addAll(splitScopes((String) theScpClaim));
        } else if (theScpClaim instanceof List) {
            for (Object theEntry : (List<?>) theScpClaim) {
                if (theEntry != null) {
                    theGranted.add(theEntry.toString());
                }
            }
        }
        return theGranted;
    }

    private static Set<String> parseScopes(String theValue) {
        return theValue == null ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<String>(splitScopes(theValue)));
    }

    private static List<String> splitScopes(String theValue) {
        java.util.ArrayList<String> theScopes = new java.util.ArrayList<String>();
        for (String thePart : theValue.split("[,\\s]+")) {
            if (thePart.length() > 0) {
                theScopes.add(thePart);
            }
        }
        return theScopes;
    }

    /** The conventional JWKS location for an issuer, used when none is configured. */
    static String defaultJwksUri(String theIssuer) {
        String theBase = theIssuer.endsWith("/")
                ? theIssuer.substring(0, theIssuer.length() - 1)
                : theIssuer;
        return theBase + "/.well-known/jwks.json";
    }

    private static String requireValue(String theName, String theValue) throws CSException {
        if (theValue == null || theValue.trim().length() == 0) {
            throw new CSException("OAuth is enabled for this server but " + theName
                    + " is unset or empty. It is needed to validate access tokens, so the server"
                    + " cannot accept any request without it.");
        }
        return theValue.trim();
    }

    private static String escape(String theText) {
        return theText.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
