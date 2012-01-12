package com.flat502.rox.processing;

import com.flat502.rox.processing.SSLConfiguration.ClientAuth;

import junit.framework.TestCase;


public class Test_SSLConfiguration extends TestCase {
	public void testDefaultHandshakeTimeout() throws Exception {
		assertEquals(10000, new SSLConfiguration().getHandshakeTimeout());
	}

	public void testDefaultClientAuth() throws Exception {
		assertEquals(ClientAuth.NONE, new SSLConfiguration().getClientAuthentication());
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_SSLConfiguration.class);
	}
}
