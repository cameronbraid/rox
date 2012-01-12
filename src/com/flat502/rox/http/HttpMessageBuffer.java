package com.flat502.rox.http;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import com.flat502.rox.processing.HttpRpcProcessor;
import com.flat502.rox.utils.Utils;

/**
 * Abstract base class for a buffer built up from one or more network
 * messages, and containing an HTTP message.
 */
public abstract class HttpMessageBuffer {
	private static final Pattern HTTP_VERSION = Pattern.compile("HTTP/(\\d+\\.\\d+)");

	private HttpRpcProcessor processor;
	private Socket socket;

	private byte[] data;
	private int offset;
	private int contentStarts;
	private Map<String, String> headers;
	private String contentType;
	private Charset contentCharset;
	private int contentLength;
	private byte[] content;
	private boolean isComplete;

	/**
	 * Construct a new buffer for the given socket.
	 * @param processor 
	 * @param socket
	 * 	The socket from which data for this buffer will be
	 * 	gathered.
	 */
	protected HttpMessageBuffer(HttpRpcProcessor processor, Socket socket) {
		this.processor = processor;
		this.socket = socket;
		this.contentStarts = -1;
	}

	/**
	 * Get the socket data for this buffer was collected from.
	 * @return
	 * 	The socket data for this buffer was collected from.
	 */
	public Socket getSocket() {
		return this.socket;
	}
	
	// TODO: Document
	public HttpRpcProcessor getOrigin() {
		return this.processor;
	}

	/**
	 * Add new data received on the socket this buffer is
	 * associated with.
	 * <p>
	 * This method gathers message fragments together.
	 * A call to this method is typically followed by a
	 * call to {@link #isComplete()} to determine whether
	 * or not the buffer contains a complete HTTP message.
	 * @param newData
	 * 	The data to add to this buffer.
	 * @param offset
	 * 	The offset within <code>newData</code> to begin
	 * 	copying from.
	 * @param count
	 * 	The number of bytes from <code>newData</code> to 
	 * 	copy.
	 * @return
	 * 	<code>-1</code> if the message is incomplete, <code>0</code> if it is complete and
	 * 	a positive integer if the message is complete and additional data remains
	 * 	in the input buffer. In the latter case the returned value is the index
	 * 	into newData at which the excess data begins.
	 * @throws Exception
	 * 	for the same reasons described under {@link #isComplete()}.
	 */
	public int addBytes(byte[] newData, int offset, int count) throws Exception {
		if (this.data == null) {
			this.data = new byte[count];
			this.offset = 0;
		} else {
			byte[] tmp = new byte[this.data.length + count];
			System.arraycopy(this.data, 0, tmp, 0, this.data.length);
			this.data = tmp;
		}
		System.arraycopy(newData, offset, this.data, this.offset, count);
		this.offset += count;
		
		if (!this.isComplete()) {
			return -1;
		}
		
		// Check if there's extra data in the buffer
		int excess = this.data.length - (this.contentStarts + this.contentLength);
		if (excess > 0) {
			// Trim our copied buffer so we don't hang onto large chunks of
			// duplicate data. This won't impact non-pipelined performance
			// since in that case we should only ever get complete (or partial)
			// messages.
			byte[] tmp = new byte[this.data.length - excess];
			System.arraycopy(this.data, 0, tmp, 0, tmp.length);
			this.data = tmp;
			return offset + count - excess;
		}
		
		return 0;
	}

	/**
	 * Test if this buffer contains a complete HTTP
	 * message.
	 * <p>
	 * Roughly speaking this test consists of three steps,
	 * ordered as follows:
	 * <ol>
	 * <li>A test if a complete set of HTTP headers are present.</li>
	 * <li>A test if the <code>Content-Length</code> header is present.</li>
	 * <li>A test if the <code>Content-Length</code> header value and the
	 * content of the HTTP message are equal.</li>
	 * </ol>
	 * @return
	 * 	<code>true</code> if this buffer holds a complete HTTP message,
	 * 	otherwise <code>false</code>.
	 * @throws Exception
	 * 	if a problem occurs applying any of the tests mention above.
	 */
	public boolean isComplete() throws Exception {
		if (this.isComplete) {
			return true;
		}

		if (this.data == null) {
			return false;
		}

		// Avoid seeking to the start of the content repeatedly
		// in the case where the content comes in in multiple packets
		if (this.contentStarts == -1) {
			// We're interested in two things:
			// 1. Do we have all the headers?
			// 2. Do we have enough body to satisfy Content-Length?
			this.contentStarts = this.seekToContentStart();
			if (this.contentStarts == -1) {
				return false;
			}

			// We have all of the headers at this point.
			// Unpack and validate.
			this.headers = this.unpackHeaders(contentStarts);
		}

		isComplete = this.isContentComplete();
		return isComplete;
	}

	/**
	 * Provides access to the headers in the underlying
	 * HTTP message.
	 * <p>
	 * This method returns an unmodifiable {@link Map} 
	 * containing the headers in the underlying HTTP 
	 * message. This map's key set iterator will return 
	 * the keys in the order in which they appeared in the
	 * request.
	 * <p>
	 * Duplicate headers will have been combined into
	 * a single value as per
	 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">section
	 * 4.2 of RFC 2616</a>.
	 * @return
	 * 	An unmodifiable {@link Map} containing the
	 * 	headers in the underlying HTTP message.
	 * @throws IllegalStateException
	 * 	if this request is not yet complete.
	 */
	public Map<String, String> getHeaders() {
		if (this.headers == null) {
			throw new IllegalStateException("This request is incomplete");
		}
		return Collections.unmodifiableMap(this.headers);
	}

	public String getHeaderValue(String name) {
		if (this.headers == null) {
			throw new IllegalStateException("This request is incomplete");
		}
		return (String) this.headers.get(this.normalizeHeaderName(name));
	}

	public byte[] getContent() {
		if (this.headers == null) {
			throw new IllegalStateException("This request is incomplete");
		}
		return this.content;
	}

	public InputStream getContentStream() throws IOException {
		return new ByteArrayInputStream(this.getContent());
	}

	public Reader getContentReader() throws IOException {
		return new InputStreamReader(this.getContentStream(), this.getContentCharset());
	}

	/**
	 * Get <code>Content-Type</code> defined in this message.
	 * @return
	 * 	The <code>Content-Type</code> or <code>null</code> if it
	 * 	is not specified.
	 */
	public String getContentType() {
		return this.contentType;
	}

	public Charset getContentCharset() {
		return this.contentCharset;
	}

	public String toString() {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
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
			try {
				if (this.getContentCharset() != null) {
					pw.println(new String(this.getContent(), this.getContentCharset().name()));
				} else {
					pw.println(new String(this.getContent()));
				}
			} catch (UnsupportedEncodingException e) {
				pw.println(Utils.toHexDump(this.getContent()));
			}
		}
		return sw.toString();
	}

	/**
	 * This is easy: just look for two CRLF pairs.
	 * 
	 * @return The index of the first byte of data after the headers or -1 if the
	 *         headers are not all present.
	 */
	private int seekToContentStart() {
		for (int i = 0; i < this.data.length; i++) {
			byte ch = this.data[i];
			if (this.data[i] == '\r') {
				if (i > this.data.length - 4) {
					return -1;
				}
				if (this.data[i + 1] == '\n' && this.data[i + 2] == '\r' && this.data[i + 3] == '\n') {
					return i + 4;
				}
			}
		}
		return -1;
	}

	private Map<String, String> unpackHeaders(int contentStarts) throws Exception {
		Map<String, String> headers = new LinkedHashMap<String, String>();
		boolean firstLine = true;
		int idx = 0;
		int lineBegins = idx;
		while (true) {
			// Seek to end of line
			while (true) {
				if (this.data[idx] == '\r' && this.data[idx + 1] == '\n') {
					if (idx + 4 < contentStarts && (this.data[idx + 2] == ' ' || this.data[idx + 2] == '\t')) {
						// Multiline header
					} else {
						break;
					}
				}
				idx++;
			}
			if (lineBegins == idx) {
				break;
			}
			String line = new String(this.data, lineBegins, idx - lineBegins, "ASCII");
			if (firstLine) {
				this.unpackPreamble(line);
				firstLine = false;
			} else {
				this.addHeader(headers, line);
			}

			lineBegins = idx + 2;
			idx += 2;
		}
		return headers;
	}

	private void addHeader(Map<String, String> headers, String line) {
		int splitIdx = line.indexOf(':');
		String name = this.normalizeHeaderName(line.substring(0, splitIdx).trim());
		String value = line.substring(splitIdx + 1).trim();

		if (headers.containsKey(name)) {
			// the spec allows us to roll these up into a single
			// key:value pair where value is all the values
			// of duplicate keys are comma separated (in
			// the order in which they occur in the request)
			String prevValue = (String) headers.get(name);
			value = prevValue + ", " + value;
		}
		headers.put(name, value);
	}

	protected abstract void unpackPreamble(String line) throws Exception;

	public abstract double getHttpVersion();

	public abstract String getHttpVersionString();

	// TODO: Document
	protected void validateHeaders() throws HttpBufferException {
	}

	/**
	 * This is more interesting. We need to find the content-length (mandatory
	 * header according to the XMLRPC spec). Then we can check if the
	 */
	private boolean isContentComplete() throws HttpBufferException {
		this.validateHeaders();

		String contentTypeValue = this.getHeaderValue(HttpConstants.Headers.CONTENT_TYPE);
		if (contentTypeValue != null) {
			this.contentType = Utils.extractContentType(contentTypeValue);
			if (this.contentType != null) {
				this.contentCharset = Utils.extractContentCharset(contentTypeValue);
			} else {
				// TODO: Should we really throw this?
				throw new InvalidHeaderException(HttpConstants.Headers.CONTENT_TYPE, contentType);
			}
		}

		String lenStr = (String) this.headers.get(this.normalizeHeaderName(HttpConstants.Headers.CONTENT_LENGTH));
		try {
			if (lenStr != null) {
				this.contentLength = Integer.parseInt(lenStr);
			}
		} catch (NumberFormatException e) {
			throw new InvalidHeaderException(HttpConstants.Headers.CONTENT_LENGTH, lenStr, e);
		}
		int receivedLen = this.data.length - this.contentStarts;
//		if (receivedLen > this.contentLength) {
//			throw new ExcessiveContentException("Content length (" + this.contentLength + ") exceeded: "
//					+ receivedLen);
//		}

		if (receivedLen < this.contentLength) {
			return false;
		}

		this.content = new byte[this.contentLength];
		System.arraycopy(this.data, this.contentStarts, this.content, 0, this.content.length);
		return true;
	}

	/**
	 * Called to normalize header names.
	 * <p>
	 * This is somewhat cosmetic in that it is used to
	 * ensure consistent access to the underlying {@link Map}
	 * of headers without producing headers that are unappealing.
	 * Most documentation uses headers like "Content-Type" and this
	 * method aims to ensure that headers are formatted using this
	 * "convention.
	 * <p>
	 * The header name is normalized as though the value were
	 * converted to lowercase split on the '-' character, the 
	 * first letter of each item in the resulting list 
	 * capitalized, and the updated list rejoined using the
	 * '-' as a separator.
	 * @param name
	 * 	The header name to normalize.
	 * @return
	 * 	The header name with the leading character of each
	 * 	"word" capitalized.
	 */
	protected String normalizeHeaderName(String name) {
		return Utils.normalizeHttpHeaderName(name);
	}
}
