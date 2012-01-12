package com.flat502.rox.server;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import junit.framework.TestCase;

public class Test_ServerMiscellaneous extends TestCase {
	private static final String HOST = "localhost";
	private static final int PORT = 8080;
	private static final String URL = "http://" + HOST + ":" + PORT + "/";

	private XmlRpcServer server;

	protected void setUp() throws Exception {
		this.server = new XmlRpcServer(PORT);
	}

	protected void tearDown() throws Exception {
		this.server.stop();
		Thread.sleep(50);
	}

	public void testDefaultGET() throws Exception {
		ManualSynchronousHandler handler = new ManualSynchronousHandler();
		server.registerHandler(null, "^server\\.", handler);
		server.start();
		URLConnection conn = new URL(URL + "server.toUpper?hello+world").openConnection();
		InputStream is = null;
		try {
			conn.connect();
			is = conn.getInputStream();
		} catch (IOException e) {
			assertTrue(e.getMessage().matches(".*HTTP response code: 405.*"));
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	public void testUnsupportedHttpMethod() throws Exception {
		ManualSynchronousHandler handler = new ManualSynchronousHandler();
		server.registerHandler(null, "^server\\.", handler);
		server.start();
		HttpURLConnection conn = (HttpURLConnection) new URL(URL + "server.toUpper?hello+world").openConnection();
		InputStream is = null;
		try {
			conn.setRequestMethod("DELETE");
			conn.connect();
			is = conn.getInputStream();
		} catch (IOException e) {
			assertTrue(e.getMessage().matches(".*HTTP response code: 501.*"));
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}

	public void testHttp10ClientIsDisconnectedAfterCall() throws Exception {
		String[] request = new String[] {
				"POST / HTTP/1.0",
				"Content-Type: text/xml",
				"Content-Length: 188",
				"",
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>server.toUpper</methodName>",
				"	<params>",
				"		<param>",
				"			<value><string>hello world</string></value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };

		ManualSynchronousHandler handler = new ManualSynchronousHandler();
		server.registerHandler(null, "^server\\.", handler);
		server.start();
		Socket socket = new Socket(HOST, PORT);
		socket.setSoTimeout(3000);
		try {
			writeMessage(request, socket);
			InputStream is = socket.getInputStream();
			List<String> rspLines = readMessage(is);
			assertEquals(7, rspLines.size());
			assertEquals("HTTP/1.0 200 OK", rspLines.get(0));
			assertTrue(Pattern.compile("^Date: ").matcher((String) rspLines.get(1)).find());
			assertTrue(Pattern.compile("^Server: ").matcher((String) rspLines.get(2)).find());
			assertTrue(Pattern.compile("^Content-Type: text/xml").matcher((String) rspLines.get(3)).find());
			assertTrue(Pattern.compile("^Content-Length: 146").matcher((String) rspLines.get(4)).find());
			assertEquals("", rspLines.get(5));

			// Make sure we get disconnected immediately.
			int nread = is.read();
			assertEquals(-1, nread);
		} finally {
			socket.close();
		}
	}

	public void testHttp11ClientIsDisconnectedAfterIdleTimeout() throws Exception {
		String[] request = new String[] {
				"POST / HTTP/1.1",
				"Host: foo",
				"Content-Type: text/xml",
				"Content-Length: 188",
				"",
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>server.toUpper</methodName>",
				"	<params>",
				"		<param>",
				"			<value><string>hello world</string></value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };

		ManualSynchronousHandler handler = new ManualSynchronousHandler();
		server.registerHandler(null, "^server\\.", handler);
		server.setIdleClientTimeout(1000);
		server.start();
		Socket socket = new Socket(HOST, PORT);
		socket.setSoTimeout(3000);
		try {
			writeMessage(request, socket);
			InputStream is = socket.getInputStream();
			List<String> rspLines = readMessage(is);
			assertEquals(8, rspLines.size());
			assertEquals("HTTP/1.1 200 OK", rspLines.get(0));
			assertTrue(Pattern.compile("^Date: ").matcher((String) rspLines.get(1)).find());
			assertTrue(Pattern.compile("^Host: ").matcher((String) rspLines.get(2)).find());
			assertTrue(Pattern.compile("^Server: ").matcher((String) rspLines.get(3)).find());
			assertTrue(Pattern.compile("^Content-Type: text/xml").matcher((String) rspLines.get(4)).find());
			assertTrue(Pattern.compile("^Content-Length: ").matcher((String) rspLines.get(5)).find());
			assertEquals("", rspLines.get(6));

			// Make sure we get disconnected immediately.
			int nread = is.read();
			assertEquals(-1, nread);
		} finally {
			socket.close();
		}
	}

	public void testHttp11ClientIsNotDisconnectedPrematurelyByIdleTimeout() throws Exception {
		String[] request = new String[] {
				"POST / HTTP/1.1",
				"Host: foo",
				"Content-Type: text/xml",
				"Content-Length: 188",
				"",
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>server.toUpper</methodName>",
				"	<params>",
				"		<param>",
				"			<value><string>hello world</string></value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };

		ManualSynchronousHandler handler = new ManualSynchronousHandler();
		server.registerHandler(null, "^server\\.", handler);
		server.setIdleClientTimeout(1000);
		server.start();
		Socket socket = new Socket(HOST, PORT);
		socket.setSoTimeout(100);
		try {
			OutputStream os = socket.getOutputStream();
			InputStream is = socket.getInputStream();
			
			// Sleep so we're a little way into our idle timeout
			Thread.sleep(500);
			
			for(int iters = 0; iters < 2; iters++) {
				for (int i = 0; i < request.length; i++) {
					byte[] req = (request[i] + "\r\n").getBytes("ASCII");
					os.write(req);
				}
				os.flush();
				List<String> rspLines = readMessage(is);
				assertEquals(8, rspLines.size());
				assertEquals("HTTP/1.1 200 OK", rspLines.get(0));
				assertTrue(Pattern.compile("^Date: ").matcher((String) rspLines.get(1)).find());
				assertTrue(Pattern.compile("^Host: ").matcher((String) rspLines.get(2)).find());
				assertTrue(Pattern.compile("^Server: ").matcher((String) rspLines.get(3)).find());
				assertTrue(Pattern.compile("^Content-Type: text/xml").matcher((String) rspLines.get(4)).find());
				assertTrue(Pattern.compile("^Content-Length: ").matcher((String) rspLines.get(5)).find());
				assertEquals("", rspLines.get(6));
				
				switch(iters) {
				case 0:
					// First iteration: sleep until we're over the idle timeout threshold
					// (relative to the creation of the socket) and then check we weren't disconnected
					Thread.sleep(600);
					try {
						is.read();
						fail();
					} catch (SocketTimeoutException e) {
						// We expect this because we should not be disconnected and there's
						// nothing to read.
					}
					break;
				case 1:
					// Second iteration: make sure we get disconnected.
					Thread.sleep(1500);
					int nread = is.read();
					assertEquals(-1, nread);
					break;
				}
			}
		} finally {
			socket.close();
		}
	}
	
	private void writeMessage(String[] request, Socket socket) throws IOException, UnsupportedEncodingException {
		OutputStream os = socket.getOutputStream();
		for (int i = 0; i < request.length; i++) {
			byte[] req = (request[i] + "\r\n").getBytes("ASCII");
			os.write(req);
		}
		os.flush();
	}

	private List<String> readMessage(InputStream is) throws IOException {
		byte[] rsp = new byte[1024];
		int numRead = is.read(rsp);
		BufferedReader rspIs = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(rsp, 0, numRead)));
		String line = null;
		List<String> rspLines = new ArrayList<String>();
		while ((line = rspIs.readLine()) != null) {
			rspLines.add(line);
		}
		return rspLines;
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_ServerMiscellaneous.class);
	}
}
