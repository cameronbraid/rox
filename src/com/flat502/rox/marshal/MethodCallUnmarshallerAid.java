package com.flat502.rox.marshal;


/**
 * An assistive interface intended to support unmarshalling.
 * <P>
 * Implementations provide additional customizations for the unmarshalling
 * process based on an RPC method name.
 * @see com.flat502.rox.server.HttpRpcServer#registerHandler(String, String, AsyncRequestHandler, MethodCallUnmarshallerAid)
 * @see com.flat502.rox.server.HttpRpcServer#registerHandler(String, String, SyncRequestHandler, MethodCallUnmarshallerAid)
 */
public abstract class MethodCallUnmarshallerAid extends UnmarshallerAid {
	/**
	 * An implementation should return a {@link Class} instance representing
	 * the type a complex parameter value (a struct or an array) at the given 
	 * index should be unmarshalled as.
	 * <p>
	 * A null return value will result in the parameter value being unmarshalled
	 * into a {@link java.util.Map} instance if, and only if, the parameter value
	 * is an XML-RPC struct and a {@link java.util.List} instance if, and only if,
	 * the value is an XML-RPC array.
	 * <p>
	 * The returned {@link Class} must be accessible, instantiable, and have an 
	 * accessible default constructor.
	 * @param methodName
	 * 	The name of the XML-RPC method call being unmarshalled.
	 * @param index
	 * 	The index of the parameter value being unmarshalled.
	 * @return
	 * 	The {@link Class} for the type the parameter value should
	 * 	be unmarshalled as, assuming it is a struct or an array.
	 */
	public abstract Class getType(String methodName, int index);
}
