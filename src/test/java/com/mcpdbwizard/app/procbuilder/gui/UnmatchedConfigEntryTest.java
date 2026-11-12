package com.mcpdbwizard.app.procbuilder.gui;

import com.mcpdbwizard.pub.ConsoleLog;
import com.mcpdbwizard.pub.IOUtils;
import com.mcpdbwizard.pub.SqlUtils;
import com.mcpdbwizard.pub.WriteableRowSet;
import org.junit.jupiter.api.Test;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A config entry that names an object the list does not have (dropped since the config was saved,
 * owned outside the config's users, or a routine whose overload number changed) used to be ignored
 * without a word, so a batch build simply did not generate it. Each model now warns, naming it.
 * Found 2026-11-09 by the MariaDB port, whose whole-corpus config named routines outside its
 * OTHER_USER_NAME pattern and a procedure-and-function pair by the wrong overload.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class UnmatchedConfigEntryTest {

    /** A log that keeps its warnings. */
    private static final class Warnings extends ConsoleLog {
        final List<String> seen = new ArrayList<>();

        @Override
        public void warning(String theMessage) {
            seen.add(theMessage);
        }

        @Override
        public void warning(String theMessage, boolean isModal, boolean isLogged) {
            seen.add(theMessage);
        }
    }

    private static WriteableRowSet rows(List<Object[]> data, int width, Warnings log) {
        String[] names = new String[width];
        String[] oracleTypes = new String[width];
        int[] underlying = new int[width];
        int[] javaTypes = new int[width];
        long[] lengths = new long[width];
        int[] decimals = new int[width];
        for (int i = 0; i < width; i++) {
            names[i] = "C" + i;
            oracleTypes[i] = "VARCHAR2";
            underlying[i] = SqlUtils.ORACLE_TEXT_DATATYPE;
            javaTypes[i] = Types.VARCHAR;
            lengths[i] = 128;
        }
        return new WriteableRowSet(data.toArray(), names, oracleTypes, underlying, javaTypes, lengths, decimals, log,
                IOUtils.getOsTempDir(), false);
    }

    @Test
    void aRoutineTheListDoesNotHaveIsNamed() {
        Warnings log = new Warnings();
        List<Object[]> data = new ArrayList<>();
        // owner, name, selected, accessed via, real owner, real name, oracle, java, fixed java, package, real package, overload
        data.add(new Object[]{"SCOTT", "RAISE", "X", "User Object", "SCOTT", "RAISE", "X", "X", "X", null, null, null});
        data.add(new Object[]{"SCOTT", "SAME_NAME", "X", "User Object", "SCOTT", "SAME_NAME", "X", "X", "X", null, null, "1"});
        FunctionTableDataModel model = new FunctionTableDataModel(rows(data, 12, log), log, null);
        Properties config = new Properties();
        config.setProperty("PROC_USER_0", "SCOTT");
        config.setProperty("PROC_NAME_0", "RAISE");
        config.setProperty("PROC_PACKAGE_0", "null");
        config.setProperty("PROC_OVERLOAD_0", "null");
        config.setProperty("PROC_USER_1", "SCOTT");
        config.setProperty("PROC_NAME_1", "GONE");
        config.setProperty("PROC_PACKAGE_1", "PAY");
        config.setProperty("PROC_OVERLOAD_1", "null");
        config.setProperty("PROC_USER_2", "SCOTT");
        config.setProperty("PROC_NAME_2", "SAME_NAME");
        config.setProperty("PROC_PACKAGE_2", "null");
        config.setProperty("PROC_OVERLOAD_2", "null");
        // The row set holds null for an absent package; the config spells it "null". Both match,
        // as they always have, by comparing ("" + x).
        data.get(0)[9] = null;
        model.readFunctionInfo(config);

        assertEquals(2, log.seen.size(), log.seen.toString());
        assertTrue(log.seen.get(0).contains("routine SCOTT.PAY.GONE") && log.seen.get(0).contains("not generated"),
                log.seen.get(0));
        assertTrue(log.seen.get(1).contains("routine SCOTT.SAME_NAME,"), "no overload is named when the config has none: "
                + log.seen.get(1));
        assertEquals(1, model.getOracleFunctions().length, "the matched routine is still selected");
    }

    @Test
    void aSequenceOrTableTheListDoesNotHaveIsNamed() {
        Warnings log = new Warnings();
        List<Object[]> seqs = new ArrayList<>();
        seqs.add(new Object[]{"SCOTT", "EMP_SEQ", "X"});
        SequenceTableDataModel sequences = new SequenceTableDataModel(rows(seqs, 3, log), log, null);
        Properties config = new Properties();
        config.setProperty("SEQUENCE_USER_0", "SCOTT");
        config.setProperty("SEQUENCE_NAME_0", "EMP_SEQ");
        config.setProperty("SEQUENCE_USER_1", "SCOTT");
        config.setProperty("SEQUENCE_NAME_1", "GONE_SEQ");
        sequences.readSequenceInfo(config);
        assertEquals(List.of("The config selects sequence SCOTT.GONE_SEQ, which is not among the sequences this login can"
                + " see, so it is not generated. Check that it still exists and that its owner is within the config's users."),
                log.seen);
    }
}
