package com.flat502.rox.client;

import com.flat502.rox.http.HttpResponseBuffer;
import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.marshal.MethodResponseUnmarshallerAid;
import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.RpcFault;
import com.flat502.rox.marshal.RpcResponse;
import com.flat502.rox.processing.RpcFaultException;

class AsynchronousNotifier implements Notifiable {
	private static Log log = LogFactory.getLog(AsynchronousNotifier.class);

	private HttpRpcClient client;
	private AsynchronousResponseHandler handler;
	private RpcCall call;
	private MethodResponseUnmarshallerAid aid;

	public AsynchronousNotifier(HttpRpcClient client, AsynchronousResponseHandler handler, RpcCall call, MethodResponseUnmarshallerAid aid) {
		this.client = client;
		this.handler = handler;
		this.call = call;
		this.aid = aid;
	}

	public void notify(HttpResponseBuffer response, RpcResponseContext context) {
		RpcResponse rsp = null;
		Exception exception = null;
		try {
			this.client.validateHttpResponse(this.call, response);
			rsp = this.client.unmarshalResponse(response.getContentReader(), this.aid);
		} catch (Exception e) {
			exception = e;
		}

		if (exception == null && rsp instanceof RpcFault) {
			exception = new RpcFaultException((RpcFault) rsp);
		}

		if (exception != null) {
			notify(exception, context);
		} else {
			notify(rsp, context);
		}
	}

	public void notify(Throwable e, RpcResponseContext context) {
		try {
			this.handler.handleException(this.call, e, context);
		} catch (Throwable t) {
			log.error("handler.handleException() threw error", t);
		}
	}

	private void notify(RpcResponse rsp, RpcResponseContext context) {
		try {
			this.handler.handleResponse(this.call, rsp, context);
		} catch (Throwable t) {
			log.error("handler.handleResponse() threw error", t);
		}
	}

	public void notifyTimedOut(Throwable cause, RpcResponseContext context) {
		this.notify(new RpcCallTimeoutException(cause), context);
	}
}
