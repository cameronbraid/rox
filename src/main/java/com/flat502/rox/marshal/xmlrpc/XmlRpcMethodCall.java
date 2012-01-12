package com.flat502.rox.marshal.xmlrpc;

import java.io.IOException;

import com.flat502.rox.marshal.FieldNameEncoder;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.utils.XmlPrinter;

/**
 * An instance of this class represents an XML-RPC method
 * call.
 * <p>
 * This class supports marshalling of XML-RPC method calls,
 * <i>not</i> execution. For that functionality see the
 * {@link com.flat502.rox.client.XmlRpcClient} class.
 * <p>
 * The relationship between Java and XML-RPC data types is
 * discussed in the description of the 
 * {@link com.flat502.rox.marshal.xmlrpc.XmlRpcMethod} class.
 */
// TODO: Document constructors
public class XmlRpcMethodCall extends XmlRpcMethod implements RpcCall {
	private String name;
	private Object[] params;

	public XmlRpcMethodCall(String name) {
		this(name, null, null);
	}

	public XmlRpcMethodCall(String name, Object[] params) {
		this(name, params, null);
	}

	public XmlRpcMethodCall(String name, Object[] params, FieldNameEncoder fieldNameEncoder) {
		super(fieldNameEncoder);
		this.setName(name);
		this.params = params;
		if (this.params == null) {
			this.params = new Object[0];
		}
	}

	protected void marshalImpl(XmlPrinter out) throws MarshallingException, IOException {
		out.openTag("methodCall");
		out.openTag("methodName");
		out.writeValue(this.name);
		out.closeTag("methodName");
		if (this.params != null && this.params.length > 0) {
			out.openTag("params");
			for (int i = 0; i < this.params.length; i++) {
				out.openTag("param");
				this.marshalValue(out, 3, this.params[i]);
				out.closeTag("param");
			}
			out.closeTag("params");
		}
		out.closeTag("methodCall");
	}

	public String getName() {
		return this.name;
	}

	public Object[] getParameters() {
		return this.params;
	}

	private void setName(String name) {
		this.name = this.validateMethodName(name);
	}

	private String validateMethodName(String name) {
		if (name == null) {
			throw new NullPointerException("null method name");
		}
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (ch >= 'a' && ch <= 'z') {
				continue;
			}
			if (ch >= 'A' && ch <= 'Z') {
				continue;
			}
			if (ch >= '0' && ch <= '9') {
				continue;
			}
			if (ch == '_' || ch <= '.' || ch <= ':' || ch <= '/' || ch <= '\\') {
				continue;
			}
			throw new IllegalArgumentException(
					"method name contains an illegal char: '" + ch + "'");
		}

		return name;
	}
}
