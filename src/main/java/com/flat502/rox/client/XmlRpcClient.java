package com.flat502.rox.client;

import java.io.IOException;
import java.net.URL;

import com.flat502.rox.marshal.FieldNameCodec;
import com.flat502.rox.marshal.MethodResponseUnmarshaller;
import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.xmlrpc.DomMethodResponseUnmarshaller;
import com.flat502.rox.marshal.xmlrpc.SaxMethodResponseUnmarshaller;
import com.flat502.rox.marshal.xmlrpc.XmlRpcMethodCall;
import com.flat502.rox.processing.SSLConfiguration;

/**
 * This is the client-side XML-RPC interface.
 * <p>
 * This is a specialization of the
 * {@link com.flat502.rox.client.HttpRpcClient}.
 * <p>
 * Typical synchronous usage of this class is illustrated
 * in the following sample:
 * <pre>
 * XmlRpcClient client = new XmlRpcClient(new URL("http://localhost:8080/"));
 * Object rsp = client.execute("math.increment", new Object[] { new Integer(42) });
 * </pre>
 * <p>
 * Typical asynchronous usage of this class is illustrated
 * in the following sample:
 * <pre>
 * public class OnceAsyncClient implements ResponseHandler {
 * 	public static boolean done = false;
 * 
 * 	public void handleResponse(XMLRPCMethodCall call, XMLRPCMethodResponse response) {
 * 		System.out.println(response.getReturnValue());
 * 		done = true;
 * 	}
 *
 * 	public void handleException(XMLRPCMethodCall call, Exception e) {
 * 		e.printStackTrace();
 * 		done = true;
 * 	}
 *
 * 	public static void main(String[] args) {
 * 		try {
 * 			XMLRPCClient client = new XMLRPCClient(new URL("http://localhost:8080/"));
 * 			client.execute("math.increment", new Object[] { new Integer(42) }, new OnceAsyncClient());
 * 			while(!done) {
 * 				Thread.yield();
 * 			}
 * 		} catch (Exception e) {
 * 			e.printStackTrace();
 * 		}
 * 	}
 * }
 * </pre>
 */
public class XmlRpcClient extends HttpRpcClient {
	private MethodResponseUnmarshaller unmarshaller;
	private boolean marshalCompactXml;
	private FieldNameCodec fieldNameCodec;

	public XmlRpcClient(URL url) throws IOException {
		this(url, null, null);
	}
	
	public XmlRpcClient(URL url, SSLConfiguration sslCfg) throws IOException {
		this(url, null, sslCfg);
	}
	
	public XmlRpcClient(URL url, ClientResourcePool workerPool) throws IOException {
		this(url, workerPool, null);
	}
	
	public XmlRpcClient(URL url, ClientResourcePool workerPool, SSLConfiguration sslCfg) throws IOException {
		super(url, workerPool, sslCfg);
		this.marshalCompactXml = true;
//		this.setUnmarshaller(new DomMethodResponseUnmarshaller());
		this.setUnmarshaller(new SaxMethodResponseUnmarshaller());
	}

	protected RpcCall newRpcCall(String name, Object[] params) {
		XmlRpcMethodCall call = new XmlRpcMethodCall(name, params, this.selectCodec());
		call.setCompactXml(this.marshalCompactXml);
		return call;
	}

	/**
	 * Configure the compactness of the marshalled form of this instance.
	 * <p>
	 * The marshalled form of instances is compact by default.
	 * 
	 * @param compact
	 *            A flag indicating whether to produce compact XML (<code>true</code>)
	 *            or more readable XML (<code>false</code>).
	 */
	public void setCompactXml(boolean compact) {
		this.marshalCompactXml = compact;
	}

	/**
	 * Configure the {@link MethodResponseUnmarshaller} instance to
	 * use when unmarshalling incoming XML-RPC responses.
	 * @param unmarshaller
	 * 	The new unmarshaller instance to use.
	 * @see DomMethodResponseUnmarshaller
	 * TODO: Link to Sax unmarshaller
	 */
	public void setUnmarshaller(MethodResponseUnmarshaller unmarshaller) {
		if (unmarshaller == null) {
			throw new NullPointerException("null unmarshaller");
		}
		this.unmarshaller = unmarshaller;
	}

	// TODO: Document, param may be null
	public void setFieldNameCodec(FieldNameCodec codec) {
		this.fieldNameCodec = codec;
	}

	protected MethodResponseUnmarshaller getMethodResponseUnmarshaller() {
		return unmarshaller;
	}

	// TODO: protected?
	private FieldNameCodec selectCodec() {
		if (this.fieldNameCodec == null) {
			return this.unmarshaller.getDefaultFieldNameCodec();
		}
		return this.fieldNameCodec;
	}
}
