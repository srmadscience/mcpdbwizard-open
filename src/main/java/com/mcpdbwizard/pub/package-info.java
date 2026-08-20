/**
 * The runtime library that MCPDBWizard-generated code links against.
 *
 * <h2>What this package is</h2>
 *
 * <p>MCPDBWizard reads an Oracle schema and emits Java: DAO factories, table managers,
 * callable-statement wrappers, SQL statement classes and — optionally — a Model Context Protocol
 * server. That emitted code is standalone except for this package, which supplies the pieces it
 * would otherwise have to repeat in every generated class: binding parameters, reading rows,
 * carrying PL/SQL collections across JDBC, logging, and reporting failure.
 *
 * <p>You do not normally construct these types yourself. Generated code does, and you call the
 * generated code. The exceptions are the {@link com.mcpdbwizard.pub.LogInterface} implementations,
 * which you may pass to a generated factory, and {@link com.mcpdbwizard.pub.SparseVector}, which
 * you build to pass a sparse VECTOR to a 23ai column.
 *
 * <h2>The compatibility contract</h2>
 *
 * <p><strong>Signatures here are load-bearing for every program this generator has ever
 * produced.</strong> A program generated years ago compiles against whatever version of this
 * library is on its classpath today. Adding a method is safe; changing or removing one breaks
 * code that no longer exists in any repository anyone can rebuild. That constraint explains a
 * good deal of what follows — notably why several classes have near-duplicate siblings rather
 * than a single method with a widened signature.
 *
 * <h2>Finding your way around</h2>
 *
 * <p><b>Running SQL.</b> {@link com.mcpdbwizard.pub.QueryStatement} for a SELECT,
 * {@link com.mcpdbwizard.pub.DmlStatement} for INSERT/UPDATE/DELETE, and
 * {@link com.mcpdbwizard.pub.CallableStatementParameters} for a PL/SQL call — the last being where
 * the awkward part of this problem lives, since a PL/SQL routine can take and return records,
 * collections and ref cursors that JDBC has no direct notion of.
 * {@link com.mcpdbwizard.pub.StatementParameters2} holds the bind logic those share.
 *
 * <p><b>Rows.</b> {@link com.mcpdbwizard.pub.ReadOnlyRowSet} is what a query and an OUT ref cursor
 * both produce; {@link com.mcpdbwizard.pub.WriteableRowSet} is what an insert or update consumes.
 * They understand the Oracle types JDBC does not surface plainly, including native {@code JSON},
 * {@code BOOLEAN} and the several {@code VECTOR} formats.
 *
 * <p><b>PL/SQL collections.</b> {@link com.mcpdbwizard.pub.PlsqlArray} is the interface every
 * generated VARRAY and nested-table class implements. {@link com.mcpdbwizard.pub.PlsqlIndexByTable}
 * and {@link com.mcpdbwizard.pub.PlsqlIndexByTable2} carry index-by tables, which Oracle exposes
 * differently again.
 *
 * <p><b>Failure.</b> Everything throws {@link com.mcpdbwizard.pub.CSException} or a subclass of it,
 * so a caller can catch one type and still distinguish causes. Two are worth knowing by name:
 * {@link com.mcpdbwizard.pub.CSDocumentChangedException} means an optimistic-lock check found the
 * row or document changed since you read it — re-read and reapply — and
 * {@link com.mcpdbwizard.pub.CSPoolExhaustedException} means a
 * {@link com.mcpdbwizard.pub.DaoFactoryPool} had nothing free, which is a capacity signal rather
 * than a database error and is worth alerting on separately.
 *
 * <p><b>Logging.</b> {@link com.mcpdbwizard.pub.LogInterface} with implementations for the console,
 * a text file, {@code java.util.logging} ({@link com.mcpdbwizard.pub.JulLog}), SLF4J
 * ({@link com.mcpdbwizard.pub.Slf4jLog}) and Log4j 2 ({@link com.mcpdbwizard.pub.Log4j2Log}). The
 * generated factory picks one from its configuration;
 * {@link com.mcpdbwizard.pub.LogBackends} lets a deployment override that choice at run time,
 * because the right backend differs between a container, a stdio process and a desktop program
 * while the generated code is the same.
 *
 * <p><b>MCP runtime.</b> When a generated server is emitted, it uses
 * {@link com.mcpdbwizard.pub.McpAuditSink} and friends for the audit trail,
 * {@link com.mcpdbwizard.pub.McpMetrics} for Prometheus counters, and
 * {@link com.mcpdbwizard.pub.McpHttpPolicy} for the transport rules the MCP specification requires.
 * These are inert in a program that is not an MCP server.
 *
 * <h2>Two hazards worth reading before you are bitten by them</h2>
 *
 * <p><strong>{@link com.mcpdbwizard.pub.ConsoleLog} blocks.</strong> It asks the console to
 * acknowledge a warning, so a program using it stops and waits rather than continuing. That is
 * reasonable for an interactive tool and wrong for anything unattended: in a test or a server it
 * does not fail, it hangs. Use {@link com.mcpdbwizard.pub.JulLog} or
 * {@link com.mcpdbwizard.pub.TextLog} for unattended code.
 *
 * <p><strong>An audit sink must never throw.</strong> {@link com.mcpdbwizard.pub.McpAuditSink}
 * implementations record what already happened; letting one propagate a failure would turn a
 * logging problem into a failed database call, which is the wrong trade in both directions.
 * Implementations here absorb and count instead, which is why they report a drop count rather
 * than raising.
 *
 * @see com.mcpdbwizard.pub.LibraryInfo
 */
package com.mcpdbwizard.pub;
