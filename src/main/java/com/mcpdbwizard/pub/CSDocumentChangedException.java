package com.mcpdbwizard.pub;

/**
 * Thrown when an etag-checked duality-view document update finds that the document
 * has been changed or deleted by another session since it was read (optimistic
 * locking failure). Re-read the document and reapply the change to recover.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class CSDocumentChangedException extends CSException {

    /**
     * Default constructor
     */
    public CSDocumentChangedException() {
        super();
    }

    /**
     * Constructor with parameters.
     *
     * @param theExceptionMessage An exception message
     */
    public CSDocumentChangedException(String theExceptionMessage) {
        super(theExceptionMessage);
    }
}
