package com.flat502.rox.server;

import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.flat502.rox.encoding.Encoding;
import com.flat502.rox.http.HttpRequestBuffer;
import com.flat502.rox.http.HttpResponse;
import com.flat502.rox.http.HttpResponseException;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.RpcResponse;

/**
 * This class is responsible for ensuring that multiple responses on the
 * same socket delivered via different ResponseChannel instances are
 * delivered in order.
 * <P>
 * This class is not thread-safe. Consumers are expected to synchronize access.
 */
class ResponseCoordinator {
	/*
	 * basic approach: this is instantiated when first request on a socket comes
	 * in. Two counters: nextToSend, nextToAssign
	 * Queue requests until we have a "flush" and send all of those together.
	 */

	private HttpRpcServer server;
	private Socket socket;

	private int nextToAssign;
	
	// The lowest assigned ID that we don't yet have a response for.
	// We can't send any responses with a higher ID than this value.
	private int nextToSend;
	
	private List<HttpResponse> queuedResponses = new LinkedList<HttpResponse>();
	
	ResponseCoordinator(HttpRpcServer server, Socket socket) {
		this.server = server;
		this.socket = socket;
	}
	
	public int nextId() {
		return nextToAssign++;
	}
	
	public void respond(int rspId, HttpRequestBuffer request, RpcResponse rsp, Encoding encoding) throws IOException, MarshallingException {
		HttpResponse httpRsp = this.server.toHttpResponse(request, rsp, encoding);
		
		if (this.nextToSend > rspId) {
			throw new IllegalStateException("Attempt to resend an asynchronous HTTP response");
		}
		
		if (this.nextToSend == rspId && this.queuedResponses.isEmpty()) {
			// Short circuit the common case
			this.sendResponse(httpRsp);
			return;
		}
		
		this.stashResponse(rspId, httpRsp);
	}

	public void respond(int rspId, HttpRequestBuffer request, HttpResponseException e) throws IOException {
		HttpResponse httpRsp = this.server.newHttpResponse(request, e);
		
		if (this.nextToSend > rspId) {
			throw new IllegalStateException("Attempt to resend an asynchronous HTTP error response");
		}
		
		if (this.nextToSend == rspId && this.queuedResponses.isEmpty()) {
			// Short circuit the common case
			this.sendResponse(httpRsp);
			return;
		}
		
		this.stashResponse(rspId, httpRsp);
	}

	public void close() throws IOException {
		this.server.deregisterResponseCoordinator(this);
		this.socket.getChannel().close();
	}
	
	protected void sendResponse(HttpResponse rsp) throws IOException {
		this.server.queueResponse(socket, rsp.marshal(), rsp.mustCloseConnection());
		this.nextToSend++;
	}

	protected void stashResponse(int rspId, HttpResponse httpRsp) throws IOException {
		try {
			while(this.queuedResponses.size() < (rspId-this.nextToSend+1)) {
				this.queuedResponses.add(null);
			}
			this.queuedResponses.set(rspId-this.nextToSend, httpRsp);
			
			Iterator<HttpResponse> iter = this.queuedResponses.iterator();
			while(iter.hasNext()) {
				HttpResponse candidate = iter.next();
				if (candidate == null) {
					return;
				}
				iter.remove();
				this.sendResponse(candidate);
			}
		} finally {
			if (this.queuedResponses.isEmpty()) {
				// All responses were sent release the resources associated with 
				// this instance. A new one will be created if any later responses
				// ultimately become available to be delivered on this channel 
				this.server.deregisterResponseCoordinator(this);
			}
		}
	}
}
