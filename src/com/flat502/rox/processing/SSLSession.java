package com.flat502.rox.processing;

import java.security.Principal;
import java.security.cert.Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;

public class SSLSession {
	// We proxy javax.net.SSLSession because it exposes just a little too much
	// to our callers.
	private javax.net.ssl.SSLSession session;
	
	public SSLSession(javax.net.ssl.SSLSession session) {
		this.session = session;
	}

	public Certificate[] getLocalCertificates() {
		return session.getLocalCertificates();
	}

	public Principal getLocalPrincipal() {
		return session.getLocalPrincipal();
	}

	public Certificate[] getPeerCertificates() throws SSLPeerUnverifiedException {
		return session.getPeerCertificates();
	}

	public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
		return session.getPeerPrincipal();
	}

	public String getCipherSuite() {
		return session.getCipherSuite();
	}

	public String getProtocol() {
		return session.getProtocol();
	}
}
