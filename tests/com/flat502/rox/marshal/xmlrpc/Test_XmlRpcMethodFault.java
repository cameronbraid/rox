package com.flat502.rox.marshal.xmlrpc;

import java.io.IOException;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.custommonkey.xmlunit.XMLTestCase;
import org.xml.sax.SAXException;

import com.flat502.rox.marshal.xmlrpc.XmlRpcMethodCall;
import com.flat502.rox.marshal.xmlrpc.XmlRpcMethodFault;

public class Test_XmlRpcMethodFault extends TestBase_XmlRpcMethod {
	public Test_XmlRpcMethodFault(String name) {
		super(name);
	}

	public void testIntStringConstructor() throws Exception {
		XmlRpcMethodFault fault = new XmlRpcMethodFault(42, "The meaning of life");
		String xml = new String(fault.marshal(), "UTF-8");

		assertXpathExists("/methodResponse/fault", xml);
		assertXpathEvaluatesTo("faultCode", "/methodResponse/fault/value/struct/member[1]/name", xml);
		assertXpathEvaluatesTo("42", "/methodResponse/fault/value/struct/member[1]/value/int", xml);
		assertXpathEvaluatesTo("faultString", "/methodResponse/fault/value/struct/member[2]/name", xml);
		assertXpathEvaluatesTo("The meaning of life", "/methodResponse/fault/value/struct/member[2]/value/string", xml);
	}

	public void testIntThrowableConstructor() throws Exception {
		XmlRpcMethodFault fault = new XmlRpcMethodFault(42, new Exception("The meaning of life"));
		String xml = new String(fault.marshal(), "UTF-8");

		assertXpathExists("/methodResponse/fault", xml);
		assertXpathEvaluatesTo("faultCode", "/methodResponse/fault/value/struct/member[1]/name", xml);
		assertXpathEvaluatesTo("42", "/methodResponse/fault/value/struct/member[1]/value/int", xml);
		assertXpathEvaluatesTo("faultString", "/methodResponse/fault/value/struct/member[2]/name", xml);
		assertXpathEvaluatesTo("The meaning of life", "/methodResponse/fault/value/struct/member[2]/value/string", xml);
	}

	public void testThrowableConstructor() throws Exception {
		XmlRpcMethodFault fault = new XmlRpcMethodFault(new Exception("The meaning of life"));
		String xml = new String(fault.marshal(), "UTF-8");

		assertXpathExists("/methodResponse/fault", xml);
		assertXpathEvaluatesTo("faultCode", "/methodResponse/fault/value/struct/member[1]/name", xml);
		assertXpathEvaluatesTo("0", "/methodResponse/fault/value/struct/member[1]/value/int", xml);
		assertXpathEvaluatesTo("faultString", "/methodResponse/fault/value/struct/member[2]/name", xml);
		assertXpathEvaluatesTo("The meaning of life", "/methodResponse/fault/value/struct/member[2]/value/string", xml);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_XmlRpcMethodFault.class);
	}
}
