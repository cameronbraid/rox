package com.flat502.rox.server;

import java.nio.ByteBuffer;

import com.flat502.rox.encoding.Encoding;
import com.flat502.rox.http.*;
import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.RpcResponse;
import com.flat502.rox.processing.HttpMessageHandler;
import com.flat502.rox.utils.BlockingQueue;

class HttpRequestHandler extends HttpMessageHandler {
	private static Log log = LogFactory.getLog(HttpRequestHandler.class);

	private ByteBuffer readBuf = ByteBuffer.allocate(1024);

	HttpRequestHandler(BlockingQueue queue) {
		super(queue);
	}

	protected void handleMessage(HttpMessageBuffer msg) throws Exception {
		if (!(msg instanceof HttpRequestBuffer)) {
			throw new IllegalArgumentException("Expected instance of " + HttpRequestBuffer.class.getName() + ", got "
					+ msg.getClass().getName());
		}
		HttpRequestBuffer request = (HttpRequestBuffer) msg;
		
		if (!(request.getOrigin() instanceof HttpRpcServer)) {
			throw new IllegalArgumentException("Expected instance of " + HttpRpcServer.class.getName() + ", got "
					+ request.getOrigin().getClass().getName());
		}
		HttpRpcServer server = (HttpRpcServer) request.getOrigin();

		if (!request.isComplete()) {
			throw new IllegalStateException("Incomplete request on my queue");
		}
		
		HttpResponse httpRsp;
		try {
			RpcResponse methodRsp = server.routeRequest(request.getSocket(), request);
			if (methodRsp == null) {
				// The handler was asynchronous and will queue a response
				// when it's done. We're done here.
				return;
			}
			// TODO: _TEST_ Select the content based on Accept-Encoding
			Encoding rspEncoding = server.selectResponseEncoding(request);
			httpRsp = server.toHttpResponse(msg, methodRsp, rspEncoding);
		} catch (HttpResponseException e) {
			httpRsp = server.newHttpResponse(msg, e);
		} catch (NoSuchMethodError e) {
			httpRsp = server.newHttpResponse(msg, HttpConstants.StatusCodes._404_NOT_FOUND,
					"Not Found (" + e.getMessage() + ")", null);
		} catch (NoSuchMethodException e) {
			httpRsp = server.newHttpResponse(msg, HttpConstants.StatusCodes._404_NOT_FOUND,
					"Not Found (" + e.getMessage() + ")", null);
		} catch (MarshallingException e) {
			httpRsp = server.newHttpResponse(msg, HttpConstants.StatusCodes._400_BAD_REQUEST,
					"Bad Request (illegal argument: " + e.getMessage() + ")", null);
		} catch (Exception e) {
			httpRsp = server.newHttpResponse(msg, HttpConstants.StatusCodes._500_INTERNAL_SERVER_ERROR,
					"Internal Server Error (" + e.getMessage() + ")", null);
			log.error("Error routing HTTP request:\n" + request.toString(), e);
		}

		server.queueResponse(request.getSocket(), httpRsp.marshal(), httpRsp.mustCloseConnection());
	}
}