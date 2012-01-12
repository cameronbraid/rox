package com.flat502.rox.marshal;

/**
 * An interface representing a generalized RPC method
 * call response.
 * <p>
 * This interface is patterned after XML-RPC and essentially
 * encapsulates a single return value.
 */
public interface RpcResponse extends RpcMethod {
	/**
	 * Get the return value for this method call
	 * response.
	 * <p>
	 * The range of types this method may return
	 * is dependent on the underlying implementation.
	 * <p>
	 * Implementations are free to return <code>null</code>
	 * but, once again, whether or this is supported
	 * is implementation-dependent.
	 * @return
	 * 	The return value for this method call
	 * 	response.
	 */
	Object getReturnValue();
}
