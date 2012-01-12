package com.flat502.rox.http;


public class HttpMessageException extends Exception {
	private HttpMessageBuffer msg;

	public HttpMessageException(String errMsg, HttpMessageBuffer msg) {
		super(errMsg);
		this.msg = msg;
	}
	
	public HttpMessageException(HttpMessageBuffer msg, Exception e) {
		super(e);
		this.msg = msg;
	}

	/**
	 * 
	 * @return
	 * 	The associated {@link HttpMessageBuffer} if
	 * 	this exception resulted from the processing
	 * 	of that message, or <code>null</code> if none
	 * 	existed.
	 */
	public HttpMessageBuffer getMsg() {
		return this.msg;
	}
}
