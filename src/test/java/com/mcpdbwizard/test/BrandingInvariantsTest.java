package com.mcpdbwizard.test;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs {@code Scripts/check-rename.sh} as part of the ordinary test suite.
 *
 * <p><b>Why this exists.</b> That script asserts the rename/branding invariants — the chain-of-title
 * count, the predecessor notices, the history notes, and a set of strings that must appear nowhere.
 * Until this test it was run by exactly one thing: {@code release.sh}, in its build phase. So the
 * only way to discover a broken invariant was to cut a release, which is the most expensive place to
 * find one and stops the release when it does.
 *
 * <p><b>It found the same defect twice, which is what made this worth writing.</b> The
 * chain-of-title count sat wrong for three days across the 2.0.14 release, and the script's own
 * commentary had already recorded the identical lapse a fortnight earlier, both times with the same
 * cause written down: a full suite was run and this was not, because it is not part of
 * {@code mvn test}. A guard whose only trigger is a release is a guard nobody runs.
 *
 * <p><b>It shells out rather than re-implementing the counts.</b> A Java copy of the rules would be
 * a second opinion free to drift from the one that gates the release, and the two disagreeing is a
 * worse failure than either alone — the same argument that keeps {@code McpToolListing} and
 * {@code McpUnexposedReport} reading the generator's own output instead of deciding for themselves.
 *
 * <p><b>What it does in the EXPORTED tree.</b> It runs there too, and correctly: the script detects
 * a partial tree by the absence of {@code web/src/main/java} and skips every whole-repository total,
 * because a count of the whole repository cannot be asserted against a subset of it. The
 * must-appear-nowhere assertions still run, which is where absence actually matters.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class BrandingInvariantsTest {

    /** Long enough for a whole-repository grep on a cold cache, short enough to fail a hang. */
    private static final int TIMEOUT_SECONDS = 180;

    @Test
    void theRenameAndBrandingInvariantsHold() throws Exception {
        Path theScript = locateScript();
        assumeTrue(theScript != null,
                "check-rename.sh was not found above the working directory");
        Path theRoot = theScript.getParent().getParent();
        assumeTrue(!System.getProperty("os.name", "").toLowerCase().startsWith("windows"),
                "the check is a POSIX shell script");

        // NOT A GIT REPOSITORY MEANS SKIP, NEVER PASS. Every assertion in the script is built on
        // `git ls-files`, so outside a work tree the file list is EMPTY -- and an empty list makes
        // every "this string appears nowhere" assertion true for the wrong reason. A check that
        // cannot fail is worse than one that is absent, because its green is believed.
        assumeTrue(isGitWorkTree(theRoot), "not a git work tree, so the script has nothing to read");

        // ABSOLUTE PATH, and it is not a tidiness preference. The script cd's to its own
        // repository root and then works out where it itself lives, from $0, to exclude its own
        // text from the counts -- it quotes the copyright lines verbatim while explaining them. A
        // RELATIVE $0 no longer resolves after that cd, the self-exclusion silently stops
        // excluding, and every total comes back inflated by however many times the file mentions
        // the thing it is counting. Measured: invoked as "sh Scripts/check-rename.sh" it reports
        // 497 chain-of-title lines against a true 495, and five other assertions fail with it. The
        // script's own comment predicts exactly this, and release.sh has always called it by
        // absolute path.
        Result theResult = run(theRoot, "sh", theScript.toString());
        assertEquals(0, theResult.exitCode,
                "Scripts/check-rename.sh reported a broken invariant. Its own output follows, and"
                        + " the file carries a running commentary explaining what each total is"
                        + " and how to verify a change to it before editing the number.\n\n"
                        + theResult.output);
    }

    /**
     * The script itself, found by walking up from the working directory, or null.
     *
     * <p>Looks for {@code app/Scripts/} and for {@code Scripts/} because both are real: the
     * development tree keeps the app module behind {@code app/}, and the exporter flattens it to
     * the top. The script then finds its own repository root regardless of where it is called
     * from, so only its path matters here.
     */
    private static Path locateScript() {
        Path theDirectory = new File(System.getProperty("user.dir", ".")).toPath().toAbsolutePath();
        while (theDirectory != null) {
            for (String theCandidate : new String[] {"app/Scripts/check-rename.sh",
                                                     "Scripts/check-rename.sh"}) {
                Path theScript = theDirectory.resolve(theCandidate);
                if (Files.isRegularFile(theScript)) {
                    return theScript;
                }
            }
            theDirectory = theDirectory.getParent();
        }
        return null;
    }

    private static boolean isGitWorkTree(Path theRoot) {
        try {
            Result theResult = run(theRoot, "git", "rev-parse", "--is-inside-work-tree");
            return theResult.exitCode == 0 && theResult.output.trim().startsWith("true");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            // No git on the PATH is a reason to skip, not to fail somebody's build.
            return false;
        }
    }

    /**
     * Run a command in {@code theRoot} and collect everything it says.
     *
     * <p>The output is READ BEFORE waiting, deliberately. A process whose stdout fills the pipe
     * buffer blocks writing to it, and a caller that waits first then reads deadlocks — with the
     * whole-repository grep this produces plenty of output, so it is not a theoretical concern.
     */
    private static Result run(Path theRoot, String... theCommand) throws IOException, InterruptedException {
        ProcessBuilder theBuilder = new ProcessBuilder(theCommand);
        theBuilder.directory(theRoot.toFile());
        theBuilder.redirectErrorStream(true);
        Process theProcess = theBuilder.start();
        String theOutput;
        try (InputStream theStream = theProcess.getInputStream()) {
            theOutput = new String(theStream.readAllBytes(), StandardCharsets.UTF_8);
        }
        if (!theProcess.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            theProcess.destroyForcibly();
            throw new IOException("timed out after " + TIMEOUT_SECONDS + "s: " + String.join(" ", theCommand));
        }
        return new Result(theProcess.exitValue(), theOutput);
    }

    private static final class Result {
        private final int exitCode;
        private final String output;

        private Result(int theExitCode, String theOutput) {
            this.exitCode = theExitCode;
            this.output = theOutput;
        }
    }
}
