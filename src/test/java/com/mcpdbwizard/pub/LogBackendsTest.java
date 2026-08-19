package com.mcpdbwizard.pub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link LogBackends} — the runtime override for a backend chosen at generation time.
 *
 * <p>The refusals matter most. A mistyped variable that silently fell back to the generated default
 * would produce exactly the failure this class exists to prevent: an operator who believes they
 * redirected the logs and has not.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class LogBackendsTest {

    /** A backend with only a no-argument constructor, like ConsoleLog. */
    public static class NoArgLog extends ConsoleLog {
        public NoArgLog() {
        }
    }

    /** A LogInterface with no usable constructor at all. */
    public static class UnusableLog extends ConsoleLog {
        public UnusableLog(int theNumber) {
        }
    }

    /** Not a LogInterface. */
    public static class NotALog {
        public NotALog(String a, String b) {
        }
    }

    // ---- the default path ----

    @Test
    void anUnsetVariableMeansUseTheGeneratedDefault() {
        // Returning null rather than a backend is what lets the generated code skip constructing its
        // default - a TextLog default would otherwise open a file and throw it away.
        assertNull(LogBackends.create(null, "user.home", "DaoFactory"));
        assertNull(LogBackends.create("", "user.home", "DaoFactory"));
        assertNull(LogBackends.create("   ", "user.home", "DaoFactory"));
    }

    // ---- aliases ----

    @Test
    void theShortNamesResolveToTheBuiltInBackends() {
        assertInstanceOf(ConsoleLog.class, LogBackends.create("console", "n", "F"));
        assertInstanceOf(JulLog.class, LogBackends.create("jul", "n", "F"));
        assertInstanceOf(TextLog.class, LogBackends.create("text", System.getProperty("java.io.tmpdir"), "F"));
    }

    @Test
    void anAliasIsCaseInsensitive() {
        assertInstanceOf(ConsoleLog.class, LogBackends.create("Console", "n", "F"));
        assertInstanceOf(ConsoleLog.class, LogBackends.create("CONSOLE", "n", "F"));
        assertInstanceOf(JulLog.class, LogBackends.create("  jul  ", "n", "F"));
    }

    @Test
    void aFullClassNameWorksToo() {
        assertInstanceOf(JulLog.class,
                LogBackends.create("com.mcpdbwizard.pub.JulLog", "n", "F"));
    }

    // ---- constructor shapes ----

    @Test
    void aTwoArgumentBackendGetsBothNames() {
        // TextLog(directory, name) and JulLog(name) have to be selectable by the same variable, so
        // the widest constructor is tried first and narrowed from there.
        TextLog theLog = (TextLog) LogBackends.create("text", System.getProperty("java.io.tmpdir"), "MyFactory");

        assertTrue(theLog.getCurrentLog().contains("MyFactory"), theLog.getCurrentLog());
    }

    @Test
    void aOneArgumentBackendGetsTheLogName() {
        JulLog theLog = (JulLog) LogBackends.create("jul", "com.example.app", "MyFactory");

        assertEquals("com.example.app", theLog.getCurrentLog());
    }

    @Test
    void aNoArgumentBackendIsStillConstructible() {
        assertInstanceOf(NoArgLog.class,
                LogBackends.create(NoArgLog.class.getName(), "n", "F"));
    }

    // ---- refusals ----

    @Test
    void anUnknownClassStopsTheProgram() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> LogBackends.create("com.example.NoSuchLog", "n", "F"));

        assertTrue(e.getMessage().contains("not on the classpath"), e.getMessage());
        assertTrue(e.getMessage().contains("console"), "the message should list the short names");
    }

    @Test
    void aClassThatIsNotALogInterfaceIsRefused() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> LogBackends.create(NotALog.class.getName(), "n", "F"));

        assertTrue(e.getMessage().contains("does not implement"), e.getMessage());
    }

    @Test
    void aClassWithNoUsableConstructorIsRefused() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> LogBackends.create(UnusableLog.class.getName(), "n", "F"));

        assertTrue(e.getMessage().contains("no public constructor"), e.getMessage());
    }

    @Test
    void aTypoDoesNotSilentlyFallBackToTheDefault() {
        // The whole point. "OB_LOG_BACKEND=jull" must stop the program, not log to the generated
        // backend while the operator believes otherwise.
        assertThrows(IllegalStateException.class, () -> LogBackends.create("jull", "n", "F"));
    }

    @Test
    void theVariableIsNotNamedAfterTheConfigKey() {
        // MCP_HTTP_TOKEN being both a config flag and an environment variable is recorded in
        // CLAUDE.md as the confusing case that catches people out; this must not repeat it. The
        // generation-time answer to "which backend" is DAO_LOG_TYPE, and the deployment-time
        // override must stay a visibly different name.
        assertEquals("MCPDBWIZARD_LOG_BACKEND", LogBackends.BACKEND_VARIABLE);
        assertNotEquals("DAO_LOG_TYPE", LogBackends.BACKEND_VARIABLE);
    }

    @Test
    void theLegacyVariableNameIsStillHonoured() {
        // OB_ was for OrindaBuild. This variable is read by pub, which ships INSIDE customers'
        // deployments -- it lives in their container environments, not in this repository, so a
        // clean break could not be fixed by editing this tree. The failure would also be silent:
        // an unrecognised variable is indistinguishable from an unset one, so logging would revert
        // to whatever was baked in at generation time without a word.
        assertEquals("OB_LOG_BACKEND", LogBackends.LEGACY_BACKEND_VARIABLE);
        assertNotEquals(LogBackends.BACKEND_VARIABLE, LogBackends.LEGACY_BACKEND_VARIABLE);
    }
}
