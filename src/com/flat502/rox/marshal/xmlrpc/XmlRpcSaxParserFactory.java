package com.flat502.rox.marshal.xmlrpc;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * SAXParserFactory for instantiating XmlRpcSaxParser's
 */
public class XmlRpcSaxParserFactory extends SAXParserFactory {
	public SAXParser newSAXParser() throws ParserConfigurationException, SAXException {
		if (isValidating()) {
			throw new ParserConfigurationException("No support for validation.");
		}
		if (isNamespaceAware()) {
			throw new ParserConfigurationException("No support for namespaces.");
		}
		// now that we've got all that off our chest
		return new XmlRpcSaxParser();
	}

	public void setFeature(String name, boolean value) throws ParserConfigurationException, SAXNotRecognizedException,
			SAXNotSupportedException {
		throw new SAXNotRecognizedException("No feature support (and specifically no support for \"" + name + "\")");
	}

	public boolean getFeature(String name) throws ParserConfigurationException, SAXNotRecognizedException,
			SAXNotSupportedException {
		throw new SAXNotRecognizedException("No feature support (and specifically no support for \"" + name + "\")");
	}
}
