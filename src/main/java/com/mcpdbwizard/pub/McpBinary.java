package com.mcpdbwizard.pub;

/**
 * How a RAW crosses the Model Context Protocol when it is an INDEX-BY table element, in one place.
 *
 * <p>MCP has no binary type: a byte string crosses as base64 text, the same way a BLOB does
 * everywhere else in the generated server. An index-by table cannot hold bytes either --
 * {@link PlsqlIndexByTable2} stores every element as a String or a BigDecimal and has no third
 * representation -- so a RAW rides its VARCHAR slot as <b>hex</b>, which is what the emitted
 * anonymous block converts with {@code HEXTORAW} on the way in and {@code RAWTOHEX} on the way out.
 *
 * <p><b>Two text encodings of the same bytes, and the gap between them is the whole of this
 * class.</b> That is why RAW stayed gated for MCP after DATE and TIMESTAMP crossed: those two were
 * a MASK disagreement, fixed by moving one character, and this is an ENCODING disagreement, which
 * needs real conversion in both directions.
 *
 * <p>It lives here rather than in emitted source for the reason {@link McpDates} does: it can be
 * unit-tested here and cannot be tested there. The generator writes one call per direction.
 *
 * <h2>Base64 in, and what is tolerated</h2>
 *
 * <p>Whitespace is stripped, the URL-safe alphabet ({@code -} and {@code _}) is accepted alongside
 * the standard one, and missing {@code =} padding is supplied. None of those three is a guess: no
 * character means two different things across the two alphabets, and an unpadded string has only
 * one possible completion. Anything that is still not base64 is REFUSED with a message naming the
 * accepted form -- see {@link McpDates} on why a message that merely echoes the input is worse than
 * useless when the caller is a model, which retries blind and turns one bad argument into a
 * connection storm.
 *
 * <p><b>What CANNOT be detected, and is therefore documented instead of guessed at.</b> A caller
 * that sends hex rather than base64 is not refused: {@code DEADBEEF} is eight characters from the
 * base64 alphabet and decodes cleanly to four completely different bytes. There is no signal to
 * key on, so the tool description says base64 out loud rather than this method trying to be clever.
 * Sniffing for hex would break every value that is legitimately both.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @since 2.0.6
 */
public final class McpBinary {

    /** Uppercase, because that is what Oracle's {@code RAWTOHEX} produces and a round trip through
     *  this class should not gratuitously change the text a caller sees on the way back. */
    private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    private McpBinary() {
    }

    /**
     * A base64 tool argument to the hex an index-by table's {@code HEXTORAW} expects.
     *
     * @param theBase64 the caller's value, or null
     * @return uppercase hex, or null for a null input
     * @throws IllegalArgumentException if the value is not base64
     */
    public static String toOracleRawText(String theBase64) {
        if (theBase64 == null) {
            return null;
        }
        byte[] theBytes = decodeBase64(theBase64);
        StringBuilder theHex = new StringBuilder(theBytes.length * 2);
        for (int i = 0; i < theBytes.length; i++) {
            theHex.append(HEX_DIGITS[(theBytes[i] >> 4) & 0x0f]);
            theHex.append(HEX_DIGITS[theBytes[i] & 0x0f]);
        }
        return theHex.toString();
    }

    /**
     * The hex an index-by table's {@code RAWTOHEX} produced, back to the base64 MCP crosses.
     *
     * <p>Oracle always renders an even number of digits, so an odd length means the value did not
     * come from {@code RAWTOHEX} and is reported rather than silently truncated -- dropping the
     * last nibble would hand the caller bytes that are almost right, which is the failure mode this
     * whole area exists to avoid.
     *
     * @param theHex the value read back from the collection, or null
     * @return base64, or null for a null input
     * @throws IllegalArgumentException if the value is not an even-length run of hex digits
     */
    public static String fromOracleRawText(String theHex) {
        if (theHex == null) {
            return null;
        }
        if ((theHex.length() & 1) != 0) {
            throw new IllegalArgumentException("Cannot read \"" + theHex + "\" as an Oracle RAW:"
                    + " expected an even number of hex digits.");
        }
        byte[] theBytes = new byte[theHex.length() / 2];
        for (int i = 0; i < theBytes.length; i++) {
            int theHigh = Character.digit(theHex.charAt(i * 2), 16);
            int theLow = Character.digit(theHex.charAt((i * 2) + 1), 16);
            if (theHigh < 0 || theLow < 0) {
                throw new IllegalArgumentException("Cannot read \"" + theHex + "\" as an Oracle RAW:"
                        + " expected hex digits only.");
            }
            theBytes[i] = (byte) ((theHigh << 4) | theLow);
        }
        return java.util.Base64.getEncoder().encodeToString(theBytes);
    }

    /**
     * Decode base64, tolerating the three differences that carry no ambiguity and refusing the
     * rest with a message that says what was wanted.
     */
    private static byte[] decodeBase64(String theBase64) {
        StringBuilder theClean = new StringBuilder(theBase64.length());
        for (int i = 0; i < theBase64.length(); i++) {
            char theChar = theBase64.charAt(i);
            if (Character.isWhitespace(theChar)) {
                continue;
            }
            // The URL-safe alphabet differs from the standard one in exactly these two characters,
            // and neither appears in the other, so accepting both cannot change what a value means.
            theClean.append(theChar == '-' ? '+' : theChar == '_' ? '/' : theChar);
        }
        if (theClean.length() % 4 == 1) {
            // No amount of padding makes this a length base64 can produce.
            throw new IllegalArgumentException(explain(theBase64));
        }
        while (theClean.length() % 4 != 0) {
            theClean.append('=');
        }
        try {
            return java.util.Base64.getDecoder().decode(theClean.toString());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(explain(theBase64));
        }
    }

    /** Say what was expected, not merely what arrived. See {@link McpDates}'s counterpart. */
    private static String explain(String theText) {
        return "Cannot read \"" + theText + "\" as binary. Expected a base64-encoded value, e.g."
                + " 3q2+7w== for the four bytes DE AD BE EF. Hex is NOT accepted: send base64.";
    }
}
