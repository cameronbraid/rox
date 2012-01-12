package com.flat502.rox.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

import com.flat502.rox.Version;
import com.flat502.rox.encoding.Encoding;
import com.flat502.rox.http.*;
import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.marshal.*;
import com.flat502.rox.processing.*;
import com.flat502.rox.utils.Utils;

/**
 * This is the server-side RPC interface.
 * <p>
 * This class supports both synchronous and asynchronous
 * handling of method calls. An instance is backed by at 
 * least two threads: a selecting thread and a worker thread. 
 * Both threads are daemon threads.
 * <p>
 * The selecting thread handles all low-level network I/O.
 * As soon as this thread identifies a complete HTTP request
 * the request is passed to the worker thread (or threads).
 * <p>
 * The number of worker threads may be adjusted dynamically
 * using the inherited 
 * {@link HttpRpcProcessor#addWorker()}
 * and {@link HttpRpcProcessor#removeWorker()}
 * methods. Worker threads are responsible for notification
 * of registered {@link com.flat502.rox.server.RequestHandler}s.
 */
public abstract class HttpRpcServer extends HttpRpcProcessor {
	private static Log log = LogFactory.getLog(HttpRpcServer.class);
	
	// Maps (normalized) URI instances to an ordered Map
	// that itself maps regular expression String instances to handlers.
	// Handlers implement one of the following interfaces:
	// 	SyncRequestHandler
	// 	AsyncRequestHandler
	// A null URI key is a wildcard.
	private Map uriHandlers = new HashMap();
	
	// Maps HTTP methods (strings) onto HttpRequestUnmarshaller instances.
	private Map reqUnmarshallers = new HashMap();

	// Maps RequestHandler instances to MethodCallUnmarshallerAid
	// instances.
	private Map handlerUnmarshallerAids = new HashMap();

	// Maps strings to their compiled Pattern
	// instance. This is stored separately from
	// the above since Pattern doesn't override
	// equals() or hashCode() and we want to be
	// able to detect duplicate patterns.
	private Map globalPatternMap = new HashMap();

	// The address (and name) and port we bind on.
	// We store the host along with the host so we
	// don't have to continually check for a null address.
	private InetAddress hostAddress;
	private String host;
	private int port;

	private ServerSocketChannel serverChannel;

	// The value used for the HTTP Header field.
	// This is stored separately so we don't "forget"
	// that the user initialized us with a null host.
	private String headerHostValue;

	// Maps Sockets to incomplete HttpRequestBuffer instances.
	private Map requestBuffers = new HashMap();

	// Maps Sockets to ByteBuffer instances that are
	// ready to be written out on the socket.
	// Placing a ByteBuffer in here effectively 'queues' it for
	// delivery to the associated SocketChannel when it next
	// becomes available for writing.
	private Map<Socket, List<ByteBuffer>> responseBuffers = new HashMap<Socket, List<ByteBuffer>>();

	private AcceptPolicy acceptPolicy;

	private ServerEncodingMap contentEncodingMap = new ServerEncodingMap();
	private boolean encodeResponses;

	private int idleClientTimeout;
	private Timer idleClientTimer;
	
	// Maps Sockets to Timer instances that are reset whenever there's
	// activity on the socket. Used to enforce idle client timeouts.
	private Map socketActivity = new HashMap();

	// Maps Sockets to an object responsible for coordinating responses
	// so we handle pipelined requests correctly.
	private Map<Socket, ResponseCoordinator> socketResponseCoordinators = new HashMap<Socket, ResponseCoordinator>();
	
	/**
	 * Initialize a new HTTP RPC server.
	 * <p>
	 * The server will not attempt to bind to a local port
	 * or being accepting connections (and by implication
	 * processing requests) until {@link Thread#start()}
	 * is invoked on this instance.
	 * @param hostAddress
	 * 	An {@link InetAddress} this instance should bind to
	 * 	when listening for connections. <code>null</code> is 
	 * 	interpreted as "listen on all interfaces".
	 * @param port
	 * @param useHttps
	 * @param workerPool
	 * @throws IOException
	 */
	// TODO: Document parameters
	public HttpRpcServer(InetAddress hostAddress, int port, boolean useHttps, ServerResourcePool workerPool) throws IOException {
		this(hostAddress, port, useHttps, workerPool, null);
	}
	
	public HttpRpcServer(InetAddress hostAddress, int port, boolean useHttps, ServerResourcePool workerPool, SSLConfiguration sslCfg)
			throws IOException {
		super(useHttps, workerPool, sslCfg);

		this.hostAddress = hostAddress;
		if (hostAddress != null) {
			this.host = hostAddress.getHostName();
			this.headerHostValue = this.host;
		} else {
			this.headerHostValue = InetAddress.getLocalHost().getCanonicalHostName();			
		}
		this.port = port;

		this.initialize();
	}

	public synchronized void registerAcceptPolicy(AcceptPolicy policy) {
		if (this.isStarted()) {
			throw new IllegalStateException("Can't modify policy: server has been started");
		}
		this.acceptPolicy = policy;
	}

	public void registerContentEncoding(Encoding encoding) {
		this.contentEncodingMap.addEncoding(encoding);
	}
	
	public void setEncodeResponses(boolean encode) {
		this.encodeResponses = encode;
	}
	
	// TODO: Document
	public void setIdleClientTimeout(int timeout) {
		if (timeout < 0) {
			throw new IllegalArgumentException("timeout is negative");
		}

		this.idleClientTimer = this.getTimer();
		this.idleClientTimeout = timeout;
	}

	/**
	 * Register an {@link HttpRequestUnmarshaller} instance
	 * for a given HTTP method.
	 * <p>
	 * If a prior unmarshaller instance exists for the named
	 * HTTP method the unmarshaller instance is replaced and
	 * the previously registered instance is returned.
	 * @param httpMethod
	 * 	The HTTP method to register the unmarshaller to handle.
	 * 	Custom HTTP methods may be used. No validation is performed
	 * 	on this value, it is only used as a mapping key.
	 * @param unmarshaller
	 * 	The {@link HttpRequestUnmarshaller} instance to register
	 * 	for the given HTTP method. This may be <code>null</code>
	 * 	which effectively deregisters any existing handler for
	 * 	the given HTTP method.
	 * @return
	 * 	The {@link HttpRequestUnmarshaller} instance previously
	 * 	registered to handle the given HTTP method, or <code>null</code>
	 * 	if no prior instance was registered.
	 * @see CgiRequestUnmarshaller
	 * @see XmlRpcRequestUnmarshaller
	 * @see com.flat502.rox.http.HttpConstants.Methods
	 */
	public HttpRequestUnmarshaller registerRequestUnmarshaller(String httpMethod, HttpRequestUnmarshaller unmarshaller) {
		synchronized(this.reqUnmarshallers) {
			return (HttpRequestUnmarshaller) this.reqUnmarshallers.put(httpMethod, unmarshaller);
		}
	}

	/**
	 * Register an asynchronous XML-RPC method call handler.
	 * 
	 * @param uriPath
	 * 	URIs for which this handler is responsible. <code>null</code>
	 * 	indicates all URIs.
	 * @param method
	 * 	A regular expression (see {@link java.util.regex.Pattern}) to
	 *    match method names on. Matching is performed as for the
	 *    {@link java.util.regex.Matcher#find()} method.
	 * @param handler
	 * 	The handler to call back when a request is received matching
	 * 	the indicated URI and method name pattern.
	 * @return 
	 * 	If a previously registered request handler existed and this
	 *    handler replaced it (i.e. their URI and method regular expression 
	 *    were identical)
	 * @see #registerHandler(String, String, SyncRequestHandler, MethodCallUnmarshallerAid)
	 * @deprecated Use {@link #registerHandler(String, String, AsynchronousRequestHandler)} instead.
	 */
	public RequestHandler registerHandler(String uriPath, String method, AsyncRequestHandler handler) {
		return this.registerHandler(uriPath, method, new AsynchronousAsyncAdapter(handler), null);
	}

	/**
	 * Register an asynchronous XML-RPC method call handler.
	 * 
	 * @param uriPath
	 * 	URIs for which this handler is responsible. <code>null</code>
	 * 	indicates all URIs.
	 * @param method
	 * 	A regular expression (see {@link java.util.regex.Pattern}) to
	 *    match method names on. Matching is performed as for the
	 *    {@link java.util.regex.Matcher#find()} method.
	 * @param handler
	 * 	The handler to call back when a request is received matching
	 * 	the indicated URI and method name pattern.
	 * @return 
	 * 	If a previously registered request handler existed and this
	 *    handler replaced it (i.e. their URI and method regular expression 
	 *    were identical)
	 * @see #registerHandler(String, String, SyncRequestHandler, MethodCallUnmarshallerAid)
	 */
	public RequestHandler registerHandler(String uriPath, String method, AsynchronousRequestHandler handler) {
		return this.registerHandler(uriPath, method, handler, null);
	}

	/**
	 * Register an asynchronous XML-RPC method call handler.
	 * 
	 * @param uriPath
	 * 	URIs for which this handler is responsible. <code>null</code>
	 * 	indicates all URIs.
	 * @param method
	 * 	A regular expression (see {@link java.util.regex.Pattern}) to
	 *    match method names on. Matching is performed as for the
	 *    {@link java.util.regex.Matcher#find()} method.
	 * @param handler
	 * 	The handler to call back when a request is received matching
	 * 	the indicated URI and method name pattern.
	 *	@param aid
	 *		A mapper to be used during the unmarshalling of parameters
	 *		of incoming method calls. May be <code>null</code>.
	 * @return 
	 * 	If a previously registered request handler existed and this
	 *    handler replaced it (i.e. their URI and method regular expression 
	 *    were identical)
	 * @see #registerHandler(String, String, SyncRequestHandler, MethodCallUnmarshallerAid)
	 * @deprecated Use {@link #registerHandler(String, String, AsynchronousRequestHandler, MethodCallUnmarshallerAid)} instead.
	 */
	public RequestHandler registerHandler(String uriPath, String method, AsyncRequestHandler handler, MethodCallUnmarshallerAid aid) {
		return this.registerHandler(uriPath, method, new AsynchronousAsyncAdapter(handler), aid);
	}

	/**
	 * Register an asynchronous XML-RPC method call handler.
	 * 
	 * @param uriPath
	 * 	URIs for which this handler is responsible. <code>null</code>
	 * 	indicates all URIs.
	 * @param method
	 * 	A regular expression (see {@link java.util.regex.Pattern}) to
	 *    match method names on. Matching is performed as for the
	 *    {@link java.util.regex.Matcher#find()} method.
	 * @param handler
	 * 	The handler to call back when a request is received matching
	 * 	the indicated URI and method name pattern.
	 *	@param aid
	 *		A mapper to be used during the unmarshalling of parameters
	 *		of incoming method calls. May be <code>null</code>.
	 * @return 
	 * 	If a previously registered request handler existed and this
	 *    handler replaced it (i.e. their URI and method regular expression 
	 *    were identical)
	 * @see #registerHandler(String, String, SyncRequestHandler, MethodCallUnmarshallerAid)
	 */
	public RequestHandler registerHandler(String uriPath, String method, AsynchronousRequestHandler handler, MethodCallUnmarshallerAid aid) {
		return this.registerHandler(uriPath, method, (RequestHandler) handler, aid);
	}

	/**
	 * Register a synchronous XML-RPC method call handler.
	 * 
	 * @param uriPath
	 * 	URIs for which this handler is responsible. <code>null</code>
	 * 	indicates all URIs.
	 * @param method
	 * 	A regular expression (see {@link java.util.regex.Pattern}) to
	 *    match method names on. Matching is performed as for the
	 *    {@link java.util.regex.Matcher#find()} method.
	 * @param handler
	 * 	The handler to call back when a request is received matching
	 * 	the indicated URI and method name pattern.
	 * @return 
	 * 	If a previously registered request handler existed and this
	 *    handler replaced it (i.e. their URI and method regular expression 
	 *    were identical)
	 * @see #registerHandler(String, String, SyncRequestHandler, MethodCallUnmarshallerAid)
	 * @deprecated Use {@link #registerHandler(String, String, SynchronousRequestHandler)} instead.
	 */
	public RequestHandler registerHandler(String uriPath, String method, SyncRequestHandler handler) {
		return this.registerHandler(uriPath, method, new SynchronousSyncAdapter(handler), null);
	}

	/**
	 * Register a synchronous XML-RPC method call handler.
	 * 
	 * @param uriPath
	 * 	URIs for which this handler is responsible. <code>null</code>
	 * 	indicates all URIs.
	 * @param method
	 * 	A regular expression (see {@link java.util.regex.Pattern}) to
	 *    match method names on. Matching is performed as for the
	 *    {@link java.util.regex.Matcher#find()} method.
	 * @param handler
	 * 	The handler to call back when a request is received matching
	 * 	the indicated URI and method name pattern.
	 * @return 
	 * 	If a previously registered request handler existed and this
	 *    handler replaced it (i.e. their URI and method regular expression 
	 *    were identical)
	 * @see #registerHandler(String, String, SyncRequestHandler, MethodCallUnmarshallerAid)
	 */
	public RequestHandler registerHandler(String uriPath, String method, SynchronousRequestHandler handler) {
		return this.registerHandler(uriPath, method, handler, null);
	}

	/**
	 * Register a synchronous XML-RPC method call handler.
	 * <P>
	 * Handlers are indexed first using the {@link Utils#normalizeURIPath(String) normalized}
	 * URI and then using the method name regular expression.
	 * <P>
	 * When a request is available the handler for the request is chosen by
	 * looking up the list of handlers registered under the associated URI (after
	 * normalization). This list is then scanned until a regular expression
	 * matches the given method name. If no match is found this is repeated using
	 * a <code>null</code> URI (i.e. a more specific URI match takes
	 * precedence).
	 * <P>
	 * If no handler is found an HTTP "404 Not Found" response is sent to the
	 * client.
	 * 
	 * @param uriPath
	 * 	URIs for which this handler is responsible. <code>null</code>
	 * 	indicates all URIs.
	 * @param method
	 * 	A regular expression (see {@link java.util.regex.Pattern}) to
	 *    match method names on. Matching is performed as for the
	 *    {@link java.util.regex.Matcher#find()} method.
	 * @param handler
	 * 	The handler to call back when a request is received matching
	 * 	the indicated URI and method name pattern.
	 *	@param aid
	 *		A mapper to be used during the unmarshalling of parameters
	 *		of incoming method calls. May be <code>null</code>.
	 * @return 
	 * 	If a previously registered request handler existed and this
	 *    handler replaced it (i.e. their URI and method regular expression 
	 *    were identical)
	 * @deprecated Use {@link #registerHandler(String, String, SynchronousRequestHandler, MethodCallUnmarshallerAid)} instead.
	 */
	public RequestHandler registerHandler(String uriPath, String method, SyncRequestHandler handler, MethodCallUnmarshallerAid aid) {
		return this.registerHandler(uriPath, method, new SynchronousSyncAdapter(handler), aid);
	}

	/**
	 * Register a synchronous XML-RPC method call handler.
	 * <P>
	 * Handlers are indexed first using the {@link Utils#normalizeURIPath(String) normalized}
	 * URI and then using the method name regular expression.
	 * <P>
	 * When a request is available the handler for the request is chosen by
	 * looking up the list of handlers registered under the associated URI (after
	 * normalization). This list is then scanned until a regular expression
	 * matches the given method name. If no match is found this is repeated using
	 * a <code>null</code> URI (i.e. a more specific URI match takes
	 * precedence).
	 * <P>
	 * If no handler is found an HTTP "404 Not Found" response is sent to the
	 * client.
	 * 
	 * @param uriPath
	 * 	URIs for which this handler is responsible. <code>null</code>
	 * 	indicates all URIs.
	 * @param method
	 * 	A regular expression (see {@link java.util.regex.Pattern}) to
	 *    match method names on. Matching is performed as for the
	 *    {@link java.util.regex.Matcher#find()} method.
	 * @param handler
	 * 	The handler to call back when a request is received matching
	 * 	the indicated URI and method name pattern.
	 *	@param aid
	 *		A mapper to be used during the unmarshalling of parameters
	 *		of incoming method calls. May be <code>null</code>.
	 * @return 
	 * 	If a previously registered request handler existed and this
	 *    handler replaced it (i.e. their URI and method regular expression 
	 *    were identical)
	 */
	public RequestHandler registerHandler(String uriPath, String method, SynchronousRequestHandler handler, MethodCallUnmarshallerAid aid) {
		return this.registerHandler(uriPath, method, (RequestHandler) handler, aid);
	}

	private RequestHandler registerHandler(String uriPath, String method, RequestHandler handler, MethodCallUnmarshallerAid aid) {
		// I anticipate a common mistake when using the ProxyingRequestHandler 
		// will be to forget to pass it in as the aid, so let's
		// cater for that.
		if (aid == null && handler instanceof MethodCallUnmarshallerAid) {
			aid = (MethodCallUnmarshallerAid) handler;
		}

		synchronized (this.uriHandlers) {
			if (uriPath != null) {
				uriPath = Utils.normalizeURIPath(uriPath);
			}

			Map patternMap = (Map) this.uriHandlers.get(uriPath);
			if (patternMap == null) {
				patternMap = new LinkedHashMap();
				this.uriHandlers.put(uriPath, patternMap);
			}

			Pattern pattern = (Pattern) this.globalPatternMap.get(method);
			if (pattern == null) {
				pattern = Pattern.compile(method);
				this.globalPatternMap.put(method, pattern);
			}
			RequestHandler prevHandler = (RequestHandler) patternMap.put(method, handler);
			synchronized (this.handlerUnmarshallerAids) {
				if (prevHandler != null) {
					this.handlerUnmarshallerAids.remove(prevHandler);
				}
				if (aid != null) {
					this.handlerUnmarshallerAids.put(handler, aid);
				}
			}
			return prevHandler;
		}
	}

	/**
	 * Routes an HTTP request to the appropriate handler.
	 * 
	 * @param sk
	 * @param request
	 * @return For synchronous handlers a method response is returned. For
	 *         asynchronous handlers <code>null</code> is returned.
	 * @throws Exception
	 */
	RpcResponse routeRequest(Socket socket, HttpRequestBuffer request) throws Exception {
		String reqMountPoint = Utils.normalizeURIPath(request.getURI());
		ServerUnmarshallerAid aid = new ServerUnmarshallerAid(reqMountPoint);
		
		HttpRequestUnmarshaller unmarshaller = null;
		synchronized(this.reqUnmarshallers) {
			unmarshaller = (HttpRequestUnmarshaller) this.reqUnmarshallers.get(request.getMethod());

			if (unmarshaller == null) {
				// No registered handler for this method.
				// TODO: Add testcase
				Iterator iter = this.reqUnmarshallers.keySet().iterator();
				String allowed = Utils.join(", ", iter);
				throw new MethodNotAllowedResponseException("(no handler for " + request.getMethod() + ")", allowed);
			}
		}
		
		RpcCall call;
		try {
			call = unmarshaller.unmarshal(request, aid);
		} catch (MethodNotAllowedException e) {
			// TODO: Add testcase
			String allowed = Utils.join(", ", e.getAllowed());
			throw new MethodNotAllowedResponseException("(misconfigured handler for " + e.getMethod() + "?)", allowed);
		}
		
		RequestHandler handler = aid.getRequestHandler();
		if (handler == null) {
			// No match with a specific URI, try with a wildcard URI
			handler = this.lookupHandler(null, call.getName());
			if (handler == null) {
				throw new HttpResponseException(HttpConstants.StatusCodes._404_NOT_FOUND, "Not Found (handler for "
						+ call.getName() + ")", request);
			}
		}

		try {
			return dispatchRpcCall(socket, request, call, handler);
		} catch (RpcFaultException e) {
			return e.toFault();
		}
	}

	private RpcResponse dispatchRpcCall(Socket socket, HttpRequestBuffer request, RpcCall call, RequestHandler handler) throws Exception, HttpResponseException {
		RpcCallContext context = this.newRpcCallContext(socket, request);
		if (handler instanceof SynchronousRequestHandler) {
			RpcResponse rsp = ((SynchronousRequestHandler) handler).handleRequest(call, context);
			if (rsp == null) {
				throw new HttpResponseException(HttpConstants.StatusCodes._500_INTERNAL_SERVER_ERROR,
						"A synchronous handler (for " + call.getName() + ") returned a null value");
			}
			return rsp;
		} else {
			// TODO: _TEST_ Select the content based on Accept-Encoding
//			Encoding rspEncoding = this.selectResponseEncoding(request);
//			SocketResponseChannel rspChannel = new SocketResponseChannel(this, request, socket, rspEncoding);
			SocketResponseChannel rspChannel = this.newSocketResponseChannel(socket, request);
			((AsynchronousRequestHandler) handler).handleRequest(call, context, rspChannel);
			return null;
		}
	}

	private SocketResponseChannel newSocketResponseChannel(Socket socket, HttpRequestBuffer request) {
		Encoding rspEncoding = this.selectResponseEncoding(request);

		ResponseCoordinator rc;
		synchronized(socketResponseCoordinators) {
			rc = socketResponseCoordinators.get(socket);
			if (rc == null) {
				socketResponseCoordinators.put(socket, rc = this.newResponseCoordinator(socket));
			}
		}
		return new SocketResponseChannel(rc, request, rspEncoding);
	}
	
	private ResponseCoordinator newResponseCoordinator(Socket socket) {
		return new ResponseCoordinator(this, socket);
	}

	protected RpcCallContext newRpcCallContext(Socket socket, HttpRequestBuffer req) {
		SocketChannel channel = socket == null ? null : socket.getChannel();
		return new RpcCallContext(channel, this.newSSLSession(socket), req);
	}

	/**
	 * This package private method exists purely to route calls to the protected
	 * {@link XmlRpcProcessor#queueWrite(Socket, byte[])} method from classes
	 * within this package without forcing it to be public.
	 * @param socket
	 * @param rspData
	 */
	void queueResponse(Socket socket, byte[] rspData, boolean close) {
		this.queueWrite(socket, rspData, close);
	}

	private RequestHandler lookupHandler(String uri, String methodName) {
		// We don't normalize the URI here because it is normalized when
		// the HTTP request comes in.
		if (log.logDebug()) {
			log.debug("Look up handler for URI [" + uri + "] and method [" + methodName + "]");
		}

		RequestHandler handler = null;
		Map patternMap = (Map) this.uriHandlers.get(uri);
		if (patternMap == null) {
			if (log.logDebug()) {
				log.debug("Nothing registered for uri [" + uri + "]");
			}
			return null;
		}
		Iterator patterns = patternMap.entrySet().iterator();
		while (patterns.hasNext()) {
			Map.Entry entry = (Map.Entry) patterns.next();
			Pattern pattern = (Pattern) this.globalPatternMap.get(entry.getKey());
			if (pattern.matcher(methodName).find()) {
				if (log.logDebug()) {
					log.debug("Handler matched on pattern [" + pattern + "]");
				}
				handler = (RequestHandler) entry.getValue();
			}
		}
		return handler;
	}

	/**
	 * Constructs a new {@link HttpResponse} containing the
	 * given XML-RPC method response.
	 * <p>
	 * This implementation encodes the response using <code>UTF-8</code>,
	 * sets the status code to <code>200</code>, and sets <code>Content-Type</code>
	 * header to <code>text/xml</code> as required. No other headers are set.
	 * @param methodRsp
	 * 	The XML-RPC method response to be returned in the HTTP response.
	 * @param encoding 
	 * 	An {@link Encoding} describing the encoding to use when 
	 * 	constructing this message. This also informs the 
	 * 	<code>Content-Encoding</code>	header value. May be
	 * 	<code>null</code>.
	 * @return
	 * 	A new {@link HttpResponse} with the marshalled XML-RPC response
	 * 	as its content.
	 * @throws IOException
	 * 	if an error occurs while marshalling the XML-RPC response.
	 * @throws MarshallingException 
	 */
	protected HttpResponse toHttpResponse(HttpMessageBuffer origMsg, RpcResponse methodRsp, Encoding encoding) throws IOException,
			MarshallingException {
		ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
		methodRsp.marshal(byteOs, Charset.forName("UTF-8"));
		byte[] rspXml = byteOs.toByteArray();
		HttpResponse httpRsp = this.newHttpResponse(origMsg, 200, "OK", encoding);
		httpRsp.addHeader(HttpConstants.Headers.CONTENT_TYPE, methodRsp.getContentType());
		httpRsp.setContent(rspXml);
		return httpRsp;
	}
	
	// TODO: Document
	protected Encoding selectResponseEncoding(HttpRequestBuffer request) {
		Map accepted = request.getAcceptedEncodings();
		if (accepted == null) {
			return null;
		}
		
		Iterator encodings = accepted.keySet().iterator();
		while (encodings.hasNext()) {
			String encodingName = (String) encodings.next();
			Encoding encoding = this.contentEncodingMap.getEncoding(encodingName);
			if (encoding != null) {
				return encoding;
			}
		}
		
		return null;
	}

	protected ResourcePool newWorkerPool() {
		return new ServerResourcePool();
	}

	/**
	 * This implementation defers to the 
	 * {@link #accept(SelectionKey)} method if a connection
	 * is pending. In all other cases it defers to it's
	 * parent.
	 * @param key
	 * 	The {@link SelectionKey} for the socket on which
	 * 	an I/O operation is pending.
	 * @throws IOException
	 * 	if an error occurs while processing the pending event.
	 */
	protected void handleSelectionKeyOperation(SelectionKey key) throws IOException {
		if (key.isValid() && key.isAcceptable()) {
			this.accept(key);
		} else {
			super.handleSelectionKeyOperation(key);
		}
	}
	
	protected void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		this.resetClientTimer(socketChannel.socket());
		super.read(key);
	}
	
	protected void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		this.resetClientTimer(socketChannel.socket());
		super.write(key);
	}
	
	protected void deregisterSocket(Socket socket) {
		TimerTask task = (TimerTask) this.socketActivity.remove(socket);
		if (task != null) {
			task.cancel();
		}
		
		super.deregisterSocket(socket);
	}

	protected void deregisterResponseCoordinator(ResponseCoordinator coordinator) {
		synchronized(socketResponseCoordinators) {
			socketResponseCoordinators.remove(coordinator);
		}
	}
	
	/**
	 * Called when a new connection is pending on the underlying
	 * {@link ServerSocketChannel}.
	 * @param key
	 * 	The {@link SelectionKey} for the socket on which
	 * 	a connection is pending.
	 * @throws IOException
	 * 	if an error occurs while accepting the
	 * 	new connection.
	 */
	protected void accept(SelectionKey key) throws IOException {
		// Pull out the socket channel that has a connection pending
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

		// Accept the connection
		SocketChannel socketChannel = serverSocketChannel.accept();
		Socket socket = socketChannel.socket();

		// Check if our AcceptPolicy will allow this new connection
		if (this.acceptPolicy != null
				&& !this.acceptPolicy.shouldRetain(socketChannel, this.getSocketSelector().keys().size())) {
			if (log.logTrace()) {
				log.trace("Closing accepted connection (accept policy enforced)");
			}
			socketChannel.close();
			return;
		}
		
		this.registerChannel(socketChannel);

		// Register the new socket. This will promote it to an SSLSocket
		// if we're configured for HTTPS.
		this.registerSocket(socket, this.host, this.port, false);

		// Add the new SocketChannel to our Selector
		socketChannel.configureBlocking(false);
		SelectionKey acceptKey = socketChannel.register(this.getSocketSelector(), SelectionKey.OP_READ);
		
		this.resetClientTimer(socketChannel.socket());
	}

	private void resetClientTimer(Socket socket) {
		if (this.idleClientTimer == null) {
			if (log.logTrace()) {
				log.trace("No idle client timeout configured, skipping timer reset");
			}
			return;
		}

		if (log.logTrace()) {
			log.trace("Resetting idle client timer: " + System.identityHashCode(socket));
		}
		
		// Store this in a local so we don't have to worry about
		// the value changing underneath us.
		long timeout = this.idleClientTimeout;

		// Cancel the existing task for this socket ...
		TimerTask curTask = (TimerTask) this.socketActivity.get(socket);
		if (curTask != null) {
			curTask.cancel();
		}
		
		// And schedule a new one.
		TimerTask task = new IdleClientTimerTask(socket);
		this.socketActivity.put(socket, task);
		this.idleClientTimer.schedule(task, timeout);
	}

	private class IdleClientTimerTask extends TimerTask {
		private Socket socket;

		IdleClientTimerTask(Socket socket) {
			this.socket = socket;
		}

		public void run() {
			try {
				if (log.logTrace()) {
					log.trace("Idle client timer expired: " + System.identityHashCode(socket));
				}
				SocketChannel socketChannel = (SocketChannel) this.socket.getChannel();
				socketChannel.keyFor(HttpRpcServer.this.getSocketSelector()).cancel();
				// This (shutting down the output stream) seems unnecessary but 
				// without it the client never sees a disconnect under Linux.
				// For good measure we shutdown the input stream too.
				socketChannel.socket().shutdownOutput();
				socketChannel.socket().shutdownInput();
				socketChannel.close();
				HttpRpcServer.this.deregisterSocket(socket);
			} catch(Exception e) {
				log.warn("IdleClientTimerTask caught an exception", e);
			}
		}
	}

	/**
	 * Converts an exception raised while processing
	 * an HTTP request into a suitable HTTP response.
	 * <p>
	 * The response is marshalled and queued for writing
	 * on the socket associated with the original
	 * request.
	 * @param msg
	 * 	The HTTP request being processed when the exception 
	 * 	occurred.
	 * @param e
	 * 	The exception that was raised.
	 * @throws IOException
	 * 	if an error occurs marshalling or writing
	 * 	the response.
	 */
	protected void handleMessageException(HttpMessageBuffer msg, Exception e) throws IOException {
		HttpResponse httpRsp;
		if (e instanceof HttpResponseException) {
			if (log.logWarn()) {
				log.warn("HttpResponseException", e);
			}
			httpRsp = this.newHttpResponse(msg, (HttpResponseException) e);
			this.queueWrite(msg.getSocket(), httpRsp.marshal(), true);
		} else if (e instanceof RemoteSocketClosedException) {
			if (log.logTrace()) {
				log.trace("Remote entity closed connection", e);
			}
		} else {
			if (log.logError()) {
				log.error("Internal Server Error", e);
			}
			httpRsp = this.newHttpResponse(msg, new HttpResponseException(HttpConstants.StatusCodes._500_INTERNAL_SERVER_ERROR,
					"Internal Server Error", e));
			this.queueWrite(msg.getSocket(), httpRsp.marshal(), true);
		}
	}

	protected void handleProcessingException(Socket socket, Exception e) {
		log.error("Exception on selecting thread (socket=" + Utils.toString(socket) + ")", e);
	}

	protected void handleTimeout(Socket socket, Exception cause) {
		log.debug("Timeout on " + Utils.toString(socket), cause);
	}
	
	protected void handleSSLHandshakeFinished(Socket socket, SSLEngine engine) {
		this.queueRead(socket);
	}

	protected void stopImpl() throws IOException {
		this.deregisterChannel(this.serverChannel);
		
		this.serverChannel.close();
		
		// Queue a cancellation for serverChannel
		this.queueCancellation(this.serverChannel);
	}

	/**
	 * Creates and initializes a {@link ServerSocketChannel}
	 * for accepting connections on.
	 */
	protected void initSelector(Selector selector) throws IOException {
		// Create a new server socket and set to non blocking mode
		this.serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);

		// Bind the server socket to the local host and port
		InetSocketAddress isa = new InetSocketAddress(this.hostAddress, this.port);
		serverChannel.socket().bind(isa);

		// Register accepts on the server socket with the selector. This
		// step tells the selector that the socket wants to be put on the
		// ready list when accept operations occur, so allowing multiplexed
		// non-blocking I/O to take place.
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		
		this.registerChannel(serverChannel);
	}
	
	protected SSLEngine initSocketSSLEngine(Socket socket) throws SSLException {
		SSLEngine engine = super.initSocketSSLEngine(socket);
		engine.setUseClientMode(false);

		switch(this.getSSLConfiguration().getClientAuthentication()) {
		case NONE:
			engine.setNeedClientAuth(false);
			engine.setWantClientAuth(false);
			break;
		case REQUEST:
			engine.setWantClientAuth(true);
			break;
		case REQUIRE:
			engine.setNeedClientAuth(true);
			break;
		}
		
		return engine;
	}

	protected HttpMessageBuffer getReadBuffer(Socket socket) {
		synchronized (this.requestBuffers) {
			HttpRequestBuffer request = (HttpRequestBuffer) this.requestBuffers.get(socket);
			if (request == null) {
				request = new HttpRequestBuffer(this, socket, this.contentEncodingMap);
				this.requestBuffers.put(socket, request);
			}
			return request;
		}
	}

	protected void removeReadBuffer(Socket socket) {
		synchronized (this.requestBuffers) {
			this.requestBuffers.remove(socket);
		}
	}

	protected void removeReadBuffers(Socket socket) {
		this.removeReadBuffer(socket);
	}

	protected void putWriteBuffer(Socket socket, ByteBuffer data) {
		synchronized (this.responseBuffers) {
			List<ByteBuffer> existing = this.responseBuffers.get(socket);
			if (existing != null) {
				existing.add(data);
			} else {
				LinkedList<ByteBuffer> list = new LinkedList<ByteBuffer>();
				list.add(data);
				this.responseBuffers.put(socket, list);
			}
		}
	}

	protected boolean isWriteQueued(Socket socket) {
		synchronized (this.responseBuffers) {
			return this.responseBuffers.containsKey(socket);
		}
	}

	protected ByteBuffer getWriteBuffer(Socket socket) {
		synchronized (this.responseBuffers) {
			return this.responseBuffers.get(socket).get(0);
		}
	}

	protected void removeWriteBuffer(Socket socket) {
		synchronized (this.responseBuffers) {
			List<ByteBuffer> existing = this.responseBuffers.get(socket);
			if (existing != null && !existing.isEmpty()) {
				existing.remove(0);
			} else {
				this.responseBuffers.remove(socket);
			}
		}
	}

	protected void removeWriteBuffers(Socket socket) {
		synchronized (this.responseBuffers) {
			this.responseBuffers.remove(socket);
		}
	}

	private class ServerUnmarshallerAid extends MethodCallUnmarshallerAid {
		private String uri;
		private RequestHandler handler;

		public ServerUnmarshallerAid(String uri) {
			this.uri = uri;
		}

		public Class getType(String methodName, int index) {
			MethodCallUnmarshallerAid aid = this.lookupAid(methodName);
			if (aid == null) {
				return null;
			}
			return aid.getType(methodName, index);
		}

		public FieldNameCodec getFieldNameCodec(String methodName) {
			UnmarshallerAid aid = this.lookupAid(methodName);
			if (aid == null) {
				return null;
			}
			return aid.getFieldNameCodec(methodName);
		}

		RequestHandler getRequestHandler() {
			return this.handler;
		}

		private MethodCallUnmarshallerAid lookupAid(String methodName) {
			this.handler = lookupHandler(this.uri, methodName);
			if (handler == null) {
				// No match with a specific URI, try with a wildcard URI
				handler = lookupHandler(null, methodName);
			}

			synchronized (handlerUnmarshallerAids) {
				return (MethodCallUnmarshallerAid) handlerUnmarshallerAids.get(handler);
			}
		}
	}

	protected HttpResponse newHttpResponse(HttpMessageBuffer msg, HttpResponseException e) {
		HttpResponse httpRsp = e.toHttpResponse(msg.getHttpVersionString());
		if (msg.getHttpVersion() > 1.0) {
			httpRsp.addHeader(HttpConstants.Headers.HOST, this.headerHostValue);
		}
		httpRsp.addHeader(HttpConstants.Headers.SERVER, this.getServerName());
		httpRsp.addHeader(HttpConstants.Headers.CONNECTION, "close");
		return httpRsp;
	}

	protected HttpResponse newHttpResponse(HttpMessageBuffer origMsg, int statusCode, String reasonPhrase, Encoding encoding) {
		Encoding responseEncoding = null;
		if (this.encodeResponses) {
			responseEncoding = encoding;
		}
		HttpResponse httpRsp;
		if (origMsg == null) {
			httpRsp = new HttpResponse(statusCode, reasonPhrase, responseEncoding);
		} else {
			httpRsp = new HttpResponse(origMsg.getHttpVersionString(), statusCode, reasonPhrase,
					responseEncoding);
		}
		if (origMsg.getHttpVersion() > 1.0) {
			httpRsp.addHeader(HttpConstants.Headers.HOST, this.headerHostValue);
		}
		httpRsp.addHeader(HttpConstants.Headers.SERVER, this.getServerName());
		return httpRsp;
	}

	protected String getServerName() {
		return Version.getDescription();
	}
}
