package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a RAW index-by element has to survive on its way across MCP and back.
 *
 * <p>The round-trip cases matter more than the individual directions: an encoding bug that is
 * symmetric — the same wrong nibble order both ways — passes a one-direction test and corrupts
 * every value in production. {@link #roundTripsEveryByteValue()} is the one that catches it, and it
 * is the reason this file exists rather than two assertions next to the generator.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class McpBinaryTest {

    /** DE AD BE EF, the four bytes the error message itself offers as an example. */
    private static final String DEADBEEF_BASE64 = "3q2+7w==";
    private static final String DEADBEEF_HEX = "DEADBEEF";

    @Test
    void convertsBase64ToTheHexOracleExpects() {
        assertEquals(DEADBEEF_HEX, McpBinary.toOracleRawText(DEADBEEF_BASE64));
    }

    @Test
    void convertsOracleHexBackToBase64() {
        assertEquals(DEADBEEF_BASE64, McpBinary.fromOracleRawText(DEADBEEF_HEX));
    }

    /**
     * Every one of the 256 byte values, in both directions.
     *
     * <p>A high/low nibble swap, a sign-extension slip on the bytes above 0x7F, or an alphabet that
     * differs at one character all survive a hand-picked example and die here.
     */
    @Test
    void roundTripsEveryByteValue() {
        byte[] theBytes = new byte[256];
        for (int i = 0; i < 256; i++) {
            theBytes[i] = (byte) i;
        }
        String theBase64 = java.util.Base64.getEncoder().encodeToString(theBytes);
        String theHex = McpBinary.toOracleRawText(theBase64);

        assertEquals(512, theHex.length());
        // Byte 0xFF is the one a sign-extended >> produces "FFFFFFFF" for.
        assertTrue(theHex.endsWith("FEFF"), "expected the last two bytes as FEFF, got a tail of "
                + theHex.substring(theHex.length() - 8));
        assertEquals(theBase64, McpBinary.fromOracleRawText(theHex));
    }

    /** Oracle's RAWTOHEX is uppercase; a value that came back lowercase must still read. */
    @Test
    void readsLowercaseHex() {
        assertEquals(DEADBEEF_BASE64, McpBinary.fromOracleRawText("deadbeef"));
    }

    /** ...but this class emits uppercase, so a round trip does not change the caller's text. */
    @Test
    void emitsUppercaseHex() {
        assertEquals(DEADBEEF_HEX, McpBinary.toOracleRawText(DEADBEEF_BASE64));
    }

    @Test
    void passesNullThrough() {
        assertNull(McpBinary.toOracleRawText(null));
        assertNull(McpBinary.fromOracleRawText(null));
    }

    /**
     * An empty value is not an error in either direction.
     *
     * <p>Oracle has no zero-length RAW — an empty one IS null — so this never arrives from the
     * database. It arrives from a caller sending an empty array element, and refusing it there
     * would be this layer inventing a rule Oracle does not have.
     */
    @Test
    void handlesEmptyValues() {
        assertEquals("", McpBinary.toOracleRawText(""));
        assertEquals("", McpBinary.fromOracleRawText(""));
    }

    /** A model that wraps its base64 output, or pads it with spaces, is still understood. */
    @Test
    void ignoresWhitespace() {
        assertEquals(DEADBEEF_HEX, McpBinary.toOracleRawText("3q2+\n7w==  "));
    }

    /** The URL-safe alphabet: '-' and '_' where the standard one has '+' and '/'. */
    @Test
    void acceptsTheUrlSafeAlphabet() {
        // 0xFB 0xFF 0xBF encodes as "+/+/" in standard base64 and "-_-_" in URL-safe.
        byte[] theBytes = {(byte) 0xfb, (byte) 0xff, (byte) 0xbf};
        String theStandard = java.util.Base64.getEncoder().encodeToString(theBytes);
        String theUrlSafe = java.util.Base64.getUrlEncoder().encodeToString(theBytes);
        assertEquals(McpBinary.toOracleRawText(theStandard), McpBinary.toOracleRawText(theUrlSafe));
        assertEquals("FBFFBF", McpBinary.toOracleRawText(theUrlSafe));
    }

    /** Unpadded base64 has exactly one possible completion, so supplying it is not a guess. */
    @Test
    void suppliesMissingPadding() {
        assertEquals(DEADBEEF_HEX, McpBinary.toOracleRawText("3q2+7w"));
        assertEquals("DEADBE", McpBinary.toOracleRawText("3q2+"));
    }

    /**
     * A length of 4n+1 cannot be produced by base64 at any padding, so it is refused rather than
     * completed. Left alone it would reach Base64.decode and come back as its own message.
     */
    @Test
    void refusesAnImpossibleLength() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> McpBinary.toOracleRawText("3q2+7"));
        assertTrue(e.getMessage().contains("base64"), e.getMessage());
    }

    /**
     * The message names the accepted form, and says hex is not it.
     *
     * <p>That second clause is the one worth pinning: hex is the single most likely wrong guess
     * here, because the parameter is a RAW and every other tool in the estate talks about it in
     * hex. A caller told only "invalid" retries with the same idea.
     */
    @Test
    void explainsWhatWasWanted() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> McpBinary.toOracleRawText("not base64 at all!"));
        assertTrue(e.getMessage().contains("base64"), e.getMessage());
        assertTrue(e.getMessage().contains("Hex is NOT accepted"), e.getMessage());
    }

    /**
     * <b>The case that CANNOT be caught, pinned so nobody later thinks it was an oversight.</b>
     *
     * <p>{@code DEADBEEF} is eight characters of the base64 alphabet. A caller who sends hex gets a
     * clean decode into four entirely different bytes, and there is no signal to key on — which is
     * why the tool description says base64 out loud instead.
     */
    @Test
    void cannotTellHexFromBase64() {
        assertEquals("0C4003044105", McpBinary.toOracleRawText(DEADBEEF_HEX));
    }

    @Test
    void refusesOddLengthHex() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> McpBinary.fromOracleRawText("DEADBEE"));
        assertTrue(e.getMessage().contains("even number"), e.getMessage());
    }

    @Test
    void refusesNonHexDigits() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> McpBinary.fromOracleRawText("DEADBEEZ"));
        assertTrue(e.getMessage().contains("hex digits"), e.getMessage());
    }
}
