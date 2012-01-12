package com.flat502.rox.marshal.xmlrpc;

import java.io.IOException;
import java.util.*;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.custommonkey.xmlunit.XMLTestCase;
import org.xml.sax.SAXException;

import com.flat502.rox.marshal.xmlrpc.XmlRpcMethodCall;

public class TestBase_XmlRpcMethod extends XMLTestCase {
	public TestBase_XmlRpcMethod(String name) {
		super(name);
	}

	protected void assertStructValueEquals(int idx, String name, String type,
			String value, String xml) throws TransformerConfigurationException,
			SAXException, IOException, ParserConfigurationException,
			TransformerException {
		assertXpathEvaluatesTo(name,
				"//params/param/value/struct/member[" + idx + "]/name",
				xml);
		assertXpathEvaluatesTo(value,
				"//params/param/value/struct/member[" + idx + "]/value/"
						+ type, xml);
	}

	protected void assertStructValueEquals(int idx, String name, String type,
			List list, String xml) throws TransformerConfigurationException,
			SAXException, IOException, ParserConfigurationException,
			TransformerException {
		assertXpathEvaluatesTo(name,
				"//params/param/value/struct/member[" + idx + "]/name",
				xml);
		Iterator items = list.iterator();
		int listIdx = 1;
		while (items.hasNext()) {
			Object item = items.next();
			assertXpathEvaluatesTo(item.toString(),
					"//params/param/value/struct/member[" + idx
							+ "]/value/array/data/value[" + listIdx + "]/" + type, xml);
			listIdx++;
		}
	}

	protected void assertEquals(byte[] expected, byte[] actual) {
		if (expected == null ^ actual == null) {
			fail();
		}
		if (expected == actual) {
			fail();
		}
		if (expected.length != actual.length) {
			fail();
		}
		for (int i = 0; i < actual.length; i++) {
			if (expected[i] != actual[i]) {
				fail();
			}
		}
	}

	protected Date newDate(int year, int month, int date, int hour,
			int min, int sec) {
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
		cal.set(Calendar.YEAR, year);
		cal.set(Calendar.MONTH, month - 1);
		cal.set(Calendar.DATE, date);
		cal.set(Calendar.HOUR_OF_DAY, hour);
		cal.set(Calendar.MINUTE, min);
		cal.set(Calendar.SECOND, sec);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTime();
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(TestBase_XmlRpcMethod.class);
	}
}
