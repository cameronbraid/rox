package com.flat502.rox.marshal;

public class PublicFieldWithMethods {
	public boolean getterCalled;
	public boolean setterCalled;
	public String stringMember;
	
	public PublicFieldWithMethods() {
	}
	
	public PublicFieldWithMethods(String stringMember) {
		this.stringMember = stringMember;
	}
	
	public String getStringMember() {
		this.getterCalled = true;
		return this.stringMember;
	}
	
	public void setStringMember(String stringMember) {
		this.setterCalled = true;
		this.stringMember = stringMember;
	}
}
