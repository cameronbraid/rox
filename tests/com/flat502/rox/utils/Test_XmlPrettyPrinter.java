package com.flat502.rox.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.Charset;

import junit.framework.TestCase;

import com.flat502.rox.utils.XmlPlainPrinter;
import com.flat502.rox.utils.XmlPrettyPrinter;

public class Test_XmlPrettyPrinter extends TestCase {
	public void testUnmarshalMethodName() throws Exception {
		ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
		XmlPrettyPrinter pp = new XmlPrettyPrinter(byteOs);
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
		
		BufferedReader reader = new BufferedReader(new StringReader(new String(byteOs.toByteArray(), "UTF-8")));
		
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", reader.readLine());
		assertEquals("<methodCall>", reader.readLine());
		assertEquals("   <methodName>somevalue</methodName>", reader.readLine());
		assertEquals("   <params>", reader.readLine());
		assertEquals("      <param>somevalue</param>", reader.readLine());
		assertEquals("      <list>", reader.readLine());
		assertEquals("         <value>somevalue1</value>", reader.readLine());
		assertEquals("         <value>somevalue2</value>", reader.readLine());
		assertEquals("         <value>somevalue3</value>", reader.readLine());
		assertEquals("      </list>", reader.readLine());
		assertEquals("   </params>", reader.readLine());
		assertEquals("   <empty/>", reader.readLine());
		assertEquals("</methodCall>", reader.readLine());
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_XmlPrettyPrinter.class);
	}
}
