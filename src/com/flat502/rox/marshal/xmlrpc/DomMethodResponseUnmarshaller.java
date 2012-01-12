package com.flat502.rox.marshal.xmlrpc;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Iterator;

import net.n3.nanoxml.*;

import com.flat502.rox.marshal.*;
import com.flat502.rox.utils.Utils;

/**
 * A DOM based{@link com.flat502.rox.marshal.MethodResponseUnmarshaller} 
 * implementation.
 */
public class DomMethodResponseUnmarshaller implements
		ExtendedMethodResponseUnmarshaller {
	private DomUnmarshaller unmarshaller;
	
	public DomMethodResponseUnmarshaller() {
		this(null);
	}

	public DomMethodResponseUnmarshaller(FieldNameCodec fieldNameCodec) {
		this.unmarshaller = XmlRpcUtils.newDomUnmarshaller(fieldNameCodec);
	}
	
	public FieldNameCodec getDefaultFieldNameCodec() {
		return this.unmarshaller.getDefaultFieldNameCodec();
	}

	public RpcResponse unmarshal(InputStream in) throws Exception {
		return unmarshal(in, null);
	}

	public RpcResponse unmarshal(InputStream in, MethodResponseUnmarshallerAid aid)
			throws Exception {
		IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
		IXMLReader reader = new StdXMLReader(Utils.newXmlReader(in, Charset
				.forName("UTF-8")));
		parser.setReader(reader);
		XmlNode root = new NanoXmlNode((IXMLElement) parser.parse());
		return this.unmarshaller.parseMethodResponse(root, aid);
	}

	public RpcResponse unmarshal(Reader in) throws Exception {
		return unmarshal(in, null);
	}

	public RpcResponse unmarshal(Reader in, MethodResponseUnmarshallerAid aid) throws Exception {
		IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
		IXMLReader reader = new StdXMLReader(in);
		parser.setReader(reader);
		XmlNode root = new NanoXmlNode((IXMLElement) parser.parse());
		return this.unmarshaller.parseMethodResponse(root, aid);
	}

	public RpcResponse unmarshal(String xml) throws Exception {
		return unmarshal(xml, null);
	}

	public RpcResponse unmarshal(String xml, MethodResponseUnmarshallerAid aid) throws Exception {
		IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
		IXMLReader reader = new StdXMLReader(new StringReader(xml));
		parser.setReader(reader);
		XmlNode root = new NanoXmlNode((IXMLElement) parser.parse());
		return this.unmarshaller.parseMethodResponse(root, aid);
	}
}
