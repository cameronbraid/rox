package com.flat502.rox.marshal.xmlrpc;

import java.io.InputStream;

import com.flat502.rox.marshal.MethodResponseUnmarshallerAid;
import com.flat502.rox.marshal.RpcResponse;

public class Test_SaxMethodResponseUnmarshaller extends TestBase_MethodResponseUnmarshaller {
	static {
		System.getProperties().remove("javax.xml.parsers.SAXParserFactory");
		SaxParserPool.reset();
	}

	public Test_SaxMethodResponseUnmarshaller(String name) {
		super(name);
	}
	
	protected RpcResponse unmarshal(String xml, final Class type) throws Exception {
		return new SaxMethodResponseUnmarshaller().unmarshal(xml, new MethodResponseUnmarshallerAid() {
			public Class getReturnType() {
				return type;
			}
		});
	}
	
	protected RpcResponse unmarshal(InputStream xml, final Class type) throws Exception {
		return new SaxMethodResponseUnmarshaller().unmarshal(xml, new MethodResponseUnmarshallerAid() {
			public Class getReturnType() {
				return type;
			}
		});
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_SaxMethodResponseUnmarshaller.class);
	}
}
