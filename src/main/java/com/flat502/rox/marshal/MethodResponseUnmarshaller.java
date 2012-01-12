package com.flat502.rox.marshal;

import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

/**
 * Encapsulates the methods required to unmarshal an
 * {@link com.flat502.rox.marshal.RpcResponse} from various sources.
 */
public interface MethodResponseUnmarshaller extends MethodUnmarshaller {
	/**
	 * Unmarshal an {@link RpcResponse} instance from an {@link InputStream}.
	 * <p>
	 * Implementations are responsible for determining the stream's
	 * character encoding (where applicable).
	 * @param in
	 * 	The {@link InputStream} from which to unmarshal a new
	 * 	{@link RpcResponse} instance.
	 * @param aid
	 * 	The {@link MethodResponseUnmarshallerAid} to use when unmarshalling
	 * 	the response. May be <code>null</code>, in which internal defaults 
	 * 	will be used
	 * @return
	 * 	A new instance of this class representing the XML structure in the
	 * 	given document.
	 * @throws Exception
	 * 	Implementations are free to throw exceptions.
	 */
	RpcResponse unmarshal(InputStream in, MethodResponseUnmarshallerAid aid)
			throws Exception;

	/**
	 * Unmarshal an {@link RpcResponse} instance from a {@link Reader}.
	 * @param in
	 * 	The {@link Reader} from which to unmarshal a new
	 * 	{@link RpcResponse} instance.
	 * @param aid
	 * 	The {@link MethodResponseUnmarshallerAid} to use when unmarshalling
	 * 	the response. May be <code>null</code>, in which internal defaults 
	 * 	will be used
	 * @return
	 * 	A new instance of this class representing the XML structure in the
	 * 	given document.
	 * @throws Exception
	 * 	Implementations are free to throw exceptions.
	 */
	RpcResponse unmarshal(Reader in, MethodResponseUnmarshallerAid aid)
			throws Exception;

	/**
	 * Unmarshal an {@link RpcResponse} instance from an XML string.
	 * @param xml
	 * 	The XML string from which to unmarshal a new
	 * 	{@link RpcResponse} instance.
	 * @param aid
	 * 	The {@link MethodResponseUnmarshallerAid} to use when unmarshalling
	 * 	the response. May be <code>null</code>, in which internal defaults 
	 * 	will be used
	 * @return
	 * 	A new instance of this class representing the XML structure in the
	 * 	given document.
	 * @throws Exception
	 * 	Implementations are free to throw exceptions.
	 */
	RpcResponse unmarshal(String xml, MethodResponseUnmarshallerAid aid)
			throws Exception;
}
