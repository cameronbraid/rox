package com.flat502.rox.marshal.cgi;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;

import com.flat502.rox.http.HttpConstants;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.RpcCall;

public class CgiRpcMethodCall implements RpcCall {
	private String methodName;
	private Object[] parameters;

	public CgiRpcMethodCall(String methodName, Object[] parameters) {
		this.methodName = methodName;
		this.parameters = parameters;
		
	}

	public String getName() {
		return this.methodName;
	}

	public Object[] getParameters() {
		return this.parameters;
	}

	public String getHttpMethod() {
		return HttpConstants.Methods.GET;
	}

	public String getHttpURI(URL url) {
		return url.getPath();
	}

	public String getContentType() {
		return "text/plain";
	}

	public void marshal(OutputStream out, Charset charSet) throws IOException, MarshallingException {
		// Empty body
	}
}
