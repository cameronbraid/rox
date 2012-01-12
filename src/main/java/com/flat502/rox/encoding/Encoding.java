package com.flat502.rox.encoding;

import java.io.*;

/**
 * An interface representing an arbitrary HTTP encoding.
 * <p>
 * An implementation of this interface can be specified on the
 * {@link com.flat502.rox.client.HttpRpcClient#setContentEncoding(Encoding) client API}
 * for request encoding, or on the
 * {@link com.flat502.rox.server.HttpRpcServer#registerContentEncoding(Encoding) server API}
 * for handling encoded requests.
 * <p>
 * An implementation essentially exposes the ability to wrap an
 * {@link java.io.OutputStream} and an {@link java.io.InputStream}. These
 * are expected to encode and decode data written and read on those streams
 * respectively.
 * @see com.flat502.rox.encoding.GZipEncoding
 * @see com.flat502.rox.encoding.DeflaterEncoding
 */
public interface Encoding {
	/**
	 * Implementations should return the canonical name of this
	 * encoding.
	 * <p>
	 * This name is sent as the value in the <code>Content-Encoding</code>
	 * and <code>Accept-Encoding</code> HTTP headers where required, and
	 * is used when locating an encoding handler on the server side.
	 * @return
	 * 	The canonical name of this encoding.
	 * @see com.flat502.rox.http.HttpConstants.ContentEncoding
	 */
	String getName();

	/**
	 * Implementations should return a stream ready for decoding.
	 * @param in
	 * 	The stream from which encoded data may be read.
	 * @return
	 * 	A decoding stream.
	 * @throws IOException
	 */
	InputStream getDecoder(InputStream in) throws IOException;

	/**
	 * Implementations should return a stream ready for encoding.
	 * <p>
	 * The returned stream will be closed before the encoded data
	 * is marshalled.
	 * @param out
	 * 	The stream to which encoded data should ultimately be
	 * 	written.
	 * @return
	 * 	An encoding stream.
	 * @throws IOException
	 */
	OutputStream getEncoder(OutputStream out) throws IOException;
}
