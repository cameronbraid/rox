package com.flat502.rox.server;

import java.nio.channels.SocketChannel;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.net.ssl.SSLPeerUnverifiedException;

import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.processing.SSLSession;

/**
 * A very simple SSL session policy that rejects sessions based on
 * a white-list of subjects and/or issuer names.
 */
public class SimpleSSLSessionPolicy implements SSLSessionPolicy {
	private static Log log = LogFactory.getLog(SimpleSSLSessionPolicy.class);
	
	private List<Pattern> subjects = new ArrayList<Pattern>();
	private List<Pattern> issuers = new ArrayList<Pattern>();
	
	public SimpleSSLSessionPolicy() {
	}
	
	public SimpleSSLSessionPolicy(String pattern) {
		this.subjects.add(Pattern.compile(pattern));
	}
	
	public SimpleSSLSessionPolicy(Pattern pattern) {
		this.subjects.add(pattern);
	}
	
	public void allowSubject(String pattern) {
		this.subjects.add(Pattern.compile(pattern));
	}
	
	public void allowSubject(Pattern pattern) {
		this.subjects.add(pattern);
	}
	
	public void allowIssuer(String pattern) {
		this.issuers.add(Pattern.compile(pattern));
	}
	
	public void allowIssuer(Pattern pattern) {
		this.issuers.add(pattern);
	}
	
	public boolean shouldRetain(SocketChannel channel, SSLSession session) {
		try {
			X509Certificate peer = (X509Certificate) session.getPeerCertificates()[0];
			String subject = peer.getSubjectDN().getName();
			for (Pattern p : this.subjects) {
				if (p.matcher(subject).find()) {
					return true;
				}
			}
			
			String issuer = peer.getIssuerDN().getName();
			System.out.println(issuer);
			for (Pattern p : this.issuers) {
				if (p.matcher(issuer).find()) {
					return true;
				}
			}
			
			return false;
		} catch (SSLPeerUnverifiedException e) {
			e.printStackTrace();
			log.debug("Rejecting SSL session: peer not verified", e);
			return false;
		}
	}
}
