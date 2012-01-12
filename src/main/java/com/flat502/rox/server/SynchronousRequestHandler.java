package com.flat502.rox.server;

import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.RpcResponse;
import com.flat502.rox.processing.SSLSession;

/**
 * Interface for synchronous RPC method call handlers.
 */
public interface SynchronousRequestHandler extends RequestHandler {
	/**
	 * Invoked to handle a method call.
	 * <p>
	 * This method is responsible for processing the method
	 * synchronously. The return value will be marshalled
	 * and sent back to the client immediately.
	 * <p>
	 * The caller is one of the underlying worker threads
	 * (see {@link com.flat502.rox.processing.HttpRpcProcessor#addWorker()})
	 * and as such should process the method as quickly as possible.
	 * <p>
	 * If processing might be length consider using
	 * {@link AsynchronousRequestHandler} and handing off the work to
	 * an application thread.
	 * <p>
	 * If an exception is raised it will be returned to the
	 * caller as an RPC fault.
	 * @param call
	 * 	The method call to be handled.
	 * @param context
	 * 	An {@link RpcCallContext} instance providing information about
	 * 	the call context.
	 * @return
	 * 	An appropriate RPC response.
	 * @throws Exception
	 * 	Implementations are permitted to raise
	 * 	an exception as part of their processing.
	 */
	RpcResponse handleRequest(RpcCall call, RpcCallContext context) throws Exception;
}
