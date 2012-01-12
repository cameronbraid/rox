package com.flat502.rox.demo;

import java.net.URL;
import java.util.Arrays;

import com.flat502.rox.client.XmlRpcClient;
import com.flat502.rox.utils.Utils;

/**
 * A demo client illustrating the {@link com.flat502.rox.client.XmlRpcClient#proxyObject(String, Class)}
 * method.
 */
public class RMIClientDemo {
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
			System.out.println("Connecting to "+url);

			XmlRpcClient client = new XmlRpcClient(new URL(url));
			RMIServerInterface server = (RMIServerInterface) client.proxyObject(
					"example.", RMIServerInterface.class);

			System.out.println("Date: " + server.getDate());
			System.out.println("Version: " + server.getVersionInfo(true));
			int[] list = new int[]{1,2,3,4,5,6};
			System.out.println("Sum of " + Utils.toString(list) + ": " + server.sum(list));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
