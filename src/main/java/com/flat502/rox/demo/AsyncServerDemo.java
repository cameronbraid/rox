package com.flat502.rox.demo;

import java.net.InetAddress;

import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.xmlrpc.XmlRpcMethodResponse;
import com.flat502.rox.server.AsynchronousRequestHandler;
import com.flat502.rox.server.ResponseChannel;
import com.flat502.rox.server.RpcCallContext;
import com.flat502.rox.server.XmlRpcServer;

/**
 * A demo asynchronous server illustrating the {@link com.flat502.rox.server.AsynchronousRequestHandler}
 * interface.
 */
public class AsyncServerDemo implements AsynchronousRequestHandler {
	public void handleRequest(RpcCall call, RpcCallContext context, ResponseChannel rspChannel) throws Exception {
		Object[] params = call.getParameters();
		System.out.println("Method [" + call.getName() + "] called with "
				+ params.length + " parameters");
		for (int i = 0; i < params.length; i++) {
			System.out.println("   Param " + (i + 1) + " [" + params[i] + "]");
		}

		// This could just as easily be done from another thread.
		rspChannel.respond(new XmlRpcMethodResponse(new TimeInfo()));
	}

	/**
	 * Start an instance of this demo server.
	 * <p>
	 * The following XML-RPC methods are supported by
	 * this server:
	 * <ul>
	 * <li>{@link RMIServerInterface#sum(int[]) rmi.sum}</li>
	 * <li>{@link RMIServerInterface#getDate() rmi.getDate}</li>
	 * <li>{@link RMIServerInterface#getVersionInfo(boolean) rmi.getVersion}</li>
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
			server.registerHandler(null, "^example\\.", new AsyncServerDemo());
			server.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
