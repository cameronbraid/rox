/**
 * 
 */
package com.flat502.rox.marshal;

import java.util.Date;

public class IntegerArraysStruct {
	public Integer[] publicIntegerArray;

	private Integer[] privateIntegerArray;

	public IntegerArraysStruct(Integer[] publicIntegerArray, Integer[] privateIntegerArray) {
		this.publicIntegerArray = publicIntegerArray;
		this.privateIntegerArray = privateIntegerArray;
	}

	public IntegerArraysStruct() {
	}

	public Integer[] getPrivateIntegerArray() {
		return this.privateIntegerArray;
	}

	public void setPrivateIntegerArray(Integer[] v) {
		this.privateIntegerArray = v;
	}
}