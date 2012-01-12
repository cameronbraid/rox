package com.flat502.rox.log;

/**
 * Abstracts logging so that we don't have to depend on something like
 * log4j but callers can easily plug us into their own logging framework. 
 * <p>
 * Five levels are supported, in order of increasing "severity":
 * <ol>
 * <li>Error</li>
 * <li>Warning</li>
 * <li>Info</li>
 * <li>Debug</li>
 * <li>Trace</li>
 * </ol>
 * <p>
 * This deliberately mimics the levels available in log4j, it being
 * the most widely deployed logging framework (and
 * reasonably representative of such frameworks).
 * @see com.flat502.rox.log.LogFactory
 */
public interface Log {
	// Actual logging methods
	public void trace(String msg);
	public void trace(String msg, Throwable e);
	public void debug(String msg);
	public void debug(String msg, Throwable e);
	public void info(String msg);
	public void info(String msg, Throwable e);
	public void warn(String msg);
	public void warn(String msg, Throwable e);
	public void error(String msg);
	public void error(String msg, Throwable e);
	
	// Methods to check if logging is enabled at a given
	// level. Allows callers to avoid constructing expensive
	// strings only to have them discarded.
	public boolean logTrace();
	public boolean logDebug();
	public boolean logInfo();
	public boolean logWarn();
	public boolean logError();
}
