package com.flat502.rox.client;

import java.net.URL;

import junit.framework.TestCase;

import com.flat502.rox.marshal.Test_ClassDescriptor;

public class Test_ConnectionPoolTimeouts extends TestCase {
	private static final Integer _1_SECOND = new Integer(1000);
	private static final Integer _2_SECONDS = new Integer(2000);
	private static final Integer _5_SECONDS = new Integer(5000);

	private static final String PREFIX = "test";
	private static final int PORT = 8080;
	private static final String URL = "http://localhost:" + PORT + "/";

	private TestServer server;

	protected void setUp() throws Exception {
		this.server = new TestServer(null, PREFIX, PORT);
	}

	protected void tearDown() throws Exception {
		this.server.stop();
	}

	public void testTimeoutNotExceeded() throws Throwable {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		client.setConnectionPoolLimit(1);
		client.setConnectionPoolTimeout(_2_SECONDS.longValue());
		try {
			TestResponseHandler handler = new TestResponseHandler();
			// This will consume the only available connection for 1 second ...
			client.execute("test.waitFor", new Object[] { _1_SECOND }, handler);
			// ... so this should block but should NOT timeout
			client.execute("test.waitFor", new Object[] { _1_SECOND }, handler);
			Object rsp = handler.waitForResponse(_5_SECONDS.intValue());
			assertNotNull(rsp);
		} finally {
			client.stop();
		}
	}

	public void testTimeoutExceeded() throws Throwable {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		client.setConnectionPoolLimit(1);
		client.setConnectionPoolTimeout(_1_SECOND.longValue());
		try {
			TestResponseHandler handler = new TestResponseHandler();
			// This will consume the only available connection for 2 seconds ...
			client.execute("test.waitFor", new Object[] { _2_SECONDS }, handler);
			// ... so this should timeout
			client.execute("test.waitFor", new Object[] { _2_SECONDS }, handler);
			fail();
		} catch (ConnectionPoolTimeoutException e) {
		} finally {
			client.stop();
		}
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_ConnectionPoolTimeouts.class);
	}
}
