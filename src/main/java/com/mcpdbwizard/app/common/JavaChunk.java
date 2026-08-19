package com.mcpdbwizard.app.common;

/**
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 *
 * @author devteam@mcpdbwizard.com
 * @version 7
 */
public class JavaChunk extends TextChunk {
    static final int CODE_MODE = 0;
    static final int COMMENT_MODE = 1;

    int currentMode = CODE_MODE;
    int indent = 0;
    int indentDelta = 2;

    String blanks = "                                                                                                         ";

    public JavaChunk() {
        super();
    }

    public void indent() {
        indent = indent + indentDelta;
    }

    public void unIndent() {
        indent = indent - indentDelta;
        if (indent < 0) {
            indent = 0;
        }
    }

    public void startComment() {
        currentMode = COMMENT_MODE;
        addLine("/**");
    }

    public void endComment() {
        currentMode = CODE_MODE;
        addLine("*/");
    }

    public void shortComment(String theComment) {
        addLine(blanks.substring(0, indent) + "// " + theComment);
    }

    public void append(String[] theLines) {
        for (int i = 0; i < theLines.length; i++) {
            addLine(theLines[i]);
        }

    }

    public void print(String theLine) {
        if (currentMode == CODE_MODE) {
            addLine(blanks.substring(0, indent) + theLine);
        } else if (currentMode == COMMENT_MODE) {
            addLine("* " + theLine);
        }
    }

    public void doIf(String theLine) {
        print("if (" + theLine + ")");
        indent();
        print("{");
    }

    public void doEndIf() {
        unIndent();
        print("}");
    }

    public void skip() {
        print("");
    }

    public void param(String paramDataType, String paramName, String desc) {
        print(" @param " + paramDataType + " " + paramName + " " + desc);
    }

    public void returnParam(String paramDataType, String desc) {
        print(" @return " + paramDataType + " " + desc);
    }

}



