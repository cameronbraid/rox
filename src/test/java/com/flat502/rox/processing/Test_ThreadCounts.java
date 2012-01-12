package com.flat502.rox.processing;

import java.net.URL;

import junit.framework.TestCase;

import com.flat502.rox.client.ClientResourcePool;
import com.flat502.rox.client.TestServer;
import com.flat502.rox.client.XmlRpcClient;
import com.flat502.rox.server.XmlRpcServer;

public class Test_ThreadCounts extends TestCase {
	private static final Integer _1_SECOND = new Integer(1000);
	private static final Integer _2_SECONDS = new Integer(2000);
	
	private static final String PREFIX = "test";
	private static final int PORT = 8080;
	private static final String URL = "http://localhost:" + PORT + "/";
	
	protected void setUp() throws Exception {
		ThreadUtils.assertZeroThreads();
	}
	
	protected void tearDown() throws Exception {
		ThreadUtils.assertZeroThreads();
	}
	
	public void testClientImplicitWorkerPoolImplicitWorker() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		
		try {
			assertEquals("Client Workers", 1, ThreadUtils.countClientWorkerThreads());
			assertEquals("Server Workers", 0, ThreadUtils.countServerWorkerThreads());
			assertEquals("Selector", 1, ThreadUtils.countSelectorThreads());
			assertEquals("Timer", 0, ThreadUtils.countWorkerPoolTimerThreads());
		} finally {
			client.stop();
		}
	}
	
	public void testClientImplicitWorkerPoolExplicitWorkerTest1() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		client.addWorker();
		client.addWorker();
		
		try {
			assertEquals("Client Workers", 2, ThreadUtils.countClientWorkerThreads());
			assertEquals("Server Workers", 0, ThreadUtils.countServerWorkerThreads());
			assertEquals("Selector", 1, ThreadUtils.countSelectorThreads());
			assertEquals("Timer", 0, ThreadUtils.countWorkerPoolTimerThreads());
		} finally {
			client.stop();
		}
	}
	
	public void testServerImplicitWorkerPoolImplicitWorker() throws Exception {
		XmlRpcServer server = new XmlRpcServer(PORT);
		server.start();
		
		try {
			assertEquals("Client Workers", 0, ThreadUtils.countClientWorkerThreads());
			assertEquals("Server Workers", 1, ThreadUtils.countServerWorkerThreads());
			assertEquals("Selector", 1, ThreadUtils.countSelectorThreads());
			assertEquals("Timer", 0, ThreadUtils.countWorkerPoolTimerThreads());
		} finally {
			server.stop();
		}
	}

	public void testPrivateWorkerPools() throws Exception {
		TestServer server = new TestServer(null, PREFIX, PORT);

		XmlRpcClient clientA = new XmlRpcClient(new URL(URL));
		XmlRpcClient clientB = new XmlRpcClient(new URL(URL));
		clientA.setRequestTimeout(_2_SECONDS.longValue());
		clientB.setRequestTimeout(_2_SECONDS.longValue());
		try {
			assertEquals("Client Workers", 2, ThreadUtils.countClientWorkerThreads());
			assertEquals("Server Workers", 1, ThreadUtils.countServerWorkerThreads());
			assertEquals("Selector", 3, ThreadUtils.countSelectorThreads());
			assertEquals("Timer", 2, ThreadUtils.countWorkerPoolTimerThreads());
		} finally {
			clientA.stop();
			clientB.stop();
			server.stop();
		}
	}

	public void testSharedWorkerPool() throws Exception {
		TestServer server = new TestServer(null, PREFIX, PORT);
		
		ClientResourcePool pool = new ClientResourcePool();
		XmlRpcClient clientA = new XmlRpcClient(new URL(URL), pool);
		XmlRpcClient clientB = new XmlRpcClient(new URL(URL), pool);
		XmlRpcClient clientC = new XmlRpcClient(new URL(URL), pool);
		XmlRpcClient clientD = new XmlRpcClient(new URL(URL), pool);
		clientA.setRequestTimeout(_2_SECONDS.longValue());
		clientB.setRequestTimeout(_2_SECONDS.longValue());
		try {
			assertEquals("Client Workers", 1, ThreadUtils.countClientWorkerThreads());
			assertEquals("Server Workers", 1, ThreadUtils.countServerWorkerThreads());
			assertEquals("Selector", 2, ThreadUtils.countSelectorThreads());
			assertEquals("Timer", 1, ThreadUtils.countWorkerPoolTimerThreads());
		} finally {
			clientA.stop();
			clientB.stop();
			server.stop();
			pool.shutdown();
		}
	}
	
	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_ThreadCounts.class);
	}
}
