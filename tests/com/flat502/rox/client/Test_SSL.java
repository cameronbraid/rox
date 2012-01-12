package com.flat502.rox.client;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.security.auth.x500.X500Principal;

import junit.framework.TestCase;

import com.flat502.rox.client.SSLUtils.Identity;
import com.flat502.rox.processing.SSLConfiguration;
import com.flat502.rox.processing.ThreadUtils;
import com.flat502.rox.processing.SSLConfiguration.ClientAuth;
import com.flat502.rox.server.ManualSynchronousHandler;
import com.flat502.rox.server.SimpleSSLSessionPolicy;
import com.flat502.rox.server.XmlRpcServer;

public class Test_SSL extends TestCase {
	private static final String PREFIX = "test";
	private static final String HOST = "localhost";
	private static final int PORT = 8080;
	private static final String HTTP_URL = "http://localhost:" + PORT + "/";
	private static final String HTTPS_URL = "https://localhost:" + PORT + "/";

	protected void setUp() throws Exception {
		ThreadUtils.assertZeroThreads();
	}

	protected void tearDown() throws Exception {
		ThreadUtils.assertZeroThreads();
	}

	public void testSSLClientSSLServer() throws Exception {
		TestServer server = new TestServer(null, PREFIX, PORT, true);
		XmlRpcClient client = new XmlRpcClient(new URL(HTTPS_URL));
		try {
			Object rsp = client.execute("test.stringResponse", null);
			assertNotNull(rsp);
			assertEquals("bar", rsp);

			assertNotNull(server.context);
			assertNotNull(server.context.getSSLSession());
			assertTrue(server.context.getSSLSession().getCipherSuite().contains("DH_anon"));

			assertNotNull(server.context.getRemoteAddr());

			InetSocketAddress remote = (InetSocketAddress) server.context.getRemoteAddr();
			InetAddress local = InetAddress.getByName("localhost");
			assertEquals(local.getHostAddress(), remote.getAddress().getHostAddress());
			assertTrue(server.context.getRemotePort() > 0);
		} finally {
			client.stop();
			server.stop();
		}
	}

	public void testSSLClientCleartextServerDefaultTimeout() throws Exception {
		TestServer server = new TestServer(null, PREFIX, PORT, false);
		XmlRpcClient client = new XmlRpcClient(new URL(HTTPS_URL));
		long start = System.currentTimeMillis();
		try {
			client.execute("test.stringResponse", null);
			fail();
		} catch (RpcCallTimeoutException e) {
			assertTrue(e.getCause() instanceof SSLException);
			long elapsed = System.currentTimeMillis() - start;
			assertTrue(9500 < elapsed);
			assertTrue(elapsed < 10500);
		} finally {
			client.stop();
			server.stop();
		}
	}

	public void testSSLClientCleartextServerExplicitTimeout() throws Exception {
		TestServer server = new TestServer(null, PREFIX, PORT, false);
		XmlRpcClient client = new XmlRpcClient(new URL(HTTPS_URL));
		client.setSSLHandshakeTimeout(3000);
		long start = System.currentTimeMillis();
		try {
			client.execute("test.stringResponse", null);
			fail();
		} catch (RpcCallTimeoutException e) {
			assertTrue(e.getCause() instanceof SSLException);
			long elapsed = System.currentTimeMillis() - start;
			assertTrue(2500 < elapsed);
			assertTrue(elapsed < 3500);
		} finally {
			client.stop();
			server.stop();
		}
	}

	public void testCleartextClientSSLServer() throws Exception {
		TestServer server = new TestServer(null, PREFIX, PORT, true);
		XmlRpcClient client = new XmlRpcClient(new URL(HTTP_URL));
		try {
			client.execute("test.stringResponse", null);
			fail();
		} catch (RpcCallFailedException e) {
		} finally {
			client.stop();
			server.stop();
		}
	}

	public void testAnonSSL() throws Exception {
		TestSecureServer server = new TestSecureServer(null, PREFIX, PORT, new SSLConfiguration());
		SSLConfiguration sslcfg = new SSLConfiguration();
		sslcfg.setCipherSuitePattern(SSLConfiguration.ANON_CIPHER_SUITES);
		XmlRpcClient client = new XmlRpcClient(new URL(HTTPS_URL), sslcfg);
		try {
			Object rsp = client.execute("test.stringResponse", null);
			assertNotNull(rsp);
			assertEquals("bar", rsp);
			assertNotNull(server.session);
			assertTrue(server.session.getCipherSuite().contains("_anon_"));
		} finally {
			client.stop();
			server.stop();
		}
	}

	// Generate better keys.
	public void testServerAuthUsingKeystore() throws Exception {
		Properties props = new Properties(System.getProperties());
		
		props.setProperty("javax.net.ssl.trustStore",
				"src/com/flat502/rox/test/test.truststore");
		props.setProperty("javax.net.ssl.trustStorePassword", "ipmanager");

		props.setProperty("javax.net.ssl.keyStore",
				"src/com/flat502/rox/test/keystore.p12");
		props.setProperty("javax.net.ssl.keyStorePassword", "changeit");
		props.setProperty("javax.net.ssl.keyStoreType", "pkcs12");

		Identity ipman = SSLUtils.loadIdentity(props, "1");
		X509Certificate ec2ra = SSLUtils.loadTrustedEntity(props, "ipmanager"); // .2 through .5
		X509Certificate ec2ca = SSLUtils.loadTrustedEntity(props, "ipmanager.2"); // .2 through .5
		//		X509Certificate ca3 = SSLUtils.loadTrustedEntity(props, "ipmanager.3"); // .2 through .5
		//		X509Certificate ca4 = SSLUtils.loadTrustedEntity(props, "ipmanager.4"); // .2 through .5
		//		X509Certificate ca5 = SSLUtils.loadTrustedEntity(props, "ipmanager.5"); // .2 through .5

		Identity serverid = ipman;
		SSLConfiguration servercfg = new SSLConfiguration();
		servercfg.addIdentity(serverid.privateKey, serverid.chain);
		servercfg.addTrustedEntity(ec2ra);
		servercfg.addTrustedEntity(ec2ca);
		servercfg.setClientAuthentication(ClientAuth.REQUEST);

		TestSecureServer server = new TestSecureServer(null, PREFIX, PORT, servercfg);

		SSLConfiguration clientcfg = new SSLConfiguration();
		clientcfg.addTrustedEntity(ec2ra);
		clientcfg.addTrustedEntity(ec2ca);
		// Ensure we don't fall back to anonymous SSL
		//		clientcfg.setCipherSuitePattern("^SSL_RSA.*");

		XmlRpcClient client = new XmlRpcClient(new URL(HTTPS_URL), clientcfg);
		try {
			Object rsp = client.execute("test.stringResponse", null);
			assertNotNull(rsp);
			assertEquals("bar", rsp);
			assertNotNull(server.session);
			assertNotNull(server.session.getLocalPrincipal());
			//			assertNotNull(server.session.getPeerPrincipal());
		} finally {
			client.stop();
			server.stop();
		}
	}

	public void testServerAuth() throws Exception {
		Identity serverid = SSLUtils.generateIdentity("server");

		SSLConfiguration servercfg = new SSLConfiguration();
		servercfg.addIdentity(serverid.privateKey, serverid.chain);
		servercfg.addTrustedEntity(serverid.chain[0]);
		TestSecureServer server = new TestSecureServer(null, PREFIX, PORT, servercfg);

		SSLConfiguration clientcfg = new SSLConfiguration();
		clientcfg.addTrustedEntity(serverid.chain[0]);
		// Ensure we don't fall back to anonymous SSL
		clientcfg.setCipherSuitePattern("^SSL_RSA.*");
		XmlRpcClient client = new XmlRpcClient(new URL(HTTPS_URL), clientcfg);
		try {
			Object rsp = client.execute("test.stringResponse", null);
			assertNotNull(rsp);
			assertEquals("bar", rsp);
			assertNotNull(server.session);
			assertNotNull(server.session.getLocalPrincipal());
			assertEquals(new X500Principal(serverid.chain[0].getSubjectDN().getName()), server.session.getLocalPrincipal());
			assertTrue(server.session.getCipherSuite().contains("SSL_RSA"));
		} finally {
			client.stop();
			server.stop();
		}
	}

	public void testClientAuthOptional() throws Exception {
		Identity serverid = SSLUtils.generateIdentity("server");

		SSLConfiguration servercfg = new SSLConfiguration();
		servercfg.addIdentity(serverid.privateKey, serverid.chain);
		servercfg.addTrustedEntity(serverid.chain[0]);
		servercfg.setClientAuthentication(ClientAuth.REQUEST);
		TestSecureServer server = new TestSecureServer(null, PREFIX, PORT, servercfg);

		SSLConfiguration clientcfg = new SSLConfiguration();
		clientcfg.addTrustedEntity(serverid.chain[0]);
		// Ensure we don't fall back to anonymous SSL
		clientcfg.setCipherSuitePattern("^SSL_RSA.*");
		XmlRpcClient client = new XmlRpcClient(new URL(HTTPS_URL), clientcfg);
		try {
			Object rsp = client.execute("test.stringResponse", null);
			assertNotNull(rsp);
			assertEquals("bar", rsp);
			assertNotNull(server.session);
			assertNotNull(server.session.getLocalPrincipal());
			assertEquals(new X500Principal(serverid.chain[0].getSubjectDN().getName()), server.session.getLocalPrincipal());
			assertTrue(server.session.getCipherSuite().contains("SSL_RSA"));
		} finally {
			client.stop();
			server.stop();
		}
	}

	public void testClientAuthMandatory() throws Exception {
		Identity serverid = SSLUtils.generateIdentity("server");
		Identity clientid = SSLUtils.generateIdentity("client");

		SSLConfiguration servercfg = new SSLConfiguration();
		servercfg.addIdentity(serverid.privateKey, serverid.chain);
		servercfg.addTrustedEntity(serverid.chain[0]);
		servercfg.addTrustedEntity(clientid.chain[0]);
		servercfg.setClientAuthentication(ClientAuth.REQUIRE);
		TestSecureServer server = new TestSecureServer(null, PREFIX, PORT, servercfg);

		SSLConfiguration clientcfg = new SSLConfiguration();
		clientcfg.addIdentity(clientid.privateKey, clientid.chain);
		clientcfg.addTrustedEntity(serverid.chain[0]);
		clientcfg.addTrustedEntity(clientid.chain[0]);
		// Ensure we don't fall back to anonymous SSL
		clientcfg.setCipherSuitePattern("^SSL_RSA.*");
		XmlRpcClient client = new XmlRpcClient(new URL(HTTPS_URL), clientcfg);
		try {
			Object rsp = client.execute("test.stringResponse", null);
			assertNotNull(rsp);
			assertEquals("bar", rsp);
			assertNotNull(server.session);
			assertNotNull(server.session.getLocalPrincipal());
			assertEquals(new X500Principal(serverid.chain[0].getSubjectDN().getName()), server.session.getLocalPrincipal());
			assertTrue(server.session.getCipherSuite().contains("SSL_RSA"));
		} finally {
			client.stop();
			server.stop();
		}
	}

	// TODO: Add tests for SSLSessionPolicy
	public void testSSLSessionPolicy() throws Exception {
		XmlRpcServer server = new XmlRpcServer(null, PORT, true);
		server.registerSSLSessionPolicy(new DecliningSSLSessionPolicy());
		XmlRpcClient client = new XmlRpcClient(new URL(HTTPS_URL));
		try {
			server.start();
			client.execute("test.stringResponse", null);
			fail();
		} catch (RpcCallFailedException e) {
		} finally {
			client.stop();
			server.stop();
		}
	}

	// Need a better way of testing this.
	public void testSimpleSSLSessionPolicySubject() throws Exception {
		Properties props = new Properties(System.getProperties());
		
		props.setProperty("javax.net.ssl.trustStore",
				"src/com/flat502/rox/test/test.truststore");
		props.setProperty("javax.net.ssl.trustStorePassword", "ipmanager");

		props.setProperty("javax.net.ssl.keyStore",
				"src/com/flat502/rox/test/keystore.p12");
		props.setProperty("javax.net.ssl.keyStorePassword", "changeit");
		props.setProperty("javax.net.ssl.keyStoreType", "pkcs12");

		// WTF? Who the hell is tracking how many times I access the keystore and
		// "incrementing" the alias?
		Identity ipman = SSLUtils.loadIdentity(props, "2");
		X509Certificate ec2ra = SSLUtils.loadTrustedEntity(props, "ipmanager"); // .2 through .5
		X509Certificate ec2ca = SSLUtils.loadTrustedEntity(props, "ipmanager.2"); // .2 through .5
		//		X509Certificate ca3 = SSLUtils.loadTrustedEntity(props, "ipmanager.3"); // .2 through .5
		//		X509Certificate ca4 = SSLUtils.loadTrustedEntity(props, "ipmanager.4"); // .2 through .5
		//		X509Certificate ca5 = SSLUtils.loadTrustedEntity(props, "ipmanager.5"); // .2 through .5

		Identity serverid = ipman;
		SSLConfiguration servercfg = new SSLConfiguration();
		servercfg.addIdentity(serverid.privateKey, serverid.chain);
		servercfg.addTrustedEntity(ec2ra);
		servercfg.addTrustedEntity(ec2ca);
		servercfg.setClientAuthentication(ClientAuth.REQUEST);

		SimpleSSLSessionPolicy policy = new SimpleSSLSessionPolicy("CN=Test");
		
		TestSecureServer server = new TestSecureServer(null, PREFIX, PORT, servercfg, policy);

		Identity clientid = ipman;
		SSLConfiguration clientcfg = new SSLConfiguration();
		clientcfg.addIdentity(clientid.privateKey, clientid.chain);
		clientcfg.addTrustedEntity(ec2ra);
		clientcfg.addTrustedEntity(ec2ca);
		// Ensure we don't fall back to anonymous SSL
		//clientcfg.setCipherSuitePattern("^SSL_RSA.*");

		XmlRpcClient client = new XmlRpcClient(new URL(HTTPS_URL), clientcfg);
		try {
			Object rsp = client.execute("test.stringResponse", null);
			assertNotNull(rsp);
			assertEquals("bar", rsp);
			assertNotNull(server.session);
			assertNotNull(server.session.getLocalPrincipal());
			assertNotNull(server.session.getPeerPrincipal());
		} finally {
			client.stop();
			server.stop();
		}
	}

//	static {
//		System.setProperty("javax.net.debug", "all");
//		LogConfig.config();
//	}

	public void testFragmentedSSLClient() throws Exception {
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
		XmlRpcServer server = new XmlRpcServer(null, PORT, true);
		server.registerHandler(null, "^server\\.", handler);
		server.start();
		
		SSLSocketFactory f = (SSLSocketFactory) SSLSocketFactory.getDefault();
		SSLSocket socket = (SSLSocket) f.createSocket(HOST, PORT);
		socket.setEnabledCipherSuites(f.getSupportedCipherSuites());
		
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
			server.stop();
			Thread.sleep(50);
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
		junit.textui.TestRunner.run(Test_SSL.class);
	}
}
