package com.flat502.rox.http;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.custommonkey.xmlunit.XMLTestCase;

import com.flat502.rox.utils.Utils;

public class Test_HttpRequestBuffer extends XMLTestCase {
	public void testSimplePOST() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"",
				"Hello" };
		HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
		assertTrue(httpReq.isComplete());
		assertEquals("POST", httpReq.getMethod());
		assertEquals("text/xml", httpReq.getHeaderValue("Content-Type"));
		assertEquals("text/xml", httpReq.getHeaderValue("CONTENT-TYPE"));
		assertEquals("Hello", new String(httpReq.getContent(), "UTF-8"));
	}

	public void testSimpleGET() throws Exception {
		String[] msg = new String[] {
				"GET /method?param HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 0",
				"",
				"" };
		HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
		assertTrue(httpReq.isComplete());
		assertEquals("GET", httpReq.getMethod());
		assertEquals("/method?param", httpReq.getURI());
		assertEquals("text/xml", httpReq.getHeaderValue("Content-Type"));
		assertEquals("text/xml", httpReq.getHeaderValue("CONTENT-TYPE"));
		assertEquals("", new String(httpReq.getContent(), "UTF-8"));
	}

	public void testUnsupportedHttpVersion() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/0.9",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"",
				"Hello" };
		try {
			HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
			httpReq.isComplete();
			fail();
		} catch (HttpResponseException e) {
			assertEquals(HttpConstants.StatusCodes._505_HTTP_VERSION_NOT_SUPPORTED, e
					.getStatusCode());
		}
	}

	public void testUnsupportedHttpMethod() throws Exception {
		String[] msg = new String[] {
				"DELETE / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"",
				"Hello" };
		try {
			HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
			httpReq.isComplete();
			fail();
		} catch (HttpResponseException e) {
			assertEquals(HttpConstants.StatusCodes._501_NOT_IMPLEMENTED, e
					.getStatusCode());
		}
	}

	public void testMalformedRequestHeader() throws Exception {
		String[] msg = new String[] {
				"Garbage",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"",
				"Hello" };
		try {
			HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
			httpReq.isComplete();
			fail();
		} catch (HttpResponseException e) {
			assertEquals(HttpConstants.StatusCodes._400_BAD_REQUEST, e
					.getStatusCode());
		}
	}

	public void testEmptyContent() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 0",
				"",
				"" };
		HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
		assertTrue(httpReq.isComplete());
		assertEquals(0, httpReq.getContent().length);
	}

	public void testNoHost() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Content-Length: 5",
				"",
				"Hello" };
		try {
			HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
			httpReq.isComplete();
			fail();
		} catch (HttpResponseException e) {
			assertEquals(HttpConstants.StatusCodes._412_PRECONDITION_FAILED, e
					.getStatusCode());
		}
	}

	public void testNoHostVersion10() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.0",
				"Content-Length: 5",
				"",
				"Hello" };
		HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
		try {
			httpReq.isComplete();
		} catch (HttpResponseException e) {
			fail();
		}
	}

	public void testNoContentType() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Host: hostname",
				"Content-Length: 5",
				"",
				"Hello" };
		HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
		assertTrue(httpReq.isComplete());
	}

	public void testExcessiveContent() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"",
				"HelloWorld" };

		HttpRequestBuffer httpReq = new HttpRequestBuffer(null, null);
		byte[] buf = toBuffer(msg);
		int excess = httpReq.addBytes(buf, 0, buf.length);
		assertEquals(buf.length-"World".length(), excess);
		assertTrue(httpReq.isComplete());
		assertEquals("POST", httpReq.getMethod());
		assertEquals("text/xml", httpReq.getHeaderValue("Content-Type"));
		assertEquals("Hello", new String(httpReq.getContent(), "UTF-8"));
	}

	public void testNoContentLength() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"",
				"Hello" };
		try {
			HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
			httpReq.isComplete();
			fail();
		} catch (HttpResponseException e) {
			assertEquals(HttpConstants.StatusCodes._412_PRECONDITION_FAILED, e
					.getStatusCode());
		}
	}

	public void testInvalidContentType() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/html",
				"Content-Length: 5",
				"",
				"Hello" };
		HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
		assertTrue(httpReq.isComplete());
	}

	public void testContentTypeCharSet() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml; charset=ASCII",
				"Content-Length: 5",
				"",
				"Hello" };
		HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
		assertTrue(httpReq.isComplete());
		assertEquals("US-ASCII", httpReq.getContentCharset().name());
	}

	public void testMultilineHeaders() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"X-Custom-Header: some",
				" multiline value",
				"Content-Length: 5",
				"",
				"Hello" };
		HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
		assertTrue(httpReq.isComplete());
		assertEquals("some\r\n multiline value", httpReq
				.getHeaderValue("X-Custom-Header"));
		assertEquals("Hello", new String(httpReq.getContent(), "UTF-8"));
	}

	public void testDuplicateHeaders() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"X-Duplicate-Header: part 1",
				"X-Duplicate-Header: part 2",
				"Content-Length: 5",
				"",
				"Hello" };
		HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
		assertTrue(httpReq.isComplete());
		assertEquals("part 1, part 2", httpReq
				.getHeaderValue("X-Duplicate-Header"));
		assertEquals("Hello", new String(httpReq.getContent(), "UTF-8"));
	}

	public void testAcceptContentEmpty() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"Accept-Encoding:",
				"",
				"Hello" };
		HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
		assertTrue(httpReq.isComplete());
		assertEquals("", httpReq.getHeaderValue("Accept-Encoding"));
		assertNotNull(httpReq.getAcceptedEncodings());
		assertEquals(0, httpReq.getAcceptedEncodings().size());
	}

	public void testAcceptContentSingleName() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"Accept-Encoding: identity",
				"",
				"Hello" };
		HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
		assertTrue(httpReq.isComplete());
		assertNotNull(httpReq.getAcceptedEncodings());
		assertEquals(1, httpReq.getAcceptedEncodings().size());
		assertTrue(httpReq.getAcceptedEncodings().containsKey("identity"));
		assertEquals(null, httpReq.getAcceptedEncodings().get("identity"));
	}

	public void testAcceptContentSingleNameUppercase() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"Accept-Encoding: IDENTITY",
				"",
				"Hello" };
		HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
		assertTrue(httpReq.isComplete());
		assertNotNull(httpReq.getAcceptedEncodings());
		assertEquals(1, httpReq.getAcceptedEncodings().size());
		assertTrue(httpReq.getAcceptedEncodings().containsKey("identity"));
		assertEquals(null, httpReq.getAcceptedEncodings().get("identity"));
	}

	public void testAcceptContentNameWithQuality() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"Accept-Encoding: identity;q=0.5",
				"",
				"Hello" };
		HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
		assertTrue(httpReq.isComplete());
		assertNotNull(httpReq.getAcceptedEncodings());
		assertEquals(1, httpReq.getAcceptedEncodings().size());
		assertTrue(httpReq.getAcceptedEncodings().containsKey("identity"));
		assertEquals(new Float(0.5), httpReq.getAcceptedEncodings().get("identity"));
	}

	public void testAcceptContentNameMultipleEncodingsNoQuality() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"Accept-Encoding: identity, deflater",
				"",
				"Hello" };
		HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
		assertTrue(httpReq.isComplete());
		assertNotNull(httpReq.getAcceptedEncodings());
		assertEquals(2, httpReq.getAcceptedEncodings().size());
		assertTrue(httpReq.getAcceptedEncodings().containsKey("identity"));
		assertEquals(null, httpReq.getAcceptedEncodings().get("identity"));
		assertTrue(httpReq.getAcceptedEncodings().containsKey("deflater"));
		assertEquals(null, httpReq.getAcceptedEncodings().get("deflater"));
		Iterator keys = httpReq.getAcceptedEncodings().keySet().iterator();
		assertEquals("identity", keys.next());
		assertEquals("deflater", keys.next());
	}

	public void testAcceptContentNameMultipleEncodingsOneQuality() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"Accept-Encoding: identity; q=0.5, deflater",
				"",
				"Hello" };
		HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
		assertTrue(httpReq.isComplete());
		assertNotNull(httpReq.getAcceptedEncodings());
		assertEquals(2, httpReq.getAcceptedEncodings().size());
		assertTrue(httpReq.getAcceptedEncodings().containsKey("identity"));
		assertEquals(new Float(0.5), httpReq.getAcceptedEncodings().get("identity"));
		assertTrue(httpReq.getAcceptedEncodings().containsKey("deflater"));
		assertEquals(null, httpReq.getAcceptedEncodings().get("deflater"));
		Iterator keys = httpReq.getAcceptedEncodings().keySet().iterator();
		assertEquals("deflater", keys.next());
		assertEquals("identity", keys.next());
	}

	public void testAcceptContentComplex() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"Accept-Encoding: gzip;q=0.5, identity; q=0.5, deflater;q= 0.5, gzip, other;q=0.1",
				"",
				"Hello" };
		HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
		assertTrue(httpReq.isComplete());
		assertNotNull(httpReq.getAcceptedEncodings());
		assertEquals(4, httpReq.getAcceptedEncodings().size());
		assertTrue(httpReq.getAcceptedEncodings().containsKey("identity"));
		assertEquals(new Float(0.5), httpReq.getAcceptedEncodings().get("identity"));
		assertTrue(httpReq.getAcceptedEncodings().containsKey("deflater"));
		assertEquals(new Float(0.5), httpReq.getAcceptedEncodings().get("deflater"));
		assertTrue(httpReq.getAcceptedEncodings().containsKey("gzip"));
		assertEquals(null, httpReq.getAcceptedEncodings().get("gzip"));
		assertTrue(httpReq.getAcceptedEncodings().containsKey("other"));
		assertEquals(new Float(0.1), httpReq.getAcceptedEncodings().get("other"));
		Iterator keys = httpReq.getAcceptedEncodings().keySet().iterator();
		assertEquals("gzip", keys.next());
		assertEquals("identity", keys.next());
		assertEquals("deflater", keys.next());
		assertEquals("other", keys.next());
	}

	public void testAcceptContentSortOrder() throws Exception {
		String[] msg = new String[] {
				"POST / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"Accept-Encoding: first;q=0.5, second;q=0.3, third;q=0.4, fourth, fifth;q=0.1, repeat, again",
				"",
				"Hello" };
		HttpRequestBuffer httpReq = this.newHttpRequestBuffer(msg);
		assertTrue(httpReq.isComplete());
		assertNotNull(httpReq.getAcceptedEncodings());
		Iterator keys = httpReq.getAcceptedEncodings().keySet().iterator();
		assertEquals("fourth", keys.next());
		assertEquals("repeat", keys.next());
		assertEquals("again", keys.next());
		assertEquals("first", keys.next());
		assertEquals("third", keys.next());
		assertEquals("second", keys.next());
		assertEquals("fifth", keys.next());
	}

	public void testPipelinedRequests() throws Exception {
		String[] msg = new String[] {
				"GET / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"",
				"HelloGET / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 5",
				"",
				"WorldGET / HTTP/1.1",
				"Host: hostname",
				"Content-Type: text/xml",
				"Content-Length: 4",
				"",
				"More"};

		HttpRequestBuffer httpReq = new HttpRequestBuffer(null, null);
		byte[] buf = toBuffer(msg);
		int excess = httpReq.addBytes(buf, 0, buf.length);
		assertEquals(82, excess);

		// Message 1
		assertTrue(httpReq.isComplete());
		assertEquals("GET", httpReq.getMethod());
		assertEquals("/", httpReq.getURI());
		assertEquals("text/xml", httpReq.getHeaderValue("Content-Type"));
		assertEquals(5, httpReq.getContent().length);
		assertEquals("Hello", new String(httpReq.getContent(), "UTF-8"));

		// Message 2
		HttpRequestBuffer httpReq2 = new HttpRequestBuffer(null, null);
		excess = httpReq2.addBytes(buf, excess, buf.length-excess);
		assertEquals(164, excess);
		assertEquals("GET", httpReq2.getMethod());
		assertEquals("/", httpReq2.getURI());
		assertEquals("text/xml", httpReq2.getHeaderValue("Content-Type"));
		assertEquals(5, httpReq2.getContent().length);
		assertEquals("World", new String(httpReq2.getContent(), "UTF-8"));

		// Message 3
		HttpRequestBuffer httpReq3 = new HttpRequestBuffer(null, null);
		excess = httpReq3.addBytes(buf, excess, buf.length-excess);
		assertEquals(0, excess);
		assertEquals("GET", httpReq3.getMethod());
		assertEquals("/", httpReq3.getURI());
		assertEquals("text/xml", httpReq3.getHeaderValue("Content-Type"));
		assertEquals(4, httpReq3.getContent().length);
		assertEquals("More", new String(httpReq3.getContent(), "UTF-8"));
	}

	private HttpRequestBuffer newHttpRequestBuffer(String[] msg)
			throws Exception {
		return this.newHttpRequestBuffer(msg, "UTF-8");
	}

	private HttpRequestBuffer newHttpRequestBuffer(String[] msg, String charSet)
			throws Exception {
		HttpRequestBuffer httpReq = new HttpRequestBuffer(null, null);
		byte[] buf = toBuffer(msg);
		httpReq.addBytes(buf, 0, buf.length);
		return httpReq;
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
		junit.textui.TestRunner.run(Test_HttpRequestBuffer.class);
	}
}
