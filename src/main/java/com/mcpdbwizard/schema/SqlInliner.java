package com.mcpdbwizard.schema;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Copies each user SQL statement's TEXT into the config that names it, so the config stands alone.
 *
 * <p>Historically a config stored only {@code SQL_FILENAME_<i>} plus one global
 * {@code SQL_FILE_DIRECTORY}, and the statements lived on disk. That is why a config can name a
 * file that is no longer there, and why a config copied to another machine stops generating — the
 * committed propfiles still carry {@code SQL_FILE_DIRECTORY=C:\DR\Work\CodeSpooks\Sqlfiles}, a path
 * on a machine that has not existed for years. Once the text is in the config, the directory is
 * only a fallback for statements that have not been migrated.
 *
 * <pre>
 *   java -cp &lt;jar&gt; com.mcpdbwizard.schema.SqlInliner &lt;config.pb2|.json&gt; &lt;sqlDir&gt; [&lt;sqlDir&gt; ...]
 * </pre>
 *
 * <p>Directories are searched <b>in order</b>, first match wins, which is how an overlay of
 * config-specific statements sits in front of a shared library.
 *
 * <h2>A .pb2 is APPENDED to, not rewritten</h2>
 *
 * <p>Rewriting through {@link Properties#store} would reorder every key — these files are 2011
 * {@code store()} output in hash order — turning a migration that adds N lines into a diff that
 * touches all of them and hides what actually changed. So the new keys are appended, escaped by
 * {@code store()} itself rather than by hand: escaping a value that may contain backslashes,
 * newlines, colons and equals signs is exactly the sort of thing not to reimplement.
 *
 * <p>A {@code .json} config is rewritten normally; its formatting is generated anyway.
 *
 * <p><b>Idempotent.</b> A statement that already carries its text is left alone, so running this
 * twice changes nothing the second time.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class SqlInliner {

    private SqlInliner() {
    }

    /** What one migration did, for reporting and for the tests. */
    public static final class Result {

        private final List<String> inlined = new ArrayList<>();
        private final List<String> alreadyInline = new ArrayList<>();
        private final List<String> notFound = new ArrayList<>();

        public List<String> getInlined() {
            return inlined;
        }

        public List<String> getAlreadyInline() {
            return alreadyInline;
        }

        /** Statements whose file could not be found in any given directory. */
        public List<String> getNotFound() {
            return notFound;
        }

        public boolean changedAnything() {
            return !inlined.isEmpty();
        }

        @Override
        public String toString() {
            return inlined.size() + " inlined, " + alreadyInline.size() + " already inline, "
                    + notFound.size() + " not found";
        }
    }

    /**
     * Fill in {@code SQL_TEXT_<i>} for every statement whose file can be found.
     *
     * <p>Works on the {@link Properties} rather than on a {@link Schema}, so the caller can write
     * the result back without a full round trip. A missing file is RECORDED, not thrown: a config
     * naming a statement nobody has any more is the condition being cured, and refusing to migrate
     * the other twenty-eight statements over it would help nobody.
     */
    public static Result inline(Properties theConfig, List<Path> theSearchPath) {
        Result theResult = new Result();
        int theMissingRun = 0;
        for (int theIndex = 0; theMissingRun <= MISSING_LIMIT; theIndex++) {
            String theName = theConfig.getProperty("SQL_FILENAME_" + theIndex);
            if (theName == null) {
                theMissingRun++;
                continue;
            }
            theMissingRun = 0;

            String theExisting = theConfig.getProperty("SQL_TEXT_" + theIndex);
            if (theExisting != null && !theExisting.isEmpty()) {
                theResult.alreadyInline.add(theName);
                continue;
            }

            Path theFile = find(theName, theSearchPath);
            if (theFile == null) {
                theResult.notFound.add(theName);
                continue;
            }
            try {
                theConfig.setProperty("SQL_TEXT_" + theIndex,
                        new String(Files.readAllBytes(theFile), StandardCharsets.UTF_8));
                theResult.inlined.add(theName);
            } catch (IOException e) {
                theResult.notFound.add(theName + " (" + e + ")");
            }
        }
        return theResult;
    }

    /** Indexes are not guaranteed contiguous; give up only after this many consecutive gaps. */
    private static final int MISSING_LIMIT = 100;

    private static Path find(String theName, List<Path> theSearchPath) {
        for (Path theDir : theSearchPath) {
            Path theCandidate = theDir.resolve(theName);
            if (Files.isRegularFile(theCandidate)) {
                return theCandidate;
            }
        }
        return null;
    }

    /**
     * The lines {@link Properties#store} would write for these keys, without its comment header.
     *
     * <p>Used to APPEND to an existing {@code .pb2} rather than rewrite it: the escaping is Java's
     * own, and the file it is appended to is untouched.
     */
    public static String escapedLinesFor(Map<String, String> theNewKeys) throws IOException {
        Properties theHolder = new Properties();
        theHolder.putAll(theNewKeys);
        StringWriter theWriter = new StringWriter();
        theHolder.store(theWriter, null);

        StringBuilder theLines = new StringBuilder();
        for (String theLine : theWriter.toString().split("\n")) {
            // store() writes a "#<date>" comment (and a null comment line is omitted); drop any
            // comment so only key=value lines are appended.
            if (theLine.startsWith("#") || theLine.isEmpty()) {
                continue;
            }
            theLines.append(theLine).append('\n');
        }
        return theLines.toString();
    }

    /** Migrate one config file in place. @return what changed */
    public static Result migrate(Path theConfigFile, List<Path> theSearchPath) throws IOException {
        if (theConfigFile.toString().toLowerCase().endsWith(".json")) {
            Schema theSchema = new Schema(new String(Files.readAllBytes(theConfigFile),
                    StandardCharsets.UTF_8));
            Properties theProperties = theSchema.toPb2();
            Result theResult = inline(theProperties, theSearchPath);
            if (theResult.changedAnything()) {
                Files.write(theConfigFile,
                        new Schema(theProperties).toJson().getBytes(StandardCharsets.UTF_8));
            }
            return theResult;
        }

        Properties theOriginal = new Properties();
        try (InputStream in = Files.newInputStream(theConfigFile)) {
            theOriginal.load(in);
        }
        Properties theMigrated = new Properties();
        theMigrated.putAll(theOriginal);
        Result theResult = inline(theMigrated, theSearchPath);
        if (!theResult.changedAnything()) {
            return theResult;
        }

        // Only the keys that are new, so the append is exactly the migration.
        Map<String, String> theAdded = new LinkedHashMap<>();
        for (String theKey : theMigrated.stringPropertyNames()) {
            if (!theOriginal.containsKey(theKey)) {
                theAdded.put(theKey, theMigrated.getProperty(theKey));
            }
        }
        String theText = new String(Files.readAllBytes(theConfigFile), StandardCharsets.UTF_8);
        if (!theText.endsWith("\n")) {
            theText = theText + "\n";
        }
        Files.write(theConfigFile,
                (theText + escapedLinesFor(theAdded)).getBytes(StandardCharsets.UTF_8));
        return theResult;
    }

    public static void main(String[] theArgs) throws IOException {
        if (theArgs.length < 2) {
            System.err.println("usage: SqlInliner <config.pb2|.json> <sqlDir> [<sqlDir> ...]");
            System.err.println("  directories are searched in order, first match wins");
            System.exit(2);
        }
        List<Path> theSearchPath = new ArrayList<>();
        for (int i = 1; i < theArgs.length; i++) {
            theSearchPath.add(new File(theArgs[i]).toPath());
        }
        Result theResult = migrate(new File(theArgs[0]).toPath(), theSearchPath);
        System.out.println(theArgs[0] + ": " + theResult);
        for (String theMiss : theResult.getNotFound()) {
            System.out.println("  NOT FOUND: " + theMiss);
        }
    }
}
