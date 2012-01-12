package com.flat502.rox.server;

import java.net.URL;
import java.util.Date;

import com.flat502.rox.client.XmlRpcClient;

import junit.framework.TestCase;

public class Test_AutomaticSyncServer extends TestCase {
	private static final String PREFIX = "test";
	private static final int PORT = 8080;
	private static final String URL = "http://localhost:" + PORT + "/";
	
	private XmlRpcServer server;

	protected void setUp() throws Exception {
		this.server = new XmlRpcServer(PORT);
	}
	
	protected void tearDown() throws Exception {
		this.server.stop();
		Thread.sleep(50);
	}
	
//	public void testWildCardURI() throws Exception {
//		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
//		ProxiedHandler handler = new ProxiedHandler();
//		server.registerProxyingHandler(null, "^server\\.(.*)", handler);
//		server.start();
//		try {
//			Object rsp = client.execute("server.toUpper", new Object[]{"hello world"});
//			assertEquals("HELLO WORLD", rsp);
//		} finally {
//			client.stop();
//		}
//	}
//
//	public void testExplicitURIMatch() throws Exception {
//		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
//		ProxiedHandler handler = new ProxiedHandler();
//		server.registerProxyingHandler("/foobar", "^server\\.(.*)", handler);
//		server.start();
//		try {
//			Object rsp = client.execute("server.toUpper", new Object[]{"hello world"});
//			assertEquals("HELLO WORLD", rsp);
//		} finally {
//			client.stop();
//		}
//	}
//
//	public void testExplicitURINoMatch() throws Exception {
//		XmlRpcClient client = new XmlRpcClient(new URL(URL+"nomatch"));
//		ProxiedHandler handler = new ProxiedHandler();
//		server.registerProxyingHandler("/foobar", "^server\\.(.*)", handler);
//		server.start();
//		try {
//			Object rsp = client.execute("server.toUpper", new Object[]{"hello world"});
//			fail();
//		} catch(NoSuchMethodException e) {
//			assertTrue(e.getMessage().startsWith("404: "));
//		} finally {
//			client.stop();
//		}
//	}
//
//	public void testExplicitMethodPrefixNoMatch() throws Exception {
//		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
//		ProxiedHandler handler = new ProxiedHandler();
//		server.registerProxyingHandler("/foobar", "^server\\.(.*)", handler);
//		server.start();
//		try {
//			Object rsp = client.execute("wrong.toUpper", new Object[]{"hello world"});
//			fail();
//		} catch(NoSuchMethodException e) {
//			assertTrue(e.getMessage().startsWith("404: "));
//		} finally {
//			client.stop();
//		}
//	}
//
//	public void testExplicitMethodNameNoMatch() throws Exception {
//		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
//		ProxiedHandler handler = new ProxiedHandler();
//		server.registerProxyingHandler("/foobar", "^server\\.(.*)", handler);
//		server.start();
//		try {
//			Object rsp = client.execute("server.badMethodName", new Object[]{"hello world"});
//			fail();
//		} catch(NoSuchMethodException e) {
//			assertTrue(e.getMessage().startsWith("404: "));
//		} finally {
//			client.stop();
//		}
//	}
//	
//	public void testNoArgsMethodLookup() throws Exception {
//		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
//		ProxiedHandler handler = new ProxiedHandler();
//		server.registerProxyingHandler(null, "^server\\.(.*)", handler);
//		server.start();
//		try {
//			Object rsp = client.execute("server.noArgs", null);
//			assertEquals("NO ARGS", rsp);
//		} finally {
//			client.stop();
//		}
//	}
//	
//	public void testNoArgsMethodLookupPassedArgs() throws Exception {
//		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
//		ProxiedHandler handler = new ProxiedHandler();
//		server.registerProxyingHandler(null, "^server\\.(.*)", handler);
//		server.start();
//		try {
//			Object rsp = client.execute("server.noArgs", new Object[]{"hello world"});
//			fail();
//		} catch(UnsupportedOperationException e) {
//			assertTrue(e.getMessage().startsWith("400: "));
//		} finally {
//			client.stop();
//		}
//	}
//
//	public void testArgsMethodLookup() throws Exception {
//		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
//		ProxiedHandler handler = new ProxiedHandler();
//		server.registerProxyingHandler(null, "^server\\.(.*)", handler);
//		server.start();
//		try {
//			Object rsp = client.execute("server.plentyOfArgs", new Object[]{new Integer(42), new Double(3.14), "Hello", new Date()});
//			assertEquals("PLENTY OF ARGS", rsp);
//		} finally {
//			client.stop();
//		}
//	}
//
//	public void testArgsMethodLookupCoercedLongToInt() throws Exception {
//		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
//		ProxiedHandler handler = new ProxiedHandler();
//		server.registerProxyingHandler(null, "^server\\.(.*)", handler);
//		server.start();
//		try {
//			Object rsp = client.execute("server.plentyOfArgs", new Object[]{new Long(42), new Double(3.14), "Hello", new Date()});
//			assertEquals("PLENTY OF ARGS", rsp);
//		} finally {
//			client.stop();
//		}
//	}
//	
//	public void testArgsMethodLookupCoercedFloatToDouble() throws Exception {
//		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
//		ProxiedHandler handler = new ProxiedHandler();
//		server.registerProxyingHandler(null, "^server\\.(.*)", handler);
//		server.start();
//		try {
//			Object rsp = client.execute("server.plentyOfArgs", new Object[]{new Long(42), new Float(3.14), "Hello", new Date()});
//			assertEquals("PLENTY OF ARGS", rsp);
//		} finally {
//			client.stop();
//		}
//	}
//	
//	public void testArgsMethodLookupCoercedIntegerToDouble() throws Exception {
//		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
//		ProxiedHandler handler = new ProxiedHandler();
//		server.registerProxyingHandler(null, "^server\\.(.*)", handler);
//		server.start();
//		try {
//			Object rsp = client.execute("server.plentyOfArgs", new Object[]{new Long(42), new Integer(3), "Hello", new Date()});
//			assertEquals("PLENTY OF ARGS", rsp);
//		} finally {
//			client.stop();
//		}
//	}
	
	public void testArgsMethodLookupWrongTypes() throws Exception {
		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
		ProxiedHandler handler = new ProxiedHandler();
		server.registerProxyingHandler(null, "^server\\.(.*)", handler);
		server.start();
		try {
			Object rsp = client.execute("server.plentyOfArgs", new Object[]{"Hello", new Integer(42), new Float(3.14), new Date()});
			fail();
		} catch(UnsupportedOperationException e) {
			assertTrue(e.getMessage().startsWith("400: "));
		} finally {
			client.stop();
		}
	}
	
//	public void testCustomTypeMethodLookup() throws Exception {
//		XmlRpcClient client = new XmlRpcClient(new URL(URL+"foobar"));
//		ProxiedHandler handler = new ProxiedHandler();
//		server.registerProxyingHandler(null, "^server\\.(.*)", handler);
//		server.start();
//		try {
//			Object rsp = client.execute("server.customTypeArg", new Object[]{new CustomType()});
//			assertEquals("CUSTOM TYPE ARG", rsp);
//		} finally {
//			client.stop();
//		}
//	}
	
	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_AutomaticSyncServer.class);
	}
}
