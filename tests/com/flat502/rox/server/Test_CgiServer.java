package com.flat502.rox.server;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import junit.framework.TestCase;

import com.flat502.rox.http.HttpConstants;
import com.flat502.rox.marshal.RpcResponse;
import com.flat502.rox.marshal.xmlrpc.DomMethodResponseUnmarshaller;
import com.flat502.rox.marshal.xmlrpc.XmlRpcMethodFault;

public class Test_CgiServer extends TestCase {
	private static final String PREFIX = "test";
	private static final int PORT = 8080;
	private static final String URL = "http://localhost:" + PORT + "/";

	private XmlRpcServer server;

	protected void setUp() throws Exception {
		this.server = new XmlRpcServer(PORT);
	}

	protected void tearDown() throws Exception {
		this.server.stop();
		Thread.sleep(50);
	}

	public void testSimpleGet() throws Exception {
		ManualSynchronousHandler handler = new ManualSynchronousHandler();
		server.registerRequestUnmarshaller(HttpConstants.Methods.GET, new CgiRequestUnmarshaller());
		server.registerHandler(null, "^server\\.", handler);
		server.start();
		URLConnection conn = new URL(URL + "server.toUpper?hello+world").openConnection();
		InputStream is = null;
		try {
			conn.connect();
			is = conn.getInputStream();
			RpcResponse rsp = new DomMethodResponseUnmarshaller().unmarshal(is);
			assertEquals("HELLO WORLD", rsp.getReturnValue());
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	public void testGetReturnsFault() throws Exception {
		ManualSynchronousHandler handler = new ManualSynchronousHandler();
		server.registerRequestUnmarshaller(HttpConstants.Methods.GET, new CgiRequestUnmarshaller());
		server.registerHandler(null, "^server\\.", handler);
		server.start();
		URLConnection conn = new URL(URL + "server.returnFault").openConnection();
		InputStream is = null;
		try {
			conn.connect();
			is = conn.getInputStream();
			RpcResponse rsp = new DomMethodResponseUnmarshaller().unmarshal(is);
			assertTrue(rsp instanceof XmlRpcMethodFault);
			assertEquals("return error", ((XmlRpcMethodFault)rsp).getFaultString());
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}
	
	public void testGetRaiseFault() throws Exception {
		ManualSynchronousHandler handler = new ManualSynchronousHandler();
		server.registerRequestUnmarshaller(HttpConstants.Methods.GET, new CgiRequestUnmarshaller());
		server.registerHandler(null, "^server\\.", handler);
		server.start();
		URLConnection conn = new URL(URL + "server.raiseFault").openConnection();
		InputStream is = null;
		try {
			conn.connect();
			is = conn.getInputStream();
			RpcResponse rsp = new DomMethodResponseUnmarshaller().unmarshal(is);
			assertTrue(rsp instanceof XmlRpcMethodFault);
			assertEquals("raise error", ((XmlRpcMethodFault)rsp).getFaultString());
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_CgiServer.class);
	}
}
