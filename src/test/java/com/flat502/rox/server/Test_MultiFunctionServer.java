package com.flat502.rox.server;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.flat502.rox.client.XmlRpcClient;
import com.flat502.rox.http.HttpConstants;
import com.flat502.rox.http.HttpRequestBuffer;
import com.flat502.rox.marshal.MethodCallUnmarshallerAid;
import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.RpcResponse;
import com.flat502.rox.marshal.xmlrpc.DomMethodResponseUnmarshaller;
import com.flat502.rox.marshal.xmlrpc.SaxMethodCallUnmarshaller;

public class Test_MultiFunctionServer extends TestCase {
	private static final String PREFIX = "test";
	private static final int PORT = 8080;
	private static final String URL = "http://localhost:" + PORT + "/";

	private XmlRpcServer server;
	private Cgi cgi;
	private XmlRpc xmlrpc;
	private ManualSynchronousHandler handler;

	private static class Cgi extends CgiRequestUnmarshaller {
		public HttpRequestBuffer request;

		public RpcCall unmarshal(HttpRequestBuffer request, MethodCallUnmarshallerAid aid) throws Exception {
			this.request = request;
			return super.unmarshal(request, aid);
		}
	}

	private static class XmlRpc extends XmlRpcRequestUnmarshaller {
		public HttpRequestBuffer request;

		public XmlRpc() {
			super(new SaxMethodCallUnmarshaller());
		}

		public RpcCall unmarshal(HttpRequestBuffer request, MethodCallUnmarshallerAid aid) throws Exception {
			this.request = request;
			return super.unmarshal(request, aid);
		}
	}

	protected void setUp() throws Exception {
		this.cgi = new Cgi();
		this.xmlrpc = new XmlRpc();
		this.handler = new ManualSynchronousHandler();

		this.server = new XmlRpcServer(PORT);
		this.server.registerRequestUnmarshaller(HttpConstants.Methods.GET, cgi);
		this.server.registerRequestUnmarshaller(HttpConstants.Methods.POST, xmlrpc);
		this.server.registerHandler(null, "^server\\.", handler);
		this.server.start();
	}

	protected void tearDown() throws Exception {
		this.server.stop();
		Thread.sleep(50);
	}

	public void testSimpleGet() throws Exception {
		URLConnection conn = new URL(URL + "server.toUpper?hello+world").openConnection();
		InputStream is = null;
		try {
			conn.connect();
			is = conn.getInputStream();
			RpcResponse rsp = new DomMethodResponseUnmarshaller().unmarshal(is);
			assertEquals("HELLO WORLD", rsp.getReturnValue());
			assertNotNull(cgi.request);
			assertNull(xmlrpc.request);
			
			HttpRequestBuffer request = cgi.request;
			assertEquals("GET", request.getMethod());
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	public void testCGIMap() throws Exception {
		URLConnection conn = new URL(URL + "server.map?foo=bar&key=value&list=one&list=two&list=three").openConnection();
		InputStream is = null;
		try {
			conn.connect();
			is = conn.getInputStream();
			RpcResponse rsp = new DomMethodResponseUnmarshaller().unmarshal(is);
			assertEquals(new Integer(3), rsp.getReturnValue());
			assertNotNull(cgi.request);
			assertNull(xmlrpc.request);
			
			HttpRequestBuffer request = cgi.request;
			assertEquals("GET", request.getMethod());
			
			assertEquals("server.map", handler.call.getName());
			assertNotNull(handler.call.getParameters());
			assertEquals(1, handler.call.getParameters().length);
			assertTrue(handler.call.getParameters()[0] instanceof Map);
			
			Map map = (Map) handler.call.getParameters()[0];
			assertEquals("bar", map.get("foo"));
			assertEquals("value", map.get("key"));
			assertTrue(map.get("list") instanceof List);
			
			List list = (List) map.get("list");
			assertEquals(3, list.size());
			assertTrue(list.contains("one"));
			assertTrue(list.contains("two"));
			assertTrue(list.contains("three"));
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	public void testSimpleXmlRpcCall() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		try {
			Object rsp = client.execute("server.toUpper", new Object[] { "thanks for all the fish" });
			assertEquals("THANKS FOR ALL THE FISH", rsp);
			assertNull(cgi.request);
			assertNotNull(xmlrpc.request);
			
			HttpRequestBuffer request = xmlrpc.request;
			assertEquals("POST", request.getMethod());
		} finally {
			client.stop();
		}
	}

	public void testXmlRpcMap() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		try {
			Object rsp = client.execute("server.map", new Object[] { new HashMap<String, Object>() {
				{
					put("foo", "bar");
					put("key", "value");
					put("list", new String[]{"one", "two", "three"});
				}
			} });
			assertEquals(new Integer(3), rsp);
			assertNull(cgi.request);
			assertNotNull(xmlrpc.request);
			
			HttpRequestBuffer request = xmlrpc.request;
			assertEquals("POST", request.getMethod());
			
			assertEquals("server.map", handler.call.getName());
			assertNotNull(handler.call.getParameters());
			assertEquals(1, handler.call.getParameters().length);
			assertTrue(handler.call.getParameters()[0] instanceof Map);
			
			Map map = (Map) handler.call.getParameters()[0];
			assertEquals("bar", map.get("foo"));
			assertEquals("value", map.get("key"));
			assertTrue(map.get("list") instanceof List);
			
			List list = (List) map.get("list");
			assertEquals(3, list.size());
			assertTrue(list.contains("one"));
			assertTrue(list.contains("two"));
			assertTrue(list.contains("three"));
		} finally {
			client.stop();
		}
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_MultiFunctionServer.class);
	}
}
