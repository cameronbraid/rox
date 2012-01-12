package com.flat502.rox.client;

import java.io.IOException;
import java.util.*;

import com.flat502.rox.encoding.Encoding;
import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.RpcResponse;
import com.flat502.rox.marshal.xmlrpc.XmlRpcFaultException;
import com.flat502.rox.marshal.xmlrpc.XmlRpcMethodFault;
import com.flat502.rox.marshal.xmlrpc.XmlRpcMethodResponse;
import com.flat502.rox.processing.SSLConfiguration;
import com.flat502.rox.processing.SSLSession;
import com.flat502.rox.server.*;

public class TestSecureServer implements SynchronousRequestHandler {
	private XmlRpcServer server;
	private Map map;
	public RpcCallContext context;
	public SSLSession session;

	public TestSecureServer(String uri, String prefix, int port) throws IOException {
		this(uri, prefix, port, null);
	}

	public TestSecureServer(String uri, String prefix, int port, SSLConfiguration cfg) throws IOException {
		this(uri, prefix, port, null, cfg, null);
	}

	public TestSecureServer(String uri, String prefix, int port, SSLConfiguration cfg, SSLSessionPolicy policy) throws IOException {
		this(uri, prefix, port, null, cfg, policy);
	}

	public TestSecureServer(String uri, String prefix, int port, Encoding enc, SSLConfiguration cfg) throws IOException {
		this(uri, prefix, port, null, cfg, null);
	}

	public TestSecureServer(String uri, String prefix, int port, Encoding enc, SSLConfiguration cfg, SSLSessionPolicy policy) throws IOException {
		this.map = new HashMap();
		this.map.put("string", "Hello World");
		this.server = new XmlRpcServer(null, port, cfg);
		this.server.registerHandler(uri, "^" + prefix + "\\.", this);

		if (policy != null) {
			this.server.registerSSLSessionPolicy(policy);
		}
		
		if (enc != null) {
			this.server.registerContentEncoding(enc);
		}
		this.server.start();
	}

	public void stop() throws Exception {
		this.server.stop();
		
		// There is a small delay between stopping the server and the listening socket
		// actually being released to the OS. It only happens when the selecting thread
		// performs it's next select() operation, which is almost immediately but asynchronous.
		// So we have to give it a little time before allowing the next server to attempt to
		// bind to this socket. This is a reasonably central (if somewhat obscure) place to
		// do so. See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5073504
		Thread.sleep(50);
	}
	
	private Object complexResponse() {
		List list = new ArrayList();
		for(int ami = 0; ami < 2; ami++) {
			Properties props = new Properties();
			props.setProperty("property-name-1-"+ami, "property-value-1-"+ami);
			props.setProperty("property-name-2-"+ami, "property-value-2-"+ami);
			props.setProperty("property-name-3-"+ami, "property-value-3-"+ami);
			props.setProperty("property-name-4-"+ami, "property-value-4-"+ami);
			props.setProperty("long-property-"+ami, "the quick brown fox jumped over the sax parser - the quick brown fox jumped over the sax parser - the quick brown fox jumped over the sax parser - the quick brown fox jumped over the sax parser - the quick brown fox jumped over the sax parser");
			
			Map struct = new HashMap();
			struct.put("ami-id", "ami-1234"+ami);
			struct.put("second", "something");
			struct.put("third", "something-else");
			struct.put("properties", props);
			
			list.add(struct);
		}
		return list;
	}

	public RpcResponse handleRequest(RpcCall call, RpcCallContext context) throws Exception {
		this.context = context;
		this.session = context.getSSLSession();
		if (call.getName().equalsIgnoreCase("test.stringResponse")) {
			return new XmlRpcMethodResponse("bar");
		} else if (call.getName().equalsIgnoreCase("test.nullResponse")) {
			return new XmlRpcMethodResponse(null);
		} else if (call.getName().equalsIgnoreCase("test.returnXmlRpcMethofFault")) {
			return new XmlRpcMethodFault(21, "Half the meaning of Life");
		} else if (call.getName().equalsIgnoreCase("test.raiseXmlRpcFaultException")) {
			throw new XmlRpcFaultException(42, "The meaning of Life");
		} else if (call.getName().equalsIgnoreCase("test.raiseOtherException")) {
			throw new Exception("Another Exception");
		} else if (call.getName().equalsIgnoreCase("test.waitFor")) {
			int delay = ((Integer) call.getParameters()[0]).intValue();
			Thread.sleep(delay);
			return new XmlRpcMethodResponse(call.getParameters()[0]);
		} else if (call.getName().equalsIgnoreCase("test.complexResponse")) {
			return new XmlRpcMethodResponse(complexResponse());
		}
		return null;
	}
}
