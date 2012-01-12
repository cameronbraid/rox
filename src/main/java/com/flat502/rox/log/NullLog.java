package com.flat502.rox.log;

import com.flat502.rox.log.Log;

/**
 * A {@link com.flat502.rox.log.Log} implementation that swallows
 * all logging and returns false for all logging level checks.
 * <p>
 * This simplifies writing code that would otherwise have to check
 * for a null log handle everywhere.
 */
public class NullLog implements Log {
	public void trace(String msg) {
	}

	public void trace(String msg, Throwable e) {
	}

	public void debug(String msg) {
	}

	public void debug(String msg, Throwable e) {
	}

	public void info(String msg) {
	}

	public void info(String msg, Throwable e) {
	}

	public void warn(String msg) {
	}

	public void warn(String msg, Throwable e) {
	}

	public void error(String msg) {
	}

	public void error(String msg, Throwable e) {
	}

	public boolean logTrace() {
		return false;
	}

	public boolean logDebug() {
		return false;
	}

	public boolean logInfo() {
		return false;
	}

	public boolean logWarn() {
		return false;
	}

	public boolean logError() {
		return false;
	}
}
