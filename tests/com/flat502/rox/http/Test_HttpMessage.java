package com.flat502.rox.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.charset.Charset;

import org.custommonkey.xmlunit.XMLTestCase;

import com.flat502.rox.encoding.Encoding;

public class Test_HttpMessage extends XMLTestCase {
	public void testNormalizeHeaders() throws Exception {
		TestHttpMessage httpMsg = new TestHttpMessage();
		assertEquals("Single", httpMsg.normalizeHeaderName("single"));
		assertEquals("Double-Barrelled", httpMsg.normalizeHeaderName("double-barrelled"));
		assertEquals("Lowercase", httpMsg.normalizeHeaderName("lowercase"));
		assertEquals("Uppercase", httpMsg.normalizeHeaderName("UPPERCASE"));
		assertEquals("-Leading", httpMsg.normalizeHeaderName("-leading"));
		assertEquals("Trailing-", httpMsg.normalizeHeaderName("trailing-"));
		assertEquals("Two--Dashes", httpMsg.normalizeHeaderName("two--dashes"));
	}
	
	private class TestHttpMessage extends HttpMessage {
		protected TestHttpMessage() {
			super(null);
		}

		public String normalizeHeaderName(String name) {
			return super.normalizeHeaderName(name);
		}

		protected void marshalStartLine(OutputStream os) throws IOException {
		}
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_HttpMessage.class);
	}
}
