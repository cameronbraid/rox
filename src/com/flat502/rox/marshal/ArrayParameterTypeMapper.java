package com.flat502.rox.marshal;


/**
 * An implementation of {@link com.flat502.rox.marshal.MethodCallUnmarshallerAid}
 * that maps a parameter index into a given array of {@link Class} instances. 
 */
public class ArrayParameterTypeMapper extends MethodCallUnmarshallerAid {
	private Class[] types;

	/**
	 * Initializes a new instance using the given array.
	 * @param types
	 * 	The array of {@link Class} instances to map parameters
	 * 	onto. May be <code>null</code>.
	 */
	public ArrayParameterTypeMapper(Class[] types) {
		this.types = types;
	}

	/**
	 * Map the given parameter index into the underlying
	 * array of {@link Class} instances.
	 * @param methodName
	 * 	The name of the XML-RPC method being invoked.
	 * 	This is ignored.
	 * @param index
	 * 	The index into the underlying array.
	 * @return
	 * 	The entry at <code>index</code> (which may be
	 * 	<code>null</code>) or <code>null</code> if either
	 * 	the backing array is <code>null</code> or the
	 * 	given index exceeds the size of the backing
	 * 	array.
	 */
	public Class getType(String methodName, int index) {
		if (this.types == null || index >= this.types.length) {
			return null;
		}
		
		return this.types[index];
	}

	/**
	 * @return
	 * 	always returns <code>null</code>.
	 */
	public FieldNameCodec getFieldNameCodec(String methodName) {
		return null;
	}
}
