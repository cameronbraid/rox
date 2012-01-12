package com.flat502.rox.client;

import java.net.URL;

import junit.framework.TestCase;

public class Test_SyncClientTimeouts extends TestCase {
	private static final Integer _1_SECOND = new Integer(1000);
	private static final Integer _2_SECONDS = new Integer(2000);
	
	private static final String PREFIX = "test";
	private static final int PORT = 8080;
	private static final String URL = "http://localhost:" + PORT + "/";

	public void testTimeoutNotExceeded() throws Exception {
		TestServer server = new TestServer(null, PREFIX, PORT);
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		client.setRequestTimeout(_2_SECONDS.longValue());
		try {
			Object rsp = client.execute("test.waitFor", new Object[]{_1_SECOND});
			assertNotNull(rsp);
			assertEquals(_1_SECOND, rsp);
		} finally {
			client.stop();
			server.stop();
		}
	}

	public void testTimeoutExceeded() throws Exception {
		TestServer server = new TestServer(null, PREFIX, PORT);
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		client.setRequestTimeout(_1_SECOND.longValue());
		try {
			client.execute("test.waitFor", new Object[]{_2_SECONDS});
			fail();
		} catch (RpcCallTimeoutException e) {
		} finally {
			client.stop();
			server.stop();
		}
	}

	public void testTimeoutExceededResourcePool() throws Exception {
		TestServer server = new TestServer(null, PREFIX, PORT);
		ClientResourcePool pool = new ClientResourcePool();
		pool.setRequestTimeout(_1_SECOND.longValue());
		XmlRpcClient client = new XmlRpcClient(new URL(URL), pool);
		try {
			client.execute("test.waitFor", new Object[]{_2_SECONDS});
			fail();
		} catch (RpcCallTimeoutException e) {
		} finally {
			client.stop();
			server.stop();
			pool.shutdown();
		}
	}

	public void testTimeoutExceededDuringConnect() throws Throwable {
		// Don't accept
		DumbServer server = new DumbServer(PORT, false);
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		client.setRequestTimeout(_1_SECOND.longValue());
		try {
			client.execute("test.waitFor", new Object[]{_2_SECONDS});
			fail();
		} catch (RpcCallTimeoutException e) {
		} finally {
			client.stop();
			server.shutdown();
		}
	}

	public void testTimeoutExceededDuringWrite() throws Throwable {
		// Accept but don't read 
		DumbServer server = new DumbServer(PORT, true);
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		client.setRequestTimeout(_1_SECOND.longValue());
		try {
			client.execute("test.waitFor", new Object[]{_2_SECONDS});
			fail();
		} catch (RpcCallTimeoutException e) {
		} finally {
			client.stop();
			server.shutdown();
		}
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_SyncClientTimeouts.class);
	}
}
