package com.flat502.rox.server;

import java.net.URI;

import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.RpcResponse;

/**
 * Interface for asynchronous RPC method call handlers.
 * @see com.flat502.rox.server.HttpRpcServer#registerHandler(String, String, AsyncRequestHandler)
 * @deprecated Use {@link com.flat502.rox.server.AsynchronousRequestHandler} instead. 
 */
public interface AsyncRequestHandler extends RequestHandler {
	/**
	 * Invoked to handle a method call.
	 * <p>
	 * This method is responsible for processing the method
	 * asynchronously. Responses can be sent using the
	 * {@link ResponseChannel#respond(RpcResponse)}
	 * method.
	 * <p>
	 * Although an implement could do all of its work using
	 * the calling thread the intention of this interface is
	 * to support handing off the work to an application thread.
	 * <p>
	 * The caller is one of the underlying worker threads
	 * (see {@link com.flat502.rox.processing.HttpRpcProcessor#addWorker()})
	 * and as such should process the method as quickly as possible.
	 * <p>
	 * If an exception is raised it will be returned to the
	 * caller as an RPC fault.
	 * @param call
	 * 	The method call to be handled.
	 * @param rspChannel
	 * 	A handle to a logic channel that can be used
	 * 	when a response is ready.
	 * @throws Exception
	 * 	Implementations are permitted to raise
	 * 	an exception as part of their processing.
	 */
	void handleRequest(RpcCall call, ResponseChannel rspChannel) throws Exception;
}
