package com.flat502.rox.marshal.xmlrpc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.Iterator;

import net.n3.nanoxml.IXMLElement;
import net.n3.nanoxml.IXMLParser;
import net.n3.nanoxml.IXMLReader;
import net.n3.nanoxml.StdXMLReader;
import net.n3.nanoxml.XMLParserFactory;

import com.flat502.rox.http.HttpConstants;
import com.flat502.rox.marshal.FieldNameEncoder;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.RpcResponse;
import com.flat502.rox.utils.Utils;
import com.flat502.rox.utils.XmlPrinter;

/**
 * An instance of this class represents an XML-RPC method
 * call response.
 * <p>
 * The relationship between Java and XML-RPC data types is
 * discussed in the description of the 
 * {@link com.flat502.rox.marshal.xmlrpc.XmlRpcMethod} class.
 */
public class XmlRpcMethodResponse extends XmlRpcMethod implements RpcResponse {
	private Object retVal;

	XmlRpcMethodResponse(FieldNameEncoder fieldNameEncoder) {
		super(fieldNameEncoder);
	}

	/**
	 * Initialize an instance with a return value.
	 * @param retVal
	 * 	The return value for this instance. 
	 * 	A <code>null</code> value is not supported.
	 */
	public XmlRpcMethodResponse(Object retVal) {
		this(retVal, null);
	}
	
	// TODO: Document
	public XmlRpcMethodResponse(Object retVal, FieldNameEncoder fieldNameEncoder) {
		super(fieldNameEncoder);
		this.setReturnValue(retVal);
	}
	
	/**
	 * Get the return value for this instance.
	 * @return
	 * 	the return value for this instance,
	 * 	never <code>null</code>.
	 */
	public Object getReturnValue() {
		return this.retVal;
	}
	
	protected void marshalImpl(XmlPrinter out) throws MarshallingException, IOException {
		out.openTag("methodResponse");
		out.openTag("params");
		out.openTag("param");
		this.marshalValue(out, 3, this.retVal);
		out.closeTag("param");
		out.closeTag("params");
		out.closeTag("methodResponse");
	}

	private void setReturnValue(Object retVal) {
		if (retVal == null) {
			throw new NullPointerException("null return values are not supported");
		}
		this.retVal = retVal;
	}
}
