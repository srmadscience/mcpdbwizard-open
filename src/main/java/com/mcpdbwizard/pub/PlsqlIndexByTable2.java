package com.mcpdbwizard.pub;

/* import changed for Oracle 11g */
//import oracle.jdbc.driver.OracleTypes;

import oracle.jdbc.OracleTypes;

import java.sql.Connection;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Holds state variables for PL/SQL Index By Table parameters
 * <p>
 * This class represents an Oracle PL/SQL Index By Table. In addition to the
 * table data it also holds information about the data type of the elements
 * in the table, their maximum length and the maximum number of elements this
 * table can have after an update or retrieval. For this reason this class
 * is always instantiated even for OUT parameters. As of Oracle 10g INDEX BY
 * tables can have two types of parameters - Numbers or Strings. Parameter types
 * can not be mixed within the same table.  MCPDBWizard converts other data
 * types to numbers or strings.
 * <p>
 * Under normal circumstances <a href="https://mcpdbwizard.com" target="_blank" class="manual">MCPDBWizard</a> users
 * will have no reason to use this class directly - the generated code will use it.
 * <p>
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 * @since Oracle 10g/5.0.2556
 */
public class PlsqlIndexByTable2 implements PlsqlArray {

    /**
     * Date format mask used by Oracle for converting timestamp to String
     */
    public static final String ORACLE_TIMESTAMP_TO_CHAR_MASK = "yyyy-mm-dd hh24:mi:ss.ff8";

    /**
     * Date format mask used by Oracle for converting a ZONED timestamp to and from String.
     *
     * <p><b>{@code TZR} rather than {@code TZH:TZM}, and the choice is not arbitrary.</b> Measured
     * against 12c and 23ai: {@code TZR} accepts both a numeric offset ({@code +05:30}) and a region
     * name ({@code Asia/Calcutta}), where {@code TZH:TZM} accepts only the offset and rejects a
     * region with ORA-01858. A region name is the only form that survives a daylight-saving
     * transition correctly, so losing it would leave the zone technically present and practically
     * wrong twice a year.
     *
     * <p><b>The cost, and why {@link #ensureFractionalSeconds()} exists.</b> Adding a zone element
     * to the mask makes Oracle stop tolerating a missing fractional-seconds part: this mask rejects
     * {@code '2019-03-01 14:25:36'} with ORA-01843, which the older unzoned
     * {@link #ORACLE_TIMESTAMP_TO_CHAR_MASK} accepts. That would have been a silent regression for
     * anyone hand-writing timestamp strings, so the value is normalised before it is bound rather
     * than the mask being weakened.
     *
     * @since 2.0.0
     */
    public static final String ORACLE_TIMESTAMPTZ_TO_CHAR_MASK = "yyyy-mm-dd hh24:mi:ss.ff9 TZR";

    /**
     * Date format mask used by Oracle for converting date to String
     */
    public static final String ORACLE_DATE_TO_CHAR_MASK = "yyyy-mm-dd hh24:mi:ss";

    /**
     * Date format mask used by Java for converting Timestamp to String
     */
    public static final String JAVA_DATE_TO_CHAR_MASK = "yyyy-MM-dd HH:mm:ss";

    /**
     * String containing zeros used by number formatter.
     * Oracle has a precision of 38 decimal places.
     */
    private final String ZERO_STRING = ".########################################";
    /**
     * Number formatter
     */
    NumberFormat formatter = null;
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
    private int elementMaxCount = 4096;
    /**
     * The  OracleTypes code for the data stored in dataArray
     * Legal values are OracleTypes.VARCHAR and OracleTypes.NUMBER
     */
    private int dataTypeCode = OracleTypes.VARCHAR;
    /**
     * Whether this array is BigDecimal or String
     */
    private int realDataTypeCode = oracle.jdbc.OracleTypes.VARCHAR;
    /**
     * Either length of string, decimal places of number, or precision of
     * timestamp in ms
     */
    private int realDataTypePrecision = 0;
    /**
     * Formatter for DATE and TIMESTAMP fields.
     */
    private SimpleDateFormat theDateFormat;

    /**
     * Contruct an empty INDEX BY table
     *
     * @param realDataTypeCode One of oracle.jdbc.OracleTypes.NUMBER or oracle.jdbc.OracleTypes.VARCHAR
     * @param realDataTypePrecision How many decimal places of precision.
     */
    public PlsqlIndexByTable2(int realDataTypeCode
            , int realDataTypePrecision) {
        this.realDataTypeCode = realDataTypeCode;
        this.realDataTypePrecision = realDataTypePrecision;

        if (realDataTypePrecision != 0) {
            formatter = new DecimalFormat("#0" + ZERO_STRING.substring(0, realDataTypePrecision));
        } else {
            formatter = new DecimalFormat("#0");
            theDateFormat = new SimpleDateFormat(JAVA_DATE_TO_CHAR_MASK);
        }

        dataArray = new Object[0];
    }

    /**
     * Get the official OracleType code for the array.
     */
    public int getDataTypeCode() {
        return (dataTypeCode);
    }

    /**
     * Get the actual OracleType code for the array.
     */
    public int getRealDataTypeCode() {
        return (realDataTypeCode);
    }

    /**
     * Set the official Oracle data type
     */
    public void setDataType(int dataTypeCode) {
        this.dataTypeCode = dataTypeCode;
    }

    /**
     * Set the actual Oracle data type
     */
    public void setRealDataType(int realDataTypeCode) {
        this.realDataTypeCode = realDataTypeCode;

        if (realDataTypePrecision == 0 && theDateFormat == null) {
            theDateFormat = new SimpleDateFormat(JAVA_DATE_TO_CHAR_MASK);
        }

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
        // Update element count
        if (this.dataArray.length < elementMaxCount) {
            this.elementMaxCount = elementMaxCount;
        }

    }

    /**
     * Get the array data
     */
    public Object[] getArray() {
        return (dataArray);
    }

    // ---- PlsqlArray -------------------------------------------------------
    //
    // This class predates PlsqlArray and models an index-by table directly, while the
    // interface was written for the GENERATED VARRAY / nested-table classes. Implementing
    // it here lets one piece of code handle both -- specifically the generated MCP server,
    // whose collectionToJson() takes a PlsqlArray and which previously could not touch an
    // index-by param at all.
    //
    // Purely additive: three new methods over the existing getArray()/setArray(Object[]),
    // no existing signature or behaviour changed, so programs already linking this class
    // are unaffected.

    /**
     * Replace the contents from a generic {@code Object[]}, as {@link PlsqlArray} requires.
     *
     * <p>Delegates to {@link #setArray(Object[])}, which coerces each element to
     * {@code String} or {@code BigDecimal} according to {@code realDataTypeCode} -- so the
     * type code passed to the constructor decides how the values are interpreted, exactly
     * as for every other entry point on this class.
     */
    @Override
    public void setNewValuesAsObject(Object[] newValues) throws CSException {
        setArray(newValues);
    }

    /**
     * The current contents. The {@code Connection} argument is part of the {@link PlsqlArray}
     * contract because a generated collection class may need to talk to the database to
     * resolve its SQL type; an index-by table is a purely client-side PL/SQL construct with
     * no SQL type of its own, so the argument is deliberately ignored and may be null.
     */
    @Override
    public Object[] getCurrentValuesAsObject(Connection theConnection) {
        return (getArray());
    }

    /**
     * An index-by table has no SQL type name -- it exists only inside PL/SQL and is bound
     * element-wise, never as a named collection -- so there is no name to return. The
     * {@link PlsqlArray} contract needs the method; callers that key off the name should
     * treat empty as "not a named SQL collection".
     */
    @Override
    public String getArrayName() {
        return ("");
    }

    /**
     * Set the contents of the Array.
     * This method assumes that the user is providing a 1 dimensional array of
     * numbers or strings. The values of elementMaxCount and elementMaxLength are
     * updated by this method. If you are passing in an array that will be
     * appended and returned to you you should call setElementMaxCount and
     * setElementMaxLength after calling setArray. 'null' is not an acceptable
     * value for dataArray and will be turned into Object[0]. This is so that
     * getArrayLength works reliably.
     *
     * @param newDataArray
     * @see com.mcpdbwizard.pub.PlsqlIndexByTable#setElementMaxCount
     * @see com.mcpdbwizard.pub.PlsqlIndexByTable#setElementMaxLength
     */
    public void setArray(Object[] newDataArray) {

        if (newDataArray == null) {
            if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                this.dataArray = new String[0];
            } else {
                this.dataArray = new BigDecimal[0];
            }
        } else {
            if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                this.dataArray = new String[newDataArray.length];
            } else {
                this.dataArray = new BigDecimal[newDataArray.length];
            }

            for (int i = 0; i < newDataArray.length; i++) {
                if (newDataArray[i] == null) {
                    this.dataArray[i] = null;
                } else {
                    if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                        if (newDataArray[i] instanceof String) {
                            this.dataArray[i] = (String) newDataArray[i];
                        } else {
                            this.dataArray[i] = new String(formatter.format(newDataArray[i]));
                        }
                    } else {
                        // Store as BigDecimal - will get converted to this anyway
                        if (newDataArray[i] instanceof String) {
                            this.dataArray[i] = new BigDecimal((String) newDataArray[i]);
                        } else {
                            this.dataArray[i] = (BigDecimal) newDataArray[i];
                        }
                    }
                }
            }
        }

        // Update element count
        if (this.dataArray.length > elementMaxCount) {
            elementMaxCount = this.dataArray.length;
        }

    }

    /**
     * Set the array data using int[]
     * This convenience method sets the array using an array of int[]
     *
     * @param newArray
     */
    public void setArray(int[] newArray) {
        if (newArray == null) {
            setArray(new Object[0]);
        } else {
            Object[] tempArray = new Object[newArray.length];

            for (int i = 0; i < tempArray.length; i++) {
                if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                    tempArray[i] = new String(formatter.format(newArray[i]));
                } else {
                    // Store as BigDecimal - will get converted to this anyway
                    tempArray[i] = new BigDecimal(newArray[i]);
                }
            }

            setArray(tempArray);
        }

    }

    /**
     * Set the array data using Integer[]
     * This convenience method returns sets the array using an array of Integer[]
     * WARNING: The precision of the value actually stored is limited to what the
     * underlying Oracle object expects, so if you pass 1.00003f into a NUMBER(4,2)
     * the '3' will be lost
     *
     * @param newArray
     */
    public void setArray(Integer[] newArray) {
        if (newArray == null) {
            setArray(new Object[0]);
        } else {
            Object[] tempArray = new Object[newArray.length];

            for (int i = 0; i < tempArray.length; i++) {
                if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                    tempArray[i] = new String(formatter.format(newArray[i]));
                } else {
                    // Store as BigDecimal - will get converted to this anyway
                    tempArray[i] = new BigDecimal(newArray[i].intValue());
                }
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
            //setDataTypeNumber();
        } else {
            Object[] tempArray = new Object[newArray.length];

            for (int i = 0; i < tempArray.length; i++) {
                if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                    tempArray[i] = new String(formatter.format(newArray[i]));
                } else {
                    // Store as BigDecimal - will get converted to this anyway
                    tempArray[i] = new BigDecimal(newArray[i]);
                }
            }

            setArray(tempArray);
        }

    }

    /**
     * Set the array data using Long[]
     * This convenience method returns sets the array using an array of Long[]
     * WARNING: The precision of the value actually stored is limited to what the
     * underlying Oracle object expects, so if you pass 1.00003f into a NUMBER(4,2)
     * the '3' will be lost
     *
     * @param newArray
     */
    public void setArray(Long[] newArray) {
        if (newArray == null) {
            setArray(new Object[0]);
        } else {
            Object[] tempArray = new Object[newArray.length];

            for (int i = 0; i < tempArray.length; i++) {
                if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                    tempArray[i] = new String(formatter.format(newArray[i]));
                } else {
                    // Store as BigDecimal - will get converted to this anyway
                    tempArray[i] = new BigDecimal(newArray[i].longValue());
                }
            }

            setArray(tempArray);
        }

    }

    /**
     * Set the array data using float[]
     * This convenience method returns sets the array using an array of float[]
     * WARNING: The precision of the value actually stored is limited to what the
     * underlying Oracle object expects, so if you pass 1.00003f into a NUMBER(4,2)
     * the '3' will be lost
     *
     * @param newArray
     */
    public void setArray(float[] newArray) {
        if (newArray == null) {
            setArray(new Object[0]);
        } else {
            Object[] tempArray = new Object[newArray.length];

            for (int i = 0; i < tempArray.length; i++) {
                if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                    tempArray[i] = new String(formatter.format(newArray[i]));
                } else {
                    // Store as BigDecimal - will get converted to this anyway
                    tempArray[i] = new BigDecimal(newArray[i]);
                }
            }

            setArray(tempArray);
        }

    }

    /**
     * Set the array data using Float[]
     * This convenience method returns sets the array using an array of Float[]
     * WARNING: The precision of the value actually stored is limited to what the
     * underlying Oracle object expects, so if you pass 1.00003f into a NUMBER(4,2)
     * the '3' will be lost
     *
     * @param newArray
     */
    public void setArray(Float[] newArray) {
        if (newArray == null) {
            setArray(new Object[0]);
        } else {
            Object[] tempArray = new Object[newArray.length];

            for (int i = 0; i < tempArray.length; i++) {
                if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                    tempArray[i] = new String(formatter.format(newArray[i]));
                } else {
                    // Store as BigDecimal - will get converted to this anyway
                    tempArray[i] = new BigDecimal(newArray[i].floatValue());
                }
            }

            setArray(tempArray);
        }

    }

    /**
     * Set the array data using double[]
     * This convenience method returns sets the array using an array of double[]
     * WARNING: The precision of the value actually stored is limited to what the
     * underlying Oracle object expects, so if you pass 1.00003f into a NUMBER(4,2)
     * the '3' will be lost
     *
     * @param newArray
     */
    public void setArray(double[] newArray) {
        if (newArray == null) {
            setArray(new Object[0]);
        } else {
            Object[] tempArray = new Object[newArray.length];

            for (int i = 0; i < tempArray.length; i++) {
                if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                    tempArray[i] = new String(formatter.format(newArray[i]));
                } else {
                    // Store as BigDecimal - will get converted to this anyway
                    tempArray[i] = new BigDecimal(newArray[i]);
                }
            }

            setArray(tempArray);
        }

    }

    /**
     * Set the array data using Double[]
     * This convenience method returns sets the array using an array of Double[]
     * WARNING: The precision of the value actually stored is limited to what the
     * underlying Oracle object expects, so if you pass 1.00003f into a NUMBER(4,2)
     * the '3' will be lost
     *
     * @param newArray
     */
    public void setArray(Double[] newArray) {
        if (newArray == null) {
            setArray(new Object[0]);
        } else {
            Object[] tempArray = new Object[newArray.length];

            for (int i = 0; i < tempArray.length; i++) {
                if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                    tempArray[i] = new String(formatter.format(newArray[i]));
                } else {
                    // Store as BigDecimal - will get converted to this anyway
                    tempArray[i] = new BigDecimal(newArray[i].doubleValue());
                }
            }

            setArray(tempArray);
        }

    }

    /**
     * Set the array data using java.sql.Timestamp[]
     * This convenience method returns sets the array using an array of java.sql.Timestamp[]
     *
     * @param newArray
     * @throws CSNoNanosAllowedException when nanoseconds provided for an Oracle DATE field
     */
    public void setArray(java.sql.Timestamp[] newArray) throws CSNoNanosAllowedException {
        if (newArray == null) {
            setArray(new String[0]);
        } else {
            Object[] tempArray = new String[newArray.length];

            for (int i = 0; i < tempArray.length; i++) {
                if (newArray[i] == null) {
                    tempArray[i] = null;
                } else {
                    if (realDataTypePrecision == 0 && newArray[i].getNanos() != 0) {
                        throw new CSNoNanosAllowedException(
                                "PlsqlIndexByTable2/setArray(java.sql.Timestamp[])", newArray[i]);

                    }
                    if (realDataTypePrecision == 0) {
                        tempArray[i] = theDateFormat.format(newArray[i]);
                    } else {
                        tempArray[i] = new String(newArray[i].toString());
                    }
                }
            }

            setArray(tempArray);
        }

    }

    /**
     * Set the array data using byte[][]
     * This convenience method returns sets the array using an array of byte[][]
     *
     * @param newArray
     */
    public void setArray(byte[][] newArray) {
        if (newArray == null) {
            setArray(new String[0]);
        } else {
            Object[] tempArray = new String[newArray.length];

            for (int i = 0; i < tempArray.length; i++) {
                if (newArray[i] == null) {
                    tempArray[i] = null;
                } else {
                    StringBuffer buffer = new StringBuffer(newArray[i].length * 2);

                    for (int j = 0; j < newArray[i].length; j++) {
                        String hexNumber = null;
                        hexNumber = "0" + Integer.toHexString(0xff & newArray[i][j]);
                        buffer.append(hexNumber.substring(hexNumber.length() - 2));

                    }

                    tempArray[i] = buffer.toString();

                }
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
     * @throws ClassCastException      if the array isn't of numbers.
     * @throws CSNumberFormatException if the array cant be turned into int.
     */
    public int[] getArrayAsInt(int nullToken) throws CSNumberFormatException {
        int[] newArray = new int[0];

        if (dataArray.length > 0) {
            newArray = new int[dataArray.length];

            for (int i = 0; i < dataArray.length; i++) {
                if (dataArray[i] == null) {
                    newArray[i] = nullToken;
                } else {
                    try {
                        if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                            newArray[i] = Integer.parseInt((String) dataArray[i]);
                        } else {
                            newArray[i] = ((BigDecimal) dataArray[i]).intValue();
                        }
                    } catch (NumberFormatException e) {
                        throw new CSNumberFormatException("PlsqlIndexByTable2.getArrayAsInt", (String) dataArray[i]);
                    }
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
    public long[] getArrayAsLong(long nullToken) throws CSNumberFormatException {
        long[] newArray = new long[0];

        if (dataArray.length > 0) {
            newArray = new long[dataArray.length];

            for (int i = 0; i < dataArray.length; i++) {
                if (dataArray[i] == null) {
                    newArray[i] = nullToken;
                } else {
                    try {
                        if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                            newArray[i] = Long.parseLong((String) dataArray[i]);
                        } else {
                            newArray[i] = ((BigDecimal) dataArray[i]).longValue();
                        }
                    } catch (NumberFormatException e) {
                        throw new CSNumberFormatException("PlsqlIndexByTable2.getArrayAsDouble", (String) dataArray[i]);
                    }
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
    public float[] getArrayAsFloat(float nullToken) throws CSNumberFormatException {
        float[] newArray = new float[0];

        if (dataArray.length > 0) {
            newArray = new float[dataArray.length];

            for (int i = 0; i < dataArray.length; i++) {
                if (dataArray[i] == null) {
                    newArray[i] = nullToken;
                } else {
                    try {
                        if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                            newArray[i] = Float.parseFloat((String) dataArray[i]);
                        } else {
                            newArray[i] = ((BigDecimal) dataArray[i]).floatValue();
                        }
                    } catch (NumberFormatException e) {
                        throw new CSNumberFormatException("PlsqlIndexByTable2.getArrayAsDouble", (String) dataArray[i]);
                    }
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
    public double[] getArrayAsDouble(double nullToken) throws CSNumberFormatException {
        double[] newArray = new double[0];

        if (dataArray.length > 0) {
            newArray = new double[dataArray.length];

            for (int i = 0; i < dataArray.length; i++) {
                if (dataArray[i] == null) {
                    newArray[i] = nullToken;
                } else {
                    try {
                        if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                            newArray[i] = Double.parseDouble((String) dataArray[i]);
                        } else {
                            newArray[i] = ((BigDecimal) dataArray[i]).doubleValue();
                        }
                    } catch (NumberFormatException e) {
                        throw new CSNumberFormatException("PlsqlIndexByTable2.getArrayAsDouble", (String) dataArray[i]);
                    }
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
     * @throws CSNumberFormatException if this isn't an array of numbers
     * @since 4.0.2108
     */
    public java.math.BigDecimal[] getArrayAsBigDecimal() throws CSNumberFormatException {
        java.math.BigDecimal[] newArray = new java.math.BigDecimal[0];

        if (dataArray.length > 0) {
            newArray = new java.math.BigDecimal[dataArray.length];

            for (int i = 0; i < dataArray.length; i++) {
                if (dataArray[i] == null) {
                    newArray[i] = null;
                } else {
                    try {
                        if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                            newArray[i] = new BigDecimal((String) dataArray[i]);
                        } else {
                            newArray[i] = (BigDecimal) dataArray[i];
                        }
                    } catch (NumberFormatException e) {
                        throw new CSNumberFormatException("PlsqlIndexByTable2.getArrayAsBigDecimal", (String) dataArray[i]);
                    }
                }
            }
        }

        return (newArray);
    }

    /**
     * Get the array data as Double[]
     * This convenience method returns the contents of the array as an array of BigDecimal.
     *
     * @return Double[] An array Double
     * @throws CSNumberFormatException if this isn't an array of numbers
     * @since 4.0.2108
     */
    public Double[] getArrayAsDoubleObject() throws CSNumberFormatException {
        Double[] newArray = new Double[0];

        if (dataArray.length > 0) {
            newArray = new Double[dataArray.length];

            for (int i = 0; i < dataArray.length; i++) {
                if (dataArray[i] == null) {
                    newArray[i] = null;
                } else {
                    try {
                        if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                            newArray[i] =  Double.valueOf((String) dataArray[i]);
                        } else {
                            newArray[i] = ((BigDecimal) dataArray[i]).doubleValue();
                        }
                    } catch (NumberFormatException e) {
                        throw new CSNumberFormatException("PlsqlIndexByTable2.getArrayAsDoubleObject", (String) dataArray[i]);
                    }
                }
            }
        }

        return (newArray);
    }

    /**
     * Get the array data as Float[]
     * This convenience method returns the contents of the array as an array of BigDecimal.
     *
     * @return Float[] An array of Float
     * @throws CSNumberFormatException if this isn't an array of numbers
     * @since 4.0.2108
     */
    public Float[] getArrayAsFloatObject() throws CSNumberFormatException {
        Float[] newArray = new Float[0];

        if (dataArray.length > 0) {
            newArray = new Float[dataArray.length];

            for (int i = 0; i < dataArray.length; i++) {
                if (dataArray[i] == null) {
                    newArray[i] = null;
                } else {
                    try {
                        if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                            newArray[i] =  Float.valueOf((String) dataArray[i]);
                        } else {
                            newArray[i] = ((BigDecimal) dataArray[i]).floatValue();
                        }
                    } catch (NumberFormatException e) {
                        throw new CSNumberFormatException("PlsqlIndexByTable2.getArrayAsFloatObject", (String) dataArray[i]);
                    }
                }
            }
        }

        return (newArray);
    }

    /**
     * Get the array data as Integer[]
     * This convenience method returns the contents of the array as an array of BigDecimal.
     *
     * @return Integer[] An array of Integer
     * @throws CSNumberFormatException if this isn't an array of numbers
     * @since 4.0.2108
     */
    public Integer[] getArrayAsIntegerObject() throws CSNumberFormatException {
        Integer[] newArray = new Integer[0];

        if (dataArray.length > 0) {
            newArray = new Integer[dataArray.length];

            for (int i = 0; i < dataArray.length; i++) {
                if (dataArray[i] == null) {
                    newArray[i] = null;
                } else {
                    try {
                        if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                            newArray[i] =  Integer.valueOf((String) dataArray[i]);
                        } else {
                            newArray[i] = ((BigDecimal) dataArray[i]).intValue();
                        }
                    } catch (NumberFormatException e) {
                        throw new CSNumberFormatException("PlsqlIndexByTable2.getArrayAsIntegerObject", (String) dataArray[i]);
                    }
                }
            }
        }

        return (newArray);
    }

    /**
     * Get the array data as Long[]
     * This convenience method returns the contents of the array as an array of BigDecimal.
     *
     * @return Long[] An array of Long
     * @throws CSNumberFormatException if this isn't an array of numbers
     * @since 4.0.2108
     */
    public Long[] getArrayAsLongObject() throws CSNumberFormatException {
        Long[] newArray = new Long[0];

        if (dataArray.length > 0) {
            newArray = new Long[dataArray.length];

            for (int i = 0; i < dataArray.length; i++) {
                if (dataArray[i] == null) {
                    newArray[i] = null;
                } else {
                    try {
                        if (realDataTypeCode == oracle.jdbc.OracleTypes.VARCHAR) {
                            newArray[i] =  Long.valueOf((String) dataArray[i]);
                        } else {
                            newArray[i] = ((BigDecimal) dataArray[i]).longValue();
                        }
                    } catch (NumberFormatException e) {
                        throw new CSNumberFormatException("PlsqlIndexByTable2.getArrayAsLongObject", (String) dataArray[i]);
                    }
                }
            }
        }

        return (newArray);
    }

    /**
     * Get the array data as java.sql.Timestamp
     * This convenience method returns the contents of the array as an array of java.sql.Timestamp.
     *
     * @return java.sql.Timestamp[] An array of numbers. If the array is empty you will get java.sql.Timestamp[0] back.
     * @throws ClassCastException if the array isn't of Strings.
     */
    public java.sql.Timestamp[] getArrayAsTimestamp() throws CSException {
        java.sql.Timestamp[] newArray = new java.sql.Timestamp[0];

        if (dataArray.length > 0) {
            newArray = new java.sql.Timestamp[dataArray.length];

            for (int i = 0; i < dataArray.length; i++) {
                if (dataArray[i] == null) {
                    newArray[i] = null;
                } else {
                    try {
                        newArray[i] = java.sql.Timestamp.valueOf((String) dataArray[i]);
                    } catch (Exception e) {
                        throw new CSException("PlsqlIndexByTable2/getArrayAsTimestamp: string could not be turned into a timestamp: " + dataArray[i]);
                    }
                }
            }
        }

        return (newArray);
    }

    /**
     * Get the array data as byte[]
     * This convenience method returns the contents of the array as an array of byte[].
     *
     * @return byte[][] An array of numbers. If the array is empty you will get byte[][0] back.
     * @throws ClassCastException if the array isn't of numbers.
     */
    public byte[][] getArrayAsRaw() {
        byte[][] newArray = new byte[0][0];

        if (dataArray.length > 0) {
            newArray = new byte[dataArray.length][];

            for (int i = 0; i < dataArray.length; i++) {
                if (dataArray[i] == null) {
                    newArray[i] = null;
                } else {
                    String tempString = (String) dataArray[i];
                    newArray[i] = new byte[tempString.length() / 2];

                    for (int j = 0; j < newArray[i].length; j++) {
                        newArray[i][j] = (byte) Integer.parseInt(tempString.substring(j * 2, (j * 2) + 2), 16);
                    }

                }
            }
        }

        return (newArray);
    }

    /**
     * Give every element a fractional-seconds part, so that a zoned conversion mask will accept it.
     *
     * <p>Generated code calls this on a {@code TIMESTAMP WITH [LOCAL] TIME ZONE} index-by table
     * immediately before binding, and on nothing else. It exists because
     * {@link #ORACLE_TIMESTAMPTZ_TO_CHAR_MASK} carries a {@code TZR} element, and Oracle stops
     * tolerating a missing fraction once the mask names a zone: {@code '2019-03-01 14:25:36'}
     * parses under the unzoned mask and raises ORA-01843 under the zoned one. Rather than weaken
     * the mask -- which would cost region-name support, the half of the fix that matters across a
     * daylight-saving boundary -- the value is made acceptable here.
     *
     * <p>The transformation is deliberately narrow: it appends {@code .0} to the <em>time</em>
     * token of a {@code date time [zone]} string that has no {@code .} in that token, and leaves
     * everything else exactly as it was. So {@code '2019-03-01 14:25:36 +05:30'} becomes
     * {@code '2019-03-01 14:25:36.0 +05:30'} -- the zone is not touched and cannot be reordered --
     * while a value that already carries a fraction, a null, a non-String element, or anything that
     * does not look like a timestamp at all is passed through untouched. **It must not try to
     * validate**: a malformed value should still reach Oracle and be rejected there, with Oracle's
     * own message, rather than be silently altered into something that parses.
     *
     * @since 2.0.0
     */
    public void ensureFractionalSeconds() {
        if (dataArray == null) {
            return;
        }

        for (int i = 0; i < dataArray.length; i++) {
            if (!(dataArray[i] instanceof String)) {
                continue;
            }

            String theValue = (String) dataArray[i];
            // date, time, and optionally a zone -- anything else is left alone rather than guessed at.
            int firstSpace = theValue.indexOf(' ');
            if (firstSpace < 0) {
                continue;
            }

            int secondSpace = theValue.indexOf(' ', firstSpace + 1);
            String theTime = (secondSpace < 0)
                    ? theValue.substring(firstSpace + 1)
                    : theValue.substring(firstSpace + 1, secondSpace);

            if (theTime.indexOf('.') > -1 || !looksLikeAClockTime(theTime)) {
                continue;
            }

            dataArray[i] = (secondSpace < 0)
                    ? theValue + ".0"
                    : theValue.substring(0, secondSpace) + ".0" + theValue.substring(secondSpace);
        }
    }

    /**
     * Is this token a clock time -- digits and colons, with at least one colon?
     *
     * <p>The guard that keeps {@link #ensureFractionalSeconds()} from editing a value it does not
     * understand. Without it the second token of any three-token string gets a {@code .0} appended,
     * so a malformed value would be altered on its way to Oracle and refused with a message
     * describing the altered text rather than what the caller actually passed.
     *
     * <p>Deliberately NOT a full validation: {@code 99:99:99} passes here and is Oracle's problem,
     * which is the correct division of labour. This only has to be sure the token is the shape a
     * fraction can be appended to.
     */
    private static boolean looksLikeAClockTime(String theToken) {
        if (theToken.length() == 0 || theToken.indexOf(':') < 0) {
            return (false);
        }

        for (int i = 0; i < theToken.length(); i++) {
            char c = theToken.charAt(i);
            if (c != ':' && (c < '0' || c > '9')) {
                return (false);
            }
        }

        return (true);
    }

    /**
     * Get the length of the array
     */
    public int getArrayLength() {
        return (dataArray.length);
    }
}



