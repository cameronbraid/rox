package com.flat502.rox.server;

import java.io.IOException;
import java.net.InetAddress;

import com.flat502.rox.http.HttpConstants;
import com.flat502.rox.marshal.MethodCallUnmarshaller;
import com.flat502.rox.marshal.MethodResponseUnmarshaller;
import com.flat502.rox.marshal.xmlrpc.DomMethodCallUnmarshaller;
import com.flat502.rox.marshal.xmlrpc.SaxMethodCallUnmarshaller;
import com.flat502.rox.processing.SSLConfiguration;

/**
 * This is the server-side XML-RPC interface.
 * <p>
 * This is a specialization of the
 * {@link com.flat502.rox.server.HttpRpcServer}.
 * <p>
 * An instance of this class will not actually bind to any
 * addresses until {@link Thread#start()} is invoked.
 * <p>
 * Typical usage of this class is illustrated
 * in the following sample:
 * <pre>
 * public class Handler implements SyncRequestHandler {
 * 	public RpcResponse handleRequest(RpcCall call) {
 * 		// Handle the call and return a response
 * 	}
 * }
 * ...
 * XmlRpcServer server = new XmlRpcServer(host, port);
 * server.registerHandler(null, "^math\\.", new Handler());
 * server.start();
 * </pre>
 * <p>
 * In addition, this class supports 
 * {@link com.flat502.rox.server.HttpRpcServer#registerHandler(String, String, AsyncRequestHandler) 
 * asynchronous method handling}
 * and {@link com.flat502.rox.server.XmlRpcMethodProxy dynamic method discovery}.
 * <p>
 * You're encouraged to read through the 
 * <a target=_top href="../../../../../examples.html">examples section</a>.
 */
public class XmlRpcServer extends HttpRpcServer {
	/**
	 * Initialize an instance listening for connections
	 * on all local addresses.
	 * <p>
	 * HTTPS (SSL) and logging are disabled and no
	 * {@link AcceptPolicy accept policy} is installed.
	 * @param port
	 * 	The port to listen on.
	 * @throws IOException
	 * 	if an error occurs initializing the underlying
	 * 	server socket.
	 */
	public XmlRpcServer(int port) throws IOException {
		this(null, port, false, null, null);
	}

	/**
	 * Initialize an instance listening for connections
	 * on a specified local address.
	 * <p>
	 * HTTPS (SSL) is disabled and no
	 * {@link AcceptPolicy accept policy} is installed.
	 * @param hostAddress
	 * 	The address to listen on. If this is <code>null</code>
	 * 	this instance will listen on all local addresses.
	 * @param port
	 * 	The port to listen on.
	 * @throws IOException
	 * 	if an error occurs initializing the underlying
	 * 	server socket.
	 */
	public XmlRpcServer(InetAddress hostAddress, int port) throws IOException {
		this(hostAddress, port, false, null, null);
	}

	public XmlRpcServer(InetAddress hostAddress, int port, SSLConfiguration sslCfg) throws IOException {
		this(hostAddress, port, (sslCfg != null), null, null, sslCfg);
	}

	/**
	 * Initialize an instance listening for connections
	 * on a specified local address.
	 * <p>
	 * No {@link AcceptPolicy accept policy} is installed.
	 * @param hostAddress
	 * 	The address to listen on. If this is <code>null</code>
	 * 	this instance will listen on all local addresses.
	 * @param port
	 * 	The port to listen on.
	 * @throws IOException
	 * 	if an error occurs initializing the underlying
	 * 	server socket.
	 */
	public XmlRpcServer(InetAddress hostAddress, int port, boolean useHttps) throws IOException {
		this(hostAddress, port, useHttps, null, null, new SSLConfiguration());
	}

	/**
	 * Initialize an instance listening for connections
	 * on a specified local address.
	 * <p>
	 * @param hostAddress
	 * 	The address to listen on. If this is <code>null</code>
	 * 	this instance will listen on all local addresses.
	 * @param port
	 * 	The port to listen on.
	 * @param useHttps
	 * 	Indicates whether or not HTTPS (SSL) should be enabled.
	 * @param acceptPolicy
	 * 	The {@link AcceptPolicy} to apply. May be <code>null</code>.
	 * @param workerPool
	 * 	A shared {@link ServerResourcePool} instance from which
	 * 	resources (like timers and threads) should be drawn.
	 * @throws IOException
	 * 	if an error occurs initializing the underlying
	 * 	server socket.
	 */
	public XmlRpcServer(InetAddress hostAddress, int port, boolean useHttps, AcceptPolicy acceptPolicy, ServerResourcePool workerPool)
		throws IOException {
		this(hostAddress, port, useHttps, acceptPolicy, workerPool, null);
	}

	public XmlRpcServer(InetAddress hostAddress, int port, boolean useHttps, AcceptPolicy acceptPolicy, ServerResourcePool workerPool, SSLConfiguration sslCfg)
		throws IOException {
		super(hostAddress, port, useHttps, workerPool, sslCfg);
		this.registerAcceptPolicy(acceptPolicy);
		this.setUnmarshaller(new SaxMethodCallUnmarshaller());
	}

	/**
	 * Register an XML-RPC aware {@link ProxyingRequestHandler} proxying
	 * the specified target object.
	 * <p>
	 * A {@link SynchronousRequestHandler} is registered for the specified
	 * URI. Method dispatch and request unmarshalling is described in the
	 * documentation for {@link RpcMethodProxy}.
	 * @param uri
	 * 	The URI for which this handler is responsible. <code>null</code>
	 * 	indicates all URIs.
	 * @param method
	 * 	A regular expression (see {@link java.util.regex.Pattern}) to
	 *    match method names on. Matching is performed as for the
	 *    {@link java.util.regex.Matcher#find()} method.
	 * @param target
	 * 	The object to delegate XML-RPC requests to.
	 * @return
	 *  The {@link RequestHandler} that was registered.
	 */
	public RequestHandler registerProxyingHandler(String uri, String method, Object target) {
		XmlRpcProxyingRequestHandler proxy = new XmlRpcProxyingRequestHandler(method, target);
		return this.registerHandler(uri, method, proxy, proxy);
	}

	/**
	 * Configure the {@link MethodResponseUnmarshaller} instance to
	 * use when unmarshalling incoming XML-RPC method calls.
	 * @param unmarshaller
	 * 	The new unmarshaller instance to use.
	 * @see DomMethodCallUnmarshaller
	 * @see SaxMethodCallUnmarshaller
	 */
	public void setUnmarshaller(MethodCallUnmarshaller unmarshaller) {
		this.registerRequestUnmarshaller(HttpConstants.Methods.POST, new XmlRpcRequestUnmarshaller(unmarshaller));
	}
}
