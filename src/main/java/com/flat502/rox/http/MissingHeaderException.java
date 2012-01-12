package com.flat502.rox.http;

/**
 * Indicates a missing HTTP header.
 */
public class MissingHeaderException extends HttpBufferException {
	private String name;

	public MissingHeaderException(String name) {
		super("Missing HTTP header: " + name);
		this.name = name;
	}
	
	public String getHeaderName() {
		return this.name;
	}
}
