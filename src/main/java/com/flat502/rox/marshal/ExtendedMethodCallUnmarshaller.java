package com.flat502.rox.marshal;

import java.io.InputStream;
import java.io.Reader;
import java.util.Map;

/**
 * Extends the {@link com.flat502.rox.marshal.MethodCallUnmarshaller}
 * interface with overloaded methods that default some parameters.
 */
public interface ExtendedMethodCallUnmarshaller extends MethodCallUnmarshaller {
	/**
	 * Unmarshal an {@link RpcCall} instance from an {@link InputStream}.
	 * <p>
	 * Implementations are responsible for determining the stream's
	 * character encoding (where applicable).
	 * @param in
	 * 	The {@link InputStream} from which to unmarshal a new
	 * 	{@link RpcCall} instance.
	 * @return
	 * 	A new instance of this class representing the XML structure in the
	 * 	given document.
	 * @throws Exception
	 * 	Implementations are free to throw exceptions.
	 */
	RpcCall unmarshal(InputStream in) throws Exception;

	/**
	 * Unmarshal an {@link RpcCall} instance from an {@link InputStream}.
	 * <p>
	 * Implementations are responsible for determining the stream's
	 * character encoding (where applicable).
	 * @param in
	 * 	The {@link InputStream} from which to unmarshal a new
	 * 	{@link RpcCall} instance.
	 * @param structClasses
	 * 	A list of classes to unmarshal parameters into if said parameters
	 * 	are structs. May be <code>null</code>, in which case
	 * 	{@link Map} instances will be used in all cases. Parameters are
	 * 	indexed into this array based on their position. If this index
	 * 	exceeds the length of this array, or if the indexed element is
	 * 	<code>null</code> a {@link Map} instance will be used.
	 * @return
	 * 	A new instance of this class representing the XML structure in the
	 * 	given document.
	 * @throws Exception
	 * 	Implementations are free to throw exceptions.
	 */
	RpcCall unmarshal(InputStream in, Class[] structClasses) throws Exception;

	/**
	 * Unmarshal an {@link RpcCall} instance from a {@link Reader}.
	 * @param in
	 * 	The {@link Reader} from which to unmarshal a new
	 * 	{@link RpcCall} instance.
	 * @return
	 * 	A new instance of this class representing the XML structure in the
	 * 	given document.
	 * @throws Exception
	 * 	Implementations are free to throw exceptions.
	 */
	RpcCall unmarshal(Reader in) throws Exception;

	/**
	 * Unmarshal an {@link RpcCall} instance from a {@link Reader}.
	 * @param in
	 * 	The {@link Reader} from which to unmarshal a new
	 * 	{@link RpcCall} instance.
	 * @param structClasses
	 * 	A list of classes to unmarshal parameters into if said parameters
	 * 	are structs. May be <code>null</code>, in which case
	 * 	{@link Map} instances will be used in all cases. Parameters are
	 * 	indexed into this array based on their position. If this index
	 * 	exceeds the length of this array, or if the indexed element is
	 * 	<code>null</code> a {@link Map} instance will be used.
	 * @return
	 * 	A new instance of this class representing the XML structure in the
	 * 	given document.
	 * @throws Exception
	 * 	Implementations are free to throw exceptions.
	 */
	RpcCall unmarshal(Reader in, Class[] structClasses) throws Exception;

	/**
	 * Unmarshal an {@link RpcCall} instance from an XML string.
	 * @param xml
	 * 	The XML string from which to unmarshal a new
	 * 	{@link RpcCall} instance.
	 * @return
	 * 	A new instance of this class representing the XML structure in the
	 * 	given document.
	 * @throws Exception
	 * 	Implementations are free to throw exceptions.
	 */
	RpcCall unmarshal(String xml) throws Exception;

	/**
	 * Unmarshal an {@link RpcCall} instance from an XML string.
	 * @param xml
	 * 	The XML string from which to unmarshal a new
	 * 	{@link RpcCall} instance.
	 * @param structClasses
	 * 	A list of classes to unmarshal parameters into if said parameters
	 * 	are structs. May be <code>null</code>, in which case
	 * 	{@link Map} instances will be used in all cases. Parameters are
	 * 	indexed into this array based on their position. If this index
	 * 	exceeds the length of this array, or if the indexed element is
	 * 	<code>null</code> a {@link Map} instance will be used.
	 * @return
	 * 	A new instance of this class representing the XML structure in the
	 * 	given document.
	 * @throws Exception
	 * 	Implementations are free to throw exceptions.
	 */
	RpcCall unmarshal(String xml, Class[] structClasses) throws Exception;
}
