package com.mcpdbwizard.pub;

/* import changed for Oracle 11g */
//import oracle.jdbc.driver.OracleTypes;

import oracle.jdbc.OracleTypes;

import java.math.BigDecimal;

/**
 * Holds state variables for PL/SQL Index By Table parameters
 * <p>
 * This class represents an Oracle PL/SQL Index By Table. In addition to the
 * table data it also holds information about the data type of the elements
 * in the table, their maximum length and the maximum number of elements this
 * table can have after an update or retrieval. For this reason this class
 * is always instantiated even for OUT parameters. As of Oracle 10g INDEX BY
 * tables can have two types of parameters - Numbers or Strings. Parameter types
 * can not be mixed within the same table.
 * <p>
 * Under normal circumstances <a href="https://mcpdbwizard.com" target="_blank" class="manual">MCPDBWizard</a> users
 * will have no reason to use this class directly - the generated code will use it.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 * @since Oracle 10g/4.0.1798
 */
public class PlsqlIndexByTable {
    /**
     * Array containing numbers or strings
     **/
    private Object[] dataArray = null;

    /**
     * The maximum length of any element
     **/
    private int elementMaxLength = 1024;

    /**
     * The maximum number of elements.
     * This is used to determine the size of the array. If you try to
     * set this to Integer.MAX_VALUE you will almost certainly kill
     * your JVM with a java.lang.OutOfMemoryError.
     **/
    private int elementMaxCount = 1024;

    /**
     * The OracleTypes code for the data stored in dataArray
     * Legal values are OracleTypes.VARCHAR and OracleTypes.NUMBER
     */
    private int dataTypeCode = OracleTypes.VARCHAR;

    /**
     * Contruct an empty INDEX BY table
     */
    public PlsqlIndexByTable() {
        dataArray = new Object[0];
        setDataTypeNumber();
    }

    /**
     * Get the maximum length of an element in the table
     */
    public int getElementMaxLength() {
        return (elementMaxLength);
    }

    /**
     * Set the maximum length of an element in the table
     */
    public void setElementMaxLength(int elementMaxLength) {
        this.elementMaxLength = elementMaxLength;
    }

    /**
     * Get the maximum size this table can be after an update or retrieval.
     */
    public int getElementMaxCount() {
        return (elementMaxCount);
    }

    /**
     * Set the maximum size this table can be after an update or retrieval.
     * This is used to decide how big the output array should be.
     */
    public void setElementMaxCount(int elementMaxCount) {
        this.elementMaxCount = elementMaxCount;
    }

    /**
     * Set the data type to Strings
     */
    public void setDataTypeString() {
        dataTypeCode = OracleTypes.VARCHAR;
    }

    /**
     * Set the data type to Numbers
     */
    public void setDataTypeNumber() {
        dataTypeCode = OracleTypes.NUMBER;
    }

    /**
     * Get the underlying OracleType code for the array.
     * This is determined by looking at the first element in the array.
     */
    public int getDataTypeCode() {
        return (dataTypeCode);
    }

    /**
     * Get the array data
     */
    public Object[] getArray() {
        return (dataArray);
    }

    /**
     * Set the contents of the Array.
     * This method assumes that the user is providing a 1 dimensional array of
     * numbers or strings. The values of elementMaxCount and elementMaxLength are
     * updated by this method. If you are passing in an array that will be
     * appended and returned to you you should call setElementMaxCount and
     * setElementMaxLength after calling serArray. 'null' is not an acceptable
     * value for dataArray and will be turned into Object[0]. This is so that
     * getArrayLength works reliably.
     *
     * @param dataArray
     * @see com.mcpdbwizard.pub.PlsqlIndexByTable#setElementMaxCount
     * @see com.mcpdbwizard.pub.PlsqlIndexByTable#setElementMaxLength
     */
    public void setArray(Object[] dataArray) {
        if (dataArray == null) {
            this.dataArray = new Object[0];
        } else {
            this.dataArray = dataArray;
        }

        setDataTypeString();

        if (this.dataArray.length > 0) {
            // Decide what data type the data is
            if (this.dataArray[0] instanceof String) {
                setDataTypeString();
            } else {
                setDataTypeNumber();
            }

            // Update element count
            if (this.dataArray.length > elementMaxCount) {
                elementMaxCount = this.dataArray.length;
            }
        }
    }

    /**
     * Set the array data using int[]
     * This convenience method returns sets the array using an array of int[]
     *
     * @param newArray
     */
    public void setArray(int[] newArray) {
        if (newArray == null) {
            setArray(new Object[0]);
            setDataTypeNumber();
        } else {
            Object[] tempArray = new Object[newArray.length];

            for (int i = 0; i < tempArray.length; i++) {
                // Store as BigDecimal - will get converted to this anyway
                tempArray[i] = new BigDecimal(newArray[i]);
            }

            setArray(tempArray);
        }

    }

    /**
     * Set the array data using long[]
     * This convenience method returns sets the array using an array of long[]
     *
     * @param newArray
     */
    public void setArray(long[] newArray) {
        if (newArray == null) {
            setArray(new Object[0]);
            setDataTypeNumber();
        } else {
            Object[] tempArray = new Object[newArray.length];

            for (int i = 0; i < tempArray.length; i++) {
                // Store as BigDecimal - will get converted to this anyway
                tempArray[i] = new BigDecimal(newArray[i]);
            }

            setArray(tempArray);
        }

    }

    /**
     * Set the array data using float[]
     * This convenience method returns sets the array using an array of float[]
     *
     * @param newArray
     */
    public void setArray(float[] newArray) {
        if (newArray == null) {
            setArray(new Object[0]);
            setDataTypeNumber();
        } else {
            Object[] tempArray = new Object[newArray.length];

            for (int i = 0; i < tempArray.length; i++) {
                // Store as BigDecimal - will get converted to this anyway
                tempArray[i] = new BigDecimal(newArray[i]);
            }

            setArray(tempArray);
        }

    }

    /**
     * Set the array data using double[]
     * This convenience method returns sets the array using an array of double[]
     *
     * @param newArray
     */
    public void setArray(double[] newArray) {
        if (newArray == null) {
            setArray(new Object[0]);
            setDataTypeNumber();
        } else {
            Object[] tempArray = new Object[newArray.length];

            for (int i = 0; i < tempArray.length; i++) {
                // Store as BigDecimal - will get converted to this anyway
                tempArray[i] = new BigDecimal(newArray[i]);
            }

            setArray(tempArray);
        }

    }

    /**
     * Get the array data as int[]
     * This convenience method returns the contents of the array as an array of int.
     * Because the array can have null elements and an 'int' can never be null you
     * need to say how nulls should be treated.
     *
     * @param nullToken The int you will use to represent null. e.g. Integer.MIN_VALUE
     * @return int[] An array of numbers. If the array is empty you will get int[0] back.
     * @throws ClassCastException if the array isn't of numbers.
     */
    public int[] getArrayAsInt(int nullToken) {
        int[] newArray = new int[0];

        if (dataArray.length > 0) {
            newArray = new int[dataArray.length];

            for (int i = 0; i < dataArray.length; i++) {
                if (dataArray[i] == null) {
                    newArray[i] = nullToken;
                } else {
                    newArray[i] = ((BigDecimal) dataArray[i]).intValue();
                }
            }
        }

        return (newArray);
    }

    /**
     * Get the array data as long[]
     * This convenience method returns the contents of the array as an array of long.
     * Because the array can have null elements and a 'long' can never be null you
     * need to say how nulls should be treated.
     *
     * @param nullToken The long you will use to represent null. e.g. Long.MIN_VALUE
     * @return long[] An array of numbers. If the array is empty you will get long[0] back.
     * @throws ClassCastException if the array isn't of numbers.
     */
    public long[] getArrayAsLong(long nullToken) {
        long[] newArray = new long[0];

        if (dataArray.length > 0) {
            newArray = new long[dataArray.length];

            for (int i = 0; i < dataArray.length; i++) {
                if (dataArray[i] == null) {
                    newArray[i] = nullToken;
                } else {
                    newArray[i] = ((BigDecimal) dataArray[i]).longValue();
                }
            }
        }

        return (newArray);
    }

    /**
     * Get the array data as float[]
     * This convenience method returns the contents of the array as an array of float.
     * Because the array can have null elements and a 'float' can never be null you
     * need to say how nulls should be treated.
     *
     * @param nullToken The float you will use to represent null. e.g. Float.MIN_VALUE
     * @return float[] An array of numbers. If the array is empty you will get float[0] back.
     * @throws ClassCastException if the array isn't of numbers.
     */
    public float[] getArrayAsFloat(float nullToken) {
        float[] newArray = new float[0];

        if (dataArray.length > 0) {
            newArray = new float[dataArray.length];

            for (int i = 0; i < dataArray.length; i++) {
                if (dataArray[i] == null) {
                    newArray[i] = nullToken;
                } else {
                    newArray[i] = ((BigDecimal) dataArray[i]).floatValue();
                }
            }
        }

        return (newArray);
    }

    /**
     * Get the array data as double[]
     * This convenience method returns the contents of the array as an array of double.
     * Because the array can have null elements and a 'double' can never be null you
     * need to say how nulls should be treated.
     *
     * @param nullToken The double you will use to represent null. e.g. Double.MIN_VALUE
     * @return double[] An array of numbers. If the array is empty you will get double[0] back.
     * @throws ClassCastException if the array isn't of numbers.
     */
    public double[] getArrayAsDouble(double nullToken) {
        double[] newArray = new double[0];

        if (dataArray.length > 0) {
            newArray = new double[dataArray.length];

            for (int i = 0; i < dataArray.length; i++) {
                if (dataArray[i] == null) {
                    newArray[i] = nullToken;
                } else {
                    newArray[i] = ((BigDecimal) dataArray[i]).doubleValue();
                }
            }
        }

        return (newArray);
    }

    /**
     * Get the array data as String[]
     * This convenience method returns the contents of the array as an array of String.
     * Because the array can have null elements and an 'double' can never be null you
     * need to say how nulls should be treated.
     *
     * @return String[] An array of Strings. If the array is empty you will get double[0] back.
     */
    public String[] getArrayAsString() {
        String[] newArray = new String[0];

        if (dataArray.length > 0) {
            newArray = new String[dataArray.length];

            for (int i = 0; i < dataArray.length; i++) {
                if (dataArray[i] == null) {
                    newArray[i] = null;
                } else {
                    if (dataArray[i] instanceof BigDecimal) {
                        newArray[i] = ((BigDecimal) dataArray[i]).toString();
                    } else {
                        newArray[i] = ((String) dataArray[i]);
                    }
                }
            }
        }

        return (newArray);
    }

    /**
     * Get the array data as java.math.BigDecimal[]
     * This convenience method returns the contents of the array as an array of BigDecimal.
     *
     * @return BigDecimal[] An array of BigDecimal
     * @throws CSException if this isn't an array of numbers
     * @since 4.0.2108
     */
    public java.math.BigDecimal[] getArrayAsBigDecimal() throws CSException {
        java.math.BigDecimal[] newArray = new java.math.BigDecimal[0];

        if (dataArray.length > 0) {
            newArray = new java.math.BigDecimal[dataArray.length];

            for (int i = 0; i < dataArray.length; i++) {
                if (dataArray[i] == null) {
                    newArray[i] = null;
                } else {
                    if (dataArray[i] instanceof BigDecimal) {
                        newArray[i] = (BigDecimal) dataArray[i];
                    } else {
                        throw new CSException("PlsqlIndexByTable/getArrayAsbigDecimal: Non numeric value found where numeric value expected: " + dataArray[i]);
                    }
                }
            }
        }

        return (newArray);
    }

    /**
     * Get the length of the array
     */
    public int getArrayLength() {
        return (dataArray.length);
    }
}


