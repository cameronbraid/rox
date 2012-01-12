package com.flat502.rox.http;

/**
 * Raised when the length of the content received exceeds the length
 * indicated by the <code>Content-Length</code> header.
 */
public class ExcessiveContentException extends HttpBufferException {
	public ExcessiveContentException(String msg) {
		super(msg);
	}
}
