package com.flat502.rox.log.slf4j;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.flat502.rox.log.Log;

/**
 */
public class Slf4jAdapter implements Log {
	private final Logger logger;
	public Slf4jAdapter(String name) {
		logger = LoggerFactory.getLogger(name);
	}
	public Slf4jAdapter(Class clazz) {
		logger = LoggerFactory.getLogger(clazz);
	}

	public boolean logTrace() {
		return logger.isTraceEnabled();
	}

	public boolean logDebug() {
		return logger.isDebugEnabled();
	}

	public boolean logInfo() {
		return logger.isInfoEnabled();
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
		logger.trace(msg);
	}

	public void trace(String msg, Throwable e) {
		logger.trace(msg, e);
	}

	public void debug(String msg) {
		logger.debug(msg);
	}

	public void debug(String msg, Throwable e) {
		logger.debug(msg, e);
	}

	public void info(String msg) {
		logger.info(msg);
	}

	public void info(String msg, Throwable e) {
		logger.info(msg, e);
	}

	public void warn(String msg) {
		logger.warn(msg);
	}

	public void warn(String msg, Throwable e) {
		logger.warn(msg, e);
	}

	public void error(String msg) {
		logger.error(msg);
	}

	public void error(String msg, Throwable e) {
		logger.error(msg, e);
	}
}
