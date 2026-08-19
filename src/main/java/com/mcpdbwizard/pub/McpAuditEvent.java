package com.mcpdbwizard.pub;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One auditable MCP tool call: what ran, how it ended, and — depending on the configured level — what
 * went in and came back.
 *
 * <h2>Truncation keeps integrity even when it loses content</h2>
 *
 * <p>A payload longer than the configured cap is cut, but the event still carries the <b>full byte
 * size</b> and a <b>SHA-256 of the whole payload</b>, and sets a truncated flag. A clipped record is
 * weak evidence on its own — "we logged the response, but not the part you are asking about" — so
 * truncation here costs readability rather than integrity, and nobody can mistake a cut record for a
 * complete one.
 *
 * <h2>Values are not recorded by default</h2>
 *
 * <p>At {@link McpAuditSinks.Level#NAMES} only argument names appear, which is what the diagnostic
 * {@link McpCallRecord} line has always done. Recording values makes this object carry production
 * data, and the retention, encryption and erasure obligations that come with it. That is a deliberate
 * decision for the deployment to take, not a default.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class McpAuditEvent {

    private final String theId;
    private final long theTimestampMillis;
    private final String theToolName;
    private final String theOutcome;
    private final long theDurationMillis;
    private final Map<String, Object> theArguments;
    private final boolean theValuesFlag;
    private final String theResponse;
    private final long theResponseBytes;
    private final String theResponseSha256;
    private final boolean theResponseTruncatedFlag;

    /**
     * Who made the call, which config they reached it through, the protocol operation, and the HTTP
     * status they got back. All four are null or zero for an event recorded by a generated server,
     * which cannot know any of them: it connects to Oracle as one shared account and sees a bearer
     * token, which is a door key rather than an identity.
     *
     * <p>They are filled in by the web application's {@code /mcp/{config}} proxy, the one component
     * that authenticates a real account. Keeping them on this event rather than inventing a second
     * one means an operator configures <em>the</em> audit sink once and receives both halves of the
     * story in one stream, in one format, with one set of delivery guarantees.
     */
    private final String theUser;
    private final String theConfig;
    private final String theOperation;
    private final int theStatus;

    /**
     * Set only for an event read back from a spool: the JSON as it was originally written.
     *
     * <p>A replayed record is re-emitted verbatim rather than parsed and rebuilt, so what a consumer
     * finally receives is byte-for-byte what was recorded at the time — a re-serialisation that
     * differed, even harmlessly, would undermine the hash the record carries.
     */
    private final String theRawJson;

    /**
     * Authentication-only fields: where the attempt came from, and why it was refused.
     *
     * <p>Null on every other kind of event. {@code theReason} is a FIXED VOCABULARY
     * ({@code bad-credentials}, {@code no-such-account}, {@code locked}, {@code disabled},
     * {@code expired}, {@code other}) and never a message from an exception — an authentication
     * failure object can carry the submitted credential, and a reason string built from one would
     * put a password into the audit trail. See {@link #ofAuth}.
     */
    private final String theSource;
    private final String theReason;

    /**
     * What an administrative action acted ON — the account created, the config deleted.
     *
     * <p>Separate from {@code user}, which is who DID it. A record naming only the actor answers
     * "who was busy" rather than "what changed", and in a dispute the second is the question.
     */
    private final String theTarget;

    McpAuditEvent(String theIdValue, long theTimestampValue, String theToolNameValue, String theOutcomeValue,
                  long theDurationValue, Map<String, Object> theArgumentsValue, boolean theValuesFlagValue,
                  String theResponseValue, long theResponseBytesValue, String theResponseHashValue,
                  boolean theTruncatedValue) {
        this.theId = theIdValue;
        this.theTimestampMillis = theTimestampValue;
        this.theToolName = theToolNameValue;
        this.theOutcome = theOutcomeValue;
        this.theDurationMillis = theDurationValue;
        this.theArguments = theArgumentsValue == null
                ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(theArgumentsValue));
        this.theValuesFlag = theValuesFlagValue;
        this.theResponse = theResponseValue;
        this.theResponseBytes = theResponseBytesValue;
        this.theResponseSha256 = theResponseHashValue;
        this.theResponseTruncatedFlag = theTruncatedValue;
        this.theRawJson = null;
        this.theUser = null;
        this.theConfig = null;
        this.theOperation = null;
        this.theStatus = 0;
        this.theSource = null;
        this.theReason = null;
        this.theTarget = null;
    }

    /** The caller-side event, carrying the four fields a generated server cannot know. */
    private McpAuditEvent(String theIdValue, long theTimestampValue, String theUserValue,
                          String theConfigValue, String theOperationValue, String theToolNameValue,
                          String theOutcomeValue, int theStatusValue, long theDurationValue) {
        this.theId = theIdValue;
        this.theTimestampMillis = theTimestampValue;
        this.theToolName = theToolNameValue;
        this.theOutcome = theOutcomeValue;
        this.theDurationMillis = theDurationValue;
        this.theArguments = Collections.<String, Object>emptyMap();
        this.theValuesFlag = false;
        this.theResponse = null;
        this.theResponseBytes = 0L;
        this.theResponseSha256 = null;
        this.theResponseTruncatedFlag = false;
        this.theRawJson = null;
        this.theUser = theUserValue;
        this.theConfig = theConfigValue;
        this.theOperation = theOperationValue;
        this.theStatus = theStatusValue;
        this.theSource = null;
        this.theReason = null;
        this.theTarget = null;
    }

    /**
     * Build the record for one request through the MCP proxy.
     *
     * <p>No arguments and no response: those belong to the tool call itself and the generated server
     * records them. This half answers who asked, for what, and whether they were allowed.
     *
     * @param theUserValue      the authenticated account, or null when the request had none
     * @param theConfigValue    the config named in the path
     * @param theOperationValue the JSON-RPC method, or the HTTP method when there is no body
     * @param theToolNameValue  the tool named by a {@code tools/call}, otherwise null
     * @param theStatusValue    the HTTP status returned to the caller
     */
    public static McpAuditEvent ofAccess(String theUserValue, String theConfigValue,
                                         String theOperationValue, String theToolNameValue,
                                         String theOutcomeValue, int theStatusValue,
                                         long theDurationValue) {
        return new McpAuditEvent(java.util.UUID.randomUUID().toString(), System.currentTimeMillis(),
                theUserValue, theConfigValue, theOperationValue, theToolNameValue, theOutcomeValue,
                theStatusValue, theDurationValue);
    }

    /** An event reconstituted from a spooled line. */
    private McpAuditEvent(String theRawJsonValue) {
        this.theId = null;
        this.theTimestampMillis = 0L;
        this.theToolName = null;
        this.theOutcome = null;
        this.theDurationMillis = 0L;
        this.theArguments = Collections.<String, Object>emptyMap();
        this.theValuesFlag = false;
        this.theResponse = null;
        this.theResponseBytes = 0L;
        this.theResponseSha256 = null;
        this.theResponseTruncatedFlag = false;
        this.theRawJson = theRawJsonValue;
        this.theUser = null;
        this.theConfig = null;
        this.theOperation = null;
        this.theStatus = 0;
        this.theSource = null;
        this.theReason = null;
        this.theTarget = null;
    }

    /** The authentication event. */
    private McpAuditEvent(String theIdValue, long theTimestampValue, String theUserValue,
                          String theOperationValue, String theOutcomeValue, String theSourceValue,
                          String theReasonValue, String theTargetValue) {
        this.theId = theIdValue;
        this.theTimestampMillis = theTimestampValue;
        this.theToolName = null;
        this.theOutcome = theOutcomeValue;
        this.theDurationMillis = 0L;
        this.theArguments = Collections.<String, Object>emptyMap();
        this.theValuesFlag = false;
        this.theResponse = null;
        this.theResponseBytes = 0L;
        this.theResponseSha256 = null;
        this.theResponseTruncatedFlag = false;
        this.theRawJson = null;
        this.theUser = theUserValue;
        this.theConfig = null;
        this.theOperation = theOperationValue;
        this.theStatus = 0;
        this.theSource = theSourceValue;
        this.theReason = theReasonValue;
        this.theTarget = theTargetValue;
    }

    /** A successful sign-in. */
    public static final String OP_LOGIN = "login";

    /** A refused sign-in — the record that matters most, and the one an attack leaves behind. */
    public static final String OP_LOGIN_FAILED = "login-failed";

    /** A sign-out. */
    public static final String OP_LOGOUT = "logout";

    /** The attempt was refused. */
    public static final String OUTCOME_DENIED = "denied";

    /** An account was created, removed, promoted or demoted, or had its password reset. */
    public static final String OP_ACCOUNT_CREATED = "account-created";
    public static final String OP_ACCOUNT_DELETED = "account-deleted";
    public static final String OP_ROLE_CHANGED = "role-changed";
    public static final String OP_PASSWORD_RESET = "password-reset";
    public static final String OP_TOKEN_ISSUED = "token-issued";
    public static final String OP_TOKEN_REVOKED = "token-revoked";

    /** Who may reach which config changed. */
    public static final String OP_ACCESS_CHANGED = "access-changed";

    /** A config was written or removed. A config decides which objects are reachable AT ALL. */
    public static final String OP_CONFIG_SAVED = "config-saved";
    public static final String OP_CONFIG_DELETED = "config-deleted";

    /**
     * Build an administrative record: who did what, to what.
     *
     * <p>These matter at least as much as the sign-in records beside them. An account created, a
     * role granted or an access-matrix row changed alters WHO CAN REACH WHAT, and a config decides
     * which database objects are reachable at all — curation here is enforced by absence from the
     * generated binary, so changing a config changes the reachable surface. None of it was recorded
     * anywhere before.
     *
     * @param theActor     the signed-in account performing the action
     * @param theOperation one of the {@code OP_} constants above
     * @param theTargetValue what was acted on: an account name, a config name, or null
     * @param theOutcomeValue {@code ok} or {@link #OUTCOME_DENIED}
     * @param theSourceValue  the remote address, or null
     */
    public static McpAuditEvent ofAdmin(String theActor, String theOperation, String theTargetValue,
                                        String theOutcomeValue, String theSourceValue) {
        return new McpAuditEvent(java.util.UUID.randomUUID().toString(), System.currentTimeMillis(),
                theActor, theOperation, theOutcomeValue, theSourceValue, null, theTargetValue);
    }

    /**
     * Build an authentication record.
     *
     * <p>Answers the question the tool-call records cannot: when an account signed in, from where,
     * and how many times it failed first. A run of failures is often the only trace an attack leaves,
     * which is why {@link #OP_LOGIN_FAILED} is recorded at least as carefully as a success.
     *
     * <p><b>Nothing derived from the submitted credential may be passed here.</b> {@code theReason}
     * is a fixed vocabulary chosen by the caller from the authentication exception's TYPE, never its
     * message and never the failed {@code Authentication} object, which in Spring Security can still
     * hold the password that was tried.
     *
     * @param theUserValue      the account name as submitted; may be an attacker's invention, so it
     *                          is recorded as given and never trusted as an identity
     * @param theOperationValue {@link #OP_LOGIN}, {@link #OP_LOGIN_FAILED} or {@link #OP_LOGOUT}
     * @param theOutcomeValue   {@code ok} or {@link #OUTCOME_DENIED}
     * @param theSourceValue    the remote address, or null when it cannot be determined
     * @param theReasonValue    why it was refused, from the fixed vocabulary; null on success
     */
    public static McpAuditEvent ofAuth(String theUserValue, String theOperationValue,
                                       String theOutcomeValue, String theSourceValue,
                                       String theReasonValue) {
        return new McpAuditEvent(java.util.UUID.randomUUID().toString(), System.currentTimeMillis(),
                theUserValue, theOperationValue, theOutcomeValue, theSourceValue, theReasonValue,
                null);
    }

    /**
     * Wrap an already-serialised record, for replay from a spool.
     *
     * @param theRawJsonValue the exact line that was written to disk
     */
    public static McpAuditEvent ofRawJson(String theRawJsonValue) {
        return new McpAuditEvent(theRawJsonValue);
    }

    /**
     * Build an event, applying the level and the truncation cap.
     *
     * @param theArgumentsValue the call's arguments; only the keys are used below
     *                          {@link McpAuditSinks.Level#VALUES}
     * @param theResponseValue  the response payload, or null
     * @param theMaxBytes       cap for the recorded response, or 0 for no cap
     */
    public static McpAuditEvent of(String theToolNameValue, Map<String, Object> theArgumentsValue,
                                   String theOutcomeValue, long theDurationValue,
                                   String theResponseValue, McpAuditSinks.Level theLevel, int theMaxBytes) {
        boolean theWantValues = theLevel == McpAuditSinks.Level.VALUES;

        String theRecordedResponse = null;
        long theFullBytes = 0L;
        String theHash = null;
        boolean theTruncated = false;

        if (theWantValues && theResponseValue != null) {
            byte[] theFull = theResponseValue.getBytes(StandardCharsets.UTF_8);
            theFullBytes = theFull.length;
            theHash = sha256(theFull);
            if (theMaxBytes > 0 && theFull.length > theMaxBytes) {
                theRecordedResponse = new String(theFull, 0, theMaxBytes, StandardCharsets.UTF_8);
                theTruncated = true;
            } else {
                theRecordedResponse = theResponseValue;
            }
        }

        return new McpAuditEvent(java.util.UUID.randomUUID().toString(),
                System.currentTimeMillis(), theToolNameValue, theOutcomeValue,
                theDurationValue, theArgumentsValue, theWantValues,
                theRecordedResponse, theFullBytes, theHash, theTruncated);
    }

    /**
     * Unique per call.
     *
     * <p>Present because spooled delivery is <b>at-least-once</b>: a crash between delivering a
     * segment and deleting it replays that segment, so a consumer sees the record twice. Without an
     * identity there is no way to collapse the duplicate, which would make the guarantee unusable
     * rather than merely imperfect.
     */
    public String getId() {
        return theId;
    }

    public long getTimestampMillis() {
        return theTimestampMillis;
    }

    public String getToolName() {
        return theToolName;
    }

    public String getOutcome() {
        return theOutcome;
    }

    public long getDurationMillis() {
        return theDurationMillis;
    }

    public Map<String, Object> getArguments() {
        return theArguments;
    }

    public String getResponse() {
        return theResponse;
    }

    public boolean isResponseTruncated() {
        return theResponseTruncatedFlag;
    }

    /** The authenticated account, or null — always null on an event a generated server built. */
    public String getUser() {
        return theUser;
    }

    /** The config the request named, or null. */
    public String getConfig() {
        return theConfig;
    }

    /** The JSON-RPC method, or the HTTP method when the request had no body. Null when unknown. */
    public String getOperation() {
        return theOperation;
    }

    /** The HTTP status returned to the caller, or 0 when this was not a proxied request. */
    public int getStatus() {
        return theStatus;
    }

    /**
     * The event as JSON — the payload a sink writes.
     *
     * <p>An extension of {@link McpCallRecord}'s shape rather than a second format, so a log line and
     * a queue message describe the same call the same way.
     */
    public String toJson() {
        if (theRawJson != null) {
            return theRawJson;
        }
        StringBuilder theJson = new StringBuilder("{\"id\":\"").append(escape(theId));
        theJson.append("\",\"ts\":").append(theTimestampMillis);

        // Caller-side fields, present only on an event the proxy built. Emitted before the tool so a
        // reader sees who before what, and omitted entirely rather than written null, so a generated
        // server's record is byte-identical to what it produced before these existed.
        if (theConfig != null || theUser != null) {
            theJson.append(",\"user\":");
            if (theUser == null) {
                theJson.append("null");
            } else {
                theJson.append('"').append(escape(theUser)).append('"');
            }
            // Omitted rather than written empty when there is none: an authentication event has no
            // config, and "" would read to a consumer grouping by config as a real bucket.
            if (theConfig != null) {
                theJson.append(",\"config\":\"").append(escape(theConfig)).append('"');
            }
            if (theOperation != null) {
                theJson.append(",\"op\":\"").append(escape(theOperation)).append('"');
            }
        }

        // An authentication event has no tool, no duration and no arguments, so it stops here
        // rather than padding the record with empty ones. It is still the same envelope - id, ts,
        // user, op, outcome - so one consumer reads every kind of record.
        if (theSource != null || theReason != null || theTarget != null
                || isAuthOperation(theOperation) || isAdminOperation(theOperation)) {
            theJson.append(",\"outcome\":\"").append(escape(theOutcome)).append('"');
            if (theTarget != null) {
                theJson.append(",\"target\":\"").append(escape(theTarget)).append('"');
            }
            if (theSource != null) {
                theJson.append(",\"source\":\"").append(escape(theSource)).append('"');
            }
            if (theReason != null) {
                theJson.append(",\"reason\":\"").append(escape(theReason)).append('"');
            }
            return theJson.append('}').toString();
        }

        theJson.append(",\"tool\":\"").append(escape(theToolName));
        theJson.append("\",\"outcome\":\"").append(escape(theOutcome));
        theJson.append("\",\"ms\":").append(theDurationMillis);
        if (theStatus > 0) {
            theJson.append(",\"status\":").append(theStatus);
        }

        theJson.append(",\"args\":");
        if (theValuesFlag) {
            theJson.append('{');
            boolean theFirstFlag = true;
            for (Map.Entry<String, Object> theEntry : theArguments.entrySet()) {
                if (!theFirstFlag) {
                    theJson.append(',');
                }
                theJson.append('"').append(escape(theEntry.getKey())).append("\":\"");
                theJson.append(escape(theEntry.getValue() == null ? "" : theEntry.getValue().toString()));
                theJson.append('"');
                theFirstFlag = false;
            }
            theJson.append('}');
        } else {
            theJson.append('[');
            boolean theFirstFlag = true;
            for (String theName : theArguments.keySet()) {
                if (!theFirstFlag) {
                    theJson.append(',');
                }
                theJson.append('"').append(escape(theName)).append('"');
                theFirstFlag = false;
            }
            theJson.append(']');
        }

        if (theResponse != null) {
            theJson.append(",\"response\":\"").append(escape(theResponse)).append('"');
            theJson.append(",\"responseBytes\":").append(theResponseBytes);
            theJson.append(",\"responseSha256\":\"").append(theResponseSha256).append('"');
            theJson.append(",\"truncated\":").append(theResponseTruncatedFlag);
        }
        return theJson.append('}').toString();
    }

    /** True for the operations {@link #ofAdmin} produces. */
    private static boolean isAdminOperation(String theOperationValue) {
        return OP_ACCOUNT_CREATED.equals(theOperationValue)
                || OP_ACCOUNT_DELETED.equals(theOperationValue)
                || OP_ROLE_CHANGED.equals(theOperationValue)
                || OP_PASSWORD_RESET.equals(theOperationValue)
                || OP_TOKEN_ISSUED.equals(theOperationValue)
                || OP_TOKEN_REVOKED.equals(theOperationValue)
                || OP_ACCESS_CHANGED.equals(theOperationValue)
                || OP_CONFIG_SAVED.equals(theOperationValue)
                || OP_CONFIG_DELETED.equals(theOperationValue);
    }

    /** What an administrative action acted on, or null. */
    public String getTarget() {
        return theTarget;
    }

    /** True for the three operations {@link #ofAuth} produces. */
    private static boolean isAuthOperation(String theOperationValue) {
        return OP_LOGIN.equals(theOperationValue)
                || OP_LOGIN_FAILED.equals(theOperationValue)
                || OP_LOGOUT.equals(theOperationValue);
    }

    /** Where the authentication attempt came from, or null. */
    public String getSource() {
        return theSource;
    }

    /** Why an authentication attempt was refused, from the fixed vocabulary, or null. */
    public String getReason() {
        return theReason;
    }

    private static String sha256(byte[] theBytes) {
        try {
            byte[] theDigest = MessageDigest.getInstance("SHA-256").digest(theBytes);
            StringBuilder theHex = new StringBuilder(theDigest.length * 2);
            for (int i = 0; i < theDigest.length; i++) {
                theHex.append(Character.forDigit((theDigest[i] >> 4) & 0xF, 16));
                theHex.append(Character.forDigit(theDigest[i] & 0xF, 16));
            }
            return theHex.toString();
        } catch (Exception e) {
            // SHA-256 is mandatory in every JRE; if it is somehow absent, a missing hash must not
            // cost us the rest of the record.
            return "";
        }
    }

    /** JSON string escaping — argument names and values come off the wire and can contain anything. */
    private static String escape(String theText) {
        if (theText == null) {
            return "";
        }
        StringBuilder theResult = new StringBuilder(theText.length());
        for (int i = 0; i < theText.length(); i++) {
            char theCharacter = theText.charAt(i);
            switch (theCharacter) {
                case '"':  theResult.append("\\\""); break;
                case '\\': theResult.append("\\\\"); break;
                case '\n': theResult.append("\\n");  break;
                case '\r': theResult.append("\\r");  break;
                case '\t': theResult.append("\\t");  break;
                default:
                    if (theCharacter < 0x20) {
                        theResult.append(String.format("\\u%04x", (int) theCharacter));
                    } else {
                        theResult.append(theCharacter);
                    }
                    break;
            }
        }
        return theResult.toString();
    }
}
