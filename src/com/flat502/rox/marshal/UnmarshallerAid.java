package com.flat502.rox.marshal;


/**
 * An assistive interface intended to support unmarshalling.
 * <P>
 * Implementations provide additional customizations for the unmarshalling
 * process.
 * @see com.flat502.rox.marshal.MethodCallUnmarshallerAid
 * @see com.flat502.rox.marshal.MethodResponseUnmarshallerAid
 */
public abstract class UnmarshallerAid {
	/**
	 * Invoked when a method call unmarshaller is required.
	 * <p>
	 * This method allows request handlers to override
	 * the default unmarshaller implementation for a given
	 * method (or class of methods) without imposing that
	 * unmarshaller on all request handlers.
	 * <p>
	 * Implementations may return the same instance for
	 * multiple calls to this method as long as the unmarshaller
	 * is thread-safe.
	 * <p>
	 * Implementations may return <code>null</code> in which
	 * case a default unmarshalling implementation will be used.
	 * @param methodName
	 * 	The name of the XML-RPC method call being unmarshalled.
	 * @return
	 * 	A new {@link MethodCallUnmarshaller} instance or
	 * 	<code>null</code>.
	 */
	public FieldNameCodec getFieldNameCodec(String methodName) {
		return null;
	}
	
	/**
	 * Implementations may override this to control whether or
	 * not missing fields are ignored during unmarshalling.
	 * @return
	 * 	<code>true</code> if missing fields should be ignored
	 * 	or <code>false</code> if they should result in an error.
	 */
	public boolean ignoreMissingFields() {
		return false;
	}
}
