package com.flat502.rox.demo;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.flat502.rox.server.XmlRpcServer;
import com.flat502.rox.utils.Utils;

/**
 * A demo server illustrating the {@link com.flat502.rox.server.ProxyingRequestHandler}
 * class.
 */
public class RMISyncServerDemo implements RMIServerInterface {
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
	 * <li>{@link RMISyncServerDemo#sum(int[]) rmi.sum}</li>
	 * <li>{@link RMISyncServerDemo#getDate() rmi.getDate}</li>
	 * <li>{@link RMISyncServerDemo#getVersionInfo(boolean) rmi.getVersion}</li>
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
			server.registerProxyingHandler(null, "^example\\.(.*)", new RMISyncServerDemo());
			server.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
