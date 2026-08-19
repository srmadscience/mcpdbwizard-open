package com.mcpdbwizard.pub;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts one spooled audit record, so a write-ahead spool is not plaintext on disk.
 *
 * <h2>What this protects, and what it does not</h2>
 *
 * <p>It protects the <b>file</b>, not the <b>process</b>. Anyone who can read this JVM's environment
 * or its memory has the key, so this is not a defence against root on the host. What it does defend
 * is everything that outlives the process and travels: disk images, volume snapshots, backups, and
 * other users on a shared box.
 *
 * <p><b>Encrypting the volume is usually the better answer</b> and costs no code — it also covers the
 * configs, the accounts and the runtime workspaces sitting in the same directory, which this does
 * not. Reach for this when the storage layer is not yours to configure, or when the records must be
 * unreadable to someone who legitimately administers the host.
 *
 * <h2>Format</h2>
 *
 * <p><code>ENC1:base64(iv‖ciphertext‖tag)</code> — AES-256-GCM, a fresh 12-byte IV per record, one
 * line per record so the spool stays line-oriented and its size accounting still works. <b>The marker
 * is what makes the feature safe to turn on</b>: a spool written before encryption was enabled still
 * holds plaintext lines, and the drainer has to read both rather than strand whatever was queued.
 *
 * <p>The key is derived by SHA-256 over the supplied secret, so any passphrase length works and the
 * cipher always gets 256 bits. That is deliberately not a password-hardening KDF — this secret comes
 * from a deployment's secret store, not from a human's memory, and pretending otherwise by adding
 * iterations would suggest a resistance to guessing that a 12-character passphrase would not have.
 *
 * <h2>Losing the key</h2>
 *
 * <p>There is no recovery. A segment written under a key you no longer have cannot be delivered and
 * cannot be read; {@link SpoolingAuditSink} quarantines it rather than retrying for ever or deleting
 * it. Rotating the key therefore means draining the spool first.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class SpoolCipher {

    /** The secret. Read through {@link EnvironmentSecret}, so {@code _FILE} works too. */
    public static final String KEY_VARIABLE = "MCP_AUDIT_SPOOL_KEY";

    /** Marks a line as encrypted, so a spool holding both forms can still be drained. */
    public static final String MARKER = "ENC1:";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec theKey;
    private final SecureRandom theRandom = new SecureRandom();

    SpoolCipher(byte[] theKeyBytes) {
        this.theKey = new SecretKeySpec(theKeyBytes, "AES");
    }

    /**
     * The cipher this deployment is configured for, or null when the spool should stay plaintext.
     *
     * @throws IllegalStateException if a key is named but unusable — a spool must not silently fall
     *                               back to plaintext when encryption was asked for, because the
     *                               operator would believe the records were protected
     */
    public static SpoolCipher fromEnvironment() {
        String theSecret;
        try {
            theSecret = EnvironmentSecret.read(KEY_VARIABLE);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read " + KEY_VARIABLE + ": " + e.getMessage(), e);
        }
        if (theSecret == null || theSecret.trim().length() == 0) {
            return null;
        }
        return new SpoolCipher(keyFrom(theSecret));
    }

    /** SHA-256 of the secret; see the class note on why this is not a password KDF. */
    static byte[] keyFrom(String theSecret) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(theSecret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 is unavailable, so the audit spool cannot be"
                    + " encrypted: " + e, e);
        }
    }

    /** One line, encrypted and marked. */
    public String encrypt(String thePlainText) {
        try {
            byte[] theIv = new byte[IV_BYTES];
            theRandom.nextBytes(theIv);
            Cipher theCipher = Cipher.getInstance(TRANSFORMATION);
            theCipher.init(Cipher.ENCRYPT_MODE, theKey, new GCMParameterSpec(TAG_BITS, theIv));
            byte[] theCipherText = theCipher.doFinal(thePlainText.getBytes(StandardCharsets.UTF_8));

            byte[] theCombined = new byte[theIv.length + theCipherText.length];
            System.arraycopy(theIv, 0, theCombined, 0, theIv.length);
            System.arraycopy(theCipherText, 0, theCombined, theIv.length, theCipherText.length);
            return MARKER + Base64.getEncoder().encodeToString(theCombined);
        } catch (Exception e) {
            // Never returns the plaintext on failure: a spool that silently writes readable records
            // when encryption was configured is the failure this whole class exists to prevent.
            throw new IllegalStateException("Could not encrypt an audit record: " + e, e);
        }
    }

    /**
     * One line, decrypted if it is marked and returned unchanged if it is not.
     *
     * <p>Passing plaintext through is what lets encryption be switched on over an existing spool.
     * A line that IS marked and cannot be decrypted throws — it is not delivered as gibberish, and
     * not silently skipped.
     */
    public String decrypt(String theLine) {
        if (theLine == null || !theLine.startsWith(MARKER)) {
            return theLine;
        }
        try {
            byte[] theCombined = Base64.getDecoder().decode(theLine.substring(MARKER.length()));
            byte[] theIv = new byte[IV_BYTES];
            System.arraycopy(theCombined, 0, theIv, 0, IV_BYTES);
            Cipher theCipher = Cipher.getInstance(TRANSFORMATION);
            theCipher.init(Cipher.DECRYPT_MODE, theKey, new GCMParameterSpec(TAG_BITS, theIv));
            byte[] thePlain = theCipher.doFinal(theCombined, IV_BYTES, theCombined.length - IV_BYTES);
            return new String(thePlain, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not decrypt a spooled audit record - wrong "
                    + KEY_VARIABLE + ", or the file is damaged: " + e, e);
        }
    }

    /** True when this line was written encrypted, whoever holds the key. */
    public static boolean isEncrypted(String theLine) {
        return theLine != null && theLine.startsWith(MARKER);
    }
}
