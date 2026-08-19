package com.mcpdbwizard.pub;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Live-database tests for {@link QueryStatement}: query execution, bind-parameter
 * handling, the row-count cap, the result cache, and the {@code StatsInterface}
 * counters. Gated on a reachable Oracle instance — see {@link DbTestSupport}.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryStatementTest {

    private static final String TABLE = "OB_QS_ITEM";

    private final LogInterface log = new ConsoleLog();
    private Connection connection;

    @BeforeAll
    void setUp() throws SQLException {
        connection = DbTestSupport.requireConnection();
        DbTestSupport.executeQuietly(connection, "DROP TABLE " + TABLE);
        DbTestSupport.execute(connection,
                "CREATE TABLE " + TABLE + " (id NUMBER(10) PRIMARY KEY, name VARCHAR2(40))");
        for (int i = 1; i <= 5; i++) {
            DbTestSupport.execute(connection,
                    "INSERT INTO " + TABLE + " (id, name) VALUES (" + i + ", 'name" + i + "')");
        }
        connection.commit();
    }

    @AfterAll
    void tearDown() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            DbTestSupport.executeQuietly(connection, "DROP TABLE " + TABLE);
            connection.close();
        }
    }

    @Test
    void executesParameterlessQueryAndReportsShape() throws CSException {
        QueryStatement query = new QueryStatement(
                "SELECT id, name FROM " + TABLE + " ORDER BY id", log, connection);

        ReadOnlyRowSet rows = query.execute();

        assertEquals(5, rows.size());
        assertEquals(2, rows.width());
        assertTrue(rows.first());
        assertEquals(1, rows.getInt("ID"));
        assertEquals("name1", rows.getString("NAME"));
    }

    @Test
    void bindsAnIntegerParameter() throws CSException {
        QueryStatement query = new QueryStatement(
                "SELECT name FROM " + TABLE + " WHERE id = ?", log, connection);
        query.setParam(1, 3);

        ReadOnlyRowSet rows = query.execute();

        assertEquals(1, rows.size());
        rows.first();
        assertEquals("name3", rows.getString("NAME"));
    }

    @Test
    void rebindingTheParameterChangesTheResult() throws CSException {
        QueryStatement query = new QueryStatement(
                "SELECT name FROM " + TABLE + " WHERE id = ?", log, connection);

        query.setParam(1, 2);
        ReadOnlyRowSet first = query.execute();
        first.first();
        assertEquals("name2", first.getString("NAME"));

        query.setParam(1, 4);
        ReadOnlyRowSet second = query.execute();
        second.first();
        assertEquals("name4", second.getString("NAME"));
    }

    @Test
    void honoursTheRowLimit() throws CSException {
        QueryStatement query = new QueryStatement(
                "SELECT id FROM " + TABLE + " ORDER BY id", log, connection);
        query.setQueryRows(2);

        ReadOnlyRowSet rows = query.execute();

        assertEquals(2, rows.size());
        assertTrue(rows.hitRowLimit(), "should report that more rows were available");
    }

    @Test
    void cachingIsOffByDefault() throws CSException {
        QueryStatement query = new QueryStatement(
                "SELECT id FROM " + TABLE, log, connection);

        query.execute();

        assertEquals(0, query.getCacheSize());
    }

    @Test
    void cachesAndReusesResultsWhenEnabled() throws CSException {
        QueryStatement query = new QueryStatement(
                "SELECT id FROM " + TABLE + " WHERE id = ?", log, connection);
        query.setCacheSeconds(QueryStatement.CACHE_FOREVER);
        query.setParam(1, 1);

        ReadOnlyRowSet firstCall = query.execute();
        assertEquals(1, query.getCacheSize());

        ReadOnlyRowSet secondCall = query.execute();
        assertSame(firstCall, secondCall, "a cache hit should return the very same row set");
        assertEquals(1, query.getCacheSize());

        query.clearCache();
        assertEquals(0, query.getCacheSize());
    }

    @Test
    void differentParametersProduceDistinctCacheEntries() throws CSException {
        QueryStatement query = new QueryStatement(
                "SELECT id FROM " + TABLE + " WHERE id = ?", log, connection);
        query.setCacheSeconds(QueryStatement.CACHE_FOREVER);

        query.setParam(1, 1);
        query.execute();
        query.setParam(1, 2);
        query.execute();

        assertEquals(2, query.getCacheSize());
    }

    @Test
    void tracksParseAndExecutionStats() throws CSException {
        QueryStatement query = new QueryStatement(
                "SELECT id FROM " + TABLE, log, connection);

        query.execute();

        assertTrue(query.getParses() >= 1, "expected at least one parse");
        assertTrue(query.getExecutions() >= 1, "expected at least one execution");

        query.resetStatsCounters();
        assertEquals(0, query.getParses());
        assertEquals(0, query.getExecutions());
    }

    @Test
    void executingWithoutAConnectionThrows() {
        QueryStatement query = new QueryStatement("SELECT 1 FROM dual", log);
        assertThrows(CSDBException.class, query::execute);
    }
}
