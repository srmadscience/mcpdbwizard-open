package com.mcpdbwizard.app.common;

import java.util.*;

/**
 * @author devteam@mcpdbwizard.com
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 * @version 2
 */
public class TextChunk {

    ArrayList textList = null;

    public TextChunk() {
        clear();
    }

    public void clear() {
        if (textList != null) {
            textList.clear();
        }

        textList = new ArrayList(10);
    }

    public void addLine(String newLine) {
        textList.add(new String(newLine));
    }

    public void addLine(String[] newLines) {
        for (int i = 0; i < newLines.length; i++) {
            textList.add(new String(newLines[i]));
        }

    }

    public void addFile(java.io.File aFile) throws com.mcpdbwizard.pub.CSException {
        textList.add(JavaUtils.readFileIntoString(aFile));
    }

    public String[] getLines() {
        textList.trimToSize();
        String[] outputLines = new String[textList.size()];
        outputLines = (String[]) textList.toArray(outputLines);
        return (outputLines);
    }

    public String[] getLines(String oldVal, String newVal) {
        textList.trimToSize();
        String[] outputLines = new String[textList.size()];
        outputLines = (String[]) textList.toArray(outputLines);

        if (oldVal != null) {
            for (int i = 0; i < outputLines.length; i++) {
                outputLines[i] = outputLines[i].replaceAll(oldVal, newVal);
            }
        }
        return (outputLines);
    }

    public void appendToLastLine(String newBit) {
        textList.trimToSize();
        String currentLastLine = (String) textList.get(textList.size() - 1);
        currentLastLine = currentLastLine + newBit;
        textList.set(textList.size() - 1, currentLastLine);
    }
}


