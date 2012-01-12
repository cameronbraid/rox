package com.flat502.rox.server;

import java.nio.channels.SocketChannel;

import com.flat502.rox.processing.SSLSession;

/**
 * A very simple accept policy interface.
 * <p>
 * An instance of this interface may be associated with an instance
 * of {@link com.flat502.rox.server.HttpRpcServer}. After a new SSL connection has
 * completed handshaking the installed {@link SSLSessionPolicy}
 * is consulted to check if the SSL session should be retained.
 * <p>
 * If policy dictates that the connection not be retained it is closed
 * immediately.
 */
public interface SSLSessionPolicy {
	/**
	 * Consulted to determine whether or not the given
	 * {@link SSLSession} should be retained.
	 * <p>
	 * Implementations should avoid any calls on the channel
	 * that may block. Blocking the calling thread will have
	 * a significant impact on throughput on the server.
	 * @param channel
	 * 	The {@link SocketChannel} that has just been
	 * 	accepted.
	 * @param session
	 * 	The {@link SSLSession} that has just completed
	 * 	handshaking.
	 * @return
	 * 	<code>true</code> if the channel should be retained,
	 * 	or <code>false</code> if it should be closed and
	 * 	discarded.
	 */
	public boolean shouldRetain(SocketChannel channel, SSLSession session);
}
