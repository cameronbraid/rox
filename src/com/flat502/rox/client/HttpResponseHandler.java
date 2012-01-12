package com.flat502.rox.client;

import java.nio.ByteBuffer;

import com.flat502.rox.http.HttpMessageBuffer;
import com.flat502.rox.http.HttpResponseBuffer;
import com.flat502.rox.http.ProcessingException;
import com.flat502.rox.processing.HttpMessageHandler;
import com.flat502.rox.utils.BlockingQueue;

class HttpResponseHandler extends HttpMessageHandler {
	private ByteBuffer readBuf = ByteBuffer.allocate(1024);

	public HttpResponseHandler(BlockingQueue queue) {
		super(queue);
	}

	protected void handleMessage(HttpMessageBuffer msg) throws Exception {
		HttpResponseBuffer response = getResponse(msg);
		if (!response.isComplete()) {
			throw new IllegalStateException("Incomplete request on my queue");
		}

		this.getClient(response).handleResponse(response);
	}

	protected void handleHttpMessageException(HttpMessageBuffer msg, Throwable exception) {
		HttpResponseBuffer response = getResponse(msg);
		this.getClient(response).handleException((HttpResponseBuffer) msg, exception);
	}
	
	protected void handleProcessingException(ProcessingException exception) {
		((HttpRpcClient)exception.getProcessor()).handleException(exception);
	}
	
	private HttpResponseBuffer getResponse(HttpMessageBuffer msg) {
		if (!(msg instanceof HttpResponseBuffer)) {
			throw new IllegalArgumentException("Expected instance of " + HttpResponseBuffer.class.getName() + ", got "
					+ msg.getClass().getName());
		}
		return (HttpResponseBuffer) msg;
	}
	
	private HttpRpcClient getClient(HttpResponseBuffer response) {
		if (!(response.getOrigin() instanceof HttpRpcClient)) {
			throw new IllegalArgumentException("Expected instance of " + HttpRpcClient.class.getName() + ", got "
					+ response.getOrigin().getClass().getName());
		}
		return (HttpRpcClient) response.getOrigin();
	}
}