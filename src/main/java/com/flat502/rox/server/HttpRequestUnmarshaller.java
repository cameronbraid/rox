package com.flat502.rox.server;

import com.flat502.rox.http.HttpRequestBuffer;
import com.flat502.rox.marshal.MethodCallUnmarshallerAid;
import com.flat502.rox.marshal.RpcCall;

/**
 * An abstract base class for request unmarshaller implementations.
 * <p>
 * Request unmarshallers bridge raw HTTP requests and
 * {@link com.flat502.rox.marshal.RpcCall} instances.
 * <p>
 * Request unmarshallers may be registered on a given
 * HTTP server using the 
 * {@link com.flat502.rox.server.HttpRpcServer#registerRequestUnmarshaller(String, HttpRequestUnmarshaller)}
 * method.
 */
public abstract class HttpRequestUnmarshaller {
	public abstract RpcCall unmarshal(HttpRequestBuffer request, MethodCallUnmarshallerAid aid) throws Exception;
}
