package com.flat502.rox.marshal.xmlrpc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.Charset;

import com.flat502.rox.http.HttpConstants;
import com.flat502.rox.marshal.FieldNameEncoder;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.RpcMethod;
import com.flat502.rox.utils.XmlPlainPrinter;
import com.flat502.rox.utils.XmlPrettyPrinter;
import com.flat502.rox.utils.XmlPrinter;

/**
 * This is the base class for XML-RPC method calls and responses.
 * <p>
 * For details about the marshalling process see 
 * {@link com.flat502.rox.marshal.xmlrpc.XmlRpcMarshaller}. 
 */
public abstract class XmlRpcMethod implements RpcMethod, XmlRpcConstants {
	private XmlRpcMarshaller marshaller;

	/**
	 * Initialize a new instance of this class.
	 * @param fieldNameEncoder
	 * 	An implementation of {@link FieldNameEncoder} used when
	 * 	struct members are marshalled. May be <code>null</code>.
	 */
	protected XmlRpcMethod(FieldNameEncoder fieldNameEncoder) {
		this.marshaller = XmlRpcUtils.newMarshaller(fieldNameEncoder);
	}

	/**
	 * Configure the compactness of the marshalled form of this instance.
	 * <p>
	 * The marshalled form of instances is compact by default.
	 * 
	 * @param compact
	 *            A flag indicating whether to produce compact XML (<code>true</code>)
	 *            or more readable XML (<code>true</code>).
	 */
	public void setCompactXml(boolean compact) {
		this.marshaller.setCompactXml(compact);
	}

	public String getHttpMethod() {
		return HttpConstants.Methods.POST;
	}
	
	public String getHttpURI(URL url) {
		return url.getPath();
	}

	/**
	 * @return
	 * 	The value <code>text/xml</code>.
	 */
	public String getContentType() {
		return "text/xml";
	}

	/**
	 * Marshals the current instance into a byte array encoded using UTF-8.
	 * 
	 * @return A byte array containing the marshalled form of this instance.
	 * @throws IOException
	 *             if an error occurs while storing the marshalled form of this
	 *             instance out this instance.
	 * @throws MarshallingException
	 *             if an error occurs while marshalling this instance.
	 */
	public byte[] marshal() throws IOException, MarshallingException {
		return this.marshal(Charset.forName("UTF-8"));
	}

	/**
	 * Marshals the current instance into a byte array encoded using the
	 * specified character set.
	 * 
	 * @param charSet
	 *            The character set to use when encoding the marshalled form of
	 *            this instance.
	 * @return A byte array containing the marshalled form of this instance.
	 * @throws IOException
	 *             if an error occurs while storing the marshalled form of this
	 *             instance out this instance.
	 * @throws MarshallingException
	 *             if an error occurs while marshalling this instance.
	 */
	public byte[] marshal(Charset charSet) throws IOException, MarshallingException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		this.marshal(os, charSet);
		return os.toByteArray();
	}

	/**
	 * Marshals the current instance to an {@link java.io.OutputStream} encoded
	 * using UTF-8.
	 * 
	 * @param out
	 *            The {@link java.io.OutputStream} to marshal this instance to.
	 * @throws IOException
	 *             if an error occurs while storing the marshalled form of this
	 *             instance out this instance.
	 * @throws MarshallingException
	 *             if an error occurs while marshalling this instance.
	 */
	public void marshal(OutputStream out) throws IOException, MarshallingException {
		this.marshal(out, Charset.forName("UTF-8"));
	}

	/**
	 * Marshals the current instance to an {@link java.io.OutputStream} encoded
	 * using the specified character set.
	 * 
	 * @param out
	 *            The {@link java.io.OutputStream} to marshal this instance to.
	 * @param charSet
	 *            The character set to use when encoding the marshalled form of
	 *            this instance.
	 * @throws IOException
	 *             if an error occurs while storing the marshalled form of this
	 *             instance out this instance.
	 * @throws MarshallingException
	 *             if an error occurs while marshalling this instance.
	 */
	public void marshal(OutputStream out, Charset charSet) throws IOException, MarshallingException {
		XmlPrinter writer = this.marshaller.newXmlWriter(out, charSet);
		writer.writeHeader("1.0", charSet);
		this.marshalImpl(writer);
		writer.finishDocument();
	}

	/**
	 * The central hook for marshalling for sub-classes.
	 * <p>
	 * Implementations should write their marshalled form to the given stream
	 * which has already been initialized with an appropriate encoding.
	 * 
	 * @param out
	 *            The stream implementations should marshal themselves to.
	 * @throws IOException
	 *             Implementations may raise an exception if a problem occurs
	 *             while writing the marshalled form of this instance out.
	 * @throws MarshallingException
	 *             Implementations may raise an exception if a problem occurs
	 *             while marshalling is being attempted.
	 */
	protected abstract void marshalImpl(XmlPrinter out) throws IOException, MarshallingException;

	protected void marshalValue(XmlPrinter out, int depth, Object param) throws MarshallingException, IOException {
		this.marshaller.marshalValue(out, depth, param);
	}
}
