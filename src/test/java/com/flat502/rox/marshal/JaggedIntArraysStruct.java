/**
 * 
 */
package com.flat502.rox.marshal;

import java.util.Date;

public class JaggedIntArraysStruct {
	public int[][] publicIntArray;

	private int[][] privateIntArray;

	public JaggedIntArraysStruct(int[][] publicIntArray, int[][] privateIntArray) {
		this.publicIntArray = publicIntArray;
		this.privateIntArray = privateIntArray;
	}

	public JaggedIntArraysStruct() {
	}

	public int[][] getPrivateIntArray() {
		return this.privateIntArray;
	}

	public void setPrivateIntArray(int[][] v) {
		this.privateIntArray = v;
	}
}