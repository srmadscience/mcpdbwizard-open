package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The S3 sink, against a stub client.
 *
 * <p>A stub rather than MinIO or LocalStack: what is worth pinning here is the batching, the key
 * layout and the honesty of {@code flush()}, none of which needs a real object store. The parts
 * that would need one — credentials and signing — are exactly the parts delegated to the SDK
 * rather than written here.
 *
 * <p>{@link #twoObjectsRolledInTheSameMillisecondDoNotCollide()} is the one that matters most and
 * is least obvious: S3 has no "create if absent", so a duplicate key silently OVERWRITES, and the
 * records it destroyed would never be missed by anybody.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class S3AuditSinkTest {

    /** Remembers what was PUT, and can be told to refuse. */
    private static final class StubS3 implements S3Client {
        final List<String> keys = Collections.synchronizedList(new ArrayList<String>());
        final List<String> bodies = Collections.synchronizedList(new ArrayList<String>());
        volatile boolean healthy = true;
        volatile boolean closed = false;

        @Override
        public PutObjectResponse putObject(PutObjectRequest theRequest, RequestBody theBody) {
            if (!healthy) {
                throw S3Exception.builder().message("AccessDenied").build();
            }
            keys.add(theRequest.key());
            try (InputStream theStream = theBody.contentStreamProvider().newStream()) {
                bodies.add(new String(theStream.readAllBytes(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            return PutObjectResponse.builder().build();
        }

        @Override
        public String serviceName() {
            return "s3";
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static McpAuditEvent event(String theTool) {
        return McpAuditEvent.of(theTool, null, "ok", 2L, null, McpAuditSinks.Level.NAMES, 0);
    }

    /** Rolls on the second record, and never on time. */
    private static S3AuditSink sink(StubS3 theClient, long theRollBytes) {
        return new S3AuditSink(theClient, "audit-bucket", "mcp-audit", "orindademo",
                theRollBytes, 3600L);
    }

    @Test
    void recordsAccumulateAndBecomeOneObject() {
        StubS3 theClient = new StubS3();
        S3AuditSink theSink = sink(theClient, 10L * 1024L);

        theSink.record(event("a"));
        theSink.record(event("b"));
        assertEquals(0, theClient.keys.size(), "a PUT per tool call would be slow and expensive");

        assertTrue(theSink.flush());

        assertEquals(1, theClient.keys.size());
        String theBody = theClient.bodies.get(0);
        assertEquals(2, theBody.split("\n").length, theBody);
        assertTrue(theBody.contains("\"tool\":\"a\""), theBody);
        assertTrue(theBody.contains("\"tool\":\"b\""), theBody);
    }

    @Test
    void anObjectRollsOnceItIsBigEnough() {
        StubS3 theClient = new StubS3();
        // Small enough that one record fills it.
        S3AuditSink theSink = sink(theClient, 10L);

        theSink.record(event("big_enough"));

        assertEquals(1, theClient.keys.size(), "the size trigger must fire inside record()");
    }

    @Test
    void aQuietServerStillRollsOnTime() {
        // Without this a server with occasional traffic holds its last records in memory
        // indefinitely and loses them when it stops.
        StubS3 theClient = new StubS3();
        S3AuditSink theSink = new S3AuditSink(theClient, "audit-bucket", "mcp-audit", "cfg",
                10L * 1024L, 0L);

        theSink.record(event("waiting"));
        assertEquals(0, theClient.keys.size());

        theSink.rollIfDue();

        assertEquals(1, theClient.keys.size(), "the timer must roll a part-full batch");
    }

    @Test
    void keysArePartitionedByDateSoALifecycleRuleCanExpireThem() {
        StubS3 theClient = new StubS3();
        S3AuditSink theSink = sink(theClient, 1L);
        theSink.record(event("a"));

        String theKey = theClient.keys.get(0);
        assertTrue(theKey.startsWith("mcp-audit/orindademo/"), theKey);
        assertTrue(theKey.endsWith(".jsonl"), theKey);
        // prefix/config/yyyy/MM/dd/<millis>-<uuid>.jsonl
        assertEquals(6, theKey.split("/").length, theKey);
        assertTrue(theKey.matches(".*/\\d{4}/\\d{2}/\\d{2}/\\d+-[0-9a-f-]+\\.jsonl"), theKey);
    }

    @Test
    void twoObjectsRolledInTheSameMillisecondDoNotCollide() {
        // S3 has no create-if-absent: a duplicate key OVERWRITES, and the records it destroyed
        // would never be missed. The uuid is what stops two servers on one config doing that.
        S3AuditSink theSink = sink(new StubS3(), 1L);
        assertNotEquals(theSink.keyFor(1_700_000_000_000L), theSink.keyFor(1_700_000_000_000L));
    }

    @Test
    void aRefusedPutIsCountedAsLostAndNotConfirmed() {
        StubS3 theClient = new StubS3();
        S3AuditSink theSink = sink(theClient, 10L * 1024L);
        theClient.healthy = false;

        theSink.record(event("denied"));
        assertFalse(theSink.flush(), "a spool must not be told these records arrived");
        assertEquals(1L, theSink.getDroppedCount());
        assertEquals(0L, theSink.getDeliveredCount());
    }

    @Test
    void aFailureInsideRecordIsStillReportedByTheNextFlush() {
        // The batch fills and is PUT inside record(), so a flush sampling its counter at the top
        // would not see the rejection that had already happened.
        StubS3 theClient = new StubS3();
        S3AuditSink theSink = sink(theClient, 10L);
        theClient.healthy = false;

        theSink.record(event("fills_and_fails"));

        assertFalse(theSink.flush(), "the earlier rejection must surface here");
        theClient.healthy = true;
        theSink.record(event("fine"));
        assertTrue(theSink.flush());
    }

    @Test
    void closeRollsWhatIsLeftAndReleasesTheClient() {
        StubS3 theClient = new StubS3();
        S3AuditSink theSink = sink(theClient, 10L * 1024L);
        theSink.record(event("last"));

        theSink.close();

        assertEquals(1, theClient.keys.size(), "shutdown must not discard a part-full batch");
        assertTrue(theClient.closed);
    }

    // ---- settings ----

    @Test
    void aPrefixIsNormalisedSoTheBucketHasNoUnnamedFolders() {
        assertEquals("mcp-audit", S3AuditSink.prefix(null));
        assertEquals("mcp-audit", S3AuditSink.prefix("   "));
        assertEquals("audit", S3AuditSink.prefix("audit/"));
        // A leading slash is legal in an S3 key and renders as a folder with no name.
        assertEquals("audit", S3AuditSink.prefix("/audit"));
        assertEquals("mcp-audit", S3AuditSink.prefix("/"));
    }

    @Test
    void aConfigNameIsMadeSafeForAKey() {
        assertEquals("my_config", S3AuditSink.sanitise("my config"));
        assertEquals("a_b", S3AuditSink.sanitise("a/b"));
        assertEquals("unknown", S3AuditSink.sanitise("   "));
    }
}
