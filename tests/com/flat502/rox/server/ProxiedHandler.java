package com.flat502.rox.server;

import java.util.Date;

import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.RpcResponse;
import com.flat502.rox.marshal.xmlrpc.XmlRpcMethodResponse;

public class ProxiedHandler {
	public String toUpper(String s) {
		return s.toUpperCase();
	}
	
	public String noArgs() {
		return "NO ARGS";
	}
	
	public String plentyOfArgs(int anInt, double aDouble, String aString, Date aDate) {
		return "PLENTY OF ARGS";
	}
	
	public String customTypeArg(CustomType arg) {
		return "CUSTOM TYPE ARG";
	}
}
