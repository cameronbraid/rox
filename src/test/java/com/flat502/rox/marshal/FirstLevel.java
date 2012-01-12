package com.flat502.rox.marshal;

public class FirstLevel {
	public SecondLevel publicSecondLevel;
	private SecondLevel privateSecondLevel;
	
	public FirstLevel() {
	}
	
	public FirstLevel(SecondLevel publicSecondLevel, SecondLevel privateSecondLevel) {
		this.publicSecondLevel = publicSecondLevel;
		this.privateSecondLevel = privateSecondLevel;
	}

	public SecondLevel getPrivateSecondLevel() {
		return privateSecondLevel;
	}
	
	public void setPrivateSecondLevel(SecondLevel privateSecondLevel) {
		this.privateSecondLevel = privateSecondLevel;
	}
}
