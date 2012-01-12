package com.flat502.rox.client;

import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.RpcResponse;

@SuppressWarnings("deprecation")
class AsynchronousResponseHandlerAdaptor implements AsynchronousResponseHandler {
	private ResponseHandler target;

	public AsynchronousResponseHandlerAdaptor(ResponseHandler target) {
		this.target = target;
	}

	public void handleResponse(RpcCall call, RpcResponse rsp, RpcResponseContext context) {
		this.target.handleResponse(call, rsp);
	}

	public void handleException(RpcCall call, Throwable e, RpcResponseContext context) {
		this.target.handleException(call, e);
	}
}
