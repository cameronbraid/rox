package com.flat502.rox.http;

/**
 * Indicates an HTTP method is not supported.
 */
public class MethodNotAllowedException extends HttpBufferException {
	private String method;
	private String[] allowed;

	public MethodNotAllowedException(String method, String[] allowed) {
		super("Method Not Allows (" + method + ")");
		this.method = method;
		this.allowed = allowed;
	}
	
	public String getMethod() {
		return this.method;
	}
	
	public String[] getAllowed() {
		return this.allowed;
	}
}
