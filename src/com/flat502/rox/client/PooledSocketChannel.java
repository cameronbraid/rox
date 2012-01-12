package com.flat502.rox.client;

import java.nio.channels.SocketChannel;

class PooledSocketChannel {
	private long creationTime = System.currentTimeMillis();
	private long accessTime = System.currentTimeMillis();

	private HttpRpcClient owner;
	private SocketChannel channel;
	private Object poolingKey;

	public PooledSocketChannel(HttpRpcClient owner, SocketChannel channel, Object poolingKey) {
		this.owner = owner;
		this.channel = channel;
		this.poolingKey = poolingKey;
	}
	
	public void setOwner(HttpRpcClient owner) {
		this.owner = owner;
	}

	public void notifyReturned() {
		this.accessTime = System.currentTimeMillis();
	}

	public HttpRpcClient getOwner() {
		return this.owner;
	}
	
	public Object getPoolingKey() {
		return this.poolingKey;
	}
	
	public SocketChannel getPhysicalConnection() {
		return this.channel;
	}

	public long getAccessTime() {
		return this.accessTime;
	}

	public long getAge() {
		return System.currentTimeMillis() - this.accessTime;
	}
}
