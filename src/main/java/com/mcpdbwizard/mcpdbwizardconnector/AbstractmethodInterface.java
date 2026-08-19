package com.mcpdbwizard.mcpdbwizardconnector;

import com.mcpdbwizard.pub.LogInterface;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public interface AbstractmethodInterface {

	public String getMethodName();

	public void setMethodName(String methodName);

	public String getMethodReturn();

	public void setMethodReturn(String methodReturn);

	public String[][] getMethodParams();

	public void setMethodParams(String[][] methodParams);

	public String[] getMethodExceptions();

	public void setMethodExceptions(String[] methodExceptions);

	public String[] getMethod(LogInterface theLog);

}