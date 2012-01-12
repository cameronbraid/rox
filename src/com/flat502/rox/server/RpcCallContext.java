package com.flat502.rox.server;

import java.nio.channels.SocketChannel;

import com.flat502.rox.http.HttpRequestBuffer;
import com.flat502.rox.processing.Context;
import com.flat502.rox.processing.SSLSession;

public class RpcCallContext extends Context {
	private HttpRequestBuffer httpRequest;

	public RpcCallContext(SocketChannel channel, SSLSession sslSession, HttpRequestBuffer req) {
		super(channel, sslSession);
		this.httpRequest = req;
	}

	public HttpRequestBuffer getHttpRequest() {
		return this.httpRequest;
	}
}
