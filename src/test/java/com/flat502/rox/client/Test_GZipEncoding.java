package com.flat502.rox.client;

import java.net.URL;

import junit.framework.TestCase;

import com.flat502.rox.encoding.Encoding;
import com.flat502.rox.encoding.GZipEncoding;

public class Test_GZipEncoding extends TestCase {
	private static final Encoding ENCODING = new GZipEncoding();
	private static final String PREFIX = "test";
	private static final int PORT = 8080;
	private static final String URL = "http://localhost:" + PORT + "/";

	private TestServer server;

	public void testPlainClientGZipServer() throws Exception {
		TestServer server = new TestServer(null, PREFIX, PORT, false, ENCODING);
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		try {
			Object rsp = client.execute("test.stringResponse", null);
			assertNotNull(rsp);
			assertEquals("bar", rsp);
		} finally {
			client.stop();
			server.stop();
		}
	}

	public void testGZipClientPlainServer() throws Exception {
		TestServer server = new TestServer(null, PREFIX, PORT, false);
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		client.setContentEncoding(ENCODING);
		try {
			client.execute("test.stringResponse", null);
			fail();
		} catch(UnsupportedOperationException e) {
			assertEquals("415: Unsupported Media Type (Bad Content-Encoding: gzip)", e.getMessage());
		} finally {
			client.stop();
			server.stop();
		}
	}

	public void testGZipClientGZipServer() throws Exception {
		TestServer server = new TestServer(null, PREFIX, PORT, false, ENCODING);
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		client.setContentEncoding(ENCODING);
		try {
			Object rsp = client.execute("test.stringResponse", null);
			assertNotNull(rsp);
			assertEquals("bar", rsp);
		} finally {
			client.stop();
			server.stop();
		}
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_GZipEncoding.class);
	}
}
