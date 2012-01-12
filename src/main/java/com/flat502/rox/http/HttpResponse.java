package com.flat502.rox.http;

import java.io.IOException;
import java.io.OutputStream;

import com.flat502.rox.encoding.Encoding;

/**
 * This class represents an HTTP response and supports streaming
 * that request out to an indicated stream.
 */
public class HttpResponse extends HttpMessage {
	private int statusCode;
	private String reasonPhrase;
	private String rspHttpVersionString;

	/**
	 * Constructs an instance
	 */
	public HttpResponse(int statusCode, String reasonPhrase) {
		this(null, statusCode, reasonPhrase, null);
	}
	
	/**
	 * Constructs an instance
	 */
	public HttpResponse(String httpVersion, int statusCode, String reasonPhrase) {
		this(httpVersion, statusCode, reasonPhrase, null);
	}
	
	/**
	 * Constructs an instance
	 */
	public HttpResponse(int statusCode, String reasonPhrase, Encoding encoding) {
		this(null, statusCode, reasonPhrase, encoding);
	}
	
	/**
	 * Constructs an instance
	 */
	public HttpResponse(String httpVersion, int statusCode, String reasonPhrase, Encoding encoding) {
		super(encoding);
		if (reasonPhrase == null) { 
			throw new NullPointerException("reasonPhrase");
		}
		this.rspHttpVersionString = httpVersion;
		this.statusCode = statusCode;
		this.reasonPhrase = reasonPhrase;
	}

	protected String getVersionString() {
		if (this.rspHttpVersionString != null) {
			return this.rspHttpVersionString;
		}
		return super.getVersionString();
	}
	
	/**
	 * Constructs and returns the HTTP status line for
	 * this response.
	 * <p>
	 * The request line is defined in
	 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html#sec6.1">section
	 * 6.1</a> of RFC 2616 and consists of the following three
	 * values delimited by spaces:
	 * <ol>
	 * <li>The HTTP version returned by {@link HttpMessage#getVersionString()}.</li>
	 * <li>The HTTP status code set in the {@link #HttpResponse(int, String) constructor}.</li>
	 * <li>The reason phrase set in the {@link #HttpResponse(int, String) constructor}.</li>
	 * </ol>
	 */
	protected void marshalStartLine(OutputStream os) throws IOException {
		// HTTP-Version SP Status-Code SP Reason-Phrase CRLF
		os.write(this.getVersionString().getBytes());
		os.write(' ');
		os.write(String.valueOf(this.statusCode).getBytes());
		os.write(' ');
		os.write(this.reasonPhrase.getBytes());
		os.write('\r');
		os.write('\n');
	}

	public boolean mustCloseConnection() {
		if (!this.getVersionString().equals("HTTP/1.1")) {
			// Always close for earlier clients
			return true;
		}

		String connectionValue = this.getHeaderValue(HttpConstants.Headers.CONNECTION);
		if (connectionValue != null && connectionValue.equalsIgnoreCase("close")) {
			return true;
		}

		return false;
	}
}
