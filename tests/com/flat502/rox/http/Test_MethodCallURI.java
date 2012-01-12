package com.flat502.rox.http;

import java.net.URI;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

public class Test_MethodCallURI extends TestCase {
	public void testEmptyURI() throws Exception {
		MethodCallURI uri = new MethodCallURI(new URI(""));
		assertEquals("/", uri.getMountPoint());
		assertEquals("", uri.getMethodName());
		assertTrue(uri.getParameters().isEmpty());
	}

	public void testSimpleURI() throws Exception {
		MethodCallURI uri = new MethodCallURI(new URI("/"));
		assertEquals("/", uri.getMountPoint());
		assertEquals("", uri.getMethodName());
		assertTrue(uri.getParameters().isEmpty());
	}

	public void testDeepURI() throws Exception {
		MethodCallURI uri = new MethodCallURI(new URI("/some/path/method"));
		assertEquals("/some/path", uri.getMountPoint());
		assertEquals("method", uri.getMethodName());
		assertTrue(uri.getParameters().isEmpty());
	}

	public void testMethodNameWithoutParameters() throws Exception {
		MethodCallURI uri = new MethodCallURI(new URI("/methodName"));
		assertEquals("/", uri.getMountPoint());
		assertEquals("methodName", uri.getMethodName());
		assertTrue(uri.getParameters().isEmpty());
	}

	public void testParametersWithoutMethodName() throws Exception {
		MethodCallURI uri = new MethodCallURI(new URI("/?foo"));
		assertEquals("/", uri.getMountPoint());
		assertEquals("", uri.getMethodName());
		assertEquals(1, uri.getParameters().size());
		assertTrue(uri.getParameters().containsKey("foo"));
		assertNull(uri.getParameters().get("foo"));
	}

	public void testParametersWithoutMethodName2() throws Exception {
		MethodCallURI uri = new MethodCallURI(new URI("/path/?foo"));
		assertEquals("/path", uri.getMountPoint());
		assertEquals("", uri.getMethodName());
		assertEquals(1, uri.getParameters().size());
		assertTrue(uri.getParameters().containsKey("foo"));
		assertNull(uri.getParameters().get("foo"));
	}

	public void testMethodNameWithKeys() throws Exception {
		MethodCallURI uri = new MethodCallURI(new URI("/methodName?foo&bar"));
		assertEquals("/", uri.getMountPoint());
		assertEquals("methodName", uri.getMethodName());
		assertEquals(2, uri.getParameters().size());
		assertTrue(uri.getParameters().containsKey("foo"));
		assertNull(uri.getParameters().get("foo"));
		assertTrue(uri.getParameters().containsKey("bar"));
		assertNull(uri.getParameters().get("bar"));
		Iterator iter = uri.getParameters().keySet().iterator();
		assertEquals("foo", iter.next());
		assertEquals("bar", iter.next());
	}

	public void testMethodNameWithDuplicateKeys() throws Exception {
		MethodCallURI uri = new MethodCallURI(new URI("/methodName?foo&bar&foo"));
		assertEquals("/", uri.getMountPoint());
		assertEquals("methodName", uri.getMethodName());
		assertEquals(2, uri.getParameters().size());
		assertTrue(uri.getParameters().containsKey("foo"));
		assertNull(uri.getParameters().get("foo"));
		assertTrue(uri.getParameters().containsKey("bar"));
		assertNull(uri.getParameters().get("bar"));
		Iterator iter = uri.getParameters().keySet().iterator();
		assertEquals("foo", iter.next());
		assertEquals("bar", iter.next());
	}

	public void testMethodNameWithKeysAndValues() throws Exception {
		MethodCallURI uri = new MethodCallURI(new URI("/methodName?foo=this&bar=that"));
		assertEquals("/", uri.getMountPoint());
		assertEquals("methodName", uri.getMethodName());
		assertEquals(2, uri.getParameters().size());
		assertTrue(uri.getParameters().containsKey("foo"));
		assertEquals("this", uri.getParameters().get("foo"));
		assertTrue(uri.getParameters().containsKey("bar"));
		assertEquals("that", uri.getParameters().get("bar"));
		Iterator iter = uri.getParameters().keySet().iterator();
		assertEquals("foo", iter.next());
		assertEquals("bar", iter.next());
	}

	public void testMethodNameWithMixed() throws Exception {
		MethodCallURI uri = new MethodCallURI(new URI("/methodName?bar&foo=this"));
		assertEquals("/", uri.getMountPoint());
		assertEquals("methodName", uri.getMethodName());
		assertEquals(2, uri.getParameters().size());
		assertTrue(uri.getParameters().containsKey("foo"));
		assertEquals("this", uri.getParameters().get("foo"));
		assertTrue(uri.getParameters().containsKey("bar"));
		assertNull(uri.getParameters().get("bar"));
		Iterator iter = uri.getParameters().keySet().iterator();
		assertEquals("bar", iter.next());
		assertEquals("foo", iter.next());
	}

	public void testEscapedParameters() throws Exception {
		MethodCallURI uri = new MethodCallURI(new URI("/methodName?foo=this+that&bar=that%41way"));
		assertEquals("/", uri.getMountPoint());
		assertEquals("methodName", uri.getMethodName());
		assertEquals(2, uri.getParameters().size());
		assertTrue(uri.getParameters().containsKey("foo"));
		assertEquals("this that", uri.getParameters().get("foo"));
		assertTrue(uri.getParameters().containsKey("bar"));
		assertEquals("thatAway", uri.getParameters().get("bar"));
		Iterator iter = uri.getParameters().keySet().iterator();
		assertEquals("foo", iter.next());
		assertEquals("bar", iter.next());
	}

	public void testListParameter() throws Exception {
		MethodCallURI uri = new MethodCallURI(new URI("/methodName?foo=this&bar&foo=that"));
		assertEquals("/", uri.getMountPoint());
		assertEquals("methodName", uri.getMethodName());
		assertEquals(2, uri.getParameters().size());
		assertTrue(uri.getParameters().containsKey("foo"));
		assertTrue(uri.getParameters().get("foo") instanceof List);
		List list = (List)uri.getParameters().get("foo");
		assertEquals(2, list.size());
		assertEquals("this", list.get(0));
		assertEquals("that", list.get(1));
		assertTrue(uri.getParameters().containsKey("bar"));
		assertNull(uri.getParameters().get("bar"));
		Iterator iter = uri.getParameters().keySet().iterator();
		assertEquals("foo", iter.next());
		assertEquals("bar", iter.next());
	}

	public void testListParameterWithNullValue() throws Exception {
		MethodCallURI uri = new MethodCallURI(new URI("/methodName?foo=this&bar&foo&foo=that"));
		assertEquals("/", uri.getMountPoint());
		assertEquals("methodName", uri.getMethodName());
		assertEquals(2, uri.getParameters().size());
		assertTrue(uri.getParameters().containsKey("foo"));
		assertTrue(uri.getParameters().get("foo") instanceof List);
		List list = (List)uri.getParameters().get("foo");
		assertEquals(3, list.size());
		assertEquals("this", list.get(0));
		assertNull(list.get(1));
		assertEquals("that", list.get(2));
		assertTrue(uri.getParameters().containsKey("bar"));
		assertNull(uri.getParameters().get("bar"));
		Iterator iter = uri.getParameters().keySet().iterator();
		assertEquals("foo", iter.next());
		assertEquals("bar", iter.next());
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_MethodCallURI.class);
	}
}
