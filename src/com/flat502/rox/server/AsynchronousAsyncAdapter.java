package com.flat502.rox.server;

import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.processing.SSLSession;

@SuppressWarnings("deprecation")
class AsynchronousAsyncAdapter implements AsynchronousRequestHandler {
	private AsyncRequestHandler target;

	public AsynchronousAsyncAdapter(AsyncRequestHandler target) {
		this.target = target;
	}

	public void handleRequest(RpcCall call, RpcCallContext context, ResponseChannel rspChannel) throws Exception {
		target.handleRequest(call, rspChannel);
	}
}
