package com.flat502.rox.marshal;

public class ProxyObject implements IProxyObject {
	public String lastMethod;
	public Object[] lastParams;

	public String fooBar(String name) {
		this.lastMethod = "fooBar";
		this.lastParams = new String[] { name };
		return "Done";
	}

	public Object returnsObject(String name) {
		this.lastMethod = "returnsObject";
		this.lastParams = new String[] { name };
		return "Done";
	}
}
