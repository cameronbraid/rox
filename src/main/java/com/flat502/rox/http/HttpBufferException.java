package com.flat502.rox.http;

/**
 * Indicates a problem during unpacking and validating of
 * an {@link com.flat502.rox.http.HttpMessageBuffer} instance.
 */
public class HttpBufferException extends Exception {
	public HttpBufferException(String msg) {
		super(msg);
	}

	public HttpBufferException(String msg, Throwable e) {
		super(msg, e);
	}
}
