package com.flat502.rox.http;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;


public class HttpResponseException extends HttpMessageException {
	private Map headers;
	private int statusCode;
	private String reasonPhrase;

	public HttpResponseException(int statusCode, String reasonPhrase) {
		this(statusCode, reasonPhrase, (HttpRequestBuffer)null);
	}

	public HttpResponseException(int statusCode, String reasonPhrase, HttpRequestBuffer req) {
		this(statusCode, reasonPhrase, req, (Map)null);
	}
	
	public HttpResponseException(int statusCode, String reasonPhrase, HttpRequestBuffer req, Map headers) {
		super(statusCode+": "+reasonPhrase, req);
		this.statusCode = statusCode;
		this.reasonPhrase = reasonPhrase;
		this.headers = headers;
	}
	
	public HttpResponseException(int statusCode, String reasonPhrase, Throwable e) {
		this(statusCode, reasonPhrase, null, e);
	}

	public HttpResponseException(int statusCode, String reasonPhrase, HttpRequestBuffer req, Throwable e) {
		this(statusCode, reasonPhrase);
		this.initCause(e);
	}

	public int getStatusCode() {
		return this.statusCode;
	}
	
	public String getReasonPhrase() {
		return this.reasonPhrase;
	}
	
	public HttpResponse toHttpResponse(String httpVersion) {
		HttpResponse rsp = new HttpResponse(httpVersion, this.getStatusCode(), this.getReasonPhrase());
		if (this.headers != null) {
			Iterator iter = this.headers.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				rsp.addHeader((String) entry.getKey(), (String) entry.getValue());
			}
			
			if (!this.headers.containsKey(HttpConstants.Headers.CONNECTION)) {
				rsp.addHeader(HttpConstants.Headers.CONNECTION, "close");
			}
		}
		return rsp;
	}
}
