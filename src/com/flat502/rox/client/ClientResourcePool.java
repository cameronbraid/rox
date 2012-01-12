/**
 * 
 */
package com.flat502.rox.client;

import java.io.IOException;
import java.nio.channels.SocketChannel;

import com.flat502.rox.processing.HttpMessageHandler;
import com.flat502.rox.processing.ResourcePool;
import com.flat502.rox.utils.Profiler;

public class ClientResourcePool extends ResourcePool {
	private Object notifierMutex = new Object();
	private int connectionPoolLimit;
	private long connectionPoolTimeout;
	private long requestTimeout;

	private SharedSocketChannelPool connPool;
	
	public void registerProfiler(Profiler p) {
		synchronized (this.notifierMutex) {
			if (this.connPool != null) {
				// Too late, exec* has been called
				throw new IllegalStateException("Profilers must be registered before executing methods");
			}
		}
		super.registerProfiler(p);
	}
	
	public Object getNotifierMutex() {
		return this.notifierMutex;
	}
	
	/**
	 * Configure a limit on the number of active connections provided at
	 * any given time by the underlying connection pool.
	 * <p>
	 * If a non-zero limit is configured, any thread requesting a new connection
	 * that would cause this limit to be exceeded will be blocked 
	 * until an existing connection is returned or until a timeout
	 * occurs (if a timeout {@link #setConnectionPoolTimeout(long) has been set}).
	 * <p>
	 * Care should be taken when using the asynchronous execution API
	 * without a limit on the connection pool. It's very easy to consume all
	 * available local connections like this.
	 * <p>
	 * If a non-zero limit is placed on the connection pool then you
	 * must be sure to set a {@link #setConnectionPoolTimeout(long) timeout}
	 * on the pool. Failure to do so will result in threads blocking
	 * indefinitely when the connection pool is exhausted.
	 * @param limit
	 * 	The maximum number of active connections allowed at any given moment.
	 * 	A value of 0 indicates no limit should be enforced (this is the default value).
	 * @throws IllegalArgumentException
	 * 	If the timeout provided is negative.
	 * @throws IllegalStateException
	 * 	If any of the <code>execute</code> methods were invoked before
	 * 	this method was invoked.
	 */
	public void setConnectionPoolLimit(int limit) {
		if (limit < 0) {
			throw new IllegalArgumentException("limit is negative");
		}
		synchronized (this.notifierMutex) {
			if (this.connPool != null) {
				// Too late, exec* has been called
				throw new IllegalStateException("Connection pool attributes must be configured before executing methods");
			}
		}
		this.connectionPoolLimit = limit;
	}

	/**
	 * Configure a timeout value for the underlying connection pool.
	 * <p>
	 * This timeout only applies if a limit has been set on the number
	 * of active connections (using {@link #setConnectionPoolLimit(int)}.
	 * <p>
	 * If a limit is configured this timeout controls how long a thread
	 * will be blocked waiting for a new connection before a
	 * {@link ConnectionPoolTimeoutException} is raised.
	 * @param timeout
	 * 	The timeout (in milliseconds). A value of 0 indicates no timeout should be
	 * 	enforced (this is the default value).
	 * @throws IllegalArgumentException
	 * 	If the timeout provided is negative.
	 * @throws IllegalStateException
	 * 	If any of the <code>execute</code> methods were invoked before
	 * 	this method was invoked.
	 */
	public void setConnectionPoolTimeout(long timeout) {
		if (timeout < 0) {
			throw new IllegalArgumentException("timeout is negative");
		}
		synchronized (this.notifierMutex) {
			if (this.connPool != null) {
				// Too late, exec* has been called
				throw new IllegalStateException("Connection pool attributes must be configured before executing methods");
			}
		}
		this.connectionPoolTimeout = timeout;
	}
	
	/**
	 * Configure a <em>default</em> timeout value for RPC method calls.
	 * <p>
	 * All {@link HttpRpcClient} instances sharing a resource pool
	 * "inherit" this timeout as their initial timeout value.
	 * @param timeout
	 * 	The timeout (in milliseconds). A value of 0 indicates no timeout should be
	 * 	enforced.
	 * @throws IllegalArgumentException
	 * 	If the timeout provided is negative.
	 * @see HttpRpcClient#setRequestTimeout(long)
	 */
	public void setRequestTimeout(long timeout) {
		if (timeout < 0) {
			throw new IllegalArgumentException("timeout is negative");
		}

		this.requestTimeout = timeout;
	}
	
	public long getRequestTimeout() {
		return this.requestTimeout;
	}
	
	public SharedSocketChannelPool getSocketChannelPool() {
		synchronized (this.notifierMutex) {
			if (this.connPool == null) {
				this.connPool = this.newSharedSocketChannelPool(this.notifierMutex, this.connectionPoolLimit,
						this.connectionPoolTimeout);
			}

			return this.connPool;
		}
	}
	
	protected SharedSocketChannelPool newSharedSocketChannelPool(Object mutex, int limit, long timeout) {
		return new SharedSocketChannelPool(mutex, limit, timeout, this.getProfilers());
	}

	public void shutdown() {
		synchronized (this.notifierMutex) {
			if (this.connPool != null) {
				this.connPool.close();
			}
		}
		super.shutdown();
	}
	
	protected HttpMessageHandler newWorker() {
		return new HttpResponseHandler(this.getQueue());
	}

	protected Thread newProcessingThread(Runnable target) {
		Thread t = super.newProcessingThread(target);
		t.setDaemon(true);
		return t;
	}
	
	protected void detach(HttpRpcClient client) throws IOException {
		synchronized (this.notifierMutex) {
			if (this.connPool != null) {
				this.connPool.detach(client);
			}
		}
		super.detach(client);
	}

	protected void notifyUnownedChannelClosure(SocketChannel channel) {
		synchronized (this.notifierMutex) {
			if (this.connPool != null) {
				this.connPool.removeClosedChannel(channel);
			}
		}
	}
}
