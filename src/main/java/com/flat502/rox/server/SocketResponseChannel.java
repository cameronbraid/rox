package com.flat502.rox.server;

import java.io.IOException;
import java.net.Socket;

import com.flat502.rox.encoding.Encoding;
import com.flat502.rox.http.HttpRequestBuffer;
import com.flat502.rox.http.HttpResponse;
import com.flat502.rox.http.HttpResponseException;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.RpcResponse;

/**
 * A response channel wrapping an underlying SocketChannel.
 * A new instance of this is passed to an async handler, providing
 * an opaque mapping between that handler and the appropriate
 * socket channel for responses.
 */
class SocketResponseChannel implements ResponseChannel {
	private ResponseCoordinator coord;
	private int rspId;

	private HttpRequestBuffer request;
	private Socket socket;
	private Encoding encoding;
	
	SocketResponseChannel(ResponseCoordinator coord, HttpRequestBuffer request, Encoding encoding) {
		this.coord = coord;
		this.rspId = coord.nextId();
		this.request = request;
		this.encoding = encoding;
	}
	
	public void respond(RpcResponse rsp) throws IOException, MarshallingException {
//		HttpResponse httpRsp = this.server.toHttpResponse(this.request, rsp, this.encoding);
//		this.server.queueResponse(socket, httpRsp.marshal(), httpRsp.mustCloseConnection());
		this.coord.respond(this.rspId, this.request, rsp, this.encoding);
	}

	public void respond(HttpResponseException e) throws IOException {
//		HttpResponse httpRsp = this.server.newHttpResponse(this.request, e);
//		this.server.queueResponse(socket, httpRsp.marshal(), true);
		this.coord.respond(this.rspId, this.request, e);
	}

	public void close() throws IOException {
//		this.socket.getChannel().close();
		this.coord.close();
	}
}
