package com.flat502.rox.http;

public class MethodNotAllowedResponseException extends HttpResponseException {
	private String allowed;

	public MethodNotAllowedResponseException(String string, String allowed) {
		super(HttpConstants.StatusCodes._405_METHOD_NOT_ALLOWED, "Method Not Allowed" + string);
		this.allowed = allowed;
	}

	public HttpResponse toHttpResponse(String httpVersion) {
		HttpResponse rsp = super.toHttpResponse(httpVersion);
		// A 405 response requires that we set the Allow header and provide a list of allowable
		// methods.
		rsp.addHeader(HttpConstants.Headers.ALLOW, this.allowed);
		return rsp;
	}
}
