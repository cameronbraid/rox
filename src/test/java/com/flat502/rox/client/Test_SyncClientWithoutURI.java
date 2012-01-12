package com.flat502.rox.client;

import java.net.ConnectException;
import java.net.URL;

import junit.framework.TestCase;

import com.flat502.rox.processing.RpcFaultException;
import com.flat502.rox.processing.ThreadUtils;

public class Test_SyncClientWithoutURI extends TestCase {
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

	public void testNoServer() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL("http://localhost:" + (PORT + 1) + "/"));
		try {
			Object rsp = client.execute("test.stringResponse", null);
			fail();
		} catch(RpcCallFailedException e) {
		} finally {
			client.stop();
		}
	}

	public void testStringResponse() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		try {
			Object rsp = client.execute("test.stringResponse", null);
			assertNotNull(rsp);
			assertEquals("bar", rsp);
		} finally {
			client.stop();
		}
	}

	public void testStringResponseWithNoTrailingSlashOnURL() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL("http://localhost:" + PORT));
		try {
			Object rsp = client.execute("test.stringResponse", null);
			assertNotNull(rsp);
			assertEquals("bar", rsp);
		} finally {
			client.stop();
		}
	}

	public void testNullResponse() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		try {
			Object rsp = client.execute("test.nullResponse", null);
			fail();
		} catch (UnsupportedOperationException e) {
		} finally {
			client.stop();
		}
	}

	public void testXmlRpcFaultResponse() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		try {
			Object rsp = client.execute("test.returnXmlRpcMethofFault", null);
			fail();
		} catch (RpcFaultException e) {
			assertEquals(21, e.getFaultCode());
			assertEquals("Half the meaning of Life", e.getFaultString());
		} finally {
			client.stop();
		}
	}

	public void testXmlRpcFaultException() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		try {
			Object rsp = client.execute("test.raiseXmlRpcFaultException", null);
			fail();
		} catch (RpcFaultException e) {
			assertEquals(42, e.getFaultCode());
			assertEquals("The meaning of Life", e.getFaultString());
		} finally {
			client.stop();
		}
	}

	public void testOtherException() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		try {
			Object rsp = client.execute("test.raiseOtherException", null);
			fail();
		} catch (UnsupportedOperationException e) {
			assertTrue(e.getMessage().indexOf("Another Exception") != -1);
		} finally {
			client.stop();
		}
	}

	public void testRemoteClosesPooledConnection() throws Exception {
		TestServer server2 = new TestServer(null, PREFIX, PORT+1);
		XmlRpcClient client = new XmlRpcClient(new URL("http://localhost:" + (PORT+1) + "/"));
		try {
			// Execute a simple request. This should result in a pooled connection
			Object rsp = client.execute("test.stringResponse", null);
			
			// Forcibly terminate the server 
			server2.stop();
			
			// Ensure the next error we get is a connection refused
			try {
				rsp = client.execute("test.stringResponse", null);
			} catch(RpcCallFailedException e) {
				assertTrue(e.getCause() instanceof ConnectException);
			}
		} finally {
			client.stop();
		}
	}

	public void testReuseAddressAcrossClientClose() throws Exception {
		ClientResourcePool pool = new ClientResourcePool();
		XmlRpcClient clientA = new XmlRpcClient(new URL(URL), pool);
		XmlRpcClient clientB = new XmlRpcClient(new URL(URL), pool);
		try {
			// Execute a simple request. This should result in a pooled connection
			Object rsp = clientA.execute("test.stringResponse", null);
			
			clientA.stop();
			
			rsp = clientB.execute("test.stringResponse", null);
		} finally {
			clientA.stop();
			clientB.stop();
			pool.shutdown();
		}
	}
	
	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_SyncClientWithoutURI.class);
	}
}
