package com.flat502.rox.processing;

import java.io.IOException;

/**
 * Raised when a read is interrupted because the remote entity closed the connection
 * cleanly.
 * <p>
 * This is intended to make it possible to differentiate between the remote entity
 * closing the connection as a result of an error (or abnormal termination) and closing
 * the connection cleanly (typically to limit the number of active connections).
 */
public class RemoteSocketClosedException extends IOException {
	public RemoteSocketClosedException(String msg) {
		super(msg);
	}
}
