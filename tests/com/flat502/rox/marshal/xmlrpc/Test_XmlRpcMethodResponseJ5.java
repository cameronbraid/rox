package com.flat502.rox.marshal.xmlrpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flat502.rox.marshal.EnumConstants;
import com.flat502.rox.marshal.SimpleStruct;

public class Test_XmlRpcMethodResponseJ5 extends TestBase_XmlRpcMethod {
	public Test_XmlRpcMethodResponseJ5(String name) {
		super(name);
	}

	public void testPlatformVersion() {
		assertTrue(XmlRpcUtils.newMarshaller(null) instanceof XmlRpcMarshallerJ5);
	}

	public void testEnumValues() throws Exception {
		SimpleStruct struct = new SimpleStruct("test");
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(EnumConstants.BAR);
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("BAR", "/methodResponse/params/param/value/string", xml);
	}

	public void testGenericArray() throws Exception {
		List<String> array = new ArrayList<String>() {
			{
				add("hey");
				add("there");
				add("bob");
			}
		};
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(array);
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("hey", "/methodResponse/params/param/value/array/data/value[1]/string", xml);
		assertXpathEvaluatesTo("there", "/methodResponse/params/param/value/array/data/value[2]/string", xml);
		assertXpathEvaluatesTo("bob", "/methodResponse/params/param/value/array/data/value[3]/string", xml);
	}

	public void testGenericMap() throws Exception {
		Map<String, Integer> struct = new HashMap<String, Integer>();
		struct.put("foo", new Integer(13));
		struct.put("bar", new Integer(42));
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(struct);
		String xml = new String(rsp.marshal(), "UTF-8");

		int idx = 1;
		assertStructValueEquals(idx++, "foo", "int", "13", xml);
		assertStructValueEquals(idx++, "bar", "int", "42", xml);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_XmlRpcMethodResponseJ5.class);
	}
}
