package com.flat502.rox.marshal.xmlrpc;

import java.io.InputStream;

import com.flat502.rox.marshal.MethodResponseUnmarshallerAid;
import com.flat502.rox.marshal.RpcResponse;

public class Test_DomMethodResponseUnmarshaller extends TestBase_MethodResponseUnmarshaller {
	public Test_DomMethodResponseUnmarshaller(String name) {
		super(name);
	}
	
	protected RpcResponse unmarshal(String xml, final Class type) throws Exception {
		return new DomMethodResponseUnmarshaller().unmarshal(xml, new MethodResponseUnmarshallerAid() {
			public Class getReturnType() {
				return type;
			}
		});
	}
	
	protected RpcResponse unmarshal(InputStream xml, final Class type) throws Exception {
		return new DomMethodResponseUnmarshaller().unmarshal(xml, new MethodResponseUnmarshallerAid() {
			public Class getReturnType() {
				return type;
			}
		});
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_DomMethodResponseUnmarshaller.class);
	}
}
