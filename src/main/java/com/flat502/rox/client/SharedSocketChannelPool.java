package com.flat502.rox.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.Map.Entry;

import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.utils.*;

class SharedSocketChannelPool {
	private static Log log = LogFactory.getLog(SharedSocketChannelPool.class);

	// These map HttpRpcClient instances to pooled and active connections
	// respectively. Destinations in the following are (protocol, host, port)
	// tuples.
	// pooledConnStacks maps Destinations -> List<PooledSocketChannel>
	//		These lists are treated as stacks so we always reuse the MRU connection
	// activeConnSets maps Destinations -> Set<PooledSocketChannel>
	//		These sets allow us to recover active channels when a client
	//		detaches.
	private Map pooledConnStacks = new HashMap();
	private Map activeConnSets = new HashMap();
	
	// Maps Destinations -> Set<HttpRpcClient> so we can track all
	// known clients for a given key (see createClientKey()).
	private Map knownClients = new HashMap();
	
	// A binary heap holding PooledSocketChannel instances that aren't
	// checked out. This is ordered by the last access time of said 
	// channels. This allows us to efficiently check for channels that 
	// should be expired after some amount of inactivity, and to 
	// efficiently locate the least recently used channel
	// (globally) when we need to evict a channel to satisfy
	// a client request.
	private MinHeap channelHeap = new MinHeap();
	
	// Tracks the total number of connections in existence
	// that are still in the pool. This provides
	// an easy check for availability.
	private int availableConnections;
	
	// Maps physical channels to their pooled wrapper
	// so we can find the pooled wrapper when a channel
	// is returned.
	private Map channelMap = new HashMap();

	// We use this to capture threads when the pool reaches it's
	// limit. This ensures that threads are serviced in FIFO order
	// which is a reasonably simple approach to fairness.
	private ThreadQueue threadQueue;

	private Object mutex;

	private int maxConnections;
	private int activeConnections;
	private long maxWait;

	private Profiler profiler;

	public SharedSocketChannelPool(Object mutex, int maxConnections, long maxWait, Profiler profiler) {
		this.mutex = mutex;
		this.maxConnections = maxConnections;
		this.maxWait = maxWait;
		this.threadQueue = new ThreadQueue(this.mutex);
		this.profiler = profiler;
		
		if (log.logTrace()) {
			log.trace("Socket channel pool initialized: limit=" + maxConnections + ", timeout=" + maxWait);
		}
	}

	public SocketChannel getChannel(HttpRpcClient client) throws IOException {
		long pid = client.hashCode() ^ System.nanoTime();
		this.profiler.begin(pid, this.getClass().getName() + ".getChannel");
		try {
			synchronized (this.mutex) {
				SocketChannel channel;
				
				dbgLog("GET starts", client, null);
				
				// Ensure this client is always recorded
				Object key = this.createClientKey(client);
				Set clientSet = (Set) this.knownClients.get(key);
				if (clientSet == null) {
					clientSet = new HashSet();
					this.knownClients.put(key, clientSet);
				}
				clientSet.add(client);
	
				// Now try to satisfy the connection request
				if (this.maxConnections == 0) {
					// No limit is defined. If we can't check a pooled connection out
					// then just create a new one.
					channel = this.checkOut(client);
					if (channel == null) {
						channel = this.getNewChannel(client);
					}
				} else {
					// A limit is defined
					if (this.activeConnections + this.availableConnections == this.maxConnections) {
						// And we've hit it. We loop here because capture()/release() below has similar semantics
						// to wait()/notify().
						while (this.activeConnections == this.maxConnections) {
							// Nothing is pooled (they're all active connections). Wait until something frees up.
							if (log.logTrace()) {
								log.trace("Connection pool limit reached, waiting for return");
							}
							try {
								dbgLog("CAPTURE", client, null);
								this.threadQueue.capture(this.maxWait);
								dbgLog("RELEASE", client, null);
							} catch (CaptureTimeoutException e) {
								throw new ConnectionPoolTimeoutException(e);
							}
						}
						
						// There's capacity but it may not be appropriate for our use.
						// Try to check out a pooled connection. If we can't then create a new one
						// (which may require us to evict an existing connection). 
						channel = this.checkOut(client);
						if (channel == null) {
							if (this.availableConnections > 0) {
								if (log.logTrace()) {
									log.trace("Replacing pooled socket channel for [" + client.getURL() + "]");
								}
								
								// Select LRU SocketChannel and close it
								this.removeLeastRecentlyUsedChannel();
							}
	
							channel = this.getNewChannel(client);
						}
					} else {
						// But we haven't hit it yet. Try to check out a pooled connection,
						// but if we can't then just create a new one.
						channel = this.checkOut(client);
						if (channel == null) {
							channel = this.getNewChannel(client);
						}
					}
				}
				
				dbgLog("GET returns", client, channel);
				return channel;
			}
		} finally {
			this.profiler.end(pid, this.getClass().getName() + ".getChannel");
		}
	}
	
	private static long classInit = System.currentTimeMillis();
	
	private void dbgLog(String id, HttpRpcClient client, SocketChannel channel) {
		if (!log.logTrace()) {
			return;
		}
		
		long time = System.currentTimeMillis()-classInit;
		Object key = client == null ? null : createClientKey(client);
		log.trace(time+": "+id+": client="+System.identityHashCode(client)+" ["+key+"]: ch="+System.identityHashCode(channel)+": "+this);
	}
	
	public void returnChannel(HttpRpcClient client, SocketChannel channel) {
		boolean bailing = false;
		synchronized (this.mutex) {
			try {
				dbgLog("RETURN starts", client, channel);
				
				if (log.logTrace()) {
					log.trace("Returning socket channel to pool for [" + client.getURL() + "]");
				}

				if (this.availableConnections > 0) {
					this.removeExpiredChannels();
				}

				// Look up the pooled channel for this channel
				PooledSocketChannel pooledChannel = (PooledSocketChannel) this.channelMap.get(channel);
				if (pooledChannel == null) {
					// This has been cleaned up by a call to detach() (via HttpRpcClient.stop())
					// Counts should all be correct. Just bail.
					dbgLog("RETURN bails", client, channel);
					bailing = true;
					return;
				}
				
				if (pooledChannel.getOwner() != client) {
					throw new IllegalArgumentException("Channel not returned by owner");
				}
				
				// Indicate it's been returned (so we can update its accessTime)
				pooledChannel.notifyReturned();
				
				// And check this connection back in
				this.checkIn(client, pooledChannel);

				dbgLog("RETURN returns", client, channel);
			} finally {
				if (!bailing) {
					dbgLog("RETURN releases", client, channel);
					this.threadQueue.release();
				}
			}
		}
	}
	
	public void removeChannel(HttpRpcClient client, SocketChannel channel) {
		synchronized (this.mutex) {
			try {
				dbgLog("REMOVE starts", client, channel);

				Object key = createClientKey(client);
				
				if (log.logTrace()) {
					log.trace("Closing pooled socket channel for [" + key + "]");
				}
				PooledSocketChannel pooledChannel = (PooledSocketChannel) this.channelMap.remove(channel);
				if (pooledChannel != null) {
					if (pooledChannel.getOwner() != client) {
						throw new IllegalArgumentException("Channel not removed by owner");
					}
					
					Set activeSet = (Set) this.activeConnSets.get(key);
					// This might be null if a timeout occurs, calls into this method
					// and the client has already detached.
					if (activeSet != null) {
						activeSet.remove(pooledChannel);
						this.activeConnections--;
					}
				}
				
				client.cancel(channel);
				
				dbgLog("REMOVE returns", client, channel);
			} finally {
				dbgLog("REMOVE releases", client, channel);
				this.threadQueue.release();
			}
		}
	}
	
	void removeClosedChannel(SocketChannel channel) {
		synchronized (this.mutex) {
			dbgLog("REMOVE_CLOSED starts", null, channel);

			if (log.logTrace()) {
				log.trace("Removing closed pooled socket channel");
			}
			PooledSocketChannel pooledChannel = (PooledSocketChannel) this.channelMap.remove(channel);
			if (pooledChannel != null) {
				if (pooledChannel.getOwner() != null) {
					throw new IllegalArgumentException("Channel has an owner");
				}

				// Remove the pooled connection if it hasn't been done already (because
				// we closed this connection as part of detaching() and this is the channel selector
				// calling through after being woken up subsequently).
				List connStack = (List) this.pooledConnStacks.get(pooledChannel.getPoolingKey());
				if (connStack != null) {
					Set clientSet = (Set) this.knownClients.get(pooledChannel.getPoolingKey());
					if (clientSet == null) {
						if (connStack.remove(pooledChannel)) {
							this.availableConnections--;
						}
					}
				}
			}
			
			dbgLog("REMOVE_CLOSED returns", null, channel);
		}
	}

	// TODO: This won't close in-use connections. Should it?
	public void close() {
		synchronized (this.mutex) {
			Iterator stacks = this.pooledConnStacks.entrySet().iterator();
			while(stacks.hasNext()) {
				Entry entry = (Map.Entry) stacks.next();
				String key = (String) entry.getKey();
				List stack = (List) entry.getValue();
				
				ListIterator pooledChannels = stack.listIterator(stack.size());
				while(pooledChannels.hasPrevious()) {
					PooledSocketChannel pooledChannel = (PooledSocketChannel) pooledChannels.previous();
					this.closePooledChannel(pooledChannel);
					pooledChannels.remove();
				}
				
				Set clientSet = (Set) this.knownClients.get(key);
				if (clientSet != null && clientSet.isEmpty()) {
					// Only remove this stack if we know of no other clients using this key (URL)
					stacks.remove();
				}
			}
		}
	}

	public String toString() {
		int total = this.channelMap.size();
		return "Pool[size=" + total + ", active=" + this.activeConnections + ", pooled=" + this.availableConnections + ", max=" + this.maxConnections + "]";
	}
	
	public void detach(HttpRpcClient client) {
		synchronized(this.mutex) {
			try {
				String hc = ""+System.identityHashCode(client);
				Object key = createClientKey(client);
				dbgLog("DETACH starts", client, null);
	
				// By definition, pooled connections are not "owned" by this client.
				// However, if no other clients we know of share their key then
				// we need to clean up those pooled connections.
				Set clientSet = (Set) this.knownClients.get(key);
				// We may never have seen connections on this URL
				if (clientSet != null) {
					clientSet.remove(client);
					if (clientSet.isEmpty()) {
						this.knownClients.remove(key);
		
						// Clean up all pooled connections for this key
						List connStack = (List) this.pooledConnStacks.remove(key);
						if (connStack != null) {
							Iterator pooledChannels = connStack.iterator();
							while(pooledChannels.hasNext()) {
								PooledSocketChannel pooledChannel = (PooledSocketChannel) pooledChannels.next();
								this.channelHeap.removeValue(pooledChannel);
								// This cleans up channelMap too
								this.closePooledChannel(pooledChannel);
							}
						}
					}
				}
				
				dbgLog("DETACH pooled done", client, null);
				
				// Reclaim any active connections owned by this client
				Set activeSet = (Set) this.activeConnSets.get(key);
				if (activeSet != null) {
					Iterator pooledChannels = activeSet.iterator();
					while(pooledChannels.hasNext()) {
						PooledSocketChannel pooledChannel = (PooledSocketChannel) pooledChannels.next();
						if (pooledChannel.getOwner() == client) {
							this.channelHeap.removeValue(pooledChannel);
							// This cleans up channelMap too
							this.closeActiveChannel(pooledChannel);
							// Remove this active channel
							pooledChannels.remove();
						}
					}
					
					// If there are no other clients using this key (URL) then we can clean up
					// entirely.
					// Note: clientSet can't be null here since we must have returned one channel
					// to have set activeConnSets up.
					if (clientSet.isEmpty()) {
						this.activeConnSets.remove(key);
					}
				}
				
				dbgLog("DETACH returns", client, null);
			} finally {
				dbgLog("DETACH releases", client, null);
				this.threadQueue.release();
			}
		}
	}
	
	protected Object createClientKey(HttpRpcClient client) {
		// It might make sense to put this method onto the client so
		// this value can be constructed once.
		URL url = client.getURL();
		int port = url.getPort();
		if (port == -1) {
			port = url.getDefaultPort();
		}
		return url.getProtocol() + "://" + url.getHost() + ":" + port;
	}

	/**
	 * Invoked when the pool is empty and a new connection
	 * is required. This method will block (if the
	 * pool's limit is non-zero and has been reached) until a new connection
	 * can be created. 
	 * @throws IOException 
	 */
	private SocketChannel getNewChannel(HttpRpcClient client) throws IOException {
		Object key = createClientKey(client);
		
		if (log.logTrace()) {
			log.trace("Creating new socket channel for [" + key + "]");
		}
		
		SocketChannel channel = this.newChannel(client);
		PooledSocketChannel pooledChannel = new PooledSocketChannel(client, channel, key);
		this.channelMap.put(channel, pooledChannel);
		
		Set activeSet = (Set) this.activeConnSets.get(key);
		if (activeSet == null) {
			activeSet = new HashSet();
			this.activeConnSets.put(key, activeSet);
		}
		activeSet.add(pooledChannel);
		this.activeConnections++;
		
		if (!this.pooledConnStacks.containsKey(key)) {
			this.pooledConnStacks.put(key, new ArrayList());
		}
		
		return channel;
	}

	private SocketChannel newChannel(HttpRpcClient client) throws IOException {
		// Create a non-blocking socket channel
		SocketChannel clientChannel = SocketChannel.open();
		clientChannel.configureBlocking(false);

		// Send a connection request to the server; this method is non-blocking
		int port = client.getURL().getPort();
		if (port == -1) {
			port = client.getURL().getDefaultPort();
		}
		clientChannel.connect(new InetSocketAddress(client.getURL().getHost(), port));

		// Register the new channel with our selector
		client.register(clientChannel);

		return clientChannel;
	}

	private void removeExpiredChannels() {
		while(true) {
			if (this.channelHeap.isEmpty()) {
				break;
			}
			
			PooledSocketChannel pooledChannel = (PooledSocketChannel) this.channelHeap.getSmallest();
			
			// Because this is an ordered heap, we're done as soon as we encounter 
			// the first entry that is younger than our expiration age.
			if (pooledChannel.getAge() < 5000) {
				break;
			}
			
			// Everything else is too old and must be expired. But first, remove
			// it from our heap.
			this.channelHeap.removeSmallest();
			
			// Fetch the related connection stack and remove this channel
			List connStack = (List) this.pooledConnStacks.get(pooledChannel.getPoolingKey());
			connStack.remove(pooledChannel);
			
			if (log.logTrace()) {
				log.trace("Closing expired pooled socket channel (age="
						+ pooledChannel.getAge() + "ms)");
			}
			
			this.closePooledChannel(pooledChannel);
		}
	}

		// Pull out the LRU pooled channel
	private void removeLeastRecentlyUsedChannel() {
		PooledSocketChannel pooledChannel = (PooledSocketChannel) this.channelHeap.removeSmallest();
		
		// Fetch the related connection stack and remove this channel
		List connStack = (List) this.pooledConnStacks.get(pooledChannel.getPoolingKey());
		connStack.remove(pooledChannel);

		// And finally, actually close the connection. This also
		// removes the physical->pooled channel mapping.
		this.closePooledChannel(pooledChannel);
	}
	
	private SocketChannel checkOut(HttpRpcClient client) {
		if (this.availableConnections > 0) {
			this.removeExpiredChannels();
		}
		
		Object key = createClientKey(client);

		if (log.logTrace()) {
			log.trace("Checking out pooled socket channel for [" + key + "]");
		}

		List pooledStack = (List) this.pooledConnStacks.get(key);
		if (pooledStack == null || pooledStack.isEmpty()) {
			// No pooled channel available
			return null;
		}

		int lastIdx = pooledStack.size() - 1;
		PooledSocketChannel pooledChannel = (PooledSocketChannel) pooledStack.remove(lastIdx);
		this.availableConnections--;
		
		// Update our record of ownership and register this channel
		// so Selector operations are routed to the new owner.
		pooledChannel.setOwner(client);
		client.registerChannel(pooledChannel.getPhysicalConnection());
		
		Set activeSet = (Set) this.activeConnSets.get(key);
		activeSet.add(pooledChannel);
		this.activeConnections++;
		
		this.channelHeap.removeValue(pooledChannel);
		
		SocketChannel channel = pooledChannel.getPhysicalConnection();
		// Vet the channel: if it's been closed then it's no use to us
		// TODO: We could be a little more efficient and try other pooled
		// connections here before giving up.
		if (!channel.isOpen()) {
			this.closePooledChannel(pooledChannel);
			return null;
		}
		
		return pooledChannel.getPhysicalConnection();
	}
	
	private void checkIn(HttpRpcClient client, PooledSocketChannel pooledChannel) {
		Object key = pooledChannel.getPoolingKey();

		if (log.logTrace()) {
			log.trace("Checking in pooled socket channel for [" + key + "]");
		}
		
		// Deregister this channel so Selector operations are not routed to the 
		// previous owner.
		pooledChannel.getOwner().deregisterChannel(pooledChannel.getPhysicalConnection());
		pooledChannel.setOwner(null);

		List pooledStack = (List) this.pooledConnStacks.get(key);
		pooledStack.add(pooledChannel);
		this.availableConnections++;

		Set activeSet = (Set) this.activeConnSets.get(key);
		activeSet.remove(pooledChannel);
		this.activeConnections--;

		this.channelHeap.insertUnordered(pooledChannel.getAccessTime(), pooledChannel);
	}
	
	private void closePooledChannel(PooledSocketChannel pooledChannel) {
		this.channelMap.remove(pooledChannel.getPhysicalConnection());
		try {
			HttpRpcClient owner = pooledChannel.getOwner();
			if (owner != null) {
				owner.cancel(pooledChannel.getPhysicalConnection());
			}
			pooledChannel.getPhysicalConnection().close();
		} catch (IOException e) {
			log.warn("Exception closing pooled connection", e);
		} finally {
			this.availableConnections--;
		}
	}
	
	private void closeActiveChannel(PooledSocketChannel pooledChannel) {
		this.channelMap.remove(pooledChannel.getPhysicalConnection());
		try {
			HttpRpcClient owner = pooledChannel.getOwner();
			if (owner != null) {
				owner.cancel(pooledChannel.getPhysicalConnection());
			}
			pooledChannel.getPhysicalConnection().close();
		} catch (IOException e) {
			log.warn("Exception closing active connection", e);
		} finally {
			this.activeConnections--;
		}
	}
}
