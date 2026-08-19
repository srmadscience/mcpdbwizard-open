package com.mcpdbwizard.pub;
// We turn Longs and Clobs into files

import java.io.*;

/**
 * This utility class is used to get LOB objects into and out of the
 * database.
 * <p>
 * Under normal circumstances <a href="https://mcpdbwizard.com" target="_blank" class="manual">MCPDBWizard</a> users
 * will have no reason to use this class directly - the generated code will use it.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class LongObjectLoader {

    /**
     * Flag used to determine whether we call the 'deleteOnExit' method
     * for files we download.
     */
    boolean keepFiles = true;

    /**
     * Buffer size for IO operations.
     */
    int bufferSize = IOUtils.IO_BUFFER_SIZE;

    /**
     * This utility class is used to get LOB objects into and out of the
     * database.
     */
    public LongObjectLoader() {
    }

    /**
     * Loads a File into a CLOB
     *
     * @param theFile File to read from
     * @param bufferSize IO Buffer size in bytes
     * @return oracle.sql.CLOB newCLOB CLOB to be updated
     */
    public static oracle.sql.CLOB loadCLOB(oracle.sql.CLOB newCLOB, File theFile, int bufferSize) throws CSException {
        // Variables needed to stream the long data from the DB.
        int bytesRead = 0;
        byte[] theBuffer = new byte[bufferSize];
        OutputStream outStream = null;
        BufferedInputStream theInputStream = null;

        try {
            if (newCLOB == null) {
                throw new CSException("CLOB newCLOB can not be null");
            }

            outStream = newCLOB.getAsciiOutputStream();
            // From 10.1 we use "setAsciiStream(0)" instead of "getAsciiOutputStream()"

            try
            {
                outStream = newCLOB.setAsciiStream(0);
            }
            catch (Exception e)
            {
                // If we get an NPE here it's cos we are using the wrong JDBC driver..
                outStream = newCLOB.setAsciiStream(1);
            }

            // From 11.2 we use "setAsciiStream(1)" instead of "getAsciiOutputStream()"
            outStream = newCLOB.setAsciiStream(1);


            if (theFile != null) {
                theInputStream = new BufferedInputStream(new FileInputStream(theFile), bufferSize);

                while (true) {
                    bytesRead = theInputStream.read(theBuffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    outStream.write(theBuffer, 0, bytesRead);
                }

                // Close inputstream...
                theInputStream.close();
            }

            // Flush and close output stream
            outStream.flush();
            outStream.close();

        } catch (java.io.FileNotFoundException e) {
            throw new CSException("File " + theFile.getAbsolutePath() + " not found");
        } catch (java.io.IOException e) {
            throw new CSException("Unable to load file " + theFile.getAbsolutePath() + ":" + e.getMessage());
        } catch (Exception e) {
            throw new CSException("Unable to load File into CLOB:" + e.getMessage());
        }

        return (newCLOB);
    }

    /**
     * Loads a File into a BLOB
     *
     * @param newBLOB BLOB to be updated
     * @param theFile The file to load into the BLOB
     * @return oracle.sql.BLOB newBLOB
     */
    public static oracle.sql.BLOB loadBLOB(oracle.sql.BLOB newBLOB, File theFile, int bufferSize) throws CSException {
        // Variables needed to stream the long data from the DB.
        int bytesRead = 0;
        byte[] theBuffer = new byte[bufferSize];
        OutputStream outStream = null;
        BufferedInputStream theInputStream = null;

        try {
            if (newBLOB == null) {
                throw new CSException("BLOB newBLOB can not be null");
            }

            outStream = newBLOB.getBinaryOutputStream();
            // From 10.1 we use "setBinaryStream(0)" instead of "getBinaryOutputStream()"
            try
            {
                outStream = newBLOB.setBinaryStream(0);
            }
            catch (Exception e)
            {
                // If we get an NPE here it's cos we are using the wrong JDBC driver..
                outStream = newBLOB.setBinaryStream(1);
            }

            // From 11.2 we use "setBinaryStream(1)" instead of "getBinaryOutputStream()"
            outStream = newBLOB.setBinaryStream(1);

            if (theFile != null) {
                theInputStream = new BufferedInputStream(new FileInputStream(theFile), bufferSize);

                while (true) {
                    bytesRead = theInputStream.read(theBuffer);
                    if (bytesRead == -1) {
                        break;
                    }
                    outStream.write(theBuffer, 0, bytesRead);
                }

                // Close inputstream...
                theInputStream.close();
            }

            // Flush and close output stream
            outStream.flush();
            outStream.close();

        } catch (java.io.FileNotFoundException e) {
            throw new CSException("File " + theFile.getAbsolutePath() + " not found");
        } catch (java.io.IOException e) {
            throw new CSException("Unable to load file " + theFile.getAbsolutePath() + ":" + e.getMessage());
        } catch (Exception e) {
            throw new CSException("Unable to load File into BLOB:" + e.getMessage());
        }

        return (newBLOB);
    }

    /**
     * Loads a byte array into a CLOB
     *
     * @param newCLOB CLOB to be updated
     * @param theArray An array of bytes to load into the CLOB
     * @return oracle.sql.CLOB newCLOB
     */
    public static oracle.sql.CLOB loadCLOB(oracle.sql.CLOB newCLOB, byte[] theArray) throws CSException {
        // Variables needed to stream the long data from the DB.
        OutputStream outStream = null;

        try {
            if (newCLOB == null) {
                throw new CSException("CLOB newCLOB can not be null");
            }


            outStream = newCLOB.getAsciiOutputStream();
            // From 10.1 we use "setAsciiStream(0)" instead of "getAsciiOutputStream()"

            try
            {
                outStream = newCLOB.setAsciiStream(0);
            }
            catch (Exception e)
            {
                // If we get an NPE here it's cos we are using the wrong JDBC driver..
                outStream = newCLOB.setAsciiStream(1);
            }

            // From 11.2 we use "setAsciiStream(1)" instead of "getAsciiOutputStream()"
            outStream = newCLOB.setAsciiStream(1);

            outStream.write(theArray);

            // Flush and close output stream
            outStream.flush();
            outStream.close();

        } catch (Exception e) {
            throw new CSException("Unable to unload byte[] into CLOB:" + e.getMessage());
        }

        return (newCLOB);
    }

    /**
     * Unloads a byte array  to a BLOB
     *
     * @param newBLOB BLOB to be updated
     * @param theArray Array of bytes
     */
    public static oracle.sql.BLOB loadBLOB(oracle.sql.BLOB newBLOB, byte[] theArray) throws CSException {
        // Variables needed to stream the long data from the DB.
        OutputStream outStream = null;

        try {
            if (newBLOB == null) {
                throw new CSException("BLOB newBLOB can not be null");
            }


            outStream = newBLOB.getBinaryOutputStream();
            // From 10.1 we use "setBinaryStream(0)" instead of "getBinaryOutputStream()"

            try
            {
                outStream = newBLOB.setBinaryStream(0);
            }
            catch (Exception e)
            {
                // If we get an NPE here it's cos we are using the wrong JDBC driver..
                outStream = newBLOB.setBinaryStream(1);
            }

            // From 11.2 we use "setBinaryStream(1)" instead of "getBinaryOutputStream()"
            outStream = newBLOB.setBinaryStream(1);

            outStream.write(theArray);

            // Flush and close output stream
            outStream.flush();
            outStream.close();

        } catch (Exception e) {
            throw new CSException("Unable to unload byte[] into BLOB:" + e.getMessage());
        }

        return (newBLOB);
    }

    /**
     * Attempts to upload a File to a BFILE
     * This always fails as BFILES are read only.
     *
     * @param newBFILE BFILE to be updated
     * @param theFile File to read from
     * @param bufferSize IO Buffer size in bytes
     * @throws CSException Because BFILEs can not be modified
     */
    public static oracle.sql.BFILE loadBFILE(oracle.sql.BFILE newBFILE, File theFile, int bufferSize) throws CSException {
        throw new CSException("BFILEs can not be modified");
    }

    /**
     * Loads a char array into a CLOB
     *
     * @param newCLOB CLOB to be updated
     * @param theArray an array of chars
     * @return oracle.sql.CLOB newCLOB
     */
    public static oracle.sql.CLOB loadCLOB(oracle.sql.CLOB newCLOB, char[] theArray) throws CSException {

        try {
            if (newCLOB == null) {
                throw new CSException("CLOB newCLOB can not be null");
            }

            java.io.Writer theWriter = null;

            theWriter = newCLOB.getCharacterOutputStream();

            try
            {
                theWriter = newCLOB.setCharacterStream(0);
            }
            catch (Exception e)
            {
                // If we get an NPE here it's cos we are using the wrong JDBC driver..
                theWriter = newCLOB.setCharacterStream(1);
            }

            // From 11.2 we use "setCharacterStream(1)" instead of "setCharacterStream(0)"
            theWriter = newCLOB.setCharacterStream(1);
            theWriter.write(theArray);
            theWriter.close();

        } catch (Exception e) {
            throw new CSException("Unable to load char[] into CLOB:" + e.getMessage());
        }

        return (newCLOB);
    }

    /**
     * Sets flag used to determine whether we call the 'deleteOnExit' method
     * for files we download.
     */
    public void setKeepFiles(boolean keepFiles) {
        this.keepFiles = keepFiles;
    }

    /**
     * Set buffer size for IO operations
     */
    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    /**
     * Unloads a Binary Stream into a file
     *
     * @param newFile File to unload stream into
     * @param theInputStream Input stream from database
     * @param columnDataType Column database type
     */
    public File unloadBinaryStream(File newFile, InputStream theInputStream, String columnDataType) throws CSException {
        // Variables needed to stream the long data from the DB.
        int bytesRead = 0;
        byte[] theBuffer = new byte[bufferSize];
        FileOutputStream outStream = null;
        BufferedOutputStream buffOutStream = null;

        try {
            outStream = new FileOutputStream(newFile);
            buffOutStream = new java.io.BufferedOutputStream(outStream, bufferSize);

            // If we are dealing with a null column longInputStream will be null. We can can only
            // call one getXXX method when dealing with streaming Long data, so we can't
            // call a getXXX method up front to see if the field is null.
            if (theInputStream != null) {
                java.io.BufferedInputStream buffInStream = new java.io.BufferedInputStream(theInputStream, bufferSize);

                while (true) {
                    bytesRead = buffInStream.read(theBuffer, 0, theBuffer.length);

                    if (bytesRead == -1) {
                        break;
                    }
                    buffOutStream.write(theBuffer, 0, bytesRead);
                }
                // Close inputstream...
                buffInStream.close();
            }

            // Flush and close output stream
            buffOutStream.flush();
            buffOutStream.close();

            // If we are supposed to delete this file afterwords make it so
            if (!keepFiles) {
                newFile.deleteOnExit();
            }

        } catch (java.io.IOException e) {
            throw new CSException("Unable to unload column of type " + columnDataType
                    + " into File " + newFile.getAbsolutePath() + ":" + e.getMessage());
        } catch (Exception e) {
            throw new CSException("Unable to unload column of type " + columnDataType
                    + " into File " + newFile.getAbsolutePath() + ":" + e.getMessage());
        }

        return (newFile);

    }

    /**
     * Unloads a Binary Stream into a byte array. This probably won't work for
     * very big LOBS.
     *
     * @param theInputStream Input stream from database
     * @param columnDataType Column database type
     * @return byte[] an array containing the contents of the binary stream.
     * @since V5.0.2303
     */
    public byte[] unloadBinaryStreamIntoByteArray(InputStream theInputStream, String columnDataType) throws CSException {
        // Variables needed to stream the long data from the DB.
        int bytesRead = 0;
        byte[] theBuffer = new byte[bufferSize];
        ByteArrayOutputStream byteOutStream = null;

        try {
            byteOutStream = new java.io.ByteArrayOutputStream();

            // If we are dealing with a null column longInputStream will be null. We can can only
            // call one getXXX method when dealing with streaming Long data, so we can't
            // call a getXXX method up front to see if the field is null.
            if (theInputStream != null) {
                java.io.BufferedInputStream buffInStream = new java.io.BufferedInputStream(theInputStream, bufferSize);

                while (true) {
                    bytesRead = buffInStream.read(theBuffer, 0, theBuffer.length);

                    if (bytesRead == -1) {
                        break;
                    }
                    byteOutStream.write(theBuffer, 0, bytesRead);
                }
                // Close inputstream...
                buffInStream.close();
            }


        } catch (Exception e) {
            throw new CSException("Unable to unload column of type " + columnDataType
                    + " into byte array:" + e.getMessage());
        }

        return (byteOutStream.toByteArray());

    }

    /**
     * Unloads a LONG column into a file
     *
     * @param newFile File to unload stream into
     * @param theInputString long column from database
     * @param columnDataType Column database type
     */
    public File unloadBinaryStream(File newFile, String theInputString, String columnDataType) throws CSException {
        // Variables needed to stream the long data from the DB.
        byte[] theBuffer = null;
        FileOutputStream outStream = null;

        try {
            outStream = new FileOutputStream(newFile);

            // If we are dealing with a null column longInputStream will be null. We can can only
            // call one getXXX method when dealing with streaming Long data, so we can't
            // call a getXXX method up front to see if the field is null.
            if (theInputString != null && theInputString.length() > 0) {
                theBuffer = theInputString.getBytes();
                outStream.write(theBuffer, 0, theBuffer.length);
            }

            // Flush and close output stream
            outStream.flush();
            outStream.close();

            if (!keepFiles) {
                newFile.deleteOnExit();
            }

        } catch (java.io.IOException e) {
            throw new CSException("Unable to unload column of type " + columnDataType
                    + " into File " + newFile.getAbsolutePath() + ":" + e.getMessage());
        } catch (Exception e) {
            throw new CSException("Unable to unload column of type " + columnDataType
                    + " into File " + newFile.getAbsolutePath() + ":" + e.getMessage());
        }

        return (newFile);
    }

    /**
     * Unloads a BFILE column into a file
     *
     * @param newFile File to unload stream into
     * @param tempBFILE BFILE to unload.
     */
    public File unloadBfile(File newFile, oracle.sql.BFILE tempBFILE) throws CSException {
        InputStream bfileInputStream;
        // See if column was null...
        try {
            if (tempBFILE == null) {
                // This BFILE column is null.
                bfileInputStream = null;
            } else {
                // All manner of strange Oracle errors can leap out at us when
                // we try and get an inputstream from a BFILE
                try {
                    tempBFILE.openFile();
                    bfileInputStream = tempBFILE.getBinaryStream();
                } catch (java.sql.SQLException e) {
                    // If you get "Permission Denied" and are using Unix check that the group execute flag is
                    // set on the parent directory. ie. 'r-x' instead of 'r--'.
                    throw new CSException("BFILE " + tempBFILE.getName() + " can not be opened:" + e.getMessage());
                }

                unloadBinaryStream(newFile, bfileInputStream, "BFILE");

                try {
                    tempBFILE.closeFile();
                } catch (java.sql.SQLException e) {
                    throw new CSException("Unable to close BFILE:" + e.getMessage());
                }
            }
        } catch (java.sql.SQLException e) {
            throw new CSException("Unable to unload column of type " + "BFILE"
                    + " into File " + newFile.getAbsolutePath() + ":" + e.getMessage());
        }

        return (newFile);
    }

    /**
     * Unloads a BLOB column into a file
     *
     * @param newFile File to unload stream into
     * @param tempBLOB BLOB to unload.
     */
    public File unloadBlob(File newFile, oracle.sql.BLOB tempBLOB) throws CSException {
        InputStream blobInputStream;
        // See if column was null...
        try {
            if (tempBLOB != null) {
                // Try and get an inputstream from a BLOB
                try {
                    blobInputStream = tempBLOB.getBinaryStream();
                } catch (java.sql.SQLException e) {
                    throw new CSException("BLOB can not be opened:" + e.getMessage());
                }

                unloadBinaryStream(newFile, blobInputStream, "BLOB");
            }
        } catch (Exception e) {
            throw new CSException("Unable to unload column of type BLOB"
                    + " into File " + newFile.getAbsolutePath() + ":" + e.getMessage());
        }

        return (newFile);
    }

    /**
     * Unloads a CLOB column into a file
     *
     * @param tempCLOB CLOB to unload.
     * @param newFile File to unload stream into
     */
    public File unloadClob(File newFile, oracle.sql.CLOB tempCLOB) throws CSException {
        InputStream clobInputStream;
        // See if column was null...
        try {
            if (tempCLOB != null) {
                // Try and get an inputstream from a CLOB
                try {
                    clobInputStream = tempCLOB.getAsciiStream();
                } catch (java.sql.SQLException e) {
                    throw new CSException("CLOB can not be opened:" + e.getMessage());
                }

                unloadBinaryStream(newFile, clobInputStream, "CLOB");
            }
        } catch (Exception e) {
            throw new CSException("Unable to unload column of type CLOB"
                    + " into File " + newFile.getAbsolutePath() + ":" + e.getMessage());
        }

        return (newFile);
    }

    /**
     * Unloads a BFILE column into a byte array
     *
     * @param tempBFILE BFILE to unload.
     * @return byte[] an array of zero or more bytes containing the BFILE
     * @since V5.0.2303
     */
    public byte[] unloadBfileIntoByteArray(oracle.sql.BFILE tempBFILE) throws CSException {
        byte[] newBytes = new byte[0];
        InputStream bfileInputStream;
        // See if column was null...
        try {
            if (tempBFILE == null) {
                // This BFILE column is null.
                bfileInputStream = null;
            } else {
                // All manner of strange Oracle errors can leap out at us when
                // we try and get an inputstream from a BFILE
                try {
                    tempBFILE.openFile();
                    bfileInputStream = tempBFILE.getBinaryStream();
                } catch (java.sql.SQLException e) {
                    // If you get "Permission Denied" and are using Unix check that the group execute flag is
                    // set on the parent directory. ie. 'r-x' instead of 'r--'.
                    throw new CSException("BFILE " + tempBFILE.getName() + " can not be opened:" + e.getMessage());
                }

                newBytes = unloadBinaryStreamIntoByteArray(bfileInputStream, "BFILE");

                try {
                    tempBFILE.closeFile();
                } catch (java.sql.SQLException e) {
                    throw new CSException("Unable to close BFILE:" + e.getMessage());
                }
            }
        } catch (java.sql.SQLException e) {
            throw new CSException("Unable to unload column of type " + "BFILE"
                    + " into byte array:" + e.getMessage());
        }

        return (newBytes);
    }

    /**
     * Unloads a BLOB column into a byte array
     *
     * @param tempBLOB BLOB to unload.
     * @return byte[] an array of zero or more bytes containing the BFILE
     * @since V5.0.2303
     */
    public byte[] unloadBlobIntoByteArray(oracle.sql.BLOB tempBLOB) throws CSException {
        byte[] newBytes = new byte[0];
        InputStream blobInputStream;
        // See if column was null...
        try {
            if (tempBLOB != null) {
                // Try and get an inputstream from a BLOB
                try {
                    blobInputStream = tempBLOB.getBinaryStream();
                } catch (java.sql.SQLException e) {
                    throw new CSException("BLOB can not be opened:" + e.getMessage());
                }

                newBytes = unloadBinaryStreamIntoByteArray(blobInputStream, "BLOB");
            }
        } catch (Exception e) {
            throw new CSException("Unable to unload column of type BLOB"
                    + " into byte array:" + e.getMessage());
        }

        return (newBytes);
    }

    /**
     * Unloads a CLOB column into a byte array
     *
     * @param tempCLOB CLOB to unload.
     * @return byte[] an array of zero or more bytes containing the BFILE
     * @since V5.0.2303
     */
    public byte[] unloadClobIntoByteArray(oracle.sql.CLOB tempCLOB) throws CSException {
        byte[] newBytes = new byte[0];
        InputStream clobInputStream;
        // See if column was null...
        try {
            if (tempCLOB != null) {
                // Try and get an inputstream from a CLOB
                try {
                    clobInputStream = tempCLOB.asciiStreamValue();
                } catch (java.sql.SQLException e) {
                    throw new CSException("CLOB can not be opened:" + e.getMessage());
                }

                newBytes = unloadBinaryStreamIntoByteArray(clobInputStream, "CLOB");
            }
        } catch (Exception e) {
            throw new CSException("Unable to unload column of type CLOB"
                    + " into byte array:" + e.getMessage());
        }

        return (newBytes);
    }

    /**
     * Unloads a CLOB column into a char array
     *
     * @param tempCLOB CLOB to unload.
     * @return byte[] an array of zero or more bytes containing the CLOB
     * @since V6.0.2765
     */
    public char[] unloadClobIntoCharArray(oracle.sql.CLOB tempCLOB) throws CSException {
        char[] newChars = new char[0];
        char[] theBuffer = new char[bufferSize];
        int bytesRead = 0;

        // See if column was null...
        try {
            if (tempCLOB != null) {
                // Try and get an inputstream from a CLOB
                try {
                    Reader r = tempCLOB.getCharacterStream();
                    CharArrayWriter w = new java.io.CharArrayWriter();

                    while (true) {
                        bytesRead = r.read(theBuffer, 0, theBuffer.length);

                        if (bytesRead == -1) {
                            break;
                        }
                        w.write(theBuffer, 0, bytesRead);
                    }

                    r.close();
                    newChars = w.toCharArray();
                    w.close();
                } catch (java.sql.SQLException e) {
                    throw new CSException("CLOB can not be opened:" + e.getMessage());
                }

            }
        } catch (Exception e) {
            throw new CSException("Unable to unload column of type CLOB"
                    + " into byte array:" + e.getMessage());
        }

        return (newChars);
    }

    /**
     * Unloads a CLOB column into a char array
     *
     * @param tempCLOB CLOB to unload.
     * @return byte[] an array of zero or more bytes containing the CLOB
     * @since V6.0.2839
     */
    public char[] unloadClobIntoCharArray(java.sql.Clob tempCLOB) throws CSException {
        char[] newChars = new char[0];
        char[] theBuffer = new char[bufferSize];
        int bytesRead = 0;

        // See if column was null...
        try {
            if (tempCLOB != null) {
                // Try and get an inputstream from a CLOB
                try {
                    Reader r = tempCLOB.getCharacterStream();
                    CharArrayWriter w = new java.io.CharArrayWriter();

                    while (true) {
                        bytesRead = r.read(theBuffer, 0, theBuffer.length);

                        if (bytesRead == -1) {
                            break;
                        }
                        w.write(theBuffer, 0, bytesRead);
                    }

                    r.close();
                    newChars = w.toCharArray();
                    w.close();
                } catch (java.sql.SQLException e) {
                    throw new CSException("Clob can not be opened:" + e.getMessage());
                }

            }
        } catch (Exception e) {
            throw new CSException("Unable to unload column of type CLOB"
                    + " into byte array:" + e.getMessage());
        }

        return (newChars);
    }
}


