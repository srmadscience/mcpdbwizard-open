package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Cover for spool encryption. The interesting cases are all about what happens when it does NOT
 * work: a wrong key, a damaged file, and a spool holding both forms at once.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class SpoolCipherTest {

    private static SpoolCipher cipher(String theSecret) {
        return new SpoolCipher(SpoolCipher.keyFrom(theSecret));
    }

    private static final String RECORD =
            "{\"id\":\"abc\",\"tool\":\"greet\",\"args\":{\"p_name\":\"Alice\"}}";

    @Test
    void aRecordRoundTrips() {
        SpoolCipher theCipher = cipher("a-deployment-secret");
        assertEquals(RECORD, theCipher.decrypt(theCipher.encrypt(RECORD)));
    }

    /** The point of the exercise: the payload must not be readable in the written line. */
    @Test
    void theWrittenLineDoesNotContainThePlaintext() {
        String theLine = cipher("s").encrypt(RECORD);
        assertFalse(theLine.contains("Alice"), theLine);
        assertFalse(theLine.contains("greet"), theLine);
        assertFalse(theLine.contains("p_name"), theLine);
        assertTrue(theLine.startsWith(SpoolCipher.MARKER), theLine);
    }

    /** GCM with a fresh IV: the same record twice must not produce the same ciphertext. */
    @Test
    void theSameRecordEncryptsDifferentlyEachTime() {
        SpoolCipher theCipher = cipher("s");
        assertNotEquals(theCipher.encrypt(RECORD), theCipher.encrypt(RECORD),
                "a repeated IV would leak that two records are identical");
    }

    /**
     * The case that makes the feature safe to switch on. A spool written before encryption was
     * enabled still holds plaintext, and those lines must still be delivered rather than stranded.
     */
    @Test
    void anUnmarkedLinePassesThroughUnchanged() {
        assertEquals(RECORD, cipher("s").decrypt(RECORD));
        assertFalse(SpoolCipher.isEncrypted(RECORD));
        assertTrue(SpoolCipher.isEncrypted(cipher("s").encrypt(RECORD)));
    }

    /** A wrong key must fail loudly, not return rubbish that would be delivered as a record. */
    @Test
    void theWrongKeyThrowsRatherThanProducingGarbage() {
        String theLine = cipher("the-right-key").encrypt(RECORD);
        IllegalStateException theFailure = assertThrows(IllegalStateException.class,
                () -> cipher("the-wrong-key").decrypt(theLine));
        assertTrue(theFailure.getMessage().contains(SpoolCipher.KEY_VARIABLE),
                "the message must name the variable to check: " + theFailure.getMessage());
    }

    /** GCM authenticates: a tampered or truncated line is refused, not silently accepted. */
    @Test
    void aDamagedLineIsRefused() {
        SpoolCipher theCipher = cipher("s");
        String theLine = theCipher.encrypt(RECORD);
        String theTampered = theLine.substring(0, theLine.length() - 6) + "AAAAAA";
        assertThrows(IllegalStateException.class, () -> theCipher.decrypt(theTampered));
        assertThrows(IllegalStateException.class,
                () -> theCipher.decrypt(SpoolCipher.MARKER + "not-base64!!"));
    }

    @Test
    void anEmptyRecordAndNullsAreHandled() {
        SpoolCipher theCipher = cipher("s");
        assertEquals("", theCipher.decrypt(theCipher.encrypt("")));
        assertNull(theCipher.decrypt(null));
        assertFalse(SpoolCipher.isEncrypted(null));
    }

    /** Any passphrase length works, because the key is derived rather than used raw. */
    @Test
    void anySecretLengthProducesA256BitKey() {
        assertEquals(32, SpoolCipher.keyFrom("x").length);
        assertEquals(32, SpoolCipher.keyFrom("a much longer passphrase than strictly needed").length);
        assertNotEquals(SpoolCipher.keyFrom("a")[0] + "," + SpoolCipher.keyFrom("a")[1],
                SpoolCipher.keyFrom("b")[0] + "," + SpoolCipher.keyFrom("b")[1]);
    }

    /** Unicode must survive: an argument value can be anything a model produced. */
    @Test
    void nonAsciiSurvivesTheRoundTrip() {
        String theRecord = "{\"args\":{\"name\":\"Ünïcødé ✓ 日本語\"}}";
        SpoolCipher theCipher = cipher("s");
        assertEquals(theRecord, theCipher.decrypt(theCipher.encrypt(theRecord)));
    }

    /** No key configured means no cipher, which is how the spool stays plaintext by default. */
    @Test
    void noKeyMeansNoCipher() {
        // MCP_AUDIT_SPOOL_KEY is not set in the test environment.
        assertNull(SpoolCipher.fromEnvironment(),
                "an unset key must yield null rather than a cipher with an empty key");
    }
}
