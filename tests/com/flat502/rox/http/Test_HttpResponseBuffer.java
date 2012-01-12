package com.flat502.rox.http;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import org.custommonkey.xmlunit.XMLTestCase;

public class Test_HttpResponseBuffer extends XMLTestCase {
	public void testSimpleMessage() throws Exception {
		String[] msg = new String[] {
				"HTTP/1.1 200 OK",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"",
				"Hello" };
		HttpResponseBuffer httpRsp = this.newHttpResponseBuffer(msg);
		assertTrue(httpRsp.isComplete());
		assertEquals(200, httpRsp.getStatusCode());
		assertEquals("text/xml", httpRsp.getHeaderValue("Content-Type"));
		assertEquals("text/xml", httpRsp.getHeaderValue("CONTENT-TYPE"));
		assertEquals("Hello", new String(httpRsp.getContent(), "UTF-8"));
	}

	public void testErrorResponse() throws Exception {
		String[] msg = new String[] {
				"HTTP/1.1 404 Not Found",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"",
				"Hello" };
		HttpResponseBuffer httpRsp = this.newHttpResponseBuffer(msg);
		assertTrue(httpRsp.isComplete());
		assertEquals(404, httpRsp.getStatusCode());
		assertEquals("Not Found", httpRsp.getReasonPhrase());
	}

	public void testUnsupportedHttpVersion() throws Exception {
		String[] msg = new String[] {
				"HTTP/0.9 200 OK",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"",
				"Hello" };
		try {
			HttpResponseBuffer httpRsp = this.newHttpResponseBuffer(msg);
			httpRsp.isComplete();
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	public void testMalformedResponseHeader() throws Exception {
		String[] msg = new String[] {
				"Garbage",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"",
				"Hello" };
		try {
			HttpResponseBuffer httpRsp = this.newHttpResponseBuffer(msg);
			httpRsp.isComplete();
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	public void testEmptyContent() throws Exception {
		String[] msg = new String[] {
				"HTTP/1.1 200 OK",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 0",
				"",
				"" };
		HttpResponseBuffer httpRsp = this.newHttpResponseBuffer(msg);
		assertTrue(httpRsp.isComplete());
		assertEquals(0, httpRsp.getContent().length);
	}

	public void testNoHostVersion11() throws Exception {
		String[] msg = new String[] {
				"HTTP/1.1 200 OK",
				"Content-Length: 5",
				"Content-Type: text/xml",
				"",
				"Hello" };
		HttpResponseBuffer httpRsp = this.newHttpResponseBuffer(msg);
		assertTrue(httpRsp.isComplete());
		assertEquals(200, httpRsp.getStatusCode());
		assertEquals("text/xml", httpRsp.getHeaderValue("Content-Type"));
		assertEquals("text/xml", httpRsp.getHeaderValue("CONTENT-TYPE"));
		assertEquals("Hello", new String(httpRsp.getContent(), "UTF-8"));
	}

	public void testNoHostVersion10() throws Exception {
		String[] msg = new String[] {
				"HTTP/1.0 200 OK",
				"Content-Length: 5",
				"",
				"Hello" };
		HttpResponseBuffer httpRsp = this.newHttpResponseBuffer(msg);
		try {
			httpRsp.isComplete();
		} catch (HttpResponseException e) {
			fail();
		}
	}

	public void testNoContentType() throws Exception {
		String[] msg = new String[] {
				"HTTP/1.1 200 OK",
				"Host: hostname",
				"Content-Length: 5",
				"",
				"Hello" };
		HttpResponseBuffer httpRsp = this.newHttpResponseBuffer(msg);
		assertTrue(httpRsp.isComplete());
	}

	public void testNoContentLength() throws Exception {
		String[] msg = new String[] {
				"HTTP/1.1 200 OK",
				"Host: hostname",
				"Content-Type: text/xml",
				"",
				"Hello" };
		try {
			HttpResponseBuffer httpRsp = this.newHttpResponseBuffer(msg);
			httpRsp.isComplete();
			fail();
		} catch (MissingHeaderException e) {
			assertEquals("Content-Length", e.getHeaderName());
		}
	}

	public void testContentTypeCharSet() throws Exception {
		String[] msg = new String[] {
				"HTTP/1.1 200 OK",
				"Host: hostname",
				"Content-Type: text/xml; charset=ASCII",
				"Content-Length: 5",
				"",
				"Hello" };
		HttpResponseBuffer httpRsp = this.newHttpResponseBuffer(msg);
		assertTrue(httpRsp.isComplete());
		assertEquals("US-ASCII", httpRsp.getContentCharset().name());
	}

	public void testMultilineHeaders() throws Exception {
		String[] msg = new String[] {
				"HTTP/1.1 200 OK",
				"Host: hostname",
				"Content-Type: text/xml",
				"X-Custom-Header: some",
				" multiline value",
				"Content-Length: 5",
				"",
				"Hello" };
		HttpResponseBuffer httpRsp = this.newHttpResponseBuffer(msg);
		assertTrue(httpRsp.isComplete());
		assertEquals("some\r\n multiline value", httpRsp.getHeaderValue("X-Custom-Header"));
		assertEquals("Hello", new String(httpRsp.getContent(), "UTF-8"));
	}

	public void testDuplicateHeaders() throws Exception {
		String[] msg = new String[] {
				"HTTP/1.1 200 OK",
				"Host: hostname",
				"Content-Type: text/xml",
				"X-Duplicate-Header: part 1",
				"X-Duplicate-Header: part 2",
				"Content-Length: 5",
				"",
				"Hello" };
		HttpResponseBuffer httpRsp = this.newHttpResponseBuffer(msg);
		assertTrue(httpRsp.isComplete());
		assertEquals("part 1, part 2", httpRsp.getHeaderValue("X-Duplicate-Header"));
		assertEquals("Hello", new String(httpRsp.getContent(), "UTF-8"));
	}

	private HttpResponseBuffer newHttpResponseBuffer(String[] msg)
			throws Exception {
		return this.newHttpResponseBuffer(msg, "UTF-8");
	}

	private HttpResponseBuffer newHttpResponseBuffer(String[] msg, String charSet)
			throws Exception {
		HttpResponseBuffer httpRsp = new HttpResponseBuffer(null, null);
		byte[] buf = toBuffer(msg);
		httpRsp.addBytes(buf, 0, buf.length);
		return httpRsp;
	}

	private byte[] toBuffer(String[] msg) throws UnsupportedEncodingException {
		return this.toBuffer(msg, "UTF-8");
	}

	private byte[] toBuffer(String[] msg, String charSet)
			throws UnsupportedEncodingException {
		ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(byteOs, true, charSet);
		for (int i = 0; i < msg.length; i++) {
			out.print(msg[i]);
			if (i < msg.length - 1) {
				out.print("\r\n");
			}
		}
		return byteOs.toByteArray();
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_HttpResponseBuffer.class);
	}
}
