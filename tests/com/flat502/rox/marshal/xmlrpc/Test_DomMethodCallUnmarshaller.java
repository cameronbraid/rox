package com.flat502.rox.marshal.xmlrpc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.custommonkey.xmlunit.XMLTestCase;
import org.xml.sax.SAXException;

import com.flat502.rox.marshal.*;

public class Test_DomMethodCallUnmarshaller extends TestBase_MethodCallUnmarshaller {
	public Test_DomMethodCallUnmarshaller(String name) {
		super(name);
	}
	
	protected RpcCall unmarshal(String xml, Class[] types) throws Exception {
		return new DomMethodCallUnmarshaller().unmarshal(xml, types);
	}
	
	protected RpcCall unmarshal(InputStream xml, Class[] types) throws Exception {
		return new DomMethodCallUnmarshaller().unmarshal(xml, types);
	}

	protected RpcCall unmarshalWithAid(String xml, MethodCallUnmarshallerAid aid) throws Exception {
		return new DomMethodCallUnmarshaller().unmarshal(xml, aid);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_DomMethodCallUnmarshaller.class);
	}
}
