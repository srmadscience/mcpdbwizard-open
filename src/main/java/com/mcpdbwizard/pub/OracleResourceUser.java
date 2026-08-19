package com.mcpdbwizard.pub;

/**
 * An interface that can be inplemented by objects that tend to use up cursors and other
 * oracle resources. All the objects in a class that implement this interface can be added
 * to a vector or a similer data structure. When the time comes to free Oracle resources the
 * class can then iterate through the vector of <code>OracleResourceUser</code> and get them to free
 * their resources.
 * <p> See <a href="https://mcpdbwizard.com/faq/connections">OracleResourceUser</a>
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public interface OracleResourceUser {
    /**
     * Used to tell if the object is using Oracle resources.
     *
     * @return <code>true</code> if the objects holds an open PreparedStatement, ResultSet or similer resource.
     */
    public boolean hasResources();

    /**
     * Used to tell an object to release its Oracle resources. This method never throws an exception. If
     * releasing the resource will create problems they should be dealt with by the implementing class, not
     * escalated to the calling class.
     *
     * @return <code>true</code> if the objects held an open PreparedStatement, ResultSet or similer resource.
     */
    public boolean releaseResources();
}



