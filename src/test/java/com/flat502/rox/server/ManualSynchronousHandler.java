package com.flat502.rox.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.RpcResponse;
import com.flat502.rox.marshal.xmlrpc.XmlRpcFaultException;
import com.flat502.rox.marshal.xmlrpc.XmlRpcMethodFault;
import com.flat502.rox.marshal.xmlrpc.XmlRpcMethodResponse;

public class ManualSynchronousHandler implements SynchronousRequestHandler {
	public RpcCall call;
	public List<RpcCall> calls = new ArrayList<RpcCall>();
	public RpcCallContext context;

	public RpcResponse handleRequest(RpcCall call, RpcCallContext context) throws Exception {
		this.calls.add(call);
		this.call = call;
		this.context = context;
		if (call.getName().equals("server.toUpper")) {
			return new XmlRpcMethodResponse(((String)call.getParameters()[0]).toUpperCase());
		} else if (call.getName().equals("server.delay")) {
			Thread.sleep(((Integer)call.getParameters()[0]).intValue());
			return new XmlRpcMethodResponse(call.getParameters()[1]);
		} else if (call.getName().equals("server.returnFault")) {
			return new XmlRpcMethodFault(0, "return error");
		} else if (call.getName().equals("server.raiseFault")) {
			throw new XmlRpcFaultException(0, "raise error");
		} else if (call.getName().equals("server.map")) {
			return new XmlRpcMethodResponse(((Map)call.getParameters()[0]).size());
		} else if (call.getName().equals("server.raiseNoSuchMethodException")) {
			throw new NoSuchMethodException(call.getName());
		}
		
		return null;
	}
}
