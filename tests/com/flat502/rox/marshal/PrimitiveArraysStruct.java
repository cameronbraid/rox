/**
 * 
 */
package com.flat502.rox.marshal;

import java.util.Date;

public class PrimitiveArraysStruct {
	public int[] publicIntArray;

	private int[] privateIntArray;

	public boolean[] publicBooleanArray;

	private boolean[] privateBooleanArray;

	public double[] publicDoubleArray;

	private double[] privateDoubleArray;

	public PrimitiveArraysStruct(int[] publicIntArray, int[] privateIntArray,
			boolean[] publicBooleanArray, boolean[] privateBooleanArray,
			double[] publicDoubleArray, double[] privateDoubleArray) {
		this.publicIntArray = publicIntArray;
		this.privateIntArray = privateIntArray;
		this.publicBooleanArray = publicBooleanArray;
		this.privateBooleanArray = privateBooleanArray;
		this.publicDoubleArray = publicDoubleArray;
		this.privateDoubleArray = privateDoubleArray;
	}

	public PrimitiveArraysStruct() {
	}

	public int[] getPrivateIntArray() {
		return this.privateIntArray;
	}

	public void setPrivateIntArray(int[] v) {
		this.privateIntArray = v;
	}

	public boolean[] getPrivateBooleanArray() {
		return this.privateBooleanArray;
	}

	public void setPrivateBooleanArray(boolean[] v) {
		this.privateBooleanArray = v;
	}

	public double[] getPrivateDoubleArray() {
		return this.privateDoubleArray;
	}

	public void setPrivateDoubleArray(double[] v) {
		this.privateDoubleArray = v;
	}
}