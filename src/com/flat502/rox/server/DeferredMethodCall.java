package com.flat502.rox.server;

import java.io.IOException;

import com.flat502.rox.http.HttpResponseException;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.RpcCall;

/**
 * A utility class that pairs up an {@link com.flat502.rox.server.RpcMethodProxy}
 * instance with an {@link com.flat502.rox.server.AsyncRequestHandler asynchronous
 * method call}.
 * <p>
 * This class provides proxying of plain old Java objects in a form that is
 * compatible with an {@link com.flat502.rox.server.AsyncRequestHandler}
 * implementation.
 */
public class DeferredMethodCall {
	private RpcCall call;
	private ResponseChannel rspChannel;
	private RpcMethodProxy proxy;

	public DeferredMethodCall(RpcMethodProxy proxy, RpcCall call,
			ResponseChannel rspChannel) {
		this.call = call;
		this.rspChannel = rspChannel;
		this.proxy = proxy;
	}

	public void invoke() throws IOException, MarshallingException {
		try {
			this.rspChannel.respond(this.proxy.invoke(this.call));
		} catch (HttpResponseException e) {
			this.rspChannel.respond(e);
		} catch (Exception e) {
			this.rspChannel.respond(this.proxy.newRpcFault(e));
		}
	}
}
