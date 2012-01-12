package com.flat502.rox.marshal;

import java.io.InputStream;
import java.io.Reader;


/**
 * Encapsulates the methods required to unmarshal an
 * {@link com.flat502.rox.marshal.RpcCall} from various sources.
 */
public interface MethodCallUnmarshaller extends MethodUnmarshaller {
	/**
	 * Unmarshal an {@link RpcCall} instance from an {@link InputStream}.
	 * <p>
	 * Implementations are responsible for determining the stream's
	 * character encoding (where applicable).
	 * @param in
	 * 	The {@link InputStream} from which to unmarshal a new
	 * 	{@link RpcCall} instance.
	 * @param aid
	 * 	A {@link MethodCallUnmarshallerAid} providing the mapping between
	 * 	method parameters when unmarshalling structs. May be 
	 * 	<code>null</code>, in which case {@link java.util.Map} instances will 
	 * 	be used in all cases. May also override the default
	 * 	{@link FieldNameCodec}.
	 * @return
	 * 	A new instance of this class representing the XML structure in the
	 * 	given document.
	 * @throws Exception
	 * 	Implementations are free to throw exceptions.
	 */
	RpcCall unmarshal(InputStream in, MethodCallUnmarshallerAid aid)
			throws Exception;

	/**
	 * Unmarshal an {@link RpcCall} instance from a {@link Reader}.
	 * @param in
	 * 	The {@link Reader} from which to unmarshal a new
	 * 	{@link RpcCall} instance.
	 * @param aid
	 * 	A {@link MethodCallUnmarshallerAid} providing the mapping between
	 * 	method parameters when unmarshalling structs. May be 
	 * 	<code>null</code>, in which case {@link java.util.Map} instances will 
	 * 	be used in all cases. May also override the default
	 * 	{@link FieldNameCodec}.
	 * @return
	 * 	A new instance of this class representing the XML structure in the
	 * 	given document.
	 * @throws Exception
	 * 	Implementations are free to throw exceptions.
	 */
	RpcCall unmarshal(Reader in, MethodCallUnmarshallerAid aid)
			throws Exception;

	/**
	 * Unmarshal an {@link RpcCall} instance from an XML string.
	 * @param xml
	 * 	The XML string from which to unmarshal a new
	 * 	{@link RpcCall} instance.
	 * @param aid
	 * 	A {@link MethodCallUnmarshallerAid} providing the mapping between
	 * 	method parameters when unmarshalling structs. May be 
	 * 	<code>null</code>, in which case {@link java.util.Map} instances will 
	 * 	be used in all cases. May also override the default
	 * 	{@link FieldNameCodec}.
	 * @return
	 * 	A new instance of this class representing the XML structure in the
	 * 	given document.
	 * @throws Exception
	 * 	Implementations are free to throw exceptions.
	 */
	RpcCall unmarshal(String xml, MethodCallUnmarshallerAid aid)
			throws Exception;
}
