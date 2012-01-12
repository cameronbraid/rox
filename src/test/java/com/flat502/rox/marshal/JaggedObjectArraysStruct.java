/**
 * 
 */
package com.flat502.rox.marshal;

import java.util.Date;

public class JaggedObjectArraysStruct {
	public Object[][] publicObjectArray;

	private Object[][] privateObjectArray;

	public JaggedObjectArraysStruct(Object[][] publicObjectArray,
			Object[][] privateObjectArray) {
		this.publicObjectArray = publicObjectArray;
		this.privateObjectArray = privateObjectArray;
	}

	public JaggedObjectArraysStruct() {
	}

	public Object[][] getPrivateObjectArray() {
		return this.privateObjectArray;
	}

	public void setprivateObjectArray(Object[][] v) {
		this.privateObjectArray = v;
	}
}