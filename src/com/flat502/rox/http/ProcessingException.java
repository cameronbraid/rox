package com.flat502.rox.http;

import java.net.Socket;

import com.flat502.rox.processing.HttpRpcProcessor;

/**
 * Encapsulates a general processing exception.
 * <p>
 * An exception that wraps up another exception that was raised
 * during processing performed in the central processing loop
 * of an {@link com.flat502.rox.processing.HttpRpcProcessor}
 * instance and that was not directly related to an HTTP message
 * instance.
 * <p>
 * A processing exception may be associated with a {@link java.net.Socket}
 * instance. If so the socket can be retrieved using the
 * {@link #getSocket()} method.
 */
public class ProcessingException extends Exception {
	private Socket socket;
	private HttpRpcProcessor processor;

	public ProcessingException(Throwable cause) {
		super(cause);
	}
	
	public ProcessingException(HttpRpcProcessor processor, Socket socket, Throwable cause) {
		super(cause);
		this.processor = processor;
		this.socket = socket;
	}
	
	/**
	 * Get the socket the processing exception is associated
	 * with.
	 * @return
	 * 	The socket associated with this exception. May be
	 * 	<code>null</code>.
	 */
	public Socket getSocket() {
		return this.socket;
	}
	
	public HttpRpcProcessor getProcessor() {
		return this.processor;
	}
}
