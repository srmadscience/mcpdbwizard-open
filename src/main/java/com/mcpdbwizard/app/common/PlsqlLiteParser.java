package com.mcpdbwizard.app.common;

/**
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 2
 */
public class PlsqlLiteParser {

    String[] plsqlCode;

    boolean isARowType = false;
    boolean isASubType = false;

    public PlsqlLiteParser(String[] plsqlCode) {
        this.plsqlCode = new String[plsqlCode.length];
        for (int i = 0; i < plsqlCode.length; i++) {
            this.plsqlCode[i] = plsqlCode[i].toUpperCase();
            if (this.plsqlCode[i].indexOf("--") >= 0) {
                this.plsqlCode[i] = this.plsqlCode[i].substring(0, this.plsqlCode[i].indexOf("--"));
            }
        }

    }

    public boolean getIsARowType() {
        return (isARowType);
    }

    public boolean getIsASubType() {
        return (isASubType);
    }

    public String getRowType(String procOrFunc, String procName, String argName) {
        String rowType = null;
        //int currentCol = 0;
        //int currentRow = 0;
        int highestRowChecked = 0;
        isARowType = false;
        isASubType = false;

        //char[] code;
        for (int i = 0; i < plsqlCode.length && rowType == null && highestRowChecked < plsqlCode.length; i++) {
            String testString = JavaUtils.removeComments(plsqlCode[i]);

            for (int j = i + 1; j < plsqlCode.length && testString.length() < 200000; j++) {
                String newString = JavaUtils.removeComments(plsqlCode[j]);
                testString = testString + " " + JavaUtils.removeDupSpaces(JavaUtils.replaceString(newString, "(", " ("));
                highestRowChecked = (j + 1);
            }


            boolean notFinished = true;

            while (notFinished) {
                int procIndex = testString.indexOf(procOrFunc);
                if (procIndex == -1) {
                    notFinished = false;
                    break;
                }

                testString = testString.substring(procIndex + procOrFunc.length());

                int procedureIndex = testString.indexOf("PROCEDURE ");
                if (procedureIndex == -1) {
                    procedureIndex = Integer.MAX_VALUE;
                }

                int functionIndex = testString.indexOf("FUNCTION ");
                if (functionIndex == -1) {
                    functionIndex = Integer.MAX_VALUE;
                }

                int procNameIndex = testString.indexOf(procName);

                if (procNameIndex == -1) {
                    notFinished = false;
                    break;
                }

                if (procNameIndex < procedureIndex
                        && procNameIndex < functionIndex) {
                    int argNameIndex = testString.indexOf(argName);
                    if (argNameIndex == -1) {
                        notFinished = false;
                        break;
                    }

                    int rowtypeIndex = testString.indexOf("%ROWTYPE", argNameIndex + argName.length());

                    if (rowtypeIndex == -1) {
                        rowtypeIndex = Integer.MAX_VALUE;
                    }

                    // Settle for a semi-colon or a comma...
                    int indexOfSemiColon = testString.indexOf(";", argNameIndex + argName.length());
                    int indexOfComma = testString.indexOf(",", argNameIndex + argName.length());
                    int indexOfRightBracket = testString.indexOf(")", argNameIndex + argName.length());

                    if (indexOfSemiColon == -1) {
                        indexOfSemiColon = Integer.MAX_VALUE;
                    }

                    if (indexOfComma == -1) {
                        indexOfComma = Integer.MAX_VALUE;
                    }

                    if (indexOfRightBracket == -1) {
                        indexOfRightBracket = Integer.MAX_VALUE;
                    }

                    if (rowtypeIndex < indexOfSemiColon
                            && rowtypeIndex < indexOfComma
                            && rowtypeIndex < indexOfRightBracket) {
                        isARowType = true;
                    }

                    if (indexOfComma < rowtypeIndex) {
                        rowtypeIndex = indexOfComma;
                    }

                    if (indexOfSemiColon < rowtypeIndex) {
                        rowtypeIndex = indexOfSemiColon;
                    }

                    if (indexOfRightBracket < rowtypeIndex) {
                        rowtypeIndex = indexOfRightBracket;
                    }

                    if (rowtypeIndex == Integer.MAX_VALUE) {
                        rowtypeIndex = 0;
                    }


                    rowType = testString.substring(argNameIndex + argName.length(), rowtypeIndex);
                    rowType = rowType.trim();

                    if (rowType.indexOf(" ") > -1) {
                        rowType = rowType.substring(rowType.lastIndexOf(" ") + 1);
                    }
                    notFinished = false;
                    break;

                }

            }

        }


        return (rowType);
    }

    public String getRowTypeofType(String typeName) {
        final String TYPE = "TYPE ";
        final String TABLE = "TABLE ";
        final String VARRAY = "VARRAY ";
        final String OF = "OF ";
        final String ROWTYPE = "%ROWTYPE";

        String rowType = null;
        //int currentCol = 0;
        //int currentRow = 0;
        int highestRowChecked = 0;
        isARowType = false;
        isASubType = false;

        //char[] code;
        for (int i = 0; i < plsqlCode.length && rowType == null && highestRowChecked < plsqlCode.length; i++) {
            String testString = JavaUtils.removeComments(plsqlCode[i]);

            for (int j = i + 1; j < plsqlCode.length && testString.length() < 200000; j++) {
                //String tempString = new String(plsqlCode[j]);
                String newString = JavaUtils.removeComments(plsqlCode[j]);
                testString = testString + " " + JavaUtils.removeDupSpaces(JavaUtils.replaceString(newString, "(", " ("));
                //  highestRowChecked = (j+1);
            }

            boolean notFinished = true;

            while (notFinished) {
                int procIndex = testString.indexOf(TYPE);
                if (procIndex == -1) {
                    notFinished = false;
                    break;
                }

                int ourTypeIndex = testString.indexOf("TYPE " + typeName + " ");
                if (ourTypeIndex == -1) {
                    notFinished = false;
                    break;
                }

                //testString = testString.substring(procIndex + TYPE.length());
                testString = testString.substring(ourTypeIndex);
                ourTypeIndex = 0;

                int tableIndex = testString.indexOf(TABLE);
                if (tableIndex == -1) {
                    tableIndex = Integer.MAX_VALUE;
                }

                int varrayIndex = testString.indexOf(VARRAY);
                if (varrayIndex == -1) {
                    varrayIndex = Integer.MAX_VALUE;
                }

                int ofIndex = testString.indexOf(OF);
                if (ofIndex == -1) {
                    ofIndex = Integer.MAX_VALUE;
                }

                int rowtypeIndex = testString.indexOf(ROWTYPE);
                if (rowtypeIndex == -1) {
                    rowtypeIndex = Integer.MAX_VALUE;
                }

                procIndex = testString.indexOf(TYPE);


                if ((ourTypeIndex < tableIndex || ourTypeIndex < varrayIndex)
                        && (ofIndex > tableIndex || ofIndex > varrayIndex)
                        && ofIndex < rowtypeIndex
                        && ourTypeIndex < Integer.MAX_VALUE
                        && (tableIndex < Integer.MAX_VALUE || varrayIndex < Integer.MAX_VALUE)
                        && ofIndex < Integer.MAX_VALUE
                        && rowtypeIndex < Integer.MAX_VALUE) {
                    // We must be looking at "TYPE OURTYPE ... [VARRAY|TABLE] ... OF ... %ROWTYPE
                    rowType = testString.substring(ofIndex + OF.length());
                    rowType = rowType.substring(0, rowType.indexOf(ROWTYPE));
                    rowType = JavaUtils.replaceString(rowType, "%", "");
                    rowType = JavaUtils.replaceString(rowType, ";", "");

                    notFinished = false;
                    break;
                }

                //
                testString = testString.substring(typeName.length() + 5 /* 'TYPE ' */);


            }

        }


        return (rowType);
    }
}


