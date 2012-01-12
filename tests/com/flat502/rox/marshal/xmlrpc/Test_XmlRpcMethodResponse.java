package com.flat502.rox.marshal.xmlrpc;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flat502.rox.marshal.DateStruct;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.PrimitivesStruct;
import com.flat502.rox.marshal.SimpleStruct;

public class Test_XmlRpcMethodResponse extends TestBase_XmlRpcMethod {
	public Test_XmlRpcMethodResponse(String name) {
		super(name);
	}

	public void testByte() throws Exception {
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(new Byte((byte) 42));
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("42", "/methodResponse/params/param/value/int", xml);
	}

	public void testShort() throws Exception {
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(new Short((short) 42));
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("42", "/methodResponse/params/param/value/int", xml);
	}

	public void testInteger() throws Exception {
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(new Integer(42));
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("42", "/methodResponse/params/param/value/int", xml);
	}

	public void testLong() throws Exception {
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(new Long(42));
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("42", "/methodResponse/params/param/value/int", xml);
	}

	public void testLongTooSmall() throws Exception {
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(new Long(Integer.MIN_VALUE - 1l));
		try {
			rsp.marshal();
			fail();
		} catch (MarshallingException e) {
		}
	}

	public void testLongTooLarge() throws Exception {
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(new Long(Integer.MAX_VALUE + 1l));
		try {
			rsp.marshal();
			fail();
		} catch (MarshallingException e) {
		}
	}

	public void testString() throws Exception {
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse("Life");
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("Life", "/methodResponse/params/param/value/string", xml);
	}

	public void testDouble() throws Exception {
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(new Double(3.14));
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("3.14", "/methodResponse/params/param/value/double", xml);
	}

	public void testFloat() throws Exception {
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(new Float(3.14));
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("3.14", "/methodResponse/params/param/value/double", xml);
	}

	public void testBoolean() throws Exception {
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(Boolean.TRUE);
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("1", "/methodResponse/params/param/value/boolean", xml);
	}

	public void testDate() throws Exception {
		Date date = this.newDate(2006, 02, 06, 20, 23, 42);
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(date);
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("20060206T20:23:42", "/methodResponse/params/param/value/dateTime.iso8601", xml);
	}

	public void testPrimitiveArrayChar() throws Exception {
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse("Life".toCharArray());
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("Life", "/methodResponse/params/param/value/string", xml);
	}

	public void testPrimitiveArrayBytes() throws Exception {
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse("Data".getBytes("UTF-8"));
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("RGF0YQ==", "/methodResponse/params/param/value/base64", xml);
	}

	public void testPrimitiveArrayInt() throws Exception {
		int[] array = new int[] { 1, 2, 3, 4 };
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(array);
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("1", "/methodResponse/params/param/value/array/data/value[1]/int", xml);
		assertXpathEvaluatesTo("2", "/methodResponse/params/param/value/array/data/value[2]/int", xml);
		assertXpathEvaluatesTo("3", "/methodResponse/params/param/value/array/data/value[3]/int", xml);
		assertXpathEvaluatesTo("4", "/methodResponse/params/param/value/array/data/value[4]/int", xml);
	}

	public void testPrimitiveArrayDouble() throws Exception {
		double[] array = new double[] { 1, 2, 3, 4 };
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(array);
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("1.0", "/methodResponse/params/param/value/array/data/value[1]/double", xml);
		assertXpathEvaluatesTo("2.0", "/methodResponse/params/param/value/array/data/value[2]/double", xml);
		assertXpathEvaluatesTo("3.0", "/methodResponse/params/param/value/array/data/value[3]/double", xml);
		assertXpathEvaluatesTo("4.0", "/methodResponse/params/param/value/array/data/value[4]/double", xml);
	}

	public void testObjectArrayChar() throws Exception {
		Object[] array = new Object[] { new Character('a'), new Character('b'), new Character('c') };
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(array);
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("a", "/methodResponse/params/param/value/array/data/value[1]/string", xml);
		assertXpathEvaluatesTo("b", "/methodResponse/params/param/value/array/data/value[2]/string", xml);
		assertXpathEvaluatesTo("c", "/methodResponse/params/param/value/array/data/value[3]/string", xml);
	}

	public void testObjectArrayString() throws Exception {
		Object[] array = new Object[] { "hey", "there", "bob" };
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(array);
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("hey", "/methodResponse/params/param/value/array/data/value[1]/string", xml);
		assertXpathEvaluatesTo("there", "/methodResponse/params/param/value/array/data/value[2]/string", xml);
		assertXpathEvaluatesTo("bob", "/methodResponse/params/param/value/array/data/value[3]/string", xml);
	}

	public void testObjectArraySimpleStruct() throws Exception {
		Object[] array = new Object[3];
		array[0] = new SimpleStruct("first");
		array[1] = new SimpleStruct("second");
		array[2] = new SimpleStruct("third");
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(array);
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("string-member",
				"/methodResponse/params/param/value/array/data/value[1]/struct/member/name", xml);
		assertXpathEvaluatesTo("first",
				"/methodResponse/params/param/value/array/data/value[1]/struct/member/value/string", xml);
		assertXpathEvaluatesTo("string-member",
				"/methodResponse/params/param/value/array/data/value[2]/struct/member/name", xml);
		assertXpathEvaluatesTo("second",
				"/methodResponse/params/param/value/array/data/value[2]/struct/member/value/string", xml);
		assertXpathEvaluatesTo("string-member",
				"/methodResponse/params/param/value/array/data/value[3]/struct/member/name", xml);
		assertXpathEvaluatesTo("third",
				"/methodResponse/params/param/value/array/data/value[3]/struct/member/value/string", xml);
	}

	public void testList() throws Exception {
		List array = new ArrayList() {
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

	public void testSimpleStruct() throws Exception {
		SimpleStruct struct = new SimpleStruct("test");
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(struct);
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("string-member", "/methodResponse/params/param/value/struct/member/name", xml);
		assertXpathEvaluatesTo("test", "/methodResponse/params/param/value/struct/member/value/string", xml);
	}

	// TODO: How should we marshal this?
	/*
	public void testSQLTimestampStruct() throws Exception {
		DateStruct struct = new DateStruct(new Timestamp(System.currentTimeMillis()));
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(struct);
		String xml = new String(rsp.marshal(), "UTF-8");

		assertXpathEvaluatesTo("date-member", "/methodResponse/params/param/value/struct/member/name", xml);
		assertXpathEvaluatesTo("test", "/methodResponse/params/param/value/struct/member/value/string", xml);
	}
	*/

	public void testPrimitivesStruct() throws Exception {
		PrimitivesStruct struct = new PrimitivesStruct((byte) 42, (short) 43, 44, 45L, 3.14f, 6.28d, true, '!');
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(struct);
		String xml = new String(rsp.marshal(), "UTF-8");

		int idx = 1;
		assertStructValueEquals(idx++, "boolean-val", "boolean", "1", xml);
		assertStructValueEquals(idx++, "byte-val", "int", "42", xml);
		assertStructValueEquals(idx++, "char-val", "string", "!", xml);
		assertStructValueEquals(idx++, "double-val", "double", "6.28", xml);
		assertStructValueEquals(idx++, "float-val", "double", "3.14", xml);
		assertStructValueEquals(idx++, "int-val", "int", "44", xml);
		assertStructValueEquals(idx++, "long-val", "int", "45", xml);
		assertStructValueEquals(idx++, "short-val", "int", "43", xml);
	}

	public void testMap() throws Exception {
		Map struct = new HashMap();
		struct.put("foo", "bar");
		struct.put("int", new Integer(42));
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(struct);
		String xml = new String(rsp.marshal(), "UTF-8");

		int idx = 1;
		assertStructValueEquals(idx++, "foo", "string", "bar", xml);
		assertStructValueEquals(idx++, "int", "int", "42", xml);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_XmlRpcMethodResponse.class);
	}
}
