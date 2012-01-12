package com.flat502.rox.server;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;

import junit.framework.TestCase;

import com.flat502.rox.client.XmlRpcClient;
import com.flat502.rox.processing.ThreadUtils;

public class Test_ManualSynchronousServer extends TestCase {
	private static final String PREFIX = "test";
	private static final int PORT = 8080;
	private static final String URL = "http://localhost:" + PORT + "/";
	
	private XmlRpcServer server;

	protected void setUp() throws Exception {
		ThreadUtils.assertZeroThreads();
		this.server = new XmlRpcServer(PORT);
	}
	
	protected void tearDown() throws Exception {
		this.server.stop();
		Thread.sleep(50);
		ThreadUtils.assertZeroThreads();
	}
	
	public void testWildCardURI() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
		ManualSynchronousHandler handler = new ManualSynchronousHandler();
		server.registerHandler(null, "^server\\.", handler);
		server.start();
		try {
			Object rsp = client.execute("server.toUpper", new Object[]{"hello world"});
			assertEquals("HELLO WORLD", rsp);
		} finally {
			client.stop();
		}
	}

	public void testExplicitURIMatch() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
		ManualSynchronousHandler handler = new ManualSynchronousHandler();
		server.registerHandler("/foobar", "^server\\.", handler);
		server.start();
		try {
			Object rsp = client.execute("server.toUpper", new Object[]{"hello world"});
			assertEquals("HELLO WORLD", rsp);
		} finally {
			client.stop();
		}
	}

	public void testExplicitURINoMatch() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL+"nomatch"));
		ManualSynchronousHandler handler = new ManualSynchronousHandler();
		server.registerHandler("/foobar", "^server\\.", handler);
		server.start();
		try {
			Object rsp = client.execute("server.toUpper", new Object[]{"hello world"});
			fail();
		} catch(NoSuchMethodException e) {
			assertTrue(e.getMessage().startsWith("404: "));
		} finally {
			client.stop();
		}
	}

	public void testExplicitMethodPrefixNoMatch() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
		ManualSynchronousHandler handler = new ManualSynchronousHandler();
		server.registerHandler("/foobar", "^server\\.", handler);
		server.start();
		try {
			Object rsp = client.execute("wrong.toUpper", new Object[]{"hello world"});
			fail();
		} catch(NoSuchMethodException e) {
			assertTrue(e.getMessage().startsWith("404: "));
		} finally {
			client.stop();
		}
	}

	public void testExplicitNoSuchMethodException() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
		ManualSynchronousHandler handler = new ManualSynchronousHandler();
		server.registerHandler("/foobar", "^server\\.", handler);
		server.start();
		try {
			// The handler will raise a NoSuchMethodException
			Object rsp = client.execute("server.raiseNoSuchMethodException", null);
			fail();
		} catch(NoSuchMethodException e) {
			assertTrue(e.getMessage().startsWith("404: "));
		} finally {
			client.stop();
		}
	}

	public void testExplicitMethodNameNoMatch() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
		ManualSynchronousHandler handler = new ManualSynchronousHandler();
		server.registerHandler("/foobar", "^server\\.", handler);
		server.start();
		try {
			// The handler will return null
			Object rsp = client.execute("server.badMethodName", new Object[]{"hello world"});
			fail();
		} catch(UnsupportedOperationException e) {
			assertTrue(e.getMessage().startsWith("500: "));
		} finally {
			client.stop();
		}
	}

	public void testRpcCallContext() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
		ManualSynchronousHandler handler = new ManualSynchronousHandler();
		server.registerHandler(null, "^server\\.", handler);
		server.start();
		try {
			Object rsp = client.execute("server.toUpper", new Object[]{"hello world"});
			assertEquals("HELLO WORLD", rsp);
			assertNotNull(handler.context);
			assertNull(handler.context.getSSLSession());
			assertNotNull(handler.context.getRemoteAddr());
			
			InetSocketAddress remote = (InetSocketAddress)handler.context.getRemoteAddr();
			InetAddress local = InetAddress.getByName("localhost");
			assertEquals(local.getHostAddress(), remote.getAddress().getHostAddress());
			assertTrue(handler.context.getRemotePort() > 0);
		} finally {
			client.stop();
		}
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_ManualSynchronousServer.class);
	}
}
