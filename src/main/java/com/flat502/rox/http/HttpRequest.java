package com.flat502.rox.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;

import com.flat502.rox.encoding.Encoding;
import com.flat502.rox.encoding.EncodingMap;
import com.flat502.rox.utils.Utils;

/**
 * This class represents an HTTP request and supports streaming that request out
 * to an indicated stream.
 */
public class HttpRequest extends HttpMessage {
	private String method;
	private String uri;

	/**
	 * Constructs a new instance using a given HTTP
	 * method and URI.
	 * <p>
	 * The URI provided is normalized by a call to
	 * {@link Utils#normalizeURIPath(String)}.
	 * @param method
	 * 	The HTTP method to use in this request. 
	 * 	May not be <code>null</code>.
	 * @param uri
	 * 	The URI to include in this request. 
	 * 	May not be <code>null</code>.
	 * @param encoding
	 * 	An {@link Encoding} describing the encoding to use when 
	 * 	constructing this message. This also informs the 
	 * 	<code>Content-Encoding</code>	header value. May be
	 * 	<code>null</code>.
	 */
	public HttpRequest(String method, String uri, Encoding encoding) {
		super(encoding);
		if (method == null) { 
			throw new NullPointerException("method");
		}
		if (uri == null) { 
			throw new NullPointerException("uri");
		}
		
		this.method = method;
		this.uri = Utils.normalizeURIPath(uri);
		if (encoding != null) {
			this.addHeader(HttpConstants.Headers.ACCEPT_ENCODING, encoding.getName());
		}
	}

	/**
	 * Constructs and returns the HTTP request line for
	 * this request.
	 * <p>
	 * The request line is defined in
	 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.1">section
	 * 5.1</a> of RFC 2616 and consists of the following three
	 * values delimited by spaces:
	 * <ol>
	 * <li>The HTTP method set in the {@link #HttpRequest(String, String, Encoding) constructor}.</li>
	 * <li>The request URI set in the {@link #HttpRequest(String, String, Encoding) constructor}.</li>
	 * <li>The HTTP version returned by {@link HttpMessage#getVersionString()}.</li>
	 * </ol>
	 * @throws IOException 
	 */
	protected void marshalStartLine(OutputStream os) throws IOException {
		// Method SP Request-URI SP HTTP-Version
		os.write(this.method.getBytes());
		os.write(' ');
		os.write(this.uri.getBytes());
		os.write(' ');
		os.write(this.getVersionString().getBytes());
		os.write('\r');
		os.write('\n');
	}
}
