package com.flat502.rox.marshal;

/**
 * A convenience class representing an XML-RPC fault.
 * <p>
 * This may be used when creating your own faults on the server side.
 */
public class Fault {
	public String faultString;
	public int faultCode;

	/**
	 * Mandatory public default constructor.
	 */
	public Fault() {
	}

	/**
	 * Initializes a new instance using the provided fault string
	 * and fault code.
	 * @param faultCode
	 * 	A code identifying the fault.
	 * @param faultString
	 * 	A string describing the fault
	 */
	public Fault(int faultCode, String faultString) {
		this.faultCode = faultCode;
		this.faultString = faultString;
	}

	/**
	 * @return
	 * 	A string representation of this object that includes the
	 * 	underlying fault code and fault string.
	 */
	public String toString() {
		return "Fault[" + this.faultCode + ": " + this.faultString + "]";
	}
}