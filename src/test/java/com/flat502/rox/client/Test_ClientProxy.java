package com.flat502.rox.client;

import java.net.URL;
import java.util.Map;

import junit.framework.TestCase;

import com.flat502.rox.processing.RpcFaultException;

public class Test_ClientProxy extends TestCase {
	private static final String PREFIX = "test";
	private static final int PORT = 8080;
	private static final String URL = "http://localhost:" + PORT + "/";

	private TestServer server;

	protected void setUp() throws Exception {
		this.server = new TestServer("/", PREFIX, PORT);
	}

	protected void tearDown() throws Exception {
		this.server.stop();
	}

	public void testReturnsObject() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		client.proxyObject(PREFIX, ProxyAPI.class);
		try {
			Object rsp = client.execute("test.returnsObject", null);
			assertNotNull(rsp);
			assertTrue(rsp instanceof Map);
		} finally {
			client.stop();
		}
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_ClientProxy.class);
	}
}
