package com.mcpdbwizard.pub;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * Lets the deployment override the logging backend the generator chose.
 *
 * <p>{@code DAO_LOG_TYPE} is a <b>generation-time</b> setting for a <b>deployment-time</b> concern.
 * When this tool was written those were the same moment: one person generated the code and put the
 * jar on a server. One config is now run in a container, over stdio, through the web Runtime page or
 * standalone, and the right backend differs for each — {@code Console Log} is fine on a desktop and
 * corrupts the protocol stream on stdio; {@code Text Log} writes to a disk that vanishes with the
 * container; {@code SLF4J} needs a binding the image may not carry.
 *
 * <p>So {@code OB_LOG_BACKEND} names a backend at run time, and the generated choice becomes the
 * default rather than the only answer. Set nothing and behaviour is exactly as before.
 *
 * <h2>Deliberately not called {@code DAO_LOG_TYPE}</h2>
 *
 * <p>There is already one setting in this project that is both a config flag and an environment
 * variable — {@code MCP_HTTP_TOKEN} — and {@code CLAUDE.md} records it as "the confusing case" that
 * catches people out. Repeating that knowingly would be indefensible, so the override has a name of
 * its own and the two can never be mistaken for each other.
 *
 * <h2>A bad value stops the program</h2>
 *
 * <p>An unusable {@code OB_LOG_BACKEND} throws rather than quietly falling back to the generated
 * default. Falling back would produce the exact failure this class exists to prevent: an operator who
 * believes they redirected the logs and did not. That matches {@code McpAuditSinks}, which refuses to
 * start rather than leave a server silently unaudited.
 *
 * <p>Accepts either a short name — {@code console}, {@code jul}, {@code text}, {@code slf4j},
 * {@code log4j2} — or the fully-qualified class name of any {@link LogInterface}. A custom class
 * needs a public constructor taking two strings, one string, or nothing; they are tried in that
 * order, which is what lets one variable drive {@code TextLog(directory, name)} and
 * {@code ConsoleLog()} alike.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class LogBackends {

    /** Names the backend to use, overriding the one baked in at generation time. */
    public static final String BACKEND_VARIABLE = "MCPDBWIZARD_LOG_BACKEND";

    /**
     * The pre-rename spelling, still honoured when {@link #BACKEND_VARIABLE} is unset.
     *
     * <p>{@code OB_} was for OrindaBuild. This variable is read by the {@code pub} library, which
     * ships <b>inside customers' deployments</b> — it is set in their container environments and
     * start scripts, not in this repository, so a clean break could not be fixed by editing this
     * tree. Its failure mode is quiet, too: an unrecognised variable is indistinguishable from an
     * unset one, so the deployment silently reverts to whatever backend was baked in at generation
     * time and the operator's logs go somewhere else without a word.
     */
    public static final String LEGACY_BACKEND_VARIABLE = "OB_LOG_BACKEND";

    private static final Map<String, String> ALIASES = new HashMap<String, String>();

    static {
        // Typing a fully-qualified class name correctly, once, under pressure, in a container
        // environment file is not a reasonable thing to ask.
        ALIASES.put("console", "com.mcpdbwizard.pub.ConsoleLog");
        ALIASES.put("jul", "com.mcpdbwizard.pub.JulLog");
        ALIASES.put("java", "com.mcpdbwizard.pub.JulLog");
        ALIASES.put("text", "com.mcpdbwizard.pub.TextLog");
        ALIASES.put("file", "com.mcpdbwizard.pub.TextLog");
        ALIASES.put("slf4j", "com.mcpdbwizard.pub.Slf4jLog");
        ALIASES.put("log4j2", "com.mcpdbwizard.pub.Log4j2Log");
    }

    private LogBackends() {
    }

    /**
     * The overriding backend, or null when none is configured.
     *
     * <p>Returning null rather than taking the default as an argument is deliberate: the generated
     * code constructs its default only if this returns nothing, so a {@code TextLog} default does not
     * open a log file that is then thrown away.
     *
     * @param theLogName    the generated log name — a logger name, or a directory for a file backend
     * @param theSecondName the second constructor argument a two-argument backend needs, usually the
     *                      factory class name; may be null
     * @return the backend named by {@code MCPDBWIZARD_LOG_BACKEND} (or the legacy
     *         {@code OB_LOG_BACKEND}), or null to use the generated default
     * @throws IllegalStateException if the variable names something unusable
     */
    public static LogInterface fromEnvironment(String theLogName, String theSecondName) {
        String theRequested = System.getenv(BACKEND_VARIABLE);
        if (theRequested == null || theRequested.trim().isEmpty()) {
            theRequested = System.getenv(LEGACY_BACKEND_VARIABLE);
        }
        return create(theRequested, theLogName, theSecondName);
    }

    /** Testable half of {@link #fromEnvironment}. */
    static LogInterface create(String theSetting, String theLogName, String theSecondName) {
        if (theSetting == null || theSetting.trim().length() == 0) {
            return null;
        }
        String theRequested = theSetting.trim();
        String theClassName = ALIASES.get(theRequested.toLowerCase(java.util.Locale.ENGLISH));
        if (theClassName == null) {
            theClassName = theRequested;
        }

        Class<?> theClass;
        try {
            theClass = Class.forName(theClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(BACKEND_VARIABLE + "=" + theRequested + " names the class "
                    + theClassName + ", which is not on the classpath. Use one of " + ALIASES.keySet()
                    + " or the full name of a com.mcpdbwizard.pub.LogInterface.");
        }
        if (!LogInterface.class.isAssignableFrom(theClass)) {
            throw new IllegalStateException(BACKEND_VARIABLE + "=" + theRequested + " names "
                    + theClassName + ", which does not implement " + LogInterface.class.getName());
        }

        Object theInstance = instantiate(theClass, theLogName, theSecondName);
        if (theInstance == null) {
            throw new IllegalStateException(BACKEND_VARIABLE + "=" + theRequested + " names "
                    + theClassName + ", which has no public constructor taking (String, String),"
                    + " (String) or ().");
        }
        return (LogInterface) theInstance;
    }

    /**
     * Try the widest constructor first.
     *
     * <p>{@code TextLog} takes a directory and a name, most backends take a logger name, and
     * {@code ConsoleLog} takes nothing. Trying them in that order means one environment variable can
     * select any of them without the operator needing to know which shape they picked.
     */
    private static Object instantiate(Class<?> theClass, String theLogName, String theSecondName) {
        if (theSecondName != null) {
            Object theWide = tryConstructor(theClass, new Class[] {String.class, String.class},
                    new Object[] {theLogName, theSecondName});
            if (theWide != null) {
                return theWide;
            }
        }
        Object theNarrow = tryConstructor(theClass, new Class[] {String.class},
                new Object[] {theLogName});
        if (theNarrow != null) {
            return theNarrow;
        }
        return tryConstructor(theClass, new Class[0], new Object[0]);
    }

    private static Object tryConstructor(Class<?> theClass, Class<?>[] theTypes, Object[] theArguments) {
        try {
            Constructor<?> theConstructor = theClass.getConstructor(theTypes);
            return theConstructor.newInstance(theArguments);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            // The constructor exists but refused the arguments - a TextLog pointed at a directory it
            // cannot write, say. That is the operator's setting failing, not the wrong shape, so it
            // must surface rather than fall through to a narrower constructor that might succeed and
            // quietly log somewhere else.
            throw new IllegalStateException("Could not create the log backend " + theClass.getName()
                    + ": " + e, e);
        }
    }
}
