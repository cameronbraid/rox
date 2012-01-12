package com.flat502.rox.server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import com.flat502.rox.http.HttpRequestBuffer;
import com.flat502.rox.http.HttpResponse;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.RpcResponse;

public class Test_ResponseCoordinator extends TestCase {
	public void testCommonCase() throws Exception {
		Socket sock = new Socket();
		StubHttpRpcServer server = new StubHttpRpcServer();
		HttpRequestBuffer req = new HttpRequestBuffer(server, sock);
		MockRpcResponse rsp = new MockRpcResponse("test-common-case");

		MockResponseCoordinator rc = new MockResponseCoordinator(server, sock);
		int id1;
		assertEquals(0, id1 = rc.nextId());

		rc.respond(id1, req, rsp, null);

		assertFalse(rc.lastRsps.isEmpty());
		assertTrue(rc.lastRsps.get(0).toString().contains("test-common-case"));
	}
	
	public void testReverseOrderResponses() throws Exception {
		Socket sock = new Socket();
		StubHttpRpcServer server = new StubHttpRpcServer();
		HttpRequestBuffer req = new HttpRequestBuffer(server, sock);

		MockResponseCoordinator rc = new MockResponseCoordinator(server, sock);

		int id1;
		assertEquals(0, id1 = rc.nextId());
		int id2;
		assertEquals(1, id2 = rc.nextId());
		int id3;
		assertEquals(2, id3 = rc.nextId());

		rc.respond(id3, req, new MockRpcResponse("third-response"), null);
		assertTrue(rc.lastRsps.isEmpty());
		rc.respond(id2, req, new MockRpcResponse("second-response"), null);
		assertTrue(rc.lastRsps.isEmpty());
		rc.respond(id1, req, new MockRpcResponse("first-response"), null);
		assertEquals(3, rc.lastRsps.size());

		assertTrue(rc.lastRsps.get(0).toString().contains("first-response"));
		assertTrue(rc.lastRsps.get(1).toString().contains("second-response"));
		assertTrue(rc.lastRsps.get(2).toString().contains("third-response"));
	}
	
	private class StubHttpRpcServer extends HttpRpcServer {
		public StubHttpRpcServer() throws Exception {
			super(InetAddress.getLocalHost(), 8080, false, null);
		}
		
		@Override
		protected void initSelector(Selector selector) throws IOException {
		}
		
		@Override
		protected void queueWrite(Socket socket) {
		}
		
		@Override
		protected void queueWrite(Socket socket, byte[] data, boolean close) {
		}
	}
	
	private class MockRpcResponse implements RpcResponse {
		private String value;
		
		public MockRpcResponse(String value) {
			this.value = value;
		}
		
		public Object getReturnValue() {
			// TODO Auto-generated method stub
			return null;
		}

		public void marshal(OutputStream out, Charset charSet) throws IOException, MarshallingException {
			out.write(value.getBytes());
		}

		public String getContentType() {
			return "text/xml";
		}
	}
	
	private class MockResponseCoordinator extends ResponseCoordinator {
		public List<HttpResponse> lastRsps = new ArrayList<HttpResponse>();
		
		public MockResponseCoordinator(StubHttpRpcServer server, Socket socket) {
			super(server, socket);
		}
		
		@Override
		protected void sendResponse(HttpResponse rsp) throws IOException {
			super.sendResponse(rsp);
			this.lastRsps.add(rsp);
		}
		
		@Override
		protected void stashResponse(int rspId, HttpResponse httpRsp) throws IOException {
			super.stashResponse(rspId, httpRsp);
		}
	}
	
	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_ResponseCoordinator.class);
	}
}
