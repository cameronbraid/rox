package com.flat502.rox.marshal;

/**
 * An exception raised when an XML-RPC method call or response could not
 * be marshalled to, or from, a stream or string.
 */
public class MarshallingException extends Exception {

	/**
    * Constructs a new exception with <code>msg</code> as its detail message.
    * @param msg
    * 	A detail message describing the problem.
    */
	public MarshallingException(String msg) {
		super(msg);
	}

	/**
    * Constructs a new exception with <code>msg</code> as its detail message
    * and <code>e</code> as the causal exception.
    * @param msg
    * 	A detail message describing the problem.
    * @param e
    * 	The exception that resulted in this exception being raised.
    */
	public MarshallingException(String msg, Throwable e) {
		super(msg, e);
	}

	/**
    * Constructs a new exception with <code>null</code> as its detail message
    * and <code>e</code> as the causal exception.
    * @param e
    * 	The exception that resulted in this exception being raised.
    */
	public MarshallingException(Throwable e) {
		super(e);
	}
}
