package com.flat502.rox.marshal.xmlrpc;

import java.io.IOException;

import com.flat502.rox.marshal.Fault;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.NopFieldNameCodec;
import com.flat502.rox.marshal.RpcFault;
import com.flat502.rox.utils.XmlPrinter;

/**
 * An instance of this class represents an XML-RPC method
 * call fault.
 * <p>
 * The relationship between Java and XML-RPC data types is
 * discussed in the description of the 
 * {@link com.flat502.rox.marshal.xmlrpc.XmlRpcMethod} class.
 */
public class XmlRpcMethodFault extends XmlRpcMethodResponse implements RpcFault {
	private Fault fault;

	public XmlRpcMethodFault(Throwable fault) {
		this(0, fault);
	}

	public XmlRpcMethodFault(int faultCode, Throwable fault) {
		//	We use NopFieldNameCodec since the XML-RPC fault code and fault 
		// string fields already use the standard Java field naming
		//	convention.
		super(new NopFieldNameCodec());
		this.fault = this.newFault(faultCode, this.toFaultString(fault));
	}

	public XmlRpcMethodFault(int faultCode, String faultString) {
		//	We use NopFieldNameCodec since the XML-RPC fault code and fault 
		// string fields already use the standard Java field naming
		//	convention.
		super(new NopFieldNameCodec());
		this.fault = this.newFault(faultCode, faultString);
	}

	protected void marshalImpl(XmlPrinter out) throws MarshallingException, IOException {
		out.openTag("methodResponse");
		out.openTag("fault");
		this.marshalValue(out, 3, this.fault);
		out.closeTag("fault");
		out.closeTag("methodResponse");
	}

	/*
	 * Documented by interface
	 */
	public String getFaultString() {
		return this.fault.faultString;
	}

	/*
	 * Documented by interface
	 */
	public int getFaultCode() {
		return this.fault.faultCode;
	}

	/**
	 * Converts an exception into a String value for use
	 * as a fault string
	 * <p>
	 * This method exists so that sub-classes can customize
	 * how the fault string is derived from an exception.
	 * @param exception
	 * 	The exception to convert.
	 * @return
	 * 	A String value suitable for use as a fault
	 * 	string.
	 */
	protected String toFaultString(Throwable exception) {
		return exception.getMessage();
	}

	/**
	 * Constructs a new instance of {@link Fault} initialized
	 * with the given fault code and fault string.
	 * <p>
	 * This factory method is exposed so that implementations
	 * can return a sub-class of {@link Fault} that may be
	 * more useful in their context.
	 * @param faultCode
	 * 	The fault code to initialize the instance with.
	 * @param faultString
	 * 	The fault string to initialize the instance with.
	 * @return
	 * 	An instance of {@link Fault} (or a sub-class)
	 * 	suitably initialized.
	 */
	protected Fault newFault(int faultCode, String faultString) {
		return new Fault(faultCode, faultString);
	}
}
