package com.flat502.rox.client;

import java.io.IOException;

/**
 * An exception raised when an RPC method call times out.
 * <p>
 * This exception is only raised if the execution time of a 
 * {@link com.flat502.rox.client.HttpRpcClient#execute(String, Object[]) synchronous}
 * RPC call exceeds the timeout set by calling 
 * {@link com.flat502.rox.client.HttpRpcClient#setRequestTimeout(long)}.
 */
public class RpcCallTimeoutException extends IOTimeoutException {
	public RpcCallTimeoutException() {
		super("RPC call timed out");
	}

	public RpcCallTimeoutException(Throwable cause) {
		super("RPC call timed out");
		if (cause != null) {
			this.initCause(cause);
		}
	}
}
