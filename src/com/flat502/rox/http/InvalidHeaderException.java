package com.flat502.rox.http;

/**
 * Indicates an HTTP header value is invalid.
 */
public class InvalidHeaderException extends HttpBufferException {
	private String name;
	private String value;

	public InvalidHeaderException(String name, String value) {
		this(name, value, null);
	}

	public InvalidHeaderException(String name, String value, Throwable e) {
		super("The value [" + value + "] for the HTTP header " + name
				+ " is invalid");
		if (e != null) {
			this.initCause(e);
		}
		this.name = name;
		this.value = value;
	}

	public String getHeaderName() {
		return this.name;
	}

	public String getHeaderValue() {
		return this.value;
	}
}
