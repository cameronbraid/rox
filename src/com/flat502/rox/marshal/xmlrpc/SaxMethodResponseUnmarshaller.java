package com.flat502.rox.marshal.xmlrpc;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.SAXParser;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.flat502.rox.marshal.*;

/**
 * Parse XML RPC method response using SAX.
 */
public class SaxMethodResponseUnmarshaller implements ExtendedMethodResponseUnmarshaller {
	private SaxParserPool pool;

	public SaxMethodResponseUnmarshaller(SaxParserPool pool) {
		this.pool = pool;
	}

	public SaxMethodResponseUnmarshaller() {
		this(SaxParserPool.DEFAULT_PARSER_POOL);
	}

	protected RpcResponse buildXmlRpcResponse(SaxUnmarshaller unmarshaller) {
		Fault fault = unmarshaller.getFault();

		if (fault != null) {
			return new XmlRpcMethodFault(fault.faultCode, fault.faultString);
		} else {
			return new XmlRpcMethodResponse(unmarshaller.getResponse());
		}
	}

	private RpcResponse unmarshalAny(Object in, MethodResponseUnmarshallerAid aid) throws Exception {
		SAXParser parser = pool.provideParser();
		try {
			SaxUnmarshaller unmarshaller = pool.provideUnmarshaller();
			try {
				unmarshaller.expectRequest(false);
				unmarshaller.setResponseAid(aid);
				if (in instanceof InputStream) {
					parser.parse((InputStream) in, unmarshaller.getSaxHandler());
				} else {
					parser.parse((InputSource) in, unmarshaller.getSaxHandler());
				}

				return buildXmlRpcResponse(unmarshaller);
			} finally {
				pool.returnUnmarshaller(unmarshaller);
			}
		} catch(SAXException e) {
			throw new MarshallingException(e);
		} finally {
			pool.returnParser(parser);
		}
	}

	public RpcResponse unmarshal(InputStream in, MethodResponseUnmarshallerAid aid) throws Exception {
		return unmarshalAny(in, aid);
	}

	protected RpcResponse unmarshal(InputSource in, MethodResponseUnmarshallerAid aid) throws Exception {
		return unmarshalAny(in, aid);
	}

	public RpcResponse unmarshal(Reader in, MethodResponseUnmarshallerAid aid) throws Exception {
		return unmarshal(new InputSource(in), aid);
	}

	public RpcResponse unmarshal(String xml, MethodResponseUnmarshallerAid aid) throws Exception {
		return unmarshal(new StringReader(xml), aid);
	}

	public RpcResponse unmarshal(InputStream in) throws Exception {
		return unmarshal(in, (MethodResponseUnmarshallerAid) null);
	}

	public RpcResponse unmarshal(Reader in) throws Exception {
		return unmarshal(in, (MethodResponseUnmarshallerAid) null);
	}

	public RpcResponse unmarshal(String xml) throws Exception {
		return unmarshal(xml, (MethodResponseUnmarshallerAid) null);
	}

	public FieldNameCodec getDefaultFieldNameCodec() {
		return new HyphenatedFieldNameCodec();
	}
}
