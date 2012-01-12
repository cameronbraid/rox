package com.flat502.rox.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.util.regex.Pattern;

import com.flat502.rox.client.HttpRpcClient;
import com.flat502.rox.encoding.Encoding;

/**
 * This class represents a buffer built up from one or more network messages,
 * and containing an HTTP response.
 */
public class HttpResponseBuffer extends HttpMessageBuffer {
	private static final Pattern STATUS_LINE = Pattern
			.compile("(\\S+) (\\S+) (.+)");
	private static final Pattern STATUS_CODE = Pattern.compile("[1-5]\\d\\d");

	private double httpVersion;
	private String httpVersionString;
	private int statusCode;
	private String reasonPhrase;
	private Encoding acceptableEncoding;
	private Encoding responseEncoding;

	public HttpResponseBuffer(HttpRpcClient client, Socket socket) {
		this(client, socket, null);
	}

	// TODO: Document: encoding only used if encoding specified (otherwise identity)
	public HttpResponseBuffer(HttpRpcClient client, Socket socket, Encoding acceptableEncoding) {
		super(client, socket);
		this.acceptableEncoding = acceptableEncoding;
	}

	protected void unpackPreamble(String line) throws Exception {
		// HTTP-Version SP Status-Code SP Reason-Phrase CRLF
		try {
			int splitIdx = line.indexOf(' ');
			this.httpVersionString = line.substring(0, splitIdx);

			if (this.httpVersionString.equals("HTTP/1.1")) {
				this.httpVersion = 1.1;
			} else if (this.httpVersionString.equals("HTTP/1.0")) {
				this.httpVersion = 1.0;
			} else {
				throw new IllegalArgumentException("Unsupported HTTP version: [" + this.httpVersionString + "]");
			}

			int splitIdx2 = line.indexOf(' ', splitIdx + 1);
			String statusCodeStr = line.substring(splitIdx + 1, splitIdx2);
			if (statusCodeStr.length() != 3) {
				throw new IllegalArgumentException("Invalid status code: [" + statusCodeStr + "]");
			}
			this.statusCode = Integer.parseInt(statusCodeStr);
			
			this.reasonPhrase = line.substring(splitIdx2 + 1);
		} catch (RuntimeException e) {
			throw (IllegalArgumentException)new IllegalArgumentException("Malformed status line: [" + line + "]").initCause(e);
		}

		//		// HTTP-Version SP Status-Code SP Reason-Phrase CRLF
//		Matcher m = STATUS_LINE.matcher(line);
//		if (!m.matches()) {
//			this.getLog().error(HttpResponseBuffer.class,
//					"Malformed HTTP response status: [" + line + "]");
//			throw new IllegalArgumentException("Malformed status line: [" + line + "]");
//		}
//
//		this.httpVersion = this.parseHttpVersion(m.group(1));
//
//		String statusCodeStr = m.group(2);
//		if (!STATUS_CODE.matcher(statusCodeStr).matches()) {
//			throw new IllegalArgumentException("Invalid status code: [" + statusCodeStr + "]");
//		}
//		this.statusCode = Integer.parseInt(statusCodeStr);
//
//		this.reasonPhrase = m.group(3);
	}
	
	public double getHttpVersion() {
		return this.httpVersion;
	}
	
	public String getHttpVersionString() {
		return this.httpVersionString;
	}

	public boolean mustCloseConnection() {
		if (this.httpVersion < 1.1) {
			return true;
		}

		String connectionValue = this.getHeaderValue("Connection");
		if (connectionValue != null && connectionValue.equalsIgnoreCase("close")) {
			return true;
		}

		return false;
	}

	public String getReasonPhrase() {
		return this.reasonPhrase;
	}

	public int getStatusCode() {
		return this.statusCode;
	}
	
	public InputStream getContentStream() throws IOException {
		if (this.responseEncoding != null) {
			return this.responseEncoding.getDecoder(super.getContentStream());
		}
		return super.getContentStream();
	}

	public String toString() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println(this.httpVersion + " " + this.statusCode + " "
				+ this.reasonPhrase);
		pw.print(super.toString());
		return sw.toString();
	}

	protected void validateHeaders() throws HttpBufferException {
		super.validateHeaders();
		
		if (this.getHeaderValue(HttpConstants.Headers.CONTENT_LENGTH) == null) {
			throw new MissingHeaderException(HttpConstants.Headers.CONTENT_LENGTH);
		}

		// Check if a Content-Encoding was specified
		String contentEncoding = this.getHeaderValue(HttpConstants.Headers.CONTENT_ENCODING);
		if (contentEncoding != null && !contentEncoding.equalsIgnoreCase(HttpConstants.ContentEncoding.IDENTITY)) {
			// Check it matches the encoding we have (and that we have one).
			if (this.acceptableEncoding == null || !contentEncoding.equalsIgnoreCase(this.acceptableEncoding.getName())) {
				throw new InvalidHeaderException(HttpConstants.Headers.CONTENT_ENCODING, contentEncoding);
			}
			// An encoding was specified, isn't the identity encoding and matches
			// the request encoding (which was indicated as acceptable in the
			// Accept-Encoding header).
			this.responseEncoding = this.acceptableEncoding;
		}
	}
}
