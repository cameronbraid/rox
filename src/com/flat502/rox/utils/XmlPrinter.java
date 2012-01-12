package com.flat502.rox.utils;

import java.io.IOException;
import java.nio.charset.Charset;

import com.flat502.rox.marshal.MarshallingException;

/**
 * A simple interface encapsulating methods for "printing" an
 * XML document.
 * <p>
 * This interface makes no attempt to cater for attributes or
 * anything more advanced than is required for XML-RPC.
 * <p>
 * Implementations will typically be backed by a stream of some
 * sort.
 * <p>
 * The form the output takes is entirely at the discretion of
 * an implementation, as long as it is logically equivalent to
 * an XML document that would be produced if each method 
 * immediately output the relevant string literal.
 */
public interface XmlPrinter {
	/**
	 * Invoked as the first method on an instance of
	 * this interface.
	 * <p>
	 * Implementations should produce an appropriate
	 * XML header, ideally including the information provided.
	 * <p>
	 * Implementations are not required to act on this call
	 * until {@link #finishDocument()} is invoked.
	 * @param version
	 * 	The XML version this document should comply with.
	 * @param charSet
	 * 	The character encoding this document is being
	 * 	encoded with.
	 */
	public void writeHeader(String version, Charset charSet) throws IOException;
	
	/**
	 * Invoked when a tag is opened.
	 * <p>
	 * Implementations are not required to act on this call
	 * until {@link #finishDocument()} is invoked.
	 * @param name
	 * 	The name of the tag being opened.
	 */
	public void openTag(String name) throws IOException;

	/**
	 * Invoked when a value is being output.
	 * <p>
	 * Implementations are not required to act on this call
	 * until {@link #finishDocument()} is invoked.
	 * @param value
	 * 	The value associated with the most recently
	 * 	opened tag.
	 */
	public void writeValue(String value) throws IOException;

	/**
	 * Invoked when a tag is closed.
	 * <p>
	 * Implementations are not required to act on this call
	 * until {@link #finishDocument()} is invoked.
	 * @param name
	 * 	The name of the tag being closed.
	 */
	public void closeTag(String name) throws IOException;
	
	/**
	 * Invoked as the final call to an instance of this
	 * interface.
	 * <p>
	 * This gives implementations a chance to flush any cached
	 * information.
	 */
	public void finishDocument() throws IOException;
}
