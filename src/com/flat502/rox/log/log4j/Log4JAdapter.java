package com.flat502.rox.log.log4j;

import org.apache.log4j.Logger;

import com.flat502.rox.log.Log;

/**
 * A <a href="http://logging.apache.org/log4j">Log4J</a> adapter.
 * <p>
 * This class implements the {@link com.flat502.rox.log.Log} interface
 * and defers all logging to the Log$J {@link org.apache.log4j.Logger}
 * class.
 * <p>
 * The <code>context</code> parameter on each of the methods on the
 * {@link com.flat502.rox.log.Log} interface is used to retrieve the
 * appropriate Log4J logger in all cases exception {@link #logWarn()}
 * and {@link #logError()}. Those methods always return <code>true</code>
 * (Log4J always assumes warnings and errors should be logged).
 */
public class Log4JAdapter implements Log {
	private String name;

	public Log4JAdapter(String name) {
		this.name = name;
	}

	public boolean logTrace() {
		return Logger.getLogger(name).isTraceEnabled();
	}

	public boolean logDebug() {
		return Logger.getLogger(name).isDebugEnabled();
	}

	public boolean logInfo() {
		return Logger.getLogger(name).isInfoEnabled();
	}

	/**
	 * @return
	 * 	Always returns <code>true</code>
	 */
	public boolean logWarn() {
		return true;
	}

	/**
	 * @return
	 * 	Always returns <code>true</code>
	 */
	public boolean logError() {
		return true;
	}

	public void trace(String msg) {
		Logger.getLogger(name).trace(msg);
	}

	public void trace(String msg, Throwable e) {
		Logger.getLogger(name).trace(msg, e);
	}

	public void debug(String msg) {
		Logger.getLogger(name).debug(msg);
	}

	public void debug(String msg, Throwable e) {
		Logger.getLogger(name).debug(msg, e);
	}

	public void info(String msg) {
		Logger.getLogger(name).info(msg);
	}

	public void info(String msg, Throwable e) {
		Logger.getLogger(name).info(msg, e);
	}

	public void warn(String msg) {
		Logger.getLogger(name).warn(msg);
	}

	public void warn(String msg, Throwable e) {
		Logger.getLogger(name).warn(msg, e);
	}

	public void error(String msg) {
		Logger.getLogger(name).error(msg);
	}

	public void error(String msg, Throwable e) {
		Logger.getLogger(name).error(msg, e);
	}
}
