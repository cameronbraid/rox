package com.flat502.rox.server;

import java.io.IOException;

import com.flat502.rox.http.HttpResponseException;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.RpcResponse;

/**
 * An interface for delivering asynchronous RPC method responses.
 * <p>
 * An instance of this is passed to the
 * {@link com.flat502.rox.server.AsynchronousRequestHandler#handleRequest(RpcCall, RpcCallContext, ResponseChannel)}
 * method. When a response is available it may be sent using this class. This makes it
 * possible to hand work off to application threads, leaving the calling thread
 * to return to the task of handling HTTP responses.
 */
public interface ResponseChannel {
	/**
	 * Deliver an RPC method response to a remote caller.
	 * @param rsp
	 * 	The response to marshal and send back.
	 * @throws IOException
	 * 	An exception may be raised while attempting to
	 * 	send the response to the client.
	 * @throws MarshallingException 
	 */
	public void respond(RpcResponse rsp) throws IOException, MarshallingException;

	/**
	 * Deliver an HTTP error response to a remote caller.
	 * @param e
	 * 	The response to marshal and send back.
	 * @throws IOException
	 * 	An exception may be raised while attempting to
	 * 	send the response to the client.
	 */
	public void respond(HttpResponseException e) throws IOException;
	
	/**
	 * Close the channel on which the original RPC method call
	 * was received.
	 * <p>
	 * Implementations are not required to close this channel
	 * under normal operations. Typically closing this channel is
	 * only required when an error occurs that may corrupt this
	 * channel's state.
	 * @throws IOException
	 * 	An exception may be raised while attempting to close
	 * 	the underlying channel.
	 */
	public void close() throws IOException;
}
