package com.flat502.rox.processing;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.flat502.rox.http.HttpRequest;
import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.RpcResponse;
import com.flat502.rox.marshal.xmlrpc.SaxMethodResponseUnmarshaller;
import com.flat502.rox.marshal.xmlrpc.XmlRpcMethodResponse;
import com.flat502.rox.server.AsynchronousRequestHandler;
import com.flat502.rox.server.ManualSynchronousHandler;
import com.flat502.rox.server.ResponseChannel;
import com.flat502.rox.server.RpcCallContext;
import com.flat502.rox.server.XmlRpcServer;
import com.flat502.rox.utils.Utils;

public class Test_Pipelining extends TestCase {
	private static final String HOST = "localhost";
	private static final int PORT = 8080;

	private XmlRpcServer server;

	protected void setUp() throws Exception {
		ThreadUtils.assertZeroThreads();
		this.server = new XmlRpcServer(PORT);
	}

	protected void tearDown() throws Exception {
		this.server.stop();
		Thread.sleep(50);
		ThreadUtils.assertZeroThreads();
	}

	public void testHttp10Pipelining() throws Exception {
		String[] request = new String[] {
				"POST / HTTP/1.0",
				"Host: name",
				"Content-Type: text/xml",
				"Content-Length: 185",
				"",
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>server.toUpper</methodName>",
				"	<params>",
				"		<param>",
				"			<value><string>first call</string></value>",
				"		</param>",
				"	</params>",
				"</methodCall>POST / HTTP/1.0",
				"Host: name",
				"Content-Type: text/xml",
				"Content-Length: 188",
				"",
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>server.toUpper</methodName>",
				"	<params>",
				"		<param>",
				"			<value><string>second call</string></value>",
				"		</param>",
				"	</params>",
				"</methodCall>"};

		ManualSynchronousHandler handler = new ManualSynchronousHandler();
		server.addWorker();
		server.registerHandler(null, "^server\\.", handler);
		server.start();
		Socket socket = new Socket(HOST, PORT);
		socket.setSoTimeout(10000);

		// Behaviour here depends on timing. Either we get a single response
		// back or our socket is ripped out from under us while we're reading,
		// depending on timing on the server side.
		// Also note that because we just unpack and queue messages on the server
		// we don't guarantee which message we'll respond to if you pipe-line
		// messages using HTTP 1.0. Basically, don't do it.
		try {
			writeMessage(request, socket);
			Thread.sleep(2000);
			InputStream is = socket.getInputStream();
			List<String> rspLines = readMessage(is);
			assertEquals(7, rspLines.size());
		} catch(SocketException e) {
			// We expect this, depending on timing
		} finally {
			socket.close();
		}
	}
	
	// The HTTP spec doesn't really allow for pipelined POSTs
	// but the spirit is really not to allow pipelined mutating operations.
	// Since XMLRPC only uses POST there's no way to differentiate. Rox
	// allows this to accomodate user requirements :-)
	public void testHttpPipeliningAsync() throws Exception {
		HttpRequest req1 = constructHttpRequest(new String[] {
			"<?xml version=\"1.0\"?>",
			"<methodCall>",
			"	<methodName>server.method</methodName>",
			"	<params>",
			"		<param>",
			"			<value><string>first call</string></value>",
			"		</param>",
			"	</params>",
			"</methodCall>"});
		HttpRequest req2 = constructHttpRequest(new String[] {
			"<?xml version=\"1.0\"?>",
			"<methodCall>",
			"	<methodName>server.method</methodName>",
			"	<params>",
			"		<param>",
			"			<value><string>second call</string></value>",
			"		</param>",
			"	</params>",
			"</methodCall>"});
		
		AsyncServerHandler handler = new AsyncServerHandler();
		server.registerHandler(null, "^server\\.", handler);
		server.start();

		Socket socket = new Socket(HOST, PORT);
		socket.setSoTimeout(5000);
		try {
			OutputStream os = socket.getOutputStream();
			req1.marshal(os);
			req2.marshal(os);
			os.flush();

			// Wait for all the calls to be received
			int iters = 0;
			while(handler.calls.size() < 2) {
				Thread.sleep(100);
				assertTrue(iters++ <= 100);
			}

			// Validate what the handler saw
			assertEquals(2, handler.calls.size());
			assertEquals("server.method", handler.calls.get(0).getName());
			assertEquals("first call", handler.calls.get(0).getParameters()[0]);
			assertEquals("server.method", handler.calls.get(1).getName());
			assertEquals("second call", handler.calls.get(1).getParameters()[0]);
			
			InputStream is = socket.getInputStream();
			List<String> rspLines = new ArrayList<String>();
			while (rspLines.size() < 15) {
				rspLines.addAll(readMessage(is));
//				System.out.println(rspLines);
				Thread.sleep(100);
			}

			// Check we have two good HTTP responses
			assertNull("Handler caught an exception", handler.exception);
			String[] pair = rspLines.toString().split("HTTP/1.1 200 OK");
			assertEquals(3, pair.length); // One empty chunk up front, thanks split :-P

			// And that they're ordered correctly
			assertTrue("First response incorrect", pair[1].contains("<string>first call</string>"));
			assertTrue("Second response incorrect", pair[2].contains("<string>second call</string>"));
		} finally {
			socket.close();
		}
	}
	
	// The HTTP spec doesn't really allow for pipelined POSTs
	// but the spirit is really not to allow pipelined mutating operations.
	// Since XMLRPC only uses POST there's no way to differentiate. Rox
	// allows this to accomodate user requirements :-)
	public void testHttpPipeliningSync() throws Exception {
		HttpRequest req1 = constructHttpRequest(new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>server.delay</methodName>",
				"	<params>",
				"		<param>",
				"			<value><int>1000</int></value>",
				"		</param>",
				"		<param>",
				"			<value><string>first call</string></value>",
				"		</param>",
				"	</params>",
				"</methodCall>"});
		HttpRequest req2 = constructHttpRequest(new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>server.delay</methodName>",
				"	<params>",
				"		<param>",
				"			<value><int>100</int></value>",
				"		</param>",
				"		<param>",
				"			<value><string>second call</string></value>",
				"		</param>",
				"	</params>",
				"</methodCall>"});
		
		ManualSynchronousHandler handler = new ManualSynchronousHandler();
//		XmlRpcServer server = new XmlRpcServer(PORT);
		server.addWorker();
		server.registerHandler(null, "^server\\.", handler);
		server.start();
		Socket socket = new Socket(HOST, PORT);
		socket.setSoTimeout(10000);
		try {
			OutputStream os = socket.getOutputStream();
			req1.marshal(os);
			req2.marshal(os);
			os.flush();

			// Wait for all the calls to be received
			int iters = 0;
			while(handler.calls.size() < 2) {
				Thread.sleep(100);
				assertTrue("Timeout waiting for all responses (waited for " + (iters * 100) + "ms)", iters++ <= 100);
			}
			
			assertEquals(2, handler.calls.size());
			
			assertEquals("server.delay", handler.calls.get(0).getName());
			assertEquals("first call", handler.calls.get(0).getParameters()[1]);

			assertEquals("server.delay", handler.calls.get(1).getName());
			assertEquals("second call", handler.calls.get(1).getParameters()[1]);

			InputStream is = socket.getInputStream();
			List<String> rsp1Lines = readMessage(is);
			assertEquals("HTTP/1.1 200 OK", rsp1Lines.get(0));
			RpcResponse rsp1 = new SaxMethodResponseUnmarshaller().unmarshal(Utils.join("", rsp1Lines.toArray(new String[0])));
			assertEquals("first call", rsp1.getReturnValue());

			List<String> rsp2Lines = readMessage(is);
			assertEquals("HTTP/1.1 200 OK", rsp2Lines.get(0));
			RpcResponse rsp2 = new SaxMethodResponseUnmarshaller().unmarshal(Utils.join("", rsp2Lines.toArray(new String[0])));
			assertEquals("second call", rsp2.getReturnValue());
		} finally {
			socket.close();
//			server.stop();
//			Thread.sleep(50);
		}
	}

	private static HttpRequest constructHttpRequest(String[] content) throws UnsupportedEncodingException {
		String bodystr = Utils.join("\r\n", content);
		HttpRequest req = new HttpRequest("POST", "/", null);
		req.addHeader("Host", "name");
		req.addHeader("Content-Type", "text/xml");
		req.addHeader("Content-Length", String.valueOf(bodystr.length()));
		req.setContent(bodystr.getBytes("ASCII"));
		return req;
	}
	
	private void writeMessage(String[] request, Socket socket) throws IOException, UnsupportedEncodingException {
		OutputStream os = socket.getOutputStream();
		for (int i = 0; i < request.length; i++) {
			byte[] req = (request[i] + "\r\n").getBytes("ASCII");
			os.write(req);
		}
		os.flush();
	}

	private List<String> readMessage(InputStream is) throws Exception {
		byte[] rsp = new byte[8192];
		int numRead = is.read(rsp);
		BufferedReader rspIs = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(rsp, 0, numRead)));
		String line = null;
		List<String> rspLines = new ArrayList<String>();
		while ((line = rspIs.readLine()) != null) {
			rspLines.add(line);
		}
		return rspLines;
	}

	private class AsyncServerHandler implements AsynchronousRequestHandler {
		public List<RpcCall> calls = new ArrayList<RpcCall>();
		public Exception exception;
		private List<ResponseChannel> channels = new ArrayList<ResponseChannel>();
		private List<XmlRpcMethodResponse> rsps = new ArrayList<XmlRpcMethodResponse>();
		
		public synchronized void handleRequest(RpcCall call, RpcCallContext context, ResponseChannel rspChannel) throws Exception {
			try {
				this.calls.add(call);
				//System.out.println(System.identityHashCode(context.getHttpRequest().getSocket()));
				channels.add(0, rspChannel);
				rsps.add(0, new XmlRpcMethodResponse(call.getParameters()[0]));

				if (rsps.size() == 2) {
					// Send them back in reverse order
					for (ResponseChannel channel : channels) {
						channel.respond(rsps.remove(0));
						Thread.sleep(100);
					}
				}
			} catch (Exception e) {
				this.exception = e;
				e.printStackTrace();
				throw e;
			}
		}
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_Pipelining.class);
	}
}
