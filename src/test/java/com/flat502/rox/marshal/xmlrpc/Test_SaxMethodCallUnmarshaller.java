package com.flat502.rox.marshal.xmlrpc;

import java.io.InputStream;

import com.flat502.rox.marshal.MethodCallUnmarshallerAid;
import com.flat502.rox.marshal.RpcCall;

public class Test_SaxMethodCallUnmarshaller extends TestBase_MethodCallUnmarshaller {
	static {
		System.getProperties().remove("javax.xml.parsers.SAXParserFactory");
		SaxParserPool.reset();
	}

	public Test_SaxMethodCallUnmarshaller(String name) {
		super(name);
	}

	protected RpcCall unmarshal(String xml, Class[] types) throws Exception {
		return new SaxMethodCallUnmarshaller().unmarshal(xml, types);
	}

	protected RpcCall unmarshal(InputStream xml, Class[] types) throws Exception {
		return new SaxMethodCallUnmarshaller().unmarshal(xml, types);
	}

	protected RpcCall unmarshalWithAid(String xml, MethodCallUnmarshallerAid aid) throws Exception {
		return new SaxMethodCallUnmarshaller().unmarshal(xml, aid);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_SaxMethodCallUnmarshaller.class);
	}
}
