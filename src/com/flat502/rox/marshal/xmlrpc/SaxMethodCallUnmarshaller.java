package com.flat502.rox.marshal.xmlrpc;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.parsers.SAXParser;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.flat502.rox.marshal.ArrayParameterTypeMapper;
import com.flat502.rox.marshal.ExtendedMethodCallUnmarshaller;
import com.flat502.rox.marshal.FieldNameCodec;
import com.flat502.rox.marshal.HyphenatedFieldNameCodec;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.MethodCallUnmarshallerAid;
import com.flat502.rox.marshal.RpcCall;

/**
 * Parse XML RPC method call using SAX
 */
public class SaxMethodCallUnmarshaller implements ExtendedMethodCallUnmarshaller {
	private SaxParserPool pool;

	public SaxMethodCallUnmarshaller(SaxParserPool pool) {
		this.pool = pool;
	}

	public SaxMethodCallUnmarshaller() {
		this(SaxParserPool.DEFAULT_PARSER_POOL);
	}

	protected RpcCall buildXmlRpcMethodCall(SaxUnmarshaller unmarshaller) {
		return new XmlRpcMethodCall(unmarshaller.getMethodName(), unmarshaller.getParams());
	}

	private RpcCall unmarshalAny(Object in, MethodCallUnmarshallerAid aid) throws Exception {
		SAXParser parser = pool.provideParser();
		try {
			SaxUnmarshaller unmarshaller = pool.provideUnmarshaller();
			unmarshaller.expectRequest(true);
			unmarshaller.setCallAid(aid);
			if (in instanceof InputStream) {
				parser.parse((InputStream) in, unmarshaller.getSaxHandler());
			} else {
				parser.parse((InputSource) in, unmarshaller.getSaxHandler());
			}

			RpcCall call = buildXmlRpcMethodCall(unmarshaller);
			pool.returnUnmarshaller(unmarshaller);
			return call;
		} catch (SAXException e) {
			throw new MarshallingException(e);
		} finally {
			pool.returnParser(parser);
		}
	}

	public RpcCall unmarshal(InputStream in, MethodCallUnmarshallerAid aid) throws Exception {
		return unmarshalAny(in, aid);
	}

	protected RpcCall unmarshal(InputSource in, MethodCallUnmarshallerAid aid) throws Exception {
		return unmarshalAny(in, aid);
	}

	public RpcCall unmarshal(Reader in, MethodCallUnmarshallerAid aid) throws Exception {
		return unmarshal(new InputSource(in), aid);
	}

	public RpcCall unmarshal(String xml, MethodCallUnmarshallerAid aid) throws Exception {
		return unmarshal(new StringReader(xml), aid);
	}

	public RpcCall unmarshal(InputStream in) throws Exception {
		return unmarshal(in, (MethodCallUnmarshallerAid) null);
	}

	public RpcCall unmarshal(InputStream in, Class[] structClasses) throws Exception {
		return unmarshal(in, new ArrayParameterTypeMapper(structClasses));
	}

	public RpcCall unmarshal(Reader in) throws Exception {
		return unmarshal(in, (MethodCallUnmarshallerAid) null);
	}

	public RpcCall unmarshal(Reader in, Class[] structClasses) throws Exception {
		return unmarshal(in, new ArrayParameterTypeMapper(structClasses));
	}

	public RpcCall unmarshal(String xml) throws Exception {
		return unmarshal(xml, (MethodCallUnmarshallerAid) null);
	}

	public RpcCall unmarshal(String xml, Class[] structClasses) throws Exception {
		return unmarshal(xml, new ArrayParameterTypeMapper(structClasses));
	}

	public FieldNameCodec getDefaultFieldNameCodec() {
		return new HyphenatedFieldNameCodec();
	}

}
