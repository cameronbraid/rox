package com.flat502.rox.server;

import com.flat502.rox.marshal.FieldNameCodec;
import com.flat502.rox.marshal.FieldNameEncoder;
import com.flat502.rox.marshal.RpcFault;
import com.flat502.rox.marshal.RpcResponse;
import com.flat502.rox.marshal.xmlrpc.XmlRpcMethodFault;
import com.flat502.rox.marshal.xmlrpc.XmlRpcMethodResponse;

/**
 * This is a specialization of {@link com.flat502.rox.server.RpcMethodProxy}
 * that creates objects that cater for XML-RPC.
 */
public class XmlRpcMethodProxy extends RpcMethodProxy {
	private FieldNameEncoder fieldNameEncoder;

	public XmlRpcMethodProxy(String namePattern, Object target) {
		this(namePattern, target, null);
	}

	public XmlRpcMethodProxy(String namePattern, Object target, FieldNameEncoder fieldNameEncoder) {
		super(namePattern, target);
		this.fieldNameEncoder = fieldNameEncoder;
	}

	protected RpcResponse newRpcResponse(Object returnValue) {
		return new XmlRpcMethodResponse(returnValue, this.fieldNameEncoder);
	}

	public RpcFault newRpcFault(Throwable e) {
		return new XmlRpcMethodFault(e);
	}
}
