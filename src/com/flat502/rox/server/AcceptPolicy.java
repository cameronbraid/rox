package com.flat502.rox.server;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * A very simple accept policy interface.
 * <p>
 * An instance of this interface may be associated with an instance
 * of {@link com.flat502.rox.server.HttpRpcServer}. After a new connection has
 * been accepted, and before that connection is added to the underlying
 * {@link java.nio.channels.Selector} the installed {@link AcceptPolicy}
 * is consulted to check if the accepted connection should be retained.
 * <p>
 * If policy dictates that the connection not be retained it is closed
 * immediately.
 */
public interface AcceptPolicy {
	/**
	 * Consulted to determine whether or not the given
	 * {@link SocketChannel} should be retained.
	 * <p>
	 * Implementations should avoid any calls on the channel
	 * that may block. Blocking the calling thread will have
	 * a significant impact on throughput on the server.
	 * @param channel
	 * 	The {@link SocketChannel} that has just been
	 * 	accepted.
	 * @param activeChannels
	 * 	An (approximate) value indicating the number of
	 * 	active channels. 
	 * @return
	 * 	<code>true</code> if the channel should be retained,
	 * 	or <code>false</code> if it should be closed and
	 * 	discarded.
	 */
	public boolean shouldRetain(SocketChannel channel, int activeChannels);
}
