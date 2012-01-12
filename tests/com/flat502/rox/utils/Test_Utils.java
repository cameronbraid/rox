package com.flat502.rox.utils;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import junit.framework.TestCase;

// TODO: Tests
public class Test_Utils extends TestCase {
	public void testNormalizeEmptyURI() throws Exception {
		String uri = Utils.normalizeURIPath("");
		assertEquals("/", uri);
	}

	public void testNormalizeParentReference() throws Exception {
		String uri = Utils.normalizeURIPath("/foo/../bar");
		assertEquals("/bar", uri);
	}

	public void testNormalizeTrailingSlash() throws Exception {
		String uri = Utils.normalizeURIPath("/foo/");
		assertEquals("/foo", uri);
	}

	public void testNormalizeNoTrailingSlash() throws Exception {
		String uri = Utils.normalizeURIPath("/foo");
		assertEquals("/foo", uri);
	}
	
	public void testContentTypeBare() {
		String ct = Utils.extractContentType("text/xml");
		assertEquals("text/xml", ct);
	}

	public void testContentTypeWithCharSet() {
		String ct = Utils.extractContentType("text/xml; charset=UTF-8");
		assertEquals("text/xml", ct);
	}
	
	public void testCharsetBare() {
		Charset cs = Utils.extractContentCharset("text/xml");
		assertEquals("ISO-8859-1", cs.name());
	}
	
	public void testCharsetExplicit() {
		Charset cs = Utils.extractContentCharset("text/xml; charset=UTF-8");
		assertEquals("UTF-8", cs.name());
	}

	public void testCharsetUnsupported() {
		try {
			Utils.extractContentCharset("text/xml; charset=badname");
			fail();
		} catch(UnsupportedCharsetException e) {
		}
	}
	
	public void testResizeSame() {
		Object o = new Object();
		Object[] dst = Utils.resize(new Object[]{o, o, o}, 3);
		assertEquals(dst.length, 3);
		assertSame(dst, dst);
	}

	public void testResizeSmaller() {
		Object o = new Object();
		Object[] dst = Utils.resize(new Object[]{o, o, o, o}, 3);
		assertEquals(dst.length, 3);
	}

	public void testSplitAt() {
		String[] pair = Utils.splitAt("HelloWorld", "World");
		assertEquals(2, pair.length);
		assertEquals("Hello", pair[0]);
		assertEquals("World", pair[1]);
	}

	public void testSplitAtNoMatch() {
		String[] pair = Utils.splitAt("HelloWorld", "Bob");
		assertEquals(2, pair.length);
		assertEquals("HelloWorld", pair[0]);
		assertEquals("", pair[1]);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_Utils.class);
	}
}
