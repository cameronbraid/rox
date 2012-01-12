package com.flat502.rox.server;

import com.flat502.rox.marshal.FieldNameCodec;
import com.flat502.rox.marshal.MethodCallUnmarshallerAid;
import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.RpcResponse;
import com.flat502.rox.processing.SSLSession;

/**
 * A {@link com.flat502.rox.server.SynchronousRequestHandler} implementation
 * that maps RPC method calls onto methods on an arbitrary
 * object using reflection.
 * <p>
 * Sub-classes provide the concrete instance of
 * {@link com.flat502.rox.server.RpcMethodProxy} and the
 * rest is handled by delegating appropriately to that instance.
 */
public abstract class ProxyingRequestHandler extends MethodCallUnmarshallerAid implements SynchronousRequestHandler {
	private RpcMethodProxy proxy;

	public ProxyingRequestHandler(RpcMethodProxy proxy) {
		this.proxy = proxy;
	}

	/**
	 * @deprecated Override {@link #handleRequest(RpcCall, RpcCallContext)} instead.
	 */
	public RpcResponse handleRequest(RpcCall call) throws Exception {
		return null;
	}

	public RpcResponse handleRequest(RpcCall call, RpcCallContext context) throws Exception {
		// Defer to the previous handler method for backwards compatibility.
		RpcResponse rsp = this.handleRequest(call);
		
		if (rsp != null) {
			// This is probably an instance of a subclasss that predates
			// the introduction of the SecureSyncHandler interface.
			return rsp;
		}
		
		return this.proxy.invoke(call, context);
	}

	public Class getType(String methodName, int index) {
		return this.proxy.getType(methodName, index);
	}

	/**
	 * @return
	 * 	this implementation always returns <code>null</code>.
	 */
	public FieldNameCodec getFieldNameCodec(String methodName) {
		return null;
	}
}
