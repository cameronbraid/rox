package com.flat502.rox.processing;

import com.flat502.rox.marshal.RpcFault;

/**
 * An exception encapsulating an {@link com.flat502.rox.marshal.RpcFault}.
 * <p>
 * On the client side, an instance of this class is created and raised 
 * when a remote RPC fault is returned in response to a method call.
 * <p>
 * On the server side implementations may raise this from within a
 * handler to indicate that a fault should be returned.
 * <p>
 * This exception is a {@link java.lang.RuntimeException}
 * to allow implementations to raise it from within objects
 * proxyied on the server side (using a
 * {@link com.flat502.rox.server.ProxyingRequestHandler})
 * without forcing them to declare the exception as part of
 * their method signature.
 */
public class RpcFaultException extends RuntimeException {
	private RpcFault fault;

	public RpcFaultException(RpcFault fault) {
		super(fault.getFaultCode() + ": " + fault.getFaultString());
		this.fault = fault;
	}

	/**
	 * Get the fault string for the underlying RPC fault.
	 * @return
	 * 	The value of the <code>faultString</code> member within
	 * 	the RPC fault.
	 */
	public String getFaultString() {
		return this.fault.getFaultString();
	}

	/**
	 * Get the fault code for the underlying RPC fault.
	 * @return
	 * 	The value of the <code>faultCode</code> member within
	 * 	the RPC fault.
	 */
	public int getFaultCode() {
		return this.fault.getFaultCode();
	}

	public RpcFault toFault() {
		return this.fault;
	}
}
