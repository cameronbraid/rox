package com.flat502.rox.utils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

import junit.framework.TestCase;

import com.flat502.rox.utils.XmlPlainPrinter;

public class Test_XmlPlainPrinter extends TestCase {
	public void testUnmarshalMethodName() throws Exception {
		ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
		XmlPlainPrinter pp = new XmlPlainPrinter(byteOs);
		pp.writeHeader("1.0", Charset.forName("UTF-8"));
		pp.openTag("methodCall");
		pp.openTag("methodName");
		pp.writeValue("somevalue");
		pp.closeTag("methodName");
		pp.openTag("params");
		pp.openTag("param");
		pp.writeValue("somevalue");
		pp.closeTag("param");
		pp.openTag("list");
		pp.openTag("value");
		pp.writeValue("somevalue1");
		pp.closeTag("value");
		pp.openTag("value");
		pp.writeValue("somevalue2");
		pp.closeTag("value");
		pp.openTag("value");
		pp.writeValue("somevalue3");
		pp.closeTag("value");
		pp.closeTag("list");
		pp.closeTag("params");
		pp.openTag("empty");
		pp.closeTag("empty");
		pp.closeTag("methodCall");

		assertEquals(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><methodCall><methodName>somevalue</methodName><params><param>somevalue</param><list><value>somevalue1</value><value>somevalue2</value><value>somevalue3</value></list></params><empty/></methodCall>",
				new String(byteOs.toByteArray(), "UTF-8"));
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_XmlPlainPrinter.class);
	}
}
