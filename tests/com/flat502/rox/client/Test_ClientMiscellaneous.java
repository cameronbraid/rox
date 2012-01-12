package com.flat502.rox.client;

import java.net.URL;

import junit.framework.TestCase;

import com.flat502.rox.log.Level;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.log.SimpleLogFactory;
import com.flat502.rox.log.StreamLog;
import com.flat502.rox.marshal.MarshallingException;

public class Test_ClientMiscellaneous extends TestCase {
	private static final String PREFIX = "test";
	private static final String HOST = "localhost";
	private static final int PORT = 8080;
	private static final String URL = "http://" + HOST + ":" + PORT + "/";

//	static {
//		LogFactory.configure(new SimpleLogFactory(new StreamLog(System.out, Level.TRACE)));
//	}
	
	public void testEmptyResponse() throws Exception {
		String[] rsp = new String[] {
				"HTTP/1.1 200 OK",
				"Server: foo",
				"Content-Type: text/xml",
				"Content-Length: 0",
				"",
				"" };
		StringBuffer sb = new StringBuffer(rsp[0]);
		for (int i = 1; i < rsp.length; i++) {
			sb.append("\r\n");
			sb.append(rsp[i]);
		}
		byte[] rspData = sb.toString().getBytes("ASCII");

		// Accept, read and respond
		DumbServer server = new DumbServer(PORT, true, rspData);
		XmlRpcClient client = new XmlRpcClient(new URL(URL));
		try {
			client.execute("test.waitFor", new Object[]{1000});
			fail();
		} catch (MarshallingException e) {
		} finally {
			client.stop();
			server.shutdown();
		}
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_ClientMiscellaneous.class);
	}
}
