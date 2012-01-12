package com.flat502.rox.demo.validation;

import java.util.Date;
import java.util.Map;

public interface IValidationSuite {
	public int arrayOfStructsTest(MoeLarryAndCurly[] list);

	public EntityInfo countTheEntities(String str);

	public int easyStructTest(MoeLarryAndCurly struct);

	public Map echoStructTest(Map struct);

	public Object[] manyTypesTest(Integer n, Boolean b, String s, Double d,
			Date dt, byte[] b64);

	public String moderateSizeArrayCheck(String[] list);

	public int nestedStructTest(Map calendar);

	public MultipliedStruct simpleStructReturnTest(int n);
}
