package com.flat502.rox.client;

import java.io.IOException;

/**
 * An exception raised when a synchronous RPC method call fails.
 * <p>
 * This exception is only raised if the execution time of a 
 * {@link com.flat502.rox.client.HttpRpcClient#execute(String, Object[]) synchronous}
 * RPC call fails for some reason other than a timeout.
 */
public class RpcCallFailedException extends IOException {
	public RpcCallFailedException(Throwable cause) {
		super("RPC call failed");
		this.initCause(cause);
	}
}
