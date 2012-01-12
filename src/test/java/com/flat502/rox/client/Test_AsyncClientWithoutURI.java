package com.flat502.rox.client;

import java.io.IOException;
import java.net.URL;
import java.nio.channels.SocketChannel;

import junit.framework.TestCase;

import com.flat502.rox.processing.RpcFaultException;
import com.flat502.rox.processing.ThreadUtils;
import com.flat502.rox.utils.LoggingProfiler;

public class Test_AsyncClientWithoutURI extends TestCase {
	private static final Integer _1_SECOND = new Integer(1000);
	private static final Integer _2_SECONDS = new Integer(2000);
	private static final Integer _3_SECONDS = new Integer(3000);
	private static final Integer _4_SECONDS = new Integer(4000);
	private static final Integer _5_SECONDS = new Integer(5000);

	private static final Integer _200_MILLISECONDS = new Integer(200);
	private static final Integer _300_MILLISECONDS = new Integer(300);
	private static final Integer _400_MILLISECONDS = new Integer(400);
	private static final Integer _500_MILLISECONDS = new Integer(500);
	private static final Integer _600_MILLISECONDS = new Integer(600);
	private static final Integer _800_MILLISECONDS = new Integer(800);
	private static final Integer _1000_MILLISECONDS = new Integer(1000);
	
	private static final String PREFIX = "test";
	private static final int PORT = 8080;
	private static final String URL = "http://localhost:" + PORT + "/";

	private TestServer server;

	protected void setUp() throws Exception {
		ThreadUtils.assertZeroThreads();
		this.server = new TestServer(null, PREFIX, PORT);
	}

	protected void tearDown() throws Exception {
		this.server.stop();
		ThreadUtils.assertZeroThreads();
	}

	public void testStringResponse() throws Throwable {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		try {
			TestResponseHandler handler = new TestResponseHandler();
			client.execute("test.stringResponse", null, handler);
			Object rsp = handler.waitForResponse(_5_SECONDS.intValue());
			assertNotNull(rsp);
			assertEquals("bar", rsp);
		} finally {
			client.stop();
		}
	}

	public void testStringResponseWithNoTrailingSlashOnURL() throws Throwable {
		XmlRpcClient client = new XmlRpcClient(new URL("http://localhost:" + PORT));
		try {
			TestResponseHandler handler = new TestResponseHandler();
			client.execute("test.stringResponse", null, handler);
			Object rsp = handler.waitForResponse(_5_SECONDS.intValue());
			assertNotNull(rsp);
			assertEquals("bar", rsp);
		} finally {
			client.stop();
		}
	}

	public void testNullResponse() throws Throwable {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		try {
			TestResponseHandler handler = new TestResponseHandler();
			client.execute("test.nullResponse", null, handler);
			Object rsp = handler.waitForResponse(_5_SECONDS.intValue());
			fail();
		} catch(UnsupportedOperationException e) {
		} finally {
			client.stop();
		}
	}

	public void testXmlRpcFaultResponse() throws Throwable {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		try {
			TestResponseHandler handler = new TestResponseHandler();
			client.execute("test.returnXmlRpcMethofFault", null, handler);
			Object rsp = handler.waitForResponse(_5_SECONDS.intValue());
			fail();
		} catch(RpcFaultException e) {
			assertEquals(21, e.getFaultCode());
			assertEquals("Half the meaning of Life", e.getFaultString());
		} finally {
			client.stop();
		}
	}

	public void testXmlRpcFaultException() throws Throwable {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		try {
			TestResponseHandler handler = new TestResponseHandler();
			client.execute("test.raiseXmlRpcFaultException", null, handler);
			Object rsp = handler.waitForResponse(_5_SECONDS.intValue());
			fail();
		} catch(RpcFaultException e) {
			assertEquals(42, e.getFaultCode());
			assertEquals("The meaning of Life", e.getFaultString());
		} finally {
			client.stop();
		}
	}

	public void testOtherException() throws Throwable {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		try {
			TestResponseHandler handler = new TestResponseHandler();
			client.execute("test.raiseOtherException", null, handler);
			Object rsp = handler.waitForResponse(_5_SECONDS.intValue());
			fail();
		} catch(UnsupportedOperationException e) {
			assertTrue(e.getMessage().indexOf("Another Exception") != -1);
		} finally {
			client.stop();
		}
	}
	
	public void testCloseClientAndReturnConnectionOnSameURL() throws Throwable {
		ClientResourcePool pool = new ClientResourcePool();
		XmlRpcClient clientA = new XmlRpcClient(new URL(URL), pool);
		XmlRpcClient clientB = new XmlRpcClient(new URL(URL), pool);
		try {
			TestResponseHandler handlerA = new TestResponseHandler();
			TestResponseHandler handlerB = new TestResponseHandler();
			
			// This will check out one connection from the pool
			clientA.execute("test.waitFor", new Object[]{_1_SECOND}, handlerA);
			
			// This will check out a second connection for the same URL
			clientB.execute("test.waitFor", new Object[]{_4_SECONDS}, handlerB);
			
			// Now wait until the first one is returned
			handlerA.waitForResponse(_2_SECONDS.intValue());
			
			// Close the first client
			clientA.stop();
			
			// And then try to return the second connection
			handlerB.waitForResponse(_5_SECONDS.intValue());
		} finally {
			clientA.stop();
			clientB.stop();
			pool.shutdown();
		}
	}
	
	public void testCloseClientAndCheckOutReturnedConnectionOnSameURL() throws Throwable {
		final SharedSocketChannelPoolProxy[] proxyRef = new SharedSocketChannelPoolProxy[1];
		ClientResourcePool pool = new ClientResourcePool() {
			protected SharedSocketChannelPool newSharedSocketChannelPool(Object mutex, int limit, long timeout) {
				synchronized(proxyRef) {
					if (proxyRef[0] == null) {
						proxyRef[0] = new SharedSocketChannelPoolProxy(super.newSharedSocketChannelPool(mutex, limit, timeout));
					}
				}
				return proxyRef[0];
			}
		};
		XmlRpcClient clientA = new XmlRpcClient(new URL(URL), pool);
		XmlRpcClient clientB = new XmlRpcClient(new URL(URL), pool);
		try {
			TestResponseHandler handlerA = new TestResponseHandler();
			TestResponseHandler handlerB = new TestResponseHandler();
			TestResponseHandler handlerC = new TestResponseHandler();
			
			// This will check out one connection from the pool
			clientA.execute("test.waitFor", new Object[]{_200_MILLISECONDS}, handlerA);
			
			// This will check out a second connection for the same URL
			clientB.execute("test.waitFor", new Object[]{_800_MILLISECONDS}, handlerB);
			SocketChannel channel1 = proxyRef[0].getLastChannel();
			
			// Now wait until the first one is returned
			handlerA.waitForResponse(_600_MILLISECONDS.intValue());
			Thread.sleep(_200_MILLISECONDS.intValue());
			
			// Close the first client
			clientA.stop();
			
			// Then try to return the second connection
			handlerB.waitForResponse(_1000_MILLISECONDS.intValue());
			Thread.sleep(_200_MILLISECONDS.intValue());
			
			// And check out another connection. This should literally be the same
			// connection.
			clientB.execute("test.waitFor", new Object[]{_200_MILLISECONDS}, handlerC);
			SocketChannel channel2 = proxyRef[0].getLastChannel();
			handlerC.waitForResponse(_400_MILLISECONDS.intValue());
			
			assertTrue(channel1 == channel2);
		} finally {
			clientA.stop();
			clientB.stop();
			pool.shutdown();
		}
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_AsyncClientWithoutURI.class);
	}

	private class SharedSocketChannelPoolProxy extends SharedSocketChannelPool {
		private SocketChannel lastChannel;
		private SharedSocketChannelPool pool;
	
		public SharedSocketChannelPoolProxy(SharedSocketChannelPool pool) {
			super(new Object(), 0, 0, new LoggingProfiler());
			this.pool = pool;
		}
		
		public SocketChannel getChannel(HttpRpcClient client) throws IOException {
			this.lastChannel = this.pool.getChannel(client);
			return lastChannel;
		}
		
		public void returnChannel(HttpRpcClient client, SocketChannel channel) {
			this.pool.returnChannel(client, channel);
		}
		
		public void removeChannel(HttpRpcClient client, SocketChannel channel) {
			this.pool.removeChannel(client, channel);
		}
		
		void removeClosedChannel(SocketChannel channel) {
			this.pool.removeClosedChannel(channel);
		}
		
		public void close() {
			this.pool.close();
		}
		
		public void detach(HttpRpcClient client) {
			this.pool.detach(client);
		}
		
		public SocketChannel getLastChannel() {
			return this.lastChannel;
		}
	}
}
