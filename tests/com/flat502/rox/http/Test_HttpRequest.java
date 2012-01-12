package com.flat502.rox.http;

import org.custommonkey.xmlunit.XMLTestCase;

public class Test_HttpRequest extends XMLTestCase {
	public void testURLWithoutTrailingSlash() throws Exception {
		HttpRequest httpReq = new HttpRequest("POST", "http://localhost:8080", null);
		assertEquals("POST / HTTP/1.1", httpReq.getStartLine());
	}

	public void testURLWithTrailingSlash() throws Exception {
		HttpRequest httpReq = new HttpRequest("POST", "http://localhost:8080/", null);
		assertEquals("POST / HTTP/1.1", httpReq.getStartLine());
	}

	public void testURLWithPath() throws Exception {
		HttpRequest httpReq = new HttpRequest("POST", "http://localhost:8080/somePath", null);
		assertEquals("POST /somePath HTTP/1.1", httpReq.getStartLine());
	}

	public void testURLWithSpacedPath() throws Exception {
		HttpRequest httpReq = new HttpRequest("POST", "http://localhost:8080/spaced%20Path", null);
		assertEquals("POST /spaced Path HTTP/1.1", httpReq.getStartLine());
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_HttpRequest.class);
	}
}
