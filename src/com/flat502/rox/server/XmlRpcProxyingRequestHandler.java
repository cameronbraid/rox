package com.flat502.rox.server;

import com.flat502.rox.marshal.FieldNameEncoder;

/**
 * A {@link com.flat502.rox.server.ProxyingRequestHandler} implementation
 * specialized for XML-RPC.
 * <p>
 * Typical usage of this class is illustrated by the following code
 * sample:
 * <pre>
 * XMLRPCServer server = new XMLRPCServer(host, port);
 * String namePattern = "^prefix\\.(.*)";
 * ProxyingRequestHandler proxy = new ProxyingRequestHandler(namePattern, new RMIServerDemo());
 * server.registerHandler(null, namePattern, proxy, proxy);
 * server.start();
 * </pre>
 */
public class XmlRpcProxyingRequestHandler extends ProxyingRequestHandler {
	public XmlRpcProxyingRequestHandler(String namePattern, Object target) {
		this(namePattern, target, null);
	}

	public XmlRpcProxyingRequestHandler(String namePattern, Object target,
			FieldNameEncoder fieldNameEncoder) {
		super(new XmlRpcMethodProxy(namePattern, target, fieldNameEncoder));
	}
}
