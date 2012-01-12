package com.flat502.rox.marshal.xmlrpc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.flat502.rox.marshal.FirstLevel;
import com.flat502.rox.marshal.IntegerArraysStruct;
import com.flat502.rox.marshal.JaggedCustomTypeArraysStruct;
import com.flat502.rox.marshal.JaggedIntArraysStruct;
import com.flat502.rox.marshal.JaggedObjectArraysStruct;
import com.flat502.rox.marshal.ListStruct;
import com.flat502.rox.marshal.PrimitiveArraysStruct;
import com.flat502.rox.marshal.SecondLevel;
import com.flat502.rox.marshal.SimpleStruct;
import com.flat502.rox.marshal.TestObject;
import com.flat502.rox.marshal.TestStruct;
import com.flat502.rox.marshal.TwoStrings;
import com.flat502.rox.marshal.TypedArrayStruct;

public class Test_XmlRpcMethodCall extends TestBase_XmlRpcMethod {
	public Test_XmlRpcMethodCall(String name) {
		super(name);
	}

	public void testTypedStructParamWithNullMembers() throws Exception {
		TestStruct struct = new TestStruct();
		XmlRpcMethodCall call = new XmlRpcMethodCall("testName", new Object[] { struct });
		String xml = new String(call.marshal(), "UTF-8");

		assertXpathEvaluatesTo("testName", "/methodCall/methodName", xml);
		int idx = 1;
		assertStructValueEquals(idx++, "boolean-val", "boolean", "0", xml);
		assertStructValueEquals(idx++, "double-val", "double", "0.0", xml);
		assertStructValueEquals(idx++, "float-val", "double", "0.0", xml);
		assertStructValueEquals(idx++, "int-val", "int", "0", xml);

		// Ensure that none of the (null) Object members are present
		assertXpathNotExists("/methodCall/params/param/value/struct/member[" + idx + "]", xml);
	}

	public void testTypedStructParamField() throws Exception {
		TestStruct struct = new TestStruct(new Integer(42), 24, new Double(3.14), 6.28, new Float(3.24), 6.48f,
				Boolean.TRUE, true, newDate(2006, 1, 12, 14, 8, 55), "string value",
				new char[] { 'h', 'e', 'l', 'l', 'o' }, "Hello".getBytes("UTF-8"), new Object[] { "Object1", "Object2" });
		XmlRpcMethodCall call = new XmlRpcMethodCall("testName", new Object[] { struct });
		String xml = new String(call.marshal(), "UTF-8");

		assertXpathEvaluatesTo("testName", "/methodCall/methodName", xml);
		int idx = 1;
		assertStructValueEquals(idx++, "boolean-object", "boolean", "1", xml);
		assertStructValueEquals(idx++, "boolean-val", "boolean", "1", xml);
		assertStructValueEquals(idx++, "byte-array", "base64", "SGVsbG8=", xml);
		assertStructValueEquals(idx++, "char-array", "string", "hello", xml);
		assertStructValueEquals(idx++, "date", "dateTime.iso8601", "20060112T14:08:55", xml);
		assertStructValueEquals(idx++, "double-object", "double", "3.14", xml);
		assertStructValueEquals(idx++, "double-val", "double", "6.28", xml);
		assertStructValueEquals(idx++, "float-object", "double", "3.24", xml);
		assertStructValueEquals(idx++, "float-val", "double", "6.48", xml);
		assertStructValueEquals(idx++, "int-object", "int", "42", xml);
		assertStructValueEquals(idx++, "int-val", "int", "24", xml);
		// Skip object-array, we test that explicitly in another test
		idx++;
		assertStructValueEquals(idx++, "string-object", "string", "string value", xml);
	}

	public void testTypedStructParamWithNullField() throws Exception {
		XmlRpcMethodCall call = new XmlRpcMethodCall("testName", new Object[] { new TwoStrings() });
		String xml = new String(call.marshal(), "UTF-8");

		assertXpathEvaluatesTo("testName", "/methodCall/methodName", xml);
		int idx = 1;
		assertStructValueEquals(idx++, "non-null-string", "string", "Not Null", xml);
		assertXpathNotExists("/params/param/value/struct/member[" + idx + "]/value/string", xml);
	}

	public void testTypedStructParamUsingGetters() throws Exception {
		List list = Arrays.asList(new String[] { "hey", "there" });
		TestObject object = new TestObject(new Integer(42), 24, new Double(3.14), 6.28, new Float(3.24), 6.48f,
				Boolean.TRUE, true, newDate(2006, 1, 12, 14, 8, 55), "string value",
				new char[] { 'h', 'e', 'l', 'l', 'o' }, "Hello".getBytes("UTF-8"), list);
		XmlRpcMethodCall call = new XmlRpcMethodCall("testName", new Object[] { object });
		String xml = new String(call.marshal(), "UTF-8");

		assertXpathEvaluatesTo("testName", "/methodCall/methodName", xml);
		int idx = 1;
		assertStructValueEquals(idx++, "boolean-object", "boolean", "1", xml);
		assertStructValueEquals(idx++, "boolean-val", "boolean", "1", xml);
		assertStructValueEquals(idx++, "byte-array", "base64", "SGVsbG8=", xml);
		assertStructValueEquals(idx++, "char-array", "string", "hello", xml);
		assertStructValueEquals(idx++, "date", "dateTime.iso8601", "20060112T14:08:55", xml);
		assertStructValueEquals(idx++, "double-object", "double", "3.14", xml);
		assertStructValueEquals(idx++, "double-val", "double", "6.28", xml);
		assertStructValueEquals(idx++, "float-object", "double", "3.24", xml);
		assertStructValueEquals(idx++, "float-val", "double", "6.48", xml);
		assertStructValueEquals(idx++, "int-object", "int", "42", xml);
		assertStructValueEquals(idx++, "int-val", "int", "24", xml);
		assertStructValueEquals(idx++, "list-value", "string", list, xml);
		assertStructValueEquals(idx++, "string-object", "string", "string value", xml);
	}

	public void testTypedStructParamNested() throws Exception {
		SecondLevel pub = new SecondLevel("public");
		SecondLevel priv = new SecondLevel("private");
		FirstLevel object = new FirstLevel(pub, priv);
		XmlRpcMethodCall call = new XmlRpcMethodCall("testName", new Object[] { object });
		String xml = new String(call.marshal(), "UTF-8");

		assertXpathEvaluatesTo("testName", "/methodCall/methodName", xml);
		assertXpathEvaluatesTo("private-second-level", "/methodCall/params/param/value/struct/member[1]/name", xml);
		assertXpathEvaluatesTo("private",
				"/methodCall/params/param/value/struct/member[1]/value/struct/member/value/string", xml);
		assertXpathEvaluatesTo("public-second-level", "/methodCall/params/param/value/struct/member[2]/name", xml);
		assertXpathEvaluatesTo("name", "/methodCall/params/param/value/struct/member[2]/value/struct/member/name", xml);
		assertXpathEvaluatesTo("public",
				"/methodCall/params/param/value/struct/member[2]/value/struct/member/value/string", xml);
	}

	public void testTypedTypedArrayFields() throws Exception {
		SimpleStruct[] publicMember = new SimpleStruct[] { new SimpleStruct("public member") };
		SimpleStruct[] privateMember = new SimpleStruct[] { new SimpleStruct("private member") };
		TypedArrayStruct struct = new TypedArrayStruct(publicMember, privateMember);
		XmlRpcMethodCall call = new XmlRpcMethodCall("testName", new Object[] { struct });
		String xml = new String(call.marshal(), "UTF-8");

		assertXpathEvaluatesTo("testName", "/methodCall/methodName", xml);
		int idx = 1;
		assertXpathEvaluatesTo("private-typed-array", "/methodCall/params/param/value/struct/member[1]/name", xml);
		assertXpathEvaluatesTo("public-typed-array", "/methodCall/params/param/value/struct/member[2]/name", xml);
		assertXpathEvaluatesTo("string-member",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value/struct/member/name", xml);
		assertXpathEvaluatesTo("private member",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value/struct/member/value/string", xml);
		assertXpathEvaluatesTo("string-member",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value/struct/member/name", xml);
		assertXpathEvaluatesTo("public member",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value/struct/member/value/string", xml);
	}

	public void testTypedPrimitiveArray() throws Exception {
		PrimitiveArraysStruct struct = new PrimitiveArraysStruct(new int[] { 42, 43 }, new int[] { 44, 45 },
				new boolean[] { true, false }, new boolean[] { false, true }, new double[] { 3.14, 6.28 }, new double[] {
						4.14,
						5.28 });
		XmlRpcMethodCall call = new XmlRpcMethodCall("testName", new Object[] { struct });
		String xml = new String(call.marshal(), "UTF-8");

		assertXpathEvaluatesTo("testName", "/methodCall/methodName", xml);
		assertXpathEvaluatesTo("0", "/methodCall/params/param/value/struct/member[1]/value/array/data/value[1]/boolean",
				xml);
		assertXpathEvaluatesTo("1", "/methodCall/params/param/value/struct/member[1]/value/array/data/value[2]/boolean",
				xml);
		assertXpathEvaluatesTo("private-boolean-array", "/methodCall/params/param/value/struct/member[1]/name", xml);

		assertXpathEvaluatesTo("private-double-array", "/methodCall/params/param/value/struct/member[2]/name", xml);
		assertXpathEvaluatesTo("4.14",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[1]/double", xml);
		assertXpathEvaluatesTo("5.28",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[2]/double", xml);
		
		assertXpathEvaluatesTo("private-int-array", "/methodCall/params/param/value/struct/member[3]/name", xml);
		assertXpathEvaluatesTo("44", "/methodCall/params/param/value/struct/member[3]/value/array/data/value[1]/int", xml);
		assertXpathEvaluatesTo("45", "/methodCall/params/param/value/struct/member[3]/value/array/data/value[2]/int", xml);

		assertXpathEvaluatesTo("public-boolean-array", "/methodCall/params/param/value/struct/member[4]/name", xml);
		assertXpathEvaluatesTo("1", "/methodCall/params/param/value/struct/member[4]/value/array/data/value[1]/boolean",
				xml);
		assertXpathEvaluatesTo("0", "/methodCall/params/param/value/struct/member[4]/value/array/data/value[2]/boolean",
				xml);
		
		assertXpathEvaluatesTo("public-double-array", "/methodCall/params/param/value/struct/member[5]/name", xml);
		assertXpathEvaluatesTo("3.14",
				"/methodCall/params/param/value/struct/member[5]/value/array/data/value[1]/double", xml);
		assertXpathEvaluatesTo("6.28",
				"/methodCall/params/param/value/struct/member[5]/value/array/data/value[2]/double", xml);

		assertXpathEvaluatesTo("public-int-array", "/methodCall/params/param/value/struct/member[6]/name", xml);
		assertXpathEvaluatesTo("42", "/methodCall/params/param/value/struct/member[6]/value/array/data/value[1]/int", xml);
		assertXpathEvaluatesTo("43", "/methodCall/params/param/value/struct/member[6]/value/array/data/value[2]/int", xml);
	}

	public void testTypedIntegerArray() throws Exception {
		IntegerArraysStruct struct = new IntegerArraysStruct(new Integer[] { new Integer(42), new Integer(43) },
				new Integer[] { new Integer(44), new Integer(45) });
		XmlRpcMethodCall call = new XmlRpcMethodCall("testName", new Object[] { struct });
		String xml = new String(call.marshal(), "UTF-8");

		assertXpathEvaluatesTo("testName", "/methodCall/methodName", xml);
		assertXpathEvaluatesTo("private-integer-array", "/methodCall/params/param/value/struct/member[1]/name", xml);
		assertXpathEvaluatesTo("44", "/methodCall/params/param/value/struct/member[1]/value/array/data/value[1]/int", xml);
		assertXpathEvaluatesTo("45", "/methodCall/params/param/value/struct/member[1]/value/array/data/value[2]/int", xml);
		assertXpathEvaluatesTo("public-integer-array", "/methodCall/params/param/value/struct/member[2]/name", xml);
		assertXpathEvaluatesTo("42", "/methodCall/params/param/value/struct/member[2]/value/array/data/value[1]/int", xml);
		assertXpathEvaluatesTo("43", "/methodCall/params/param/value/struct/member[2]/value/array/data/value[2]/int", xml);
	}

	public void testTypedJaggedIntArray() throws Exception {
		int[][] pub = new int[2][];
		pub[0] = new int[] { 42, 43 };
		pub[1] = new int[] { 44, 45, 46 };
		int[][] priv = new int[2][];
		priv[0] = new int[] { 52, 53 };
		priv[1] = new int[] { 54, 55, 56 };
		JaggedIntArraysStruct struct = new JaggedIntArraysStruct(pub, priv);
		XmlRpcMethodCall call = new XmlRpcMethodCall("testName", new Object[] { struct });
		String xml = new String(call.marshal(), "UTF-8");

		assertXpathEvaluatesTo("testName", "/methodCall/methodName", xml);
		assertXpathEvaluatesTo("private-int-array", "/methodCall/params/param/value/struct/member[1]/name", xml);
		assertXpathEvaluatesTo("52",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[1]/array/data/value[1]/int", xml);
		assertXpathEvaluatesTo("53",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[1]/array/data/value[2]/int", xml);
		assertXpathEvaluatesTo("54",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[2]/array/data/value[1]/int", xml);
		assertXpathEvaluatesTo("55",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[2]/array/data/value[2]/int", xml);
		assertXpathEvaluatesTo("56",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[2]/array/data/value[3]/int", xml);

		assertXpathEvaluatesTo("public-int-array", "/methodCall/params/param/value/struct/member[2]/name", xml);
		assertXpathEvaluatesTo("42",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[1]/array/data/value[1]/int", xml);
		assertXpathEvaluatesTo("43",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[1]/array/data/value[2]/int", xml);
		assertXpathEvaluatesTo("44",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[2]/array/data/value[1]/int", xml);
		assertXpathEvaluatesTo("45",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[2]/array/data/value[2]/int", xml);
		assertXpathEvaluatesTo("46",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[2]/array/data/value[3]/int", xml);
	}

	public void testTypedJaggedCustomTypeArray() throws Exception {
		SimpleStruct[][] pub = new SimpleStruct[2][];
		pub[0] = new SimpleStruct[] { new SimpleStruct("forty two"), new SimpleStruct("forty three") };
		pub[1] = new SimpleStruct[] {
				new SimpleStruct("forty four"),
				new SimpleStruct("forty five"),
				new SimpleStruct("forty six") };
		SimpleStruct[][] priv = new SimpleStruct[2][];
		priv[0] = new SimpleStruct[] { new SimpleStruct("fifty two"), new SimpleStruct("fifty three") };
		priv[1] = new SimpleStruct[] {
				new SimpleStruct("fifty four"),
				new SimpleStruct("fifty five"),
				new SimpleStruct("fifty six") };
		JaggedCustomTypeArraysStruct struct = new JaggedCustomTypeArraysStruct(pub, priv);
		XmlRpcMethodCall call = new XmlRpcMethodCall("testName", new Object[] { struct });
		String xml = new String(call.marshal(), "UTF-8");

		assertXpathEvaluatesTo("testName", "/methodCall/methodName", xml);

		assertXpathEvaluatesTo("private-custom-array", "/methodCall/params/param/value/struct/member[1]/name", xml);
		assertXpathEvaluatesTo(
				"fifty two",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[1]/array/data/value[1]/struct/member/value/string",
				xml);
		assertXpathEvaluatesTo(
				"fifty three",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[1]/array/data/value[2]/struct/member/value/string",
				xml);
		assertXpathEvaluatesTo(
				"fifty four",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[2]/array/data/value[1]/struct/member/value/string",
				xml);
		assertXpathEvaluatesTo(
				"fifty five",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[2]/array/data/value[2]/struct/member/value/string",
				xml);
		assertXpathEvaluatesTo(
				"fifty six",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[2]/array/data/value[3]/struct/member/value/string",
				xml);
		
		assertXpathEvaluatesTo("public-custom-array", "/methodCall/params/param/value/struct/member[2]/name", xml);
		assertXpathEvaluatesTo(
				"forty two",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[1]/array/data/value[1]/struct/member/value/string",
				xml);
		assertXpathEvaluatesTo(
				"forty three",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[1]/array/data/value[2]/struct/member/value/string",
				xml);
		assertXpathEvaluatesTo(
				"forty four",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[2]/array/data/value[1]/struct/member/value/string",
				xml);
		assertXpathEvaluatesTo(
				"forty five",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[2]/array/data/value[2]/struct/member/value/string",
				xml);
		assertXpathEvaluatesTo(
				"forty six",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[2]/array/data/value[3]/struct/member/value/string",
				xml);
	}

	public void testTypedJaggedObjectArray() throws Exception {
		Object[][] pub = new Object[2][];
		pub[0] = new Object[] { "foo", new Integer(42) };
		pub[1] = new Object[] { new Double(3.14), Boolean.TRUE };
		Object[][] priv = new Object[2][];
		priv[0] = new Object[] { "foo", new Integer(42) };
		priv[1] = new Object[] { new Double(3.14), Boolean.TRUE };
		JaggedObjectArraysStruct struct = new JaggedObjectArraysStruct(pub, priv);
		XmlRpcMethodCall call = new XmlRpcMethodCall("testName", new Object[] { struct });
		String xml = new String(call.marshal(), "UTF-8");

		assertXpathEvaluatesTo("testName", "/methodCall/methodName", xml);
		assertXpathEvaluatesTo("private-object-array", "/methodCall/params/param/value/struct/member[1]/name", xml);
		assertXpathEvaluatesTo("foo",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[1]/array/data/value[1]/string", xml);
		assertXpathEvaluatesTo("42",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[1]/array/data/value[2]/int", xml);
		assertXpathEvaluatesTo("3.14",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[2]/array/data/value[1]/double", xml);
		assertXpathEvaluatesTo("1",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[2]/array/data/value[2]/boolean",
				xml);

		assertXpathEvaluatesTo("public-object-array", "/methodCall/params/param/value/struct/member[2]/name", xml);
		assertXpathEvaluatesTo("foo",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[1]/array/data/value[1]/string", xml);
		assertXpathEvaluatesTo("42",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[1]/array/data/value[2]/int", xml);
		assertXpathEvaluatesTo("3.14",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[2]/array/data/value[1]/double", xml);
		assertXpathEvaluatesTo("1",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[2]/array/data/value[2]/boolean",
				xml);
	}

	public void testTypedNestedLists() throws Exception {
		List pub = new ArrayList();
		pub.add(new ArrayList() {
			{
				add(new Integer(42));
				add(new Integer(43));
			}
		});
		pub.add(new ArrayList() {
			{
				add(new Integer(44));
				add(new Integer(45));
				add(new Integer(46));
			}
		});
		List priv = new ArrayList();
		priv.add(new ArrayList() {
			{
				add(new Integer(52));
				add(new Integer(53));
			}
		});
		priv.add(new ArrayList() {
			{
				add(new Integer(54));
				add(new Integer(55));
				add(new Integer(56));
			}
		});
		ListStruct struct = new ListStruct(pub, priv);
		XmlRpcMethodCall call = new XmlRpcMethodCall("testName", new Object[] { struct });
		String xml = new String(call.marshal(), "UTF-8");

		assertXpathEvaluatesTo("testName", "/methodCall/methodName", xml);

		assertXpathEvaluatesTo("private-list", "/methodCall/params/param/value/struct/member[1]/name", xml);
		assertXpathEvaluatesTo("52",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[1]/array/data/value[1]/int", xml);
		assertXpathEvaluatesTo("53",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[1]/array/data/value[2]/int", xml);
		assertXpathEvaluatesTo("54",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[2]/array/data/value[1]/int", xml);
		assertXpathEvaluatesTo("55",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[2]/array/data/value[2]/int", xml);
		assertXpathEvaluatesTo("56",
				"/methodCall/params/param/value/struct/member[1]/value/array/data/value[2]/array/data/value[3]/int", xml);

		assertXpathEvaluatesTo("public-list", "/methodCall/params/param/value/struct/member[2]/name", xml);
		assertXpathEvaluatesTo("42",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[1]/array/data/value[1]/int", xml);
		assertXpathEvaluatesTo("43",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[1]/array/data/value[2]/int", xml);
		assertXpathEvaluatesTo("44",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[2]/array/data/value[1]/int", xml);
		assertXpathEvaluatesTo("45",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[2]/array/data/value[2]/int", xml);
		assertXpathEvaluatesTo("46",
				"/methodCall/params/param/value/struct/member[2]/value/array/data/value[2]/array/data/value[3]/int", xml);
	}
	
	public void testStringEscaped() throws Exception {
		XmlRpcMethodCall call = new XmlRpcMethodCall("testName", new Object[] { "<div>foo&bar</div>" });
		String xml = new String(call.marshal(), "UTF-8");
		
		assertXpathEvaluatesTo("testName", "/methodCall/methodName", xml);
		// XMLUnit appears to unescape escaped XML before the comparison. GAH. 
		assertXpathEvaluatesTo("<div>foo&bar</div>",
				"/methodCall/params/param/value/string", xml);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_XmlRpcMethodCall.class);
	}
}
