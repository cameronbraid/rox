package com.flat502.rox.marshal;

import java.net.URL;

import com.flat502.rox.processing.SSLSession;

/**
 * An interface representing a generalized RPC method
 * call.
 * <p>
 * This interface is patterned after XML-RPC and essentially
 * encapsulates a method name and a list of parameters.
 */
public interface RpcCall extends RpcMethod {
	/**
	 * Get the method name to invoke (or being invoked).
	 * @return
	 * 	The name of the RPC method this instance
	 * 	represents.
	 */
	String getName();

	/**
	 * Get a list of parameters to pass to (or that were
	 * passed to) this method call.
	 * <p>
	 * Implementations must not return <code>null</code>.
	 * If no parameters exist a zero length array should
	 * be returned to simplify logic that depends on this
	 * method.
	 * @return
	 * 	An array containing the parameters associated
	 * 	with this RPC method call. Never <code>null</code>.
	 */
	Object[] getParameters();

	public String getHttpMethod();
	
	/**
	 * Map the destination URL onto a URI for use in an HTTP request.
	 * <p>
	 * This method provides implementations with an opportunity
	 * to generate an appropriate URI (possibly tranformed) before
	 * the underlying HTTP request is contructed and sent.
	 * @param baseUrl 
	 * 	The URL the client making the call was	directed at.
	 * @return
	 * 	A string value for use as the URI in an HTTP request that
	 * 	will represent this call.
	 */
	public String getHttpURI(URL baseUrl);
}
