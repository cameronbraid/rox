package com.flat502.rox.marshal;

import java.util.Date;

public class PrimitivesStruct {
	public byte byteVal;
	public short shortVal;
	public int intVal;
	public long longVal;
	public float floatVal;
	public double doubleVal;
	public boolean booleanVal;
	public char charVal;

	public PrimitivesStruct() {
	}

	public PrimitivesStruct(byte byteVal, short shortVal, int intVal, long longVal, float floatVal, double doubleVal,
			boolean booleanVal, char charVal) {
		this.byteVal = byteVal;
		this.shortVal = shortVal;
		this.intVal = intVal;
		this.longVal = longVal;
		this.floatVal = floatVal;
		this.doubleVal = doubleVal;
		this.booleanVal = booleanVal;
		this.charVal = charVal;
	}
}
