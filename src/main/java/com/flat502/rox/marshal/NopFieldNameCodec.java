package com.flat502.rox.marshal;

// TODO: Document
public class NopFieldNameCodec implements FieldNameCodec {
	public String encodeFieldName(String name) {
		return name;
	}
	
	public String decodeFieldName(String name) {
		return name;
	}
}
