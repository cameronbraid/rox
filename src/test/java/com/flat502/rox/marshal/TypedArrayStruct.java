/**
 * 
 */
package com.flat502.rox.marshal;

import java.util.Date;

public class TypedArrayStruct {
	public SimpleStruct[] publicTypedArray;
	private SimpleStruct[] privateTypedArray;

	public TypedArrayStruct() {
	}

	public TypedArrayStruct(SimpleStruct[] publicTypedArray, SimpleStruct[] privateTypedArray) {
		this.publicTypedArray = publicTypedArray;
		this.privateTypedArray = privateTypedArray;
	}
	
	public SimpleStruct[] getPrivateTypedArray() {
		return this.privateTypedArray;
	}
	
	public void setPrivateTypedArray(SimpleStruct[] v) {
		this.privateTypedArray = v;
	}
}