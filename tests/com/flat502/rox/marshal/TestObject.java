/**
 * 
 */
package com.flat502.rox.marshal;

import java.util.Date;
import java.util.List;

public class TestObject {
	private Integer intObject;
	private int intVal;
	private Double doubleObject;
	private double doubleVal;
	private Float floatObject;
	private float floatVal;
	private Boolean booleanObject;
	private boolean booleanVal;
	private Date date;
	private String stringObject;
	private char[] charArray;
	private byte[] byteArray;
	private List listValue;

	public TestObject() {
	}

	public TestObject(Integer intObject, int intVal, Double doubleObject,
			double doubleVal, Float floatObject, float floatVal,
			Boolean booleanObject, boolean booleanVal, Date date,
			String stringObject, char[] charArray, byte[] byteArray, List list) {
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
		this.listValue = list;
	}

	public Boolean getBooleanObject() {
		return booleanObject;
	}

	public void setBooleanObject(Boolean booleanObject) {
		this.booleanObject = booleanObject;
	}

	public boolean getBooleanVal() {
		return booleanVal;
	}

	public void setBooleanVal(boolean booleanVal) {
		this.booleanVal = booleanVal;
	}

	public byte[] getByteArray() {
		return byteArray;
	}

	public void setByteArray(byte[] byteArray) {
		this.byteArray = byteArray;
	}

	public char[] getCharArray() {
		return charArray;
	}

	public void setCharArray(char[] charArray) {
		this.charArray = charArray;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Double getDoubleObject() {
		return doubleObject;
	}

	public void setDoubleObject(Double doubleObject) {
		this.doubleObject = doubleObject;
	}

	public double getDoubleVal() {
		return doubleVal;
	}

	public void setDoubleVal(double doubleVal) {
		this.doubleVal = doubleVal;
	}

	public Float getFloatObject() {
		return floatObject;
	}

	public void setFloatObject(Float floatObject) {
		this.floatObject = floatObject;
	}

	public float getFloatVal() {
		return floatVal;
	}

	public void setFloatVal(float floatVal) {
		this.floatVal = floatVal;
	}

	public Integer getIntObject() {
		return intObject;
	}

	public void setIntObject(Integer intObject) {
		this.intObject = intObject;
	}

	public int getIntVal() {
		return intVal;
	}

	public void setIntVal(int intVal) {
		this.intVal = intVal;
	}

	public String getStringObject() {
		return stringObject;
	}

	public void setStringObject(String stringObject) {
		this.stringObject = stringObject;
	}
	
	public List getListValue() {
		return this.listValue;
	}
	
	public void setListValue(List listValue) {
		this.listValue = listValue;
	}
	
	public String toString() {
		return "Testing";
	}
}