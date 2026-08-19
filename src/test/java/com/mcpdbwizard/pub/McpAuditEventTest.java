package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the audit record.
 *
 * <p>Two properties carry the weight. At the default level no production data may appear, because
 * switching auditing on must not start exporting personal data. And a truncated payload must remain
 * verifiable — the full size and a hash of the whole thing survive the cut, so a clipped record is
 * still evidence rather than an anecdote.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpAuditEventTest {

    private static Map<String, Object> args(String... theNamesAndValues) {
        Map<String, Object> theArguments = new LinkedHashMap<String, Object>();
        for (int i = 0; i < theNamesAndValues.length; i += 2) {
            theArguments.put(theNamesAndValues[i], theNamesAndValues[i + 1]);
        }
        return theArguments;
    }

    @Test
    void theDefaultLevelRecordsNoProductionData() {
        McpAuditEvent theEvent = McpAuditEvent.of("get_customer",
                args("email", "ada@example.com"), "ok", 12,
                "{\"name\":\"Ada Lovelace\"}", McpAuditSinks.Level.NAMES, 8192);

        String theJson = theEvent.toJson();
        assertTrue(theJson.contains("\"email\""), theJson);
        assertFalse(theJson.contains("ada@example.com"), "argument values must not appear: " + theJson);
        assertFalse(theJson.contains("Ada Lovelace"), "the response must not appear: " + theJson);
    }

    @Test
    void theValuesLevelRecordsBothSides() {
        McpAuditEvent theEvent = McpAuditEvent.of("get_customer",
                args("id", "4471"), "ok", 12, "{\"name\":\"Ada\"}",
                McpAuditSinks.Level.VALUES, 8192);

        String theJson = theEvent.toJson();
        assertTrue(theJson.contains("\"id\":\"4471\""), theJson);
        assertTrue(theJson.contains("Ada"), theJson);
        assertTrue(theJson.contains("\"truncated\":false"), theJson);
    }

    @Test
    void aTruncatedResponseKeepsItsFullSizeAndHash() {
        StringBuilder theBig = new StringBuilder();
        for (int i = 0; i < 500; i++) {
            theBig.append("0123456789");
        }
        McpAuditEvent theEvent = McpAuditEvent.of("t", args(), "ok", 1,
                theBig.toString(), McpAuditSinks.Level.VALUES, 100);

        String theJson = theEvent.toJson();
        assertTrue(theEvent.isResponseTruncated());
        assertEquals(100, theEvent.getResponse().length());
        assertTrue(theJson.contains("\"responseBytes\":5000"), "the full size must survive: " + theJson);
        assertTrue(theJson.contains("\"truncated\":true"), theJson);
        // 64 hex characters of SHA-256 over the WHOLE payload, not the clipped one.
        assertTrue(theJson.matches(".*\"responseSha256\":\"[0-9a-f]{64}\".*"), theJson);
    }

    @Test
    void theHashCoversTheWholePayloadNotTheTruncation() {
        String theWhole = "abcdefghij" + "X".repeat(200);
        McpAuditEvent theClipped = McpAuditEvent.of("t", args(), "ok", 1, theWhole,
                McpAuditSinks.Level.VALUES, 10);
        McpAuditEvent theFull = McpAuditEvent.of("t", args(), "ok", 1, theWhole,
                McpAuditSinks.Level.VALUES, 0);

        String theClippedHash = theClipped.toJson().replaceAll(".*\"responseSha256\":\"([0-9a-f]+)\".*", "$1");
        String theFullHash = theFull.toJson().replaceAll(".*\"responseSha256\":\"([0-9a-f]+)\".*", "$1");
        assertEquals(theFullHash, theClippedHash,
                "a clipped record must hash the same payload as an unclipped one, or it proves nothing");
    }

    @Test
    void aCraftedArgumentNameCannotForgeStructure() {
        String theJson = McpAuditEvent.of("t", args("a\",\"b\nfake", "v"), "ok", 1, null,
                McpAuditSinks.Level.NAMES, 8192).toJson();

        assertEquals(1, theJson.split("\n", -1).length, "the record must stay a single line");
        assertTrue(theJson.contains("\\n"));
    }

    @Test
    void levelAndCapAreParsedStrictly() {
        assertEquals(McpAuditSinks.Level.NAMES, McpAuditSinks.level(null));
        assertEquals(McpAuditSinks.Level.VALUES, McpAuditSinks.level("values"));
        assertEquals(McpAuditSinks.Level.VALUES, McpAuditSinks.level("VALUES"));
        assertThrows(IllegalArgumentException.class, () -> McpAuditSinks.level("everything"));

        assertEquals(McpAuditSinks.DEFAULT_MAX_BYTES, McpAuditSinks.maxBytes(null));
        assertEquals(0, McpAuditSinks.maxBytes("0"));
        assertThrows(IllegalArgumentException.class, () -> McpAuditSinks.maxBytes("lots"));
    }

    @Test
    void theNoOpSinkAcceptsAnythingIncludingNothing() {
        McpAuditSink theSink = McpAuditSinks.noOp();
        theSink.record(McpAuditEvent.of("t", null, "ok", 1, null, McpAuditSinks.Level.NAMES, 0));
        theSink.close();
    }

    // ---- the caller-side event (docs/logging-plan.md 2.1) ----

    @Test
    void anAccessEventCarriesTheCallerTheConfigAndTheStatus() {
        String theJson = McpAuditEvent.ofAccess("sam", "payroll", "tools/call", "greet",
                "ok", 200, 41).toJson();

        assertTrue(theJson.contains("\"user\":\"sam\""), theJson);
        assertTrue(theJson.contains("\"config\":\"payroll\""), theJson);
        assertTrue(theJson.contains("\"op\":\"tools/call\""), theJson);
        assertTrue(theJson.contains("\"tool\":\"greet\""), theJson);
        assertTrue(theJson.contains("\"status\":200"), theJson);
    }

    @Test
    void anUnauthenticatedAccessEventRecordsAnExplicitNullUser() {
        // "nobody was authenticated" has to be distinguishable from "this build did not record it".
        String theJson = McpAuditEvent.ofAccess(null, "payroll", "POST", null,
                "bad-token", 401, 0).toJson();

        assertTrue(theJson.contains("\"user\":null"), theJson);
        assertTrue(theJson.contains("\"config\":\"payroll\""), theJson);
    }

    @Test
    void aGeneratedServersEventIsUnchangedByTheNewFields() {
        // The four fields are additive. A server-side record must serialise exactly as it did before
        // they existed, or every existing audit consumer has to be updated for a feature that does
        // not apply to it.
        String theJson = McpAuditEvent.of("greet", null, "ok", 12, null,
                McpAuditSinks.Level.NAMES, 0).toJson();

        assertFalse(theJson.contains("user"), theJson);
        assertFalse(theJson.contains("config"), theJson);
        assertFalse(theJson.contains("\"op\""), theJson);
        assertFalse(theJson.contains("status"), theJson);
    }

    @Test
    void anAccessEventStillGetsAUniqueIdForDeduplication() {
        // At-least-once delivery applies to these records too, so they need the same identity the
        // server-side ones have.
        McpAuditEvent theFirst = McpAuditEvent.ofAccess("sam", "p", "POST", null, "ok", 200, 1);
        McpAuditEvent theSecond = McpAuditEvent.ofAccess("sam", "p", "POST", null, "ok", 200, 1);

        assertNotNull(theFirst.getId());
        assertNotEquals(theFirst.getId(), theSecond.getId());
    }

    @Test
    void aCraftedUsernameCannotForgeASecondRecord() {
        String theEvil = "x\",\"outcome\":\"ok";

        String theJson = McpAuditEvent.ofAccess(theEvil, "payroll", "POST", null,
                "forbidden", 403, 1).toJson();

        assertTrue(theJson.contains("\"outcome\":\"forbidden\""), theJson);
        assertEquals(1, theJson.split("\n", -1).length);
    }

    // ---- authentication records -------------------------------------------------------------

    @Test
    void aSuccessfulLoginRecordsWhoAndFromWhere() {
        String theJson = McpAuditEvent.ofAuth("admin", McpAuditEvent.OP_LOGIN, "ok",
                "192.0.2.7", null).toJson();
        assertTrue(theJson.contains("\"user\":\"admin\""), theJson);
        assertTrue(theJson.contains("\"op\":\"login\""), theJson);
        assertTrue(theJson.contains("\"outcome\":\"ok\""), theJson);
        assertTrue(theJson.contains("\"source\":\"192.0.2.7\""), theJson);
        // No tool ran and no config was involved, so the record does not pretend either existed.
        assertFalse(theJson.contains("\"tool\""), theJson);
        assertFalse(theJson.contains("\"config\""), theJson);
        assertFalse(theJson.contains("\"args\""), theJson);
    }

    @Test
    void aRefusedLoginCarriesTheReason() {
        String theJson = McpAuditEvent.ofAuth("root", McpAuditEvent.OP_LOGIN_FAILED,
                McpAuditEvent.OUTCOME_DENIED, "203.0.113.9", "bad-credentials").toJson();
        assertTrue(theJson.contains("\"op\":\"login-failed\""), theJson);
        assertTrue(theJson.contains("\"outcome\":\"denied\""), theJson);
        assertTrue(theJson.contains("\"reason\":\"bad-credentials\""), theJson);
        // Recorded as submitted: an attacker picks this, so it is evidence, not an identity.
        assertTrue(theJson.contains("\"user\":\"root\""), theJson);
    }

    /**
     * What this class can actually guarantee: the reason it is given is what appears, verbatim and
     * nowhere else. It cannot stop a caller passing an exception message — only the caller can, and
     * {@code AuthenticationAuditListenerTest} is where that is asserted, because the failure event
     * Spring hands over can still hold the password that was tried.
     */
    @Test
    void theReasonAppearsVerbatimAndTheAttemptDoesNot() {
        String theJson = McpAuditEvent.ofAuth("admin", McpAuditEvent.OP_LOGIN_FAILED,
                McpAuditEvent.OUTCOME_DENIED, "10.0.0.1", "bad-credentials").toJson();
        assertTrue(theJson.contains("\"reason\":\"bad-credentials\""), theJson);
        // The submitted password is never an input to this factory, so it cannot appear.
        assertFalse(theJson.contains("hunter2"), theJson);
    }

    @Test
    void aLogoutIsRecorded() {
        String theJson = McpAuditEvent.ofAuth("admin", McpAuditEvent.OP_LOGOUT, "ok", null, null)
                .toJson();
        assertTrue(theJson.contains("\"op\":\"logout\""), theJson);
        // Source is genuinely unknown here rather than empty, so it is omitted.
        assertFalse(theJson.contains("\"source\""), theJson);
        assertFalse(theJson.contains("\"reason\""), theJson);
    }

    /** Every record, whatever its kind, must be de-duplicable: spooled delivery is at-least-once. */
    @Test
    void authRecordsCarryAnIdAndTimestampLikeEveryOther() {
        McpAuditEvent theEvent = McpAuditEvent.ofAuth("a", McpAuditEvent.OP_LOGIN, "ok", null, null);
        assertNotNull(theEvent.getId());
        assertTrue(theEvent.getTimestampMillis() > 0L);
        assertNotEquals(theEvent.getId(),
                McpAuditEvent.ofAuth("a", McpAuditEvent.OP_LOGIN, "ok", null, null).getId());
    }

    // ---- administrative records ------------------------------------------------------------

    /**
     * These matter at least as much as the sign-ins beside them: creating an account or changing
     * the access matrix alters WHO CAN REACH WHAT, and saving a config alters WHAT IS REACHABLE AT
     * ALL, because an object a config does not select has no code generated for it.
     */
    @Test
    void anAdminRecordNamesTheActorAndWhatWasTouched() {
        String theJson = McpAuditEvent.ofAdmin("admin", McpAuditEvent.OP_ACCOUNT_CREATED,
                "newuser", "ok", "10.0.0.5").toJson();
        assertTrue(theJson.contains("\"user\":\"admin\""), theJson);
        assertTrue(theJson.contains("\"op\":\"account-created\""), theJson);
        assertTrue(theJson.contains("\"target\":\"newuser\""), theJson);
        assertTrue(theJson.contains("\"source\":\"10.0.0.5\""), theJson);
        assertFalse(theJson.contains("\"tool\""), theJson);
    }

    /**
     * ACTOR AND TARGET ARE DIFFERENT FIELDS, and conflating them would make the record useless: a
     * trail saying only "admin was busy" cannot answer which account was promoted.
     */
    @Test
    void theActorAndTheTargetAreDistinct() {
        McpAuditEvent theEvent = McpAuditEvent.ofAdmin("alice", McpAuditEvent.OP_ROLE_CHANGED,
                "bob", "ok", null);
        assertEquals("alice", theEvent.getUser());
        assertEquals("bob", theEvent.getTarget());
    }

    /**
     * A REFUSED action is recorded too. Repeated failures to create or promote an account are the
     * trace of somebody probing what they are allowed to do, and a trail holding only successes
     * cannot show it.
     */
    @Test
    void arefusedAdminActionIsRecordedAsDenied() {
        String theJson = McpAuditEvent.ofAdmin("mallory", McpAuditEvent.OP_ROLE_CHANGED,
                "admin", McpAuditEvent.OUTCOME_DENIED, "203.0.113.9").toJson();
        assertTrue(theJson.contains("\"outcome\":\"denied\""), theJson);
        assertTrue(theJson.contains("\"target\":\"admin\""), theJson);
    }

    /** An action with no single target - the whole access grid - omits the field rather than faking one. */
    @Test
    void anActionWithNoSingleTargetOmitsIt() {
        String theJson = McpAuditEvent.ofAdmin("admin", McpAuditEvent.OP_ACCESS_CHANGED,
                null, "ok", null).toJson();
        assertTrue(theJson.contains("\"op\":\"access-changed\""), theJson);
        assertFalse(theJson.contains("\"target\""), theJson);
    }

    @Test
    void configChangesAreRecorded() {
        assertTrue(McpAuditEvent.ofAdmin("admin", McpAuditEvent.OP_CONFIG_SAVED, "com.example.pay",
                "ok", null).toJson().contains("\"op\":\"config-saved\""));
        assertTrue(McpAuditEvent.ofAdmin("admin", McpAuditEvent.OP_CONFIG_DELETED, "com.example.pay",
                "ok", null).toJson().contains("\"op\":\"config-deleted\""));
    }

    /** Every record must be de-duplicable, whatever its kind: spooled delivery is at-least-once. */
    @Test
    void adminRecordsCarryAnIdAndTimestamp() {
        McpAuditEvent theEvent = McpAuditEvent.ofAdmin("a", McpAuditEvent.OP_TOKEN_ISSUED, "b", "ok", null);
        assertNotNull(theEvent.getId());
        assertTrue(theEvent.getTimestampMillis() > 0L);
    }
}
