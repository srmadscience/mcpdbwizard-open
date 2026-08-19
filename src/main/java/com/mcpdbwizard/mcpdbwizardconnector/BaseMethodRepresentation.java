package com.mcpdbwizard.mcpdbwizardconnector;

import com.mcpdbwizard.pub.LogInterface;

/**
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public class BaseMethodRepresentation implements AbstractmethodInterface {

	public String methodName = "";
	public String methodReturn = "";
	public String[][] methodParams = null;
	public String[] methodExceptions = {};

	/*
	 * (non-Javadoc)
	 *
	 * @see com.mcpdbwizard.mcpdbwizardconnector.AbstractmethodInterface#getMethodName()
	 */
	// @Override
	@Override
	public String getMethodName() {
		return methodName;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.mcpdbwizard.mcpdbwizardconnector.AbstractmethodInterface#setMethodName(java.
	 * lang.String)
	 */
	// @Override
	@Override
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.mcpdbwizard.mcpdbwizardconnector.AbstractmethodInterface#getMethodReturn()
	 */
	// @Override
	@Override
	public String getMethodReturn() {
		return methodReturn;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.mcpdbwizard.mcpdbwizardconnector.AbstractmethodInterface#setMethodReturn(java.
	 * lang.String)
	 */
	// @Override
	@Override
	public void setMethodReturn(String methodReturn) {
		this.methodReturn = methodReturn;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.mcpdbwizard.mcpdbwizardconnector.AbstractmethodInterface#getMethodParams()
	 */
	// @Override
	@Override
	public String[][] getMethodParams() {
		return methodParams;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.mcpdbwizard.mcpdbwizardconnector.AbstractmethodInterface#setMethodParams(java.
	 * lang.String[][])
	 */
	// @Override
	@Override
	public void setMethodParams(String[][] methodParams) {
		this.methodParams = methodParams;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.mcpdbwizard.mcpdbwizardconnector.AbstractmethodInterface#getMethodExceptions()
	 */
	// @Override
	@Override
	public String[] getMethodExceptions() {
		return methodExceptions;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.mcpdbwizard.mcpdbwizardconnector.AbstractmethodInterface#setMethodExceptions(
	 * java.lang.String[])
	 */
	// @Override
	@Override
	public void setMethodExceptions(String[] methodExceptions) {
		this.methodExceptions = methodExceptions;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.mcpdbwizard.mcpdbwizardconnector.AbstractmethodInterface#getMethod(com.
	 * mcpdbwizard.pub.LogInterface)
	 */
	// @Override
	@Override
	public String[] getMethod(LogInterface theLog) {
		String[] emptyArray = { "" };

		return emptyArray;
	}

}
