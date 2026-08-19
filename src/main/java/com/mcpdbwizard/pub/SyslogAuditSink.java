package com.mcpdbwizard.pub;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sends audit records to a syslog collector as RFC 5424 messages.
 *
 * <p>Select it with {@code MCP_AUDIT_SINK=com.mcpdbwizard.pub.SyslogAuditSink}. It needs no library:
 * syslog is a line on a socket, which is most of why it is worth having — nearly every SIEM already
 * listens for it, and adding one costs this project no dependency at all.
 *
 * <table>
 *   <caption>Environment</caption>
 *   <tr><td>{@code MCP_AUDIT_SYSLOG_HOST}</td><td>Required. The collector.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_SYSLOG_PORT}</td><td>Default {@value #DEFAULT_PORT}.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_SYSLOG_PROTOCOL}</td><td>{@code tcp} (default) or {@code udp}.</td></tr>
 *   <tr><td>{@code MCP_AUDIT_SYSLOG_FACILITY}</td><td>0-23, default {@value #DEFAULT_FACILITY}
 *       (log audit).</td></tr>
 *   <tr><td>{@code MCP_AUDIT_SYSLOG_APP_NAME}</td><td>APP-NAME field, default
 *       {@value #DEFAULT_APP_NAME}.</td></tr>
 * </table>
 *
 * <h2>Use TCP. UDP cannot tell you whether the trail is complete.</h2>
 *
 * <p>Over UDP a record is written to a socket and nothing ever comes back: a collector that is down,
 * full, or behind a dropping firewall is indistinguishable from one that recorded everything. That
 * is tolerable for logs and a poor fit for evidence, so the default is TCP and choosing UDP is
 * warned about at start-up.
 *
 * <h2>What {@link #flush()} means here, precisely</h2>
 *
 * <p>It reports whether any send since the previous flush <b>failed locally</b> — a refused
 * connection, a broken pipe, a socket error. It does <b>not</b> mean the collector indexed the
 * record, because syslog has no acknowledgement in either transport; even over TCP the guarantee
 * stops at the far end's kernel accepting the bytes.
 *
 * <p><b>That matters most in front of a spool.</b> {@link SpoolingAuditSink} deletes a segment when
 * this returns true, so with syslog the spool protects against <em>this process dying</em> and
 * against <em>the collector being unreachable</em>, and not against a collector that accepts bytes
 * and discards them. Returning false unconditionally over UDP was considered and rejected: it would
 * be just as untrue, and it would make the spool grow until it hit its cap and began dropping
 * records — trading a small uncertainty for a certain loss.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class SyslogAuditSink implements McpAuditSink {

    public static final String HOST_VARIABLE = "MCP_AUDIT_SYSLOG_HOST";
    public static final String PORT_VARIABLE = "MCP_AUDIT_SYSLOG_PORT";
    public static final String PROTOCOL_VARIABLE = "MCP_AUDIT_SYSLOG_PROTOCOL";
    public static final String FACILITY_VARIABLE = "MCP_AUDIT_SYSLOG_FACILITY";
    public static final String APP_NAME_VARIABLE = "MCP_AUDIT_SYSLOG_APP_NAME";

    public static final int DEFAULT_PORT = 514;

    /** Facility 13, "log audit". The one the RFC set aside for exactly this. */
    public static final int DEFAULT_FACILITY = 13;

    public static final String DEFAULT_APP_NAME = "mcpdbwizard";

    /** Informational. An audit record is not an alarm; a SIEM decides what is alarming. */
    private static final int SEVERITY = 6;

    private static final int CONNECT_TIMEOUT_MS = 5000;

    /** RFC 5424 wants RFC 3339 with a real offset; UTC with millis is unambiguous everywhere. */
    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private final String theHost;
    private final int thePort;
    private final boolean theUdpFlag;
    private final int theFacility;
    private final String theAppName;
    private final String theLocalHost;
    private final String theProcessId;
    private final LogInterface theLog = new JulLog("SyslogAuditSink");

    private final AtomicLong theDropped = new AtomicLong();
    private final AtomicLong theDelivered = new AtomicLong();
    private long theDroppedAtLastFlush;

    private Socket theSocket;
    private OutputStream theStream;
    private DatagramSocket theDatagramSocket;

    /** Built reflectively by {@link McpAuditSinks#fromEnvironment()}. */
    public SyslogAuditSink() {
        this(required(HOST_VARIABLE),
                number(PORT_VARIABLE, McpAuditSinks.setting(PORT_VARIABLE), DEFAULT_PORT),
                "udp".equalsIgnoreCase(trim(McpAuditSinks.setting(PROTOCOL_VARIABLE))),
                facility(McpAuditSinks.setting(FACILITY_VARIABLE)),
                appName(McpAuditSinks.setting(APP_NAME_VARIABLE)));
    }

    public SyslogAuditSink(String theHostValue, int thePortValue, boolean theUdpFlagValue,
                           int theFacilityValue, String theAppNameValue) {
        this.theHost = theHostValue;
        this.thePort = thePortValue;
        this.theUdpFlag = theUdpFlagValue;
        this.theFacility = theFacilityValue;
        this.theAppName = theAppNameValue;
        this.theLocalHost = localHost();
        this.theProcessId = Long.toString(ProcessHandle.current().pid());

        if (theUdpFlag) {
            theLog.warning("Audit records will go to syslog over UDP, which cannot confirm that any"
                    + " of them arrived. Use " + PROTOCOL_VARIABLE + "=tcp for an audit trail"
                    + " anybody is going to rely on.");
        }
    }

    /** {@inheritDoc} <p>Never throws. */
    public void record(McpAuditEvent theEvent) {
        if (theEvent == null) {
            return;
        }
        try {
            send(format(theEvent));
            theDelivered.incrementAndGet();
        } catch (Exception e) {
            countDrop(e.toString());
        }
    }

    /**
     * One RFC 5424 message.
     *
     * <p>The record's JSON is the MSG, so a collector that understands JSON can index the fields and
     * one that does not still has the whole record on one line. Structured data is {@code -}: every
     * field worth having is already in the JSON, and duplicating it into SD-PARAMs would create two
     * spellings of the same record that could drift.
     */
    String format(McpAuditEvent theEvent) {
        int thePriority = (theFacility * 8) + SEVERITY;
        return "<" + thePriority + ">1 "
                + TIMESTAMP.format(Instant.ofEpochMilli(theEvent.getTimestampMillis())) + " "
                + theLocalHost + " " + theAppName + " " + theProcessId + " mcp-audit - "
                + theEvent.toJson();
    }

    private synchronized void send(String theMessage) throws IOException {
        byte[] theBytes = theMessage.getBytes(StandardCharsets.UTF_8);
        if (theUdpFlag) {
            if (theDatagramSocket == null) {
                theDatagramSocket = new DatagramSocket();
            }
            theDatagramSocket.send(new DatagramPacket(theBytes, theBytes.length,
                    InetAddress.getByName(theHost), thePort));
            return;
        }
        try {
            writeFramed(theBytes);
        } catch (IOException e) {
            // A collector restart drops the connection, and the first write afterwards is the one
            // that finds out. Reconnecting and retrying once turns a routine restart into nothing
            // rather than into a hole in the trail.
            closeTcp();
            writeFramed(theBytes);
        }
    }

    /**
     * Octet-counted framing (RFC 6587).
     *
     * <p>{@code <length> <message>}, rather than terminating with a newline. A record's JSON can
     * legitimately contain an escaped newline, and with newline framing one such record splits into
     * two malformed ones at the collector. Length-prefixing cannot be confused by the payload.
     */
    private void writeFramed(byte[] theBytes) throws IOException {
        if (theStream == null || peerHasClosed()) {
            closeTcp();
            connectTcp();
        }
        theStream.write((theBytes.length + " ").getBytes(StandardCharsets.US_ASCII));
        theStream.write(theBytes);
        theStream.flush();
    }

    /**
     * Has the collector gone away since the last write?
     *
     * <p><b>Checking this is not optional, and finding out the hard way is instructive.</b> Writing
     * to a TCP socket whose peer has closed SUCCEEDS: the bytes go into the local send buffer and
     * the connection is only discovered to be dead on a later write. Relying on the write to throw
     * therefore loses exactly one record every time a collector restarts — silently, counted as
     * delivered. A test that restarted the collector caught it; nothing else would have.
     *
     * <p>A one-millisecond read is enough to tell: syslog over TCP is one-way, so the collector
     * never sends anything, and the only thing a read can return is EOF. A timeout means the
     * connection is still up.
     *
     * <p><b>The race is real and cannot be closed here.</b> If the collector closes between this
     * check and the write, that record is still lost and the next one's check finds the broken
     * connection. Closing that gap needs an acknowledgement, which syslog does not have — it is
     * what {@link SpoolingAuditSink} in front of this is for.
     */
    private boolean peerHasClosed() {
        if (theSocket == null) {
            return false;
        }
        try {
            int thePrevious = theSocket.getSoTimeout();
            theSocket.setSoTimeout(1);
            try {
                return theSocket.getInputStream().read() == -1;
            } catch (java.net.SocketTimeoutException e) {
                return false;
            } finally {
                theSocket.setSoTimeout(thePrevious);
            }
        } catch (IOException e) {
            return true;
        }
    }

    private void connectTcp() throws IOException {
        Socket theNew = new Socket();
        theNew.connect(new InetSocketAddress(theHost, thePort), CONNECT_TIMEOUT_MS);
        theSocket = theNew;
        theStream = theNew.getOutputStream();
    }

    private void closeTcp() {
        try {
            if (theSocket != null) {
                theSocket.close();
            }
        } catch (IOException ignored) {
            // Already broken; nothing here can be lost by failing to close it.
        }
        theSocket = null;
        theStream = null;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Compared against the count at the END OF THE PREVIOUS FLUSH, not the start of this one —
     * the same rule {@link KafkaAuditSink#flush()} documents at length. {@link #record} fails
     * synchronously here, so sampling the counter at the top of this method could not see a
     * failure that had already happened, and a spool would delete a segment whose records never
     * left the machine.
     */
    @Override
    public synchronized boolean flush() {
        long theNow = theDropped.get();
        boolean theCleanFlag = theNow == theDroppedAtLastFlush;
        theDroppedAtLastFlush = theNow;
        return theCleanFlag;
    }

    private void countDrop(String theReason) {
        long theTotal = theDropped.incrementAndGet();
        if (theTotal == 1L || theTotal % 1000L == 0L) {
            theLog.error("Audit record not delivered to syslog (" + theTotal + " so far): "
                    + theReason);
        }
    }

    public long getDroppedCount() {
        return theDropped.get();
    }

    public long getDeliveredCount() {
        return theDelivered.get();
    }

    @Override
    public String describe() {
        return "syslog " + (theUdpFlag ? "udp" : "tcp") + "://" + theHost + ":" + thePort;
    }

    public synchronized void close() {
        closeTcp();
        if (theDatagramSocket != null) {
            theDatagramSocket.close();
            theDatagramSocket = null;
        }
        if (theDropped.get() > 0L) {
            theLog.error("Audit trail is incomplete: " + theDropped.get()
                    + " record(s) never reached syslog.");
        }
    }

    // ---- settings ----

    private static String required(String theVariable) {
        String theValue = McpAuditSinks.setting(theVariable);
        if (theValue == null || theValue.trim().length() == 0) {
            throw new IllegalArgumentException(theVariable + " must be set to use "
                    + SyslogAuditSink.class.getName());
        }
        return theValue.trim();
    }

    static int facility(String theSetting) {
        int theValue = number(FACILITY_VARIABLE, theSetting, DEFAULT_FACILITY);
        if (theValue < 0 || theValue > 23) {
            throw new IllegalArgumentException(FACILITY_VARIABLE
                    + " must be between 0 and 23, not '" + theSetting.trim() + "'");
        }
        return theValue;
    }

    static String appName(String theSetting) {
        if (theSetting == null || theSetting.trim().length() == 0) {
            return DEFAULT_APP_NAME;
        }
        // RFC 5424 fields are space-delimited, so a space in one silently shifts every field after
        // it and the collector reads the wrong things into the wrong places.
        return theSetting.trim().replace(' ', '_');
    }

    private static int number(String theName, String theSetting, int theDefault) {
        if (theSetting == null || theSetting.trim().length() == 0) {
            return theDefault;
        }
        try {
            return Integer.parseInt(theSetting.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(theName + " must be a whole number, not '"
                    + theSetting.trim() + "'");
        }
    }

    private static String trim(String theValue) {
        return theValue == null ? "" : theValue.trim();
    }

    private static String localHost() {
        try {
            return InetAddress.getLocalHost().getHostName().replace(' ', '_');
        } catch (Exception e) {
            // NILVALUE. A message with a wrong hostname is worse than one that admits it has none.
            return "-";
        }
    }
}
