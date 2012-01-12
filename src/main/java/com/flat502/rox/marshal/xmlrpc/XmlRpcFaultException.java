package com.flat502.rox.marshal.xmlrpc;

import com.flat502.rox.processing.RpcFaultException;

// TODO: Document
public class XmlRpcFaultException extends RpcFaultException {
	public XmlRpcFaultException(int faultCode, Throwable cause) {
		super(new XmlRpcMethodFault(faultCode, cause));
		this.initCause(cause);
	}

	public XmlRpcFaultException(int faultCode, String faultString) {
		super(new XmlRpcMethodFault(faultCode, faultString));
	}

	public XmlRpcFaultException(Throwable cause) {
		super(new XmlRpcMethodFault(cause));
		this.initCause(cause);
	}
}
