package com.flat502.rox.marshal;

// TODO: Document
public interface FieldNameCodec extends FieldNameEncoder, FieldNameDecoder {
	String encodeFieldName(String name);
	String decodeFieldName(String name);
}
