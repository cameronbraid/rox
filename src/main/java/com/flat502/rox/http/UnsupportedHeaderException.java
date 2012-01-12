package com.flat502.rox.http;

/**
 * Indicates an HTTP header was received that is not supported.
 * <p>
 * This may be due to configuration, or it may be unsupported in
 * this implementation.
 */
public class UnsupportedHeaderException extends HttpBufferException {
	private String name;
	private String value;

	public UnsupportedHeaderException(String name, String value) {
		this(name, value, null);
	}

	public UnsupportedHeaderException(String name, String value, Throwable e) {
		super("The HTTP header " + name + " with a value of [" + value + "] is not supported");
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
