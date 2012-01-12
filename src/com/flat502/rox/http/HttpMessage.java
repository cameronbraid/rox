package com.flat502.rox.http;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.flat502.rox.encoding.Encoding;
import com.flat502.rox.utils.DateFormatThreadLocal;
import com.flat502.rox.utils.Utils;

/**
 * This class represents an abstract HTTP message and provides support for
 * various aspects of an HTTP message common to both requests and responses.
 * <P>
 * This class is not thread-safe.
 */
public abstract class HttpMessage {
	private static final DateFormatThreadLocal HTTP_DATE_FORMATS = new DateFormatThreadLocal(new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z"));

	private Map headers = new LinkedHashMap();

	private byte[] content;

	private Encoding encoding;

	/**
	 * Constructs an instance
	 * <p>
	 * This method sets the <code>Date</code> header
	 * to the current date. 
	 * @param encoding
	 * 	An {@link Encoding} describing the encoding to use when 
	 * 	constructing this message. This also informs the 
	 * 	<code>Content-Encoding</code>	header value. May be
	 * 	<code>null</code>.
	 */
	protected HttpMessage(Encoding encoding) {
		this.encoding = encoding;

		this.addHeader(HttpConstants.Headers.DATE, this.formatDate(new Date()));
		if (this.encoding != null) {
			this.addHeader(HttpConstants.Headers.CONTENT_ENCODING, encoding.getName());
		}
	}

	/**
	 * Formats a {@link Date} instance as described in
	 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1">section
	 * 3.1.1</a> of RFC 2616.
	 * <p>
	 * The actual format used is that described in RC 822 and mentioned
	 * in section 3.1.1 of RFC 2616 as the preferred format.
	 * @param date
	 * 	The date to format.
	 * @return
	 * 	A date formatted for use in an HTTP header.
	 */
	public String formatDate(Date date) {
		return HTTP_DATE_FORMATS.getFormatter().format(date);
	}

	/**
	 * Set a header value, replacing any previously set value.
	 * @param name
	 * 	The header name.
	 * @param value
	 * 	The header value.
	 * @return
	 * 	The previous value of the indicated header or <code>null</code>
	 * 	if no value was set.
	 * @see #addHeader(String, String)
	 */
	public String setHeader(String name, String value) {
		if (name == null) {
			throw new NullPointerException();
		}
		return (String) this.headers.put(this.normalizeHeaderName(name), value);
	}

	/**
	 * Add a header and value to this instance.
	 * <p>
	 * Header order is preserved. Headers are 
	 * {@link #marshal(OutputStream) marshalled} in the
	 * order in which they are added to this instance, with
	 * one exception: duplicate headers.
	 * <p>
	 * If this method is called multiple times for
	 * the same header values are folded into a single 
	 * comma-separated value as allowed for by 
	 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">section 4</a>
	 * of RFC 2616.
	 * <p>
	 * Header names are {@link #normalizeHeaderName(String) normalized}
	 * before being stored. As such, case is unimportant.
	 * <p>
	 * {@link #setHeader(String, String)} allows for the
	 * replacement of headers.
	 * @param name
	 * 	The header name.
	 * @param value
	 * 	The header value.
	 * @return
	 * 	The new header value (including previous values if the 
	 * 	header already existed).
	 * @see HttpConstants.Headers
	 * @see #setHeader(String, String)
	 * @see #normalizeHeaderName(String)
	 */
	public String addHeader(String name, String value) {
		if (name == null) {
			throw new NullPointerException();
		}
		name = this.normalizeHeaderName(name);
		String prevValue = (String) headers.get(name);
		if (prevValue != null) {
			value = prevValue + ", " + value;
		}
		headers.put(name, value);
		return value;
	}
	
	protected String getHeaderValue(String name) {
		return (String) this.headers.get(this.normalizeHeaderName(name));
	}

	/**
	 * Normalizes an HTTP header name.
	 * <p>
	 * Normalization produces the logical equivalent
	 * of the following algorithm.
	 * <p>
	 * The name is split into words using the
	 * '-' character. Each word is then normalized
	 * by converting the first character to uppercase
	 * and all remaining characters to lowercase.
	 * The resulting words are joined again using
	 * the '-' character. The result is returned. 
	 * @param name
	 * 	The header name.
	 * @return
	 * 	The normalized header name.
	 */
	protected String normalizeHeaderName(String name) {
		return Utils.normalizeHttpHeaderName(name);
	}

	/**
	 * Set the content for this HTTP message.
	 * <p>
	 * This method sets the <code>Content-Length</code>
	 * header (using {@link #setHeader(String, String)}) 
	 * to the length of <code>content</code>.
	 * @param content
	 * 	The content to include in this HTTP message.
	 */
	public void setContent(byte[] content) {
		if (content == null) {
			throw new NullPointerException();
		}
		this.content = content;
		this.setHeader(HttpConstants.Headers.CONTENT_LENGTH, String.valueOf(this.content.length));
	}

	/**
	 * Writes the HTTP message represented by this instance to
	 * a given {@link java.io.OutputStream}.
	 * @param os
	 * 	The stream to write to.
	 * @throws IOException
	 * 	If an error occurs writing to the stream or if
	 * 	any required character set cannot be loaded.
	 */
	public void marshal(OutputStream os) throws IOException {
		//		PrintStream headerStream = new PrintStream(os, false, "US-ASCII");
		//		this.marshalStartLine(headerStream);
		this.marshalStartLine(os);

		if (this.encoding != null) {
			ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
			OutputStream encodedStream = this.encoding.getEncoder(byteOs);
			encodedStream.write(this.content);
			encodedStream.close();
			byte[] encodedContent = byteOs.toByteArray();
			this.setHeader(HttpConstants.Headers.CONTENT_LENGTH, String.valueOf(encodedContent.length));

			this.marshalHeaders(os);
			os.write(encodedContent);
		} else {
			this.marshalHeaders(os);
			this.marshalContent(os);
		}
		os.flush();
	}

	/**
	 * Writes the HTTP start line for this message.
	 * <p>
	 * This implementation writes the string returned
	 * by {@link #getStartLine()} followed by a newline
	 * marker.
	 * <p>
	 * The "start line" is described in 
	 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.1">section
	 * 4.1</a> of RFC 2616.
	 * @param os
	 * 	The stream to write the start line to.
	 */
	protected abstract void marshalStartLine(OutputStream os) throws IOException;

	/**
	 * Implementation should return the HTTP start line for this
	 * message (without a newline marker).
	 * @return
	 *      The start line for this message.
	 */
	protected String getStartLine() {
		ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
		try {
			this.marshalStartLine(byteOs);
			byte[] data = byteOs.toByteArray();
			return new String(data, 0, data.length - 2, "ASCII");
		} catch (IOException e) {
			return "<<error marshalling start line: " + e.getMessage() + ">>";
		}
	}

	/**
	 * Writes all defined HTTP headers to the given stream.
	 * <p>
	 * If the <code>Content-Type</code> header has not yet
	 * been set it is set to 0 (but see {@link #setContent(byte[])} 
	 * as well).
	 * <p>
	 * Headers are rendered in the order in which they were
	 * <i>first</i> added (using either {@link #addHeader(String, String)}
	 * or {@link #setHeader(String, String)}). If an existing 
	 * header is replaced with a call to {@link #setHeader(String, String)}
	 * it's rendered order is <i>not</i> affected. 
	 * @param os
	 * 	The stream to write the start line to.
	 * @throws IOException 
	 */
	protected void marshalHeaders(OutputStream os) throws IOException {
		if (!this.isHeaderSet(HttpConstants.Headers.CONTENT_LENGTH)) {
			this.setHeader(HttpConstants.Headers.CONTENT_LENGTH, "0");
		}

		Iterator headersIter = this.headers.entrySet().iterator();
		while (headersIter.hasNext()) {
			Map.Entry entry = (Map.Entry) headersIter.next();
			String name = (String) entry.getKey();
			String value = (String) entry.getValue();
			os.write(name.getBytes());
			os.write(':');
			os.write(' ');
			os.write(value.getBytes());
			os.write('\r');
			os.write('\n');
		}

		// Terminate headers with an empty line
		os.write('\r');
		os.write('\n');
	}

	/**
	 * Writes this instance's content to the given stream.
	 * <p>
	 * If {@link #setContent(byte[])} has not been invoked
	 * this method returns without doing anything.
	 * @param contentStream
	 * 	The stream to write to.
	 * @throws IOException
	 * 	if an error occurs writing to the stream.
	 */
	protected void marshalContent(OutputStream contentStream) throws IOException {
		if (this.content != null) {
			contentStream.write(this.content);
		}
	}

	/**
	 * A convenience routine that constructs a byte array
	 * representing this instance.
	 * <p>
	 * This method is equivalent to invoking
	 * {@link #marshal(OutputStream)} to a
	 * {@link ByteArrayOutputStream} instance and
	 * retrieving the byte array.
	 * @return
	 * 	A byte array representing this instance
	 * 	"on the wire".
	 * @throws IOException
	 * 	If an error occurs writing to the stream or if
	 * 	any required character set cannot be loaded.
	 */
	public byte[] marshal() throws IOException {
		int size = 32 + this.headers.size() * 15;
		if (this.content != null) {
			size += this.content.length;
		}
		ByteArrayOutputStream byteOs = new ByteArrayOutputStream(size);
		this.marshal(byteOs);
		return byteOs.toByteArray();
	}

	/**
	 * Get the HTTP version string for this instance.
	 * @return
	 * 	Always returns <code>HTTP/1.1</code>.
	 */
	protected String getVersionString() {
		return "HTTP/1.1";
	}

	/**
	 * Check if a header has been set.
	 * <p>
	 * Case is unimportant.
	 * @param name
	 * 	The header name.
	 * @return
	 * 	<code>true</code> if the named header has been
	 * 	set.
	 */
	protected boolean isHeaderSet(String name) {
		return this.headers.containsKey(this.normalizeHeaderName(name));
	}

	/*
	 *  (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println(this.getStartLine());
		if (this.headers != null) {
			Iterator headers = this.headers.entrySet().iterator();
			while (headers.hasNext()) {
				Map.Entry entry = (Map.Entry) headers.next();
				pw.print(entry.getKey());
				pw.print(": ");
				pw.println(entry.getValue());
			}
		}
		pw.println();
		if (this.content == null) {
			pw.println("<null content>");
		} else {
			pw.println(Utils.toHexDump(this.content));
		}
		return sw.toString();
	}
}
