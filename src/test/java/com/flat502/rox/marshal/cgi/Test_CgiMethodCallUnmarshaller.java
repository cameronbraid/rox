package com.flat502.rox.marshal.cgi;

import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import com.flat502.rox.http.MethodCallURI;
import com.flat502.rox.marshal.ArrayParameterTypeMapper;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.RpcCall;

public class Test_CgiMethodCallUnmarshaller extends TestCase {
	public Test_CgiMethodCallUnmarshaller(String name) {
		super(name);
	}

	public void testMethodName() throws Exception {
		//		String[] xmlLines = new String[] {
		//				"<methodCall>",
		//				"	<methodName>testMethod</methodName>",
		//				"</methodCall>" };
		//		String uriString = "/testMethod?" + join("&", xmlLines);

		RpcCall call = this.unmarshal("/testMethod");
		assertEquals("testMethod", call.getName());
		assertEquals(0, call.getParameters().length);
	}

	public void testKeysOnly() throws Exception {
		String[] params = new String[] { "foo", "bar", "2" };
		String uriString = "/testMethod?" + join("&", params);

		RpcCall call = this.unmarshal(uriString);
		assertEquals("testMethod", call.getName());
		assertEquals(3, call.getParameters().length);
		assertEquals("foo", call.getParameters()[0]);
		assertEquals("bar", call.getParameters()[1]);
		assertEquals("2", call.getParameters()[2]);
	}

	public void testKeysOnlyWithDuplicates() throws Exception {
		String[] params = new String[] { "foo", "bar", "2", "bar" };
		String uriString = "/testMethod?" + join("&", params);

		RpcCall call = this.unmarshal(uriString);
		assertEquals("testMethod", call.getName());
		assertEquals(4, call.getParameters().length);
		assertEquals("foo", call.getParameters()[0]);
		assertEquals("bar", call.getParameters()[1]);
		assertEquals("2", call.getParameters()[2]);
		assertEquals("bar", call.getParameters()[3]);
	}

	public void testKeysOnlyWithExplicitTypes() throws Exception {
		String[] params = new String[] { "foo", "bar", "2" };
		String uriString = "/testMethod?" + join("&", params);

		RpcCall call = this.unmarshal(uriString, new Class[] { null, null, Integer.class });
		assertEquals("testMethod", call.getName());
		assertEquals(3, call.getParameters().length);
		assertEquals("foo", call.getParameters()[0]);
		assertEquals("bar", call.getParameters()[1]);
		assertEquals(new Integer(2), call.getParameters()[2]);
	}

	public void testKeyValues() throws Exception {
		String[] params = new String[] { "foo=bar", "hello=2" };
		String uriString = "/testMethod?" + join("&", params);

		RpcCall call = this.unmarshal(uriString);
		assertEquals("testMethod", call.getName());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof Map);
		Map map = (Map) call.getParameters()[0];
		assertEquals(2, map.size());
		assertEquals("bar", map.get("foo"));
		assertEquals("2", map.get("hello"));
	}

	public void testListKeyValues() throws Exception {
		String[] params = new String[] { "foo=bar", "hello=2", "foo=bar2" };
		String uriString = "/testMethod?" + join("&", params);

		RpcCall call = this.unmarshal(uriString);
		assertEquals("testMethod", call.getName());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof Map);
		Map map = (Map) call.getParameters()[0];
		assertEquals(2, map.size());
		assertEquals("2", map.get("hello"));
		assertTrue(map.get("foo") instanceof List);
		List list = (List) map.get("foo");
		assertEquals("bar", list.get(0));
		assertEquals("bar2", list.get(1));
	}

	public void testKeyValuesExplicitMapStructClass() throws Exception {
		String[] params = new String[] { "foo=bar", "hello=2" };
		String uriString = "/testMethod?" + join("&", params);

		RpcCall call = this.unmarshal(uriString, new Class[] { Map.class });
		assertEquals("testMethod", call.getName());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof Map);
		Map map = (Map) call.getParameters()[0];
		assertEquals(2, map.size());
		assertEquals("bar", map.get("foo"));
		assertEquals("2", map.get("hello"));
	}

	public void testKeyValuesExplicitListStructClass() throws Exception {
		String[] params = new String[] { "foo=bar", "hello=2" };
		String uriString = "/testMethod?" + join("&", params);

		try {
			this.unmarshal(uriString, new Class[] { List.class });
			fail();
		} catch (MarshallingException e) {
		}
	}

	public void testKeyValuesUserStructClass() throws Exception {
		String[] params = new String[] { "foo=bar", "hello=2" };
		String uriString = "/testMethod?" + join("&", params);

		RpcCall call = this.unmarshal(uriString, new Class[] { Struct.class });
		assertEquals("testMethod", call.getName());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof Struct);
		Struct struct = (Struct) call.getParameters()[0];
		assertEquals("bar", struct.foo);
		assertEquals(2, struct.hello);
	}

	public void testKeyValuesUserStructClassWithTypedArrayMember() throws Exception {
		String[] params = new String[] { "foo=1", "bar=hello", "foo=2" };
		String uriString = "/testMethod?" + join("&", params);

		RpcCall call = this.unmarshal(uriString, new Class[] { StructWithTypedArray.class });
		assertEquals("testMethod", call.getName());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof StructWithTypedArray);
		StructWithTypedArray struct = (StructWithTypedArray) call.getParameters()[0];
		assertEquals("hello", struct.bar);
		assertEquals(1, struct.foo[0]);
		assertEquals(2, struct.foo[1]);
	}

	public void testFoo() throws Exception {
		// Tests
		// Lists
		// Lists with sub types
	}

	private String join(String delim, String[] items) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < items.length; i++) {
			sb.append(items[i]);
			if (i < items.length - 1) {
				sb.append(delim);
			}
		}
		return sb.toString();
	}

	private RpcCall unmarshal(String uriString) throws Exception {
		MethodCallURI uri = new MethodCallURI(uriString);
		return new CgiMethodCallUnmarshaller().unmarshal(uri, null);
	}

	private RpcCall unmarshal(String uriString, Class[] types) throws Exception {
		MethodCallURI uri = new MethodCallURI(uriString);
		return new CgiMethodCallUnmarshaller().unmarshal(uri, new ArrayParameterTypeMapper(types));
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_CgiMethodCallUnmarshaller.class);
	}
}
