package com.flat502.rox.marshal;

/**
 * Encapsulates methods common to all unmarshallers and provides type-safety.
 */
public interface MethodUnmarshaller {
	/**
	 * This method is invoked when no specific
	 * {@link FieldNameCodec} exists.
	 * <p>
	 * For client implementations this typically means
	 * no codec has been set on the client.
	 * <p>
	 * For server implementations this typically means
	 * {@link UnmarshallerAid#getFieldNameCodec(String)}
	 * has returned <code>null</code>.
	 * @return
	 * 	the default codec associated with this
	 * 	unmarshaller.
	 */
	FieldNameCodec getDefaultFieldNameCodec();
}
