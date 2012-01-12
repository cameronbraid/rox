package com.flat502.rox.marshal;

/**
 * An interface representing a generalized RPC method
 * call fault.
 * <p>
 * This interface is patterned after XML-RPC and extends
 * the {@link com.flat502.rox.marshal.RpcResponse} interface
 * by adding the idea of a fault code and fault string.
 */
public interface RpcFault extends RpcResponse {
	/**
	 * Get the fault code for this RPC fault.
	 * <P>
	 * Fault codes are implementation-dependent.
	 * @return
	 * 	The fault code for this RPC fault.
	 */
	int getFaultCode();

	/**
	 * Get the fault string describing the fault
	 * code and the fault in more detail.
	 * <p>
	 * Fault strings are implementation-dependent.
	 * In general fault codes should be used for
	 * programmatic handling of faults and fault
	 * strings for display only.
	 * @return
	 * 	The fault string for this RPC fault.
	 */
	String getFaultString();
}
