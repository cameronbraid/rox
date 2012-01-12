package com.flat502.rox.client;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.flat502.rox.marshal.Test_ClassDescriptor;
import com.flat502.rox.processing.RpcFaultException;
import com.flat502.rox.processing.ThreadUtils;

public class Test_SyncClientWithXerces extends TestCase {
	private static final String PREFIX = "test";
	private static final int PORT = 8080;
	private static final String URL = "http://localhost:" + PORT + "/RPC2";

	private TestServer server;
	
	static {
		System.setProperty("javax.xml.parsers.SAXParserFactory",
				"com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
	}

	protected void setUp() throws Exception {
		ThreadUtils.assertZeroThreads();
		this.server = new TestServer("/RPC2", PREFIX, PORT);
	}

	protected void tearDown() throws Exception {
		this.server.stop();
		Thread.sleep(50);
		ThreadUtils.assertZeroThreads();
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

	public void testStringResponseWithTrailingSlashOnURL() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL("http://localhost:" + PORT+"/RPC2/"));
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
		} catch(UnsupportedOperationException e) {
		} finally {
			client.stop();
		}
	}

	public void testXmlRpcFaultResponse() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		try {
			client.execute("test.returnXmlRpcMethofFault", null);
			fail();
		} catch(RpcFaultException e) {
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
		} catch(RpcFaultException e) {
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
		} catch(UnsupportedOperationException e) {
			assertTrue(e.getMessage().indexOf("Another Exception") != -1);
		} finally {
			client.stop();
		}
	}

	public void testComplexResponse() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		try {
			Object rsp = client.execute("test.complexResponse", null);
			assertNotNull(rsp);
			assertTrue(rsp instanceof List);
			List rspList = (List)rsp;
			assertEquals(2, rspList.size());
			assertTrue(rspList.get(0) instanceof Map);
			assertTrue(rspList.get(1) instanceof Map);
			
			Map map0 = (Map)rspList.get(0);
			assertEquals(4, map0.size());
			assertEquals("ami-12340", map0.get("ami-id"));
			assertEquals("something", map0.get("second"));
			assertEquals("something-else", map0.get("third"));
			assertTrue(map0.get("properties") instanceof Map);
			Map props0 = (Map)map0.get("properties");
			assertEquals("property-value-1-0", props0.get("property-name-1-0"));
			assertEquals("property-value-2-0", props0.get("property-name-2-0"));
			assertEquals("property-value-3-0", props0.get("property-name-3-0"));
			assertEquals("property-value-4-0", props0.get("property-name-4-0"));
			assertEquals("the quick brown fox jumped over the sax parser - the quick brown fox jumped over the sax parser - the quick brown fox jumped over the sax parser - the quick brown fox jumped over the sax parser - the quick brown fox jumped over the sax parser", props0.get("long-property-0"));
			
			Map map1 = (Map)rspList.get(1);
			assertEquals(4, map1.size());
			assertEquals("ami-12341", map1.get("ami-id"));
			assertEquals("something", map1.get("second"));
			assertEquals("something-else", map1.get("third"));
			assertTrue(map1.get("properties") instanceof Map);
			Map props1 = (Map)map1.get("properties");
			assertEquals("property-value-1-1", props1.get("property-name-1-1"));
			assertEquals("property-value-2-1", props1.get("property-name-2-1"));
			assertEquals("property-value-3-1", props1.get("property-name-3-1"));
			assertEquals("property-value-4-1", props1.get("property-name-4-1"));
			assertEquals("the quick brown fox jumped over the sax parser - the quick brown fox jumped over the sax parser - the quick brown fox jumped over the sax parser - the quick brown fox jumped over the sax parser - the quick brown fox jumped over the sax parser", props1.get("long-property-1"));
		} finally {
			client.stop();
		}
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_SyncClientWithXerces.class);
	}
}
