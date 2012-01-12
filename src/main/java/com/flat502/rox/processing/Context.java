package com.flat502.rox.processing;

import java.net.SocketAddress;
import java.nio.channels.SocketChannel;

/**
 * Abstract base class for describing the context of an RPC
 * call or response.
 */
public abstract class Context {
	private SocketAddress remoteAddr;
	private int remotePort;
	private SSLSession sslSession;
	
	protected Context(SocketChannel channel, SSLSession sslSession) {
		if (channel != null && channel.socket() != null) {
			this.remoteAddr = channel.socket().getRemoteSocketAddress();
			this.remotePort = channel.socket().getPort();
		}
		this.sslSession = sslSession;
	}

	/**
	 * Get the SSL session the requested was delivered over.
	 * <p>
	 * If no SSL session exists (for example, if HTTP is used) this
	 * parameter will be <code>null</code>.
	 * @return
	 * 	An {@link SSLSession} instance or <code>null</code>
	 */
	public SSLSession getSSLSession() {
		return this.sslSession;
	}

	public SocketAddress getRemoteAddr() {
		return remoteAddr;
	}

	public int getRemotePort() {
		return remotePort;
	}
}
