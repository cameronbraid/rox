/**
 * 
 */
package com.flat502.rox.marshal;

import java.util.Date;

public class TestStruct {
	public Integer intObject;
	public int intVal;
	public Double doubleObject;
	public double doubleVal;
	public Float floatObject;
	public float floatVal;
	public Boolean booleanObject;
	public boolean booleanVal;
	public Date date;
	public String stringObject;
	public char[] charArray;
	public byte[] byteArray;
	public Object[] objectArray;

	public TestStruct() {
	}

	public TestStruct(Integer intObject, int intVal, Double doubleObject,
			double doubleVal, Float floatObject, float floatVal,
			Boolean booleanObject, boolean booleanVal, Date date,
			String stringObject, char[] charArray, byte[] byteArray, Object[] objectArray) {
		this.intObject = intObject;
		this.intVal = intVal;
		this.doubleObject = doubleObject;
		this.doubleVal = doubleVal;
		this.floatObject = floatObject;
		this.floatVal = floatVal;
		this.booleanObject = booleanObject;
		this.booleanVal = booleanVal;
		this.date = date;
		this.stringObject = stringObject;
		this.charArray = charArray;
		this.byteArray = byteArray;
		this.objectArray = objectArray;
	}
}