package com.flat502.rox.marshal.xmlrpc;

import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.flat502.rox.marshal.FieldNameCodec;
import com.flat502.rox.marshal.HyphenatedFieldNameCodec;

/**
 * Maintains a pool of SAX parsers and unmarshallers.
 */
public class SaxParserPool {
	private static boolean USE_INTERNAL_PARSER_IMPL;
	public static SaxParserPool DEFAULT_PARSER_POOL;
	
	static {
		reset();
	}
	
	private boolean resetChecked = false;
	private boolean resetAvailable;

	private SAXParserFactory factory = null;
	private Stack parsers = new Stack();
	private Stack unmarshallers = new Stack();

	private FieldNameCodec codec;
	
	/**
	 * Package private reset method for forcing reinitialization so we can unit
	 * test more effectively.
	 */
	static void reset() {
		// Unless explicitly overridden use our internal SAX parser implementation.
		USE_INTERNAL_PARSER_IMPL = System.getProperty("javax.xml.parsers.SAXParserFactory") == null;
		DEFAULT_PARSER_POOL = new SaxParserPool();
	}

	public SaxParserPool() {
		this(new HyphenatedFieldNameCodec());
	}

	public SaxParserPool(FieldNameCodec codec) {
		this.codec = codec;
	}

	public synchronized SAXParser provideParser() throws Exception {
		if (parsers.isEmpty()) {
			//	    System.out.println( "*********************** Creating new parser ****************************" );
			if (USE_INTERNAL_PARSER_IMPL) {
				return new XmlRpcSaxParser();
			}
			if (factory == null) {
				factory = SAXParserFactory.newInstance();
				factory.setNamespaceAware(false);
				factory.setValidating(false);
			}
			return factory.newSAXParser();
		} else {
			return (SAXParser) parsers.pop();
		}
	}

	public synchronized void returnParser(SAXParser parser) {
		try {
			if (!resetChecked) {
				try {
					parser.getClass().getDeclaredMethod("reset", (Class[])null);
					resetAvailable = true;
				} catch (NoSuchMethodException e) {
					resetAvailable = false;
				}
				resetChecked = true;
			}

			if (resetAvailable) {
				parser.reset();
				parsers.push(parser);
			} else if (USE_INTERNAL_PARSER_IMPL) {
				((XmlRpcSaxParser) parser).reset();
				parsers.push(parser);
			}
		} catch (Exception e) {
			e.printStackTrace(); // TODO log properly
		}
	}

	public synchronized SaxUnmarshaller provideUnmarshaller() throws Exception {
		if (unmarshallers.isEmpty()) {
			return XmlRpcUtils.newSaxUnmarshaller(this.codec);
		} else {
			return (SaxUnmarshaller) unmarshallers.pop();
		}
	}

	public synchronized void returnUnmarshaller(SaxUnmarshaller unmarshaller) {
		unmarshallers.push(unmarshaller);
	}

}
