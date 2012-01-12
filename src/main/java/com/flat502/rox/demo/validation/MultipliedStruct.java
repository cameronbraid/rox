/**
 * 
 */
package com.flat502.rox.demo.validation;

public class MultipliedStruct {
	public int times10;
	public int times100;
	public int times1000;
	
	public MultipliedStruct(int n) {
		times10 = n*10;
		times100 = n*100;
		times1000 = n*1000;
	}
}