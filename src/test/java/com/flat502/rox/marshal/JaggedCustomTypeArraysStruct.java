/**
 * 
 */
package com.flat502.rox.marshal;

import java.util.Date;

public class JaggedCustomTypeArraysStruct {
	public SimpleStruct[][] publicCustomArray;

	private SimpleStruct[][] privateCustomArray;

	public JaggedCustomTypeArraysStruct(SimpleStruct[][] publicCustomArray,
			SimpleStruct[][] privateCustomArray) {
		this.publicCustomArray = publicCustomArray;
		this.privateCustomArray = privateCustomArray;
	}

	public JaggedCustomTypeArraysStruct() {
	}

	public SimpleStruct[][] getPrivateCustomArray() {
		return this.privateCustomArray;
	}

	public void setprivateCustomArray(SimpleStruct[][] v) {
		this.privateCustomArray = v;
	}
}