package com.flat502.rox.client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLServerSocketFactory;

import junit.framework.TestCase;

import com.flat502.rox.marshal.MethodResponseUnmarshaller;
import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.utils.LoggingProfiler;

public class Test_SharedSocketChannelPool extends TestCase {
	private static final int PORT = 8080;
	private static final String URL1 = "http://localhost:" + PORT;
	private static final String URL2 = "http://127.0.0.1:" + PORT;

	private Server server;

	protected void setUp() throws Exception {
		this.server = new Server();
		this.server.start();
	}

	protected void tearDown() throws Exception {
		this.server.shutdown();
	}

	public void testImplicitPort() throws Exception {
		SharedSocketChannelPool pool = newSharedSocketChannelPool();
		MockHttpRpcClient client = new MockHttpRpcClient("http://localhost");
		try {
			pool.getChannel(client);
		} finally {
			client.stop();
			pool.close();
		}
	}

	public void testCloseWithOutstandingChannels() throws Exception {
		MockHttpRpcClient client = new MockHttpRpcClient(URL1);
		SocketChannel ch = null;
		SharedSocketChannelPool pool = newSharedSocketChannelPool();
		try {
			ch = pool.getChannel(client);
			assertTrue(ch.isOpen());
		} finally {
			client.stop();
			pool.close();
			assertTrue(ch.isOpen());
		}
	}

	public void testCloseWithoutOutstandingChannels() throws Exception {
		MockHttpRpcClient client = new MockHttpRpcClient(URL1);
		SocketChannel ch = null;
		SharedSocketChannelPool pool = newSharedSocketChannelPool();
		try {
			ch = pool.getChannel(client);
			assertTrue(ch.isOpen());
			pool.returnChannel(client, ch);
		} finally {
			client.stop();
			pool.close();
			assertFalse(ch.isOpen());
		}
	}

	public void testReturnedByWrongClient() throws Exception {
		MockHttpRpcClient clientA = new MockHttpRpcClient(URL1);
		MockHttpRpcClient clientB = new MockHttpRpcClient(URL2);
		SharedSocketChannelPool pool = newSharedSocketChannelPool();
		try {
			SocketChannel ch = pool.getChannel(clientA);
			pool.returnChannel(clientB, ch);
			fail();
		} catch (IllegalArgumentException e) {
		} finally {
			clientA.stop();
			clientB.stop();
			pool.close();
		}
	}

	public void testRemovedByWrongClient() throws Exception {
		MockHttpRpcClient clientA = new MockHttpRpcClient(URL1);
		MockHttpRpcClient clientB = new MockHttpRpcClient(URL2);
		SharedSocketChannelPool pool = newSharedSocketChannelPool();
		try {
			SocketChannel ch = pool.getChannel(clientA);
			pool.removeChannel(clientB, ch);
			fail();
		} catch (IllegalArgumentException e) {
		} finally {
			clientA.stop();
			clientB.stop();
			pool.close();
		}
	}

	public void testOneClientTwoConnections() throws Exception {
		MockHttpRpcClient client = new MockHttpRpcClient(URL1);
		SharedSocketChannelPool pool = newSharedSocketChannelPool();
		try {
			SocketChannel ch1 = pool.getChannel(client);
			SocketChannel ch2 = pool.getChannel(client);
			pool.returnChannel(client, ch1);
			pool.returnChannel(client, ch2);
		} finally {
			client.stop();
			pool.close();
		}
	}

	public void testBasicPooling() throws Exception {
		MockHttpRpcClient client = new MockHttpRpcClient(URL1);
		SharedSocketChannelPool pool = newSharedSocketChannelPool();
		try {
			SocketChannel ch1 = pool.getChannel(client);
			pool.returnChannel(client, ch1);
			SocketChannel ch2 = pool.getChannel(client);
			pool.returnChannel(client, ch2);
			assertTrue(ch2 == ch1);
		} finally {
			client.stop();
			pool.close();
		}
	}
	
	public void testUnusedConnectionRemoved() throws Exception {
		MockHttpRpcClient client = new MockHttpRpcClient(URL1);
		SharedSocketChannelPool pool = newSharedSocketChannelPool();
		try {
			SocketChannel ch1 = pool.getChannel(client);
			pool.returnChannel(client, ch1);
			
			SocketChannel ch2 = pool.getChannel(client);
			assertTrue(ch1 == ch2);
			pool.returnChannel(client, ch2);
			
			Thread.sleep(6000);
			
			SocketChannel ch3 = pool.getChannel(client);
			assertTrue(ch1 != ch3);
			pool.returnChannel(client, ch3);
		} finally {
			client.stop();
			pool.close();
		}
	}

	public void testUnusedConnectionRemovedTwice() throws Exception {
		MockHttpRpcClient client = new MockHttpRpcClient(URL1);
		SharedSocketChannelPool pool = newSharedSocketChannelPool();
		try {
			SocketChannel ch1 = pool.getChannel(client);
			pool.returnChannel(client, ch1);
			
			SocketChannel ch2 = pool.getChannel(client);
			assertTrue(ch1 == ch2);
			pool.returnChannel(client, ch2);
			
			Thread.sleep(6000);
			
			SocketChannel ch3 = pool.getChannel(client);
			assertTrue(ch1 != ch3);
			pool.returnChannel(client, ch3);

			// And again, to test for a previous bug
			
			SocketChannel ch4 = pool.getChannel(client);
			pool.returnChannel(client, ch4);
			
			SocketChannel ch5 = pool.getChannel(client);
			assertTrue(ch4 == ch5);
			pool.returnChannel(client, ch5);
			
			Thread.sleep(6000);
			
			SocketChannel ch6 = pool.getChannel(client);
			assertTrue(ch4 != ch6);
			pool.returnChannel(client, ch6);
		} finally {
			client.stop();
			pool.close();
		}
	}

	public void testRemoveConnection() throws Exception {
		MockHttpRpcClient client = new MockHttpRpcClient(URL1);
		SharedSocketChannelPool pool = newSharedSocketChannelPool();
		try {
			SocketChannel ch1 = pool.getChannel(client);
			pool.removeChannel(client, ch1);
			SocketChannel ch2 = pool.getChannel(client);
			pool.returnChannel(client, ch2);
			assertTrue(ch2 != ch1);
		} finally {
			client.stop();
			pool.close();
		}
	}

	public void testForcedConnectionReplacement() throws Exception {
		MockHttpRpcClient clientA = new MockHttpRpcClient(URL1);
		MockHttpRpcClient clientB = new MockHttpRpcClient(URL2);
		SharedSocketChannelPool pool = new SharedSocketChannelPool(new Object(), 2, 0, new LoggingProfiler());
		try {
			SocketChannel ch1 = pool.getChannel(clientA);
			SocketChannel ch2 = pool.getChannel(clientB);
			pool.returnChannel(clientB, ch2);
			// Now ask for another connection for A.
			SocketChannel ch3 = pool.getChannel(clientA);
			// Put it back and ask for another for B. It should be new.
			pool.returnChannel(clientA, ch1);
			SocketChannel ch4 = pool.getChannel(clientB);
			pool.returnChannel(clientA, ch3);
			pool.returnChannel(clientB, ch4);
			assertTrue(ch2 != ch4);
		} finally {
			clientA.stop();
			clientB.stop();
			pool.close();
		}
	}

	public void testTwoClientsSameAddress() throws Exception {
		MockHttpRpcClient clientA = new MockHttpRpcClient(URL1);
		MockHttpRpcClient clientB = new MockHttpRpcClient(URL1);
		SharedSocketChannelPool pool = newSharedSocketChannelPool();
		try {
			SocketChannel ch1 = pool.getChannel(clientA);
			pool.returnChannel(clientA, ch1);
			SocketChannel ch2 = pool.getChannel(clientB);
			pool.returnChannel(clientB, ch2);
			assertTrue(ch2 == ch1);
		} finally {
			clientA.stop();
			clientB.stop();
			pool.close();
		}
	}

	public void testTwoClientsDifferentProtocol() throws Exception {
		Server sslServer = new Server(PORT+1, true);
		sslServer.start();
		MockHttpRpcClient clientA = new MockHttpRpcClient("http://localhost:" + (PORT+1));
		MockHttpRpcClient clientB = new MockHttpRpcClient("https://localhost:" + (PORT+1));
		SharedSocketChannelPool pool = newSharedSocketChannelPool();
		try {
			SocketChannel ch1 = pool.getChannel(clientA);
			pool.returnChannel(clientA, ch1);
			SocketChannel ch2 = pool.getChannel(clientB);
			pool.returnChannel(clientB, ch2);
			assertTrue(ch2 != ch1);
		} finally {
			clientA.stop();
			clientB.stop();
			pool.close();
			sslServer.shutdown();
		}
	}

	public void testTwoClientsDifferentHost() throws Exception {
		MockHttpRpcClient clientA = new MockHttpRpcClient(URL1);
		MockHttpRpcClient clientB = new MockHttpRpcClient(URL2);
		SharedSocketChannelPool pool = newSharedSocketChannelPool();
		try {
			SocketChannel ch1 = pool.getChannel(clientA);
			SocketChannel ch2 = pool.getChannel(clientB);
			pool.returnChannel(clientA, ch1);
			pool.returnChannel(clientB, ch2);
		} finally {
			clientA.stop();
			clientB.stop();
			pool.close();
		}
	}

	public void testTwoClientsDifferentPort() throws Exception {
		Server server2 = new Server(PORT+1, false);
		server2.start();
		MockHttpRpcClient clientA = new MockHttpRpcClient("http://localhost:" + PORT);
		MockHttpRpcClient clientB = new MockHttpRpcClient("http://localhost:" + (PORT+1));
		SharedSocketChannelPool pool = newSharedSocketChannelPool();
		try {
			SocketChannel ch1 = pool.getChannel(clientA);
			pool.returnChannel(clientA, ch1);
			SocketChannel ch2 = pool.getChannel(clientB);
			pool.returnChannel(clientB, ch2);
			assertTrue(ch2 != ch1);
		} finally {
			clientA.stop();
			clientB.stop();
			pool.close();
			server2.shutdown();
		}
	}

	private SharedSocketChannelPool newSharedSocketChannelPool() {
		return new SharedSocketChannelPool(new Object(), 0, 0, new LoggingProfiler());
	}

	public void testOneClientTimeout() throws Exception {
		final MockHttpRpcClient client = new MockHttpRpcClient(URL1);
		final SharedSocketChannelPool pool = new SharedSocketChannelPool(new Object(), 1, 1000, new LoggingProfiler());
		final boolean[] timedOut = new boolean[1];
		final Exception[] unexpectedException = new Exception[1];
		try {
			SocketChannel ch1 = pool.getChannel(client);
			new Thread() {
				public void run() {
					try {
						SocketChannel ch2 = pool.getChannel(client);
						// Should block here and eventually time out
					} catch (ConnectionPoolTimeoutException e) {
						timedOut[0] = true;
					} catch (IOException e) {
						unexpectedException[0] = e;
					}
				};
			}.start();
			Thread.sleep(2000);
			if (unexpectedException[0] != null) {
				throw unexpectedException[0];
			}
			assertTrue(timedOut[0]);
			pool.returnChannel(client, ch1);
		} finally {
			client.stop();
			pool.close();
		}
	}

	public void testOneClientBlocksWithoutTimeout() throws Exception {
		final MockHttpRpcClient client = new MockHttpRpcClient(URL1);
		final SharedSocketChannelPool pool = new SharedSocketChannelPool(new Object(), 1, 5000, new LoggingProfiler());
		final boolean[] timedOut = new boolean[1];
		final Exception[] unexpectedException = new Exception[1];
		try {
			SocketChannel ch1 = pool.getChannel(client);
			new Thread() {
				public void run() {
					try {
						SocketChannel ch2 = pool.getChannel(client);
						// Should block here but we don't expect a timeout
					} catch (ConnectionPoolTimeoutException e) {
						timedOut[0] = true;
					} catch (IOException e) {
						unexpectedException[0] = e;
					}
				};
			}.start();
			long now = System.currentTimeMillis();
			Thread.sleep(1000);
			long elapsed = System.currentTimeMillis() - now;
			if (unexpectedException[0] != null) {
				throw unexpectedException[0];
			}
			assertFalse(timedOut[0]);
			assertTrue(elapsed >= 900);
			pool.returnChannel(client, ch1);
		} finally {
			client.stop();
			pool.close();
		}
	}

	private class MockHttpRpcClient extends HttpRpcClient {
		protected MockHttpRpcClient(String urlString) throws IOException {
			super(new URL(urlString), null);
		}

		protected RpcCall newRpcCall(String name, Object[] params) {
			return null;
		}

		protected MethodResponseUnmarshaller getMethodResponseUnmarshaller() {
			return null;
		}

		protected String getContentType() {
			return null;
		}

		void register(SocketChannel channel) {
		}

		void cancel(SocketChannel channel) {
		}
	}

	private class Server extends Thread {
		private int port;
		private boolean useHttps;
		private IOException exception;

		private ServerSocket serverSocket;
		
		public Server() {
			this.port = PORT;
		}

		public Server(int port, boolean useHttps) {
			this.port = port;
			this.useHttps = useHttps;
		}

		public void run() {
			try {
				if (this.useHttps) {
					this.serverSocket = SSLServerSocketFactory.getDefault().createServerSocket(this.port);
				} else {
					this.serverSocket = new ServerSocket(this.port);
				}
			} catch (IOException e) {
				this.exception = e;
			}
		}

		public void shutdown() throws IOException {
			if (this.exception != null) {
				throw this.exception;
			}
			while (this.serverSocket == null)
				Thread.yield();
			this.serverSocket.close();
		}
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_SharedSocketChannelPool.class);
	}
}
