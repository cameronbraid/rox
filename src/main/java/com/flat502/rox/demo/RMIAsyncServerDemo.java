package com.flat502.rox.demo;

import java.net.InetAddress;
import java.util.Date;

import com.flat502.rox.marshal.FieldNameCodec;
import com.flat502.rox.marshal.MethodCallUnmarshallerAid;
import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.server.*;
import com.flat502.rox.utils.Utils;

/**
 * A demo server illustrating the {@link com.flat502.rox.server.ProxyingRequestHandler}
 * class.
 */
public class RMIAsyncServerDemo extends MethodCallUnmarshallerAid implements AsynchronousRequestHandler,
		RMIServerInterface {
	private static final String NAME_PATTERN = "^example\\.(.*)";
	private XmlRpcMethodProxy proxy;
	
	public RMIAsyncServerDemo() {
		// Create a proxy for the object that we actually want
		// to handle XML-RPC methods. This does the mapping
		// between XML-RPC metho calls and Java method calls.
		this.proxy = new XmlRpcMethodProxy(NAME_PATTERN, this);
	}
	
	public void handleRequest(RpcCall call, RpcCallContext context, ResponseChannel rspChannel) throws Exception {
		// A deferred method call can be handed off to an application thread ...
		DeferredMethodCall defCall = new DeferredMethodCall(this.proxy, call, rspChannel);
		
		Object[] params = call.getParameters();
		System.out.println("Method [" + call.getName() + "] called with "
				+ params.length + " parameters");
		for (int i = 0; i < params.length; i++) {
			System.out.println("   Param " + (i + 1) + " [" + params[i] + "]");
		}

		// ... and executed like this.
		defCall.invoke();
	}

	public Class getType(String methodName, int index) {
		return this.proxy.getType(methodName, index);
	}

	public FieldNameCodec getFieldNameCodec(String methodName) {
		// The default codec is fine.
		return null;
	}
	
	/**
	 * Sums an array of integers;
	 * @param list
	 * 	The list of integers
	 * @return
	 * 	The sum of the input list of values.
	 */
	public int sum(int[] list) {
		System.out.print("sum(" + Utils.toString(list) + ") invoked ... ");
		int total = 0;
		for (int i = 0; i < list.length; i++) {
			total += list[i];
		}
		System.out.println("returning " + total);
		return total;
	}

	/**
	 * Fetch a version string.
	 * @param verbose
	 * 	A flag indicating whether or not the returned
	 * 	version info is verbose.
	 * @return
	 * 	A version string
	 */
	public String getVersionInfo(boolean verbose) {
		System.out.print("getVersionInfo(" + verbose + ") invoked ... ");
		String version = "1.0";
		if (verbose) {
			version = "Version " + version;
		}
		System.out.println("returning [" + version + "]");
		return version;
	}

	/**
	 * Get the current date and time.
	 * @return
	 * 	A new {@link Date} instance set to
	 * 	the current date and time.
	 */
	public Date getDate() {
		System.out.print("getDate() invoked ... ");
		Date today = new Date();
		System.out.println("returning " + today);
		return today;
	}

	/**
	 * Start an instance of this demo server.
	 * <p>
	 * The following XML-RPC methods are supported by
	 * this server:
	 * <ul>
	 * <li>{@link RMIAsyncServerDemo#sum(int[]) rmi.sum}</li>
	 * <li>{@link RMIAsyncServerDemo#getDate() rmi.getDate}</li>
	 * <li>{@link RMIAsyncServerDemo#getVersionInfo(boolean) rmi.getVersion}</li>
	 * </ul>
	 * @param args
	 * 	A list of parameters indicating
	 * 	the <code>host/address</code> and
	 * 	<code>port</code> to bind to. These default to 
	 * 	<code>localhost</code> and <code>8080</code> if
	 * 	not specified.
	 */
	public static void main(String[] args) {
		try {
			String host = "localhost";
			int port = 8080;

			if (args != null && args.length > 0) {
				host = args[0];
				if (args.length > 1) {
					port = Integer.parseInt(args[1]);
				}
			}
			System.out.println("Starting server on " + host + ":" + port);

			XmlRpcServer server = new XmlRpcServer(InetAddress.getByName(host), port);
			RMIAsyncServerDemo handler = new RMIAsyncServerDemo();
			server.registerHandler(null, NAME_PATTERN, handler, handler);
			server.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
