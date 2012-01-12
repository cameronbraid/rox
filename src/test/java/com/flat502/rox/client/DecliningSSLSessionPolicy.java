package com.flat502.rox.client;

import java.nio.channels.SocketChannel;

import com.flat502.rox.processing.SSLSession;
import com.flat502.rox.server.SSLSessionPolicy;

public class DecliningSSLSessionPolicy implements SSLSessionPolicy {
	public boolean invoked;
	public SocketChannel channel;
	public SSLSession session;

	public boolean shouldRetain(SocketChannel channel, SSLSession session) {
		this.invoked = true;
		this.channel = channel;
		this.session = session;
		return false;
	}
}
