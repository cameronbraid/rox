package com.flat502.rox.server;

import com.flat502.rox.http.*;
import com.flat502.rox.marshal.MethodCallUnmarshallerAid;
import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.cgi.CgiMethodCallUnmarshaller;

/**
 * An {@link HttpRequestUnmarshaller} implementation that maps
 * CGI-style HTTP GET requests onto XML-RPC methods.
 * <p>
 * For specifics regarding the mapping of URIs and CGI parameters
 * see the {@link com.flat502.rox.http.MethodCallURI} and
 * {@link com.flat502.rox.marshal.cgi.CgiMethodCallUnmarshaller}
 * classes, respectively.
 */
public class CgiRequestUnmarshaller extends HttpRequestUnmarshaller {
	public RpcCall unmarshal(HttpRequestBuffer request, MethodCallUnmarshallerAid aid) throws Exception {
		this.validateRequest(request);
		return new CgiMethodCallUnmarshaller().unmarshal(new MethodCallURI(request.getURI()), aid);
	}

	/**
	 * Validate an XML-RPC HTTP request.
	 * <p>
	 * This implementation checks for the following
	 * cases:
	 * <ul>
	 * <li>The HTTP method specified is <code>GET</code>.</li>
	 * <li><code>Content-Length</code> is absent or set to zero.</li>
	 * </ul>
	 * @param httpReq
	 * 	The complete HTTP message to validate.
	 * @throws MethodNotAllowedException
	 * 	if the HTTP method is anything other than <code>POST</code>.
	 * @throws InvalidHeaderException
	 * 	if <code>Content-Length</code> is not present and set to
	 * 	zero.
	 * @throws HttpBufferException
	 * 	Implementations may throw other exceptions if 
	 * 	implementation-specific validation fails.
	 */
	protected void validateRequest(HttpRequestBuffer httpReq) throws HttpBufferException {
		if (!HttpConstants.Methods.GET.equals(httpReq.getMethod())) {
			throw new MethodNotAllowedException(httpReq.getMethod(), new String[] { HttpConstants.Methods.GET });
		}

		// This is actually already validated by HttpMessageBuffer but the
		// code is here against the day that no longer holds true.
		String lenStr = httpReq.getHeaderValue(HttpConstants.Headers.CONTENT_LENGTH);
		if (lenStr != null && !lenStr.equals("0")) {
			throw new InvalidHeaderException(HttpConstants.Headers.CONTENT_LENGTH, lenStr);
		}
	}
}
