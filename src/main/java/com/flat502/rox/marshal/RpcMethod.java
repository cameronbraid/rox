package com.flat502.rox.marshal;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * An interface representing an RPC method that can be marshalled to
 * an {@link java.io.OutputStream}.
 * <p>
 * This is the base interface for calls and responses.
 * @see com.flat502.rox.marshal.RpcCall
 * @see com.flat502.rox.marshal.RpcResponse
 */
public interface RpcMethod {
	/**
	 * Marshal the current instance to an {@link java.io.OutputStream} 
	 * encoded using the specified character set.
	 * <p>
	 * Implementations are free to ignore the character set
	 * but only if a character set has been agreed upon in advance
	 * by both sides. 
	 * @param out
	 * 	The {@link java.io.OutputStream} to marshal
	 * 	this instance to.
	 * @param charSet
	 * 	The character set to use when encoding
	 * 	the marshalled form of this instance. When
	 * 	this interface is invoked by an
	 * 	{@link com.flat502.rox.processing.HttpRpcProcessor}
	 * 	this is the value that will be sent as part of the
	 * 	<code>Content-Type</code> HTTP header.
	 * @throws IOException
	 * 	if an error occurs while storing the marshalled
	 * 	form of this instance out this instance.
	 * @throws MarshallingException
	 * 	if an error occurs while marshalling this instance.
	 */
	public void marshal(OutputStream out, Charset charSet) throws IOException,
			MarshallingException;
	
	/**
	 * Called to get the value for the <code>Content-Type</code>
	 * HTTP header.
	 * <p>
	 * This is used when constructing requests and responses, and
	 * when validating requests and responses.
	 * @return
	 * 	An HTTP <code>Content-Type</code> string description
	 * 	without the <code>charset</code> attribute.
	 */
	public String getContentType();
}
