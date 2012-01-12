package com.flat502.rox.client;

import java.net.URL;

import com.flat502.rox.log.Level;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.log.SimpleLogFactory;
import com.flat502.rox.log.StreamLog;
import com.flat502.rox.processing.ThreadUtils;

import junit.framework.TestCase;

public class Test_AsyncClientTimeouts extends TestCase {
	private static final Integer _1_SECOND = new Integer(1000);
	private static final Integer _2_SECONDS = new Integer(2000);
	private static final Integer _5_SECONDS = new Integer(5000);
	
	private static final String PREFIX = "test";
	private static final int PORT = 8080;
	private static final String URL = "http://localhost:" + PORT + "/";
	
	protected void setUp() throws Exception {
		ThreadUtils.assertZeroThreads();
	}
	
	protected void tearDown() throws Exception {
		ThreadUtils.assertZeroThreads();
	}
	
	public void testTimeoutNotExceeded() throws Throwable {
		TestServer server = new TestServer(null, PREFIX, PORT);
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		client.setRequestTimeout(_2_SECONDS.longValue());
		try {
			TestResponseHandler handler = new TestResponseHandler();
			client.execute("test.waitFor", new Object[]{_1_SECOND}, handler);
			Object rsp = handler.waitForResponse(_5_SECONDS.intValue());
			assertNotNull(rsp);
			assertEquals(_1_SECOND, rsp);
		} finally {
			client.stop();
			server.stop();
		}
	}

	public void testTimeoutExceeded() throws Throwable {
		TestServer server = new TestServer(null, PREFIX, PORT);
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		client.setRequestTimeout(_1_SECOND.longValue());
		try {
			TestResponseHandler handler = new TestResponseHandler();
			client.execute("test.waitFor", new Object[]{_2_SECONDS}, handler);
			Object rsp = handler.waitForResponse(_5_SECONDS.intValue());
			fail();
		} catch (RpcCallTimeoutException e) {
		} finally {
			client.stop();
			server.stop();
		}
	}

	public void testTimeoutExceededResourcePool() throws Throwable {
		TestServer server = new TestServer(null, PREFIX, PORT);
		ClientResourcePool pool = new ClientResourcePool();
		pool.setRequestTimeout(_1_SECOND.longValue());
		XmlRpcClient client = new XmlRpcClient(new URL(URL), pool);
		try {
			TestResponseHandler handler = new TestResponseHandler();
			client.execute("test.waitFor", new Object[]{_2_SECONDS}, handler);
			Object rsp = handler.waitForResponse(_5_SECONDS.intValue());
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
			TestResponseHandler handler = new TestResponseHandler();
			client.execute("test.waitFor", new Object[]{_2_SECONDS}, handler);
			Object rsp = handler.waitForResponse(_5_SECONDS.intValue());
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
			TestResponseHandler handler = new TestResponseHandler();
			client.execute("test.waitFor", new Object[]{_2_SECONDS}, handler);
			Object rsp = handler.waitForResponse(_5_SECONDS.intValue());
			fail();
		} catch (RpcCallTimeoutException e) {
		} finally {
			client.stop();
			server.shutdown();
		}
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_AsyncClientTimeouts.class);
	}
}
