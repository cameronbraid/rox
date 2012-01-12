package com.flat502.rox.marshal;

import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

/**
 * Extends the {@link com.flat502.rox.marshal.MethodResponseUnmarshaller}
 * interface with overloaded methods that default some parameters.
 */
public interface ExtendedMethodResponseUnmarshaller extends MethodResponseUnmarshaller {
	/**
	 * Unmarshal an {@link RpcResponse} instance from an {@link InputStream}.
	 * <p>
	 * Implementations are responsible for determining the stream's
	 * character encoding (where applicable).
	 * @param in
	 * 	The {@link InputStream} from which to unmarshal a new
	 * 	{@link RpcResponse} instance.
	 * @return
	 * 	A new instance of this class representing the XML structure in the
	 * 	given document.
	 * @throws Exception
	 * 	Implementations are free to throw exceptions.
	 */
	RpcResponse unmarshal(InputStream in) throws Exception;

	/**
	 * Unmarshal an {@link RpcResponse} instance from a {@link Reader}.
	 * @param in
	 * 	The {@link Reader} from which to unmarshal a new
	 * 	{@link RpcResponse} instance.
	 * @return
	 * 	A new instance of this class representing the XML structure in the
	 * 	given document.
	 * @throws Exception
	 * 	Implementations are free to throw exceptions.
	 */
	RpcResponse unmarshal(Reader in) throws Exception;

	/**
	 * Unmarshal an {@link RpcResponse} instance from an XML string.
	 * @param xml
	 * 	The XML string from which to unmarshal a new
	 * 	{@link RpcResponse} instance.
	 * @return
	 * 	A new instance of this class representing the XML structure in the
	 * 	given document.
	 * @throws Exception
	 * 	Implementations are free to throw exceptions.
	 */
	RpcResponse unmarshal(String xml) throws Exception;
}
