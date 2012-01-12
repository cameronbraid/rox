package com.flat502.rox.server;

import java.nio.channels.SelectionKey;

import com.flat502.rox.http.*;
import com.flat502.rox.marshal.MethodCallUnmarshaller;
import com.flat502.rox.marshal.MethodCallUnmarshallerAid;
import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.xmlrpc.XmlRpcConstants;

public class XmlRpcRequestUnmarshaller extends HttpRequestUnmarshaller {
	private MethodCallUnmarshaller unmarshaller;

	public XmlRpcRequestUnmarshaller(MethodCallUnmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;
	}

	public RpcCall unmarshal(HttpRequestBuffer request, MethodCallUnmarshallerAid aid) throws Exception {
		this.validateRequest(request);
		return this.unmarshaller.unmarshal(request.getContentReader(), aid);
	}

	/**
	 * Validate an XML-RPC HTTP request.
	 * <p>
	 * This implementation checks for the following
	 * cases:
	 * <ul>
	 * <li>The HTTP method specified is <code>POST</code>.</li>
	 * <li><code>Content-Length</code> is present.</li>
	 * <li><code>Content-Type</code> is present and set
	 * to the value returned by {@link #getContentType()}.</li>
	 * </ul>
	 * @param httpReq
	 * 	The complete HTTP message to validate.
	 * @throws MethodNotAllowedException
	 * 	if the HTTP method is anything other than <code>POST</code>.
	 * @throws MissingHeaderException
	 * 	if <code>Content-Length</code>, <code>Content-Type</code>
	 * 	or <code>Host</code> is missing.
	 * @throws InvalidHeaderException
	 * 	if <code>Content-Type</code> is not the value
	 * 	returned by {@link #getContentType()} or if another
	 * 	header contains an invalid value.
	 * @throws HttpBufferException
	 * 	Implementations may throw other exceptions if 
	 * 	implementation-specific validation fails.
	 */
	protected void validateRequest(HttpRequestBuffer httpReq) throws HttpBufferException {
		if (!HttpConstants.Methods.POST.equals(httpReq.getMethod())) {
			throw new MethodNotAllowedException(httpReq.getMethod(), new String[]{HttpConstants.Methods.POST});
		}
		
		// This is actually already validated by HttpMessageBuffer but the
		// code is here against the day that no longer holds true.
		String lenStr = httpReq.getHeaderValue(HttpConstants.Headers.CONTENT_LENGTH);
		if (lenStr == null) {
			throw new MissingHeaderException(HttpConstants.Headers.CONTENT_LENGTH);
		}

		if (httpReq.getContentType() == null) {
			throw new MissingHeaderException(HttpConstants.Headers.CONTENT_TYPE);
		}

		if (!httpReq.getContentType().equalsIgnoreCase(this.getContentType())) {
			throw new InvalidHeaderException(HttpConstants.Headers.CONTENT_TYPE, httpReq.getContentType());
		}
	}

	/**
	 * @return
	 * 	The value <code>text/xml</code>.
	 */
	protected String getContentType() {
		return XmlRpcConstants.ContentTypes.TEXT_XML;
	}
}
