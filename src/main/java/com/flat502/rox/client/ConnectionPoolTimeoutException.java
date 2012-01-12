package com.flat502.rox.client;

import java.io.IOException;

/**
 * An exception raised to indicate a timeout occurred waiting for
 * a connection from the underlying connection pool.
 * <p>
 * This exception is only raised if a non-zero 
 * {@link com.flat502.rox.client.HttpRpcClient#setConnectionPoolLimit(int) limit} 
 * and 
 * {@link com.flat502.rox.client.HttpRpcClient#setConnectionPoolTimeout(long) timeout}
 * have been set on the underling connection pool, a connection is requested after
 * the limit has been reached, and a connection does not become available within
 * the specified timeout interval.
 */
public class ConnectionPoolTimeoutException extends IOTimeoutException {
	public ConnectionPoolTimeoutException(Throwable cause) {
		super("Timed out waiting for a connection");
		super.initCause(cause);
	}
}
