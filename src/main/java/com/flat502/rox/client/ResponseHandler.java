package com.flat502.rox.client;

import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.RpcResponse;

/**
 * This interface represents an RPC response handler.
 * <p>
 * Callers that want to be notified asynchronously when
 * an RPC method call returns should pass an implementation
 * of this interface to the 
 * {@link com.flat502.rox.client.HttpRpcClient#execute(String, Object[], Class, ResponseHandler)}
 * or
 * {@link com.flat502.rox.client.HttpRpcClient#execute(String, Object[], ResponseHandler)}
 * methods.
 * @deprecated Use {@link com.flat502.rox.client.AsynchronousResponseHandler} instead.
 */
public interface ResponseHandler {
	/**
	 * This method is called when a successful response is received from the
	 * server.
	 * <P>
	 * A successful response is defined as one that contains a return value
	 * that is not an RPC fault.
	 * @param call
	 * 	The original method call that this response applies to.
	 * @param rsp
	 * 	The response to the method call.
	 */
	public void handleResponse(RpcCall call, RpcResponse rsp);
	
	/**
	 * This method is called for both local and remote exceptions.
	 * <p>
	 * A local exception is one that is raised within this JVM as a result 
	 * of a failure while handling either the request or response. This may
	 * be an instance of any sub-class of {@link Throwable} <i>other</i>
	 * than {@link com.flat502.rox.processing.RpcFaultException}.
	 * <p>
	 * A remote exception is always an instance of 
	 * {@link com.flat502.rox.processing.RpcFaultException}
	 * wrapping an RPC fault response.
	 * @param call
	 * 	The original method call that this response applies to.
	 * @param e
	 * 	An instance of {@link com.flat502.rox.processing.RpcFaultException} 
	 * 	for a remote exception. For a local exception any other exception type.
	 */
	public void handleException(RpcCall call, Throwable e);
}
