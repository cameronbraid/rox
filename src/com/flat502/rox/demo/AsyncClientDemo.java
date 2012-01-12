package com.flat502.rox.demo;

import java.net.URL;

import com.flat502.rox.client.AsynchronousResponseHandler;
import com.flat502.rox.client.RpcResponseContext;
import com.flat502.rox.client.XmlRpcClient;
import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.RpcResponse;

/**
 * A demo asynchronous client illustrating the 
 * {@link com.flat502.rox.client.XmlRpcClient#execute(String, Object[], Class, AsynchronousResponseHandler)}
 * method and the {@link com.flat502.rox.client.AsynchronousResponseHandler} interface.
 */
public class AsyncClientDemo implements AsynchronousResponseHandler {
	private static boolean responseReceived = false;
	
	public void handleResponse(RpcCall call, RpcResponse response, RpcResponseContext context) {
		TimeInfo timeinfo = (TimeInfo) response.getReturnValue();
		System.out.println("getDate() returned with");
		System.out.println("   today is " + timeinfo.today);
		System.out.println("   info: " + timeinfo.info);
		
		responseReceived = true;
	}

	public void handleException(RpcCall call, Throwable e, RpcResponseContext context) {
		e.printStackTrace();
		
		responseReceived = true;
	}

	/**
	 * Call each of the exposed methods on the remote demo server.
	 * @param args
	 * 	A list of parameters. Only the first is used and if
	 * 	present must be the URL of the remote server. This 
	 * 	defaults to <code>http://localhost:8080/</code> if
	 * 	not specified.
	 */
	public static void main(String[] args) {
		try {
			String url = "http://localhost:8080/";

			if (args != null && args.length > 0) {
				url = args[0];
			}
			System.out.println("Connecting to " + url);

			XmlRpcClient client = new XmlRpcClient(new URL(url));
			client
					.execute("example.getDate", null, TimeInfo.class, new AsyncClientDemo());

			// XmlRpcClient uses daemon threads for it's background tasks.
			// This means we need to wait for the response before letting
			// the main thread terminuate otherwise the entire JVM may exit
			// before we see the response.
			while(!responseReceived) {
				Thread.yield();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
