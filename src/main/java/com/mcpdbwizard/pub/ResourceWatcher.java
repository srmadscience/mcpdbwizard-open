package com.mcpdbwizard.pub;

/**
 * A static class for keeping track of free memory.
 *
 * <p>This class is used by ReadOnlyRowSet's caching mechanism. If
 * the JVM appears to be running out of memory ReadOnlyRowSet will
 * decline to add a query result to its cache. This class is
 * problematic as some JVM's wait till the absolute last moment (i.e.
 * no free memory left at all) before getting more memory.
 * <p>
 * Future versions of MCPDBWizard may make this class a lot
 * smarter.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 * @see ReadOnlyRowSet
 */
public class ResourceWatcher {
    /**
     * Constant used to decide when we are about to run out of memory.
     * <p>
     * We need a way of telling when we are low on memory. Most JVMs will be created
     * with a relativly small amount of memory and will allocate more as the need arises
     * until the limit on memory is reached. The problem is that the JVM may wait till
     * the last moment before allocating memory.
     * Most JVM's will have allocated all the memory they can before they get to the point where
     * only MIN_SAFE_MEMORY_PCT is free. If free memory falls to this level then it
     * is unlikely that the JVM will allocate any more. Continuing to extend memory
     * structures such as HashMaps etc is dangerous as there is a real chance of running
     * out of memory.
     */
    public static final int MIN_SAFE_MEMORY_PCT = 3;

    /**
     * Runtime object used to get memory usage
     */
    static Runtime theRuntime = Runtime.getRuntime();

    /**
     * A static class for keeping track of free memory.
     */
    ResourceWatcher() {
    }

    /**
     * Return free memory expressed as a percentage of total memory.
     */
    public static long freeMemAsPct() {
        return ((100 * theRuntime.freeMemory()) / theRuntime.totalMemory());
    }
}


