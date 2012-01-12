package com.flat502.rox.processing;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.Map.Entry;

import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.utils.Utils;

class ChannelSelector implements Runnable {
	private static Log log = LogFactory.getLog(ChannelSelector.class);

	private boolean shouldShutdown;
	private Object mutex = new Object();
	private Set processors = new HashSet();
	private Map channelOwners = new HashMap();

	// A local buffer used when we read() to check for remote closure
	private ByteBuffer closureTestBuf = ByteBuffer.allocate(16);
	
	private Selector socketSelector;
	private ResourcePool resourcePool;

	public ChannelSelector(ResourcePool pool) throws IOException {
		this.socketSelector = SelectorProvider.provider().openSelector();
		this.resourcePool = pool;
	}

	protected Selector getSocketSelector() {
		return this.socketSelector;
	}

	protected void register(HttpRpcProcessor processor) {
		if (log.logTrace()) {
			log.trace("ChannelSelector registers " + processor);
		}
		
		synchronized (this.mutex) {
			this.processors.add(processor);
		}
	}

	protected void deregister(HttpRpcProcessor processor) throws IOException {
		if (log.logTrace()) {
			log.trace("ChannelSelector deregisters " + processor);
		}
		
		synchronized (this.mutex) {
			this.processors.remove(processor);
			
			// Cleaning up all channels associated with this processor.
			Iterator i = this.channelOwners.entrySet().iterator();
			while (i.hasNext()) {
				Entry entry = (Map.Entry) i.next();
				if (processor == entry.getValue()) {
					i.remove();
					
					// Physically close the associated channel
					((SelectableChannel)entry.getKey()).close();
				}
			}

			// Ensure the associated processor has a chance to process
			// pending changes (e.g. ServerChannel cancellation).
			try {
				processor.processPendingSelectorChanges();
			} catch (IOException e) {
				processor.handleProcessingException(null, e);
			}
		}

		// Kick the selector thread so it notices this change
		this.socketSelector.wakeup();
	}

	protected void addChannel(HttpRpcProcessor processor, SelectableChannel channel) {
		synchronized (this.mutex) {
			this.channelOwners.put(channel, processor);
		}
	}

	protected void removeChannel(SelectableChannel channel) {
		synchronized (this.mutex) {
			this.channelOwners.remove(channel);
		}
	}

	public void shutdown() {
		this.shouldShutdown = true;
		this.socketSelector.wakeup();
	}

	public void run() {
		while (true) {
			if (this.shouldShutdown) {
				break;
			}

			try {
				this.processPendingSelectorChanges();

				if (log.logTrace()) {
					log.trace(resourcePool.getClass().getSimpleName()+": select() call");
				}
				this.socketSelector.select();
				if (log.logTrace()) {
					log.trace("select() returns");
				}

				Set readyKeys = this.socketSelector.selectedKeys();
				if (readyKeys.isEmpty()) {
					// We were woken up or interrupted.
					if (log.logTrace()) {
						log.trace("select() returned empty key set");
					}
					continue;
				}

				if (log.logTrace()) {
					log.trace("select() returned " + readyKeys.size() + " selected key(s)");
				}

				// Someone is ready for I/O, get the ready keys
				Iterator i = readyKeys.iterator();

				// Process the next event
				while (i.hasNext()) {
					SelectionKey key = (SelectionKey) i.next();
					i.remove();

					if (log.logTrace()) {
						log.trace("select() returned key: " + Utils.toString(key));
					}
					
					HttpRpcProcessor processor = null;
					if (key.isValid()) {
						// Decide who should handle it
						synchronized (this) {
							processor = (HttpRpcProcessor) this.channelOwners.get(key.channel());
						}

						if (processor == null) {
							// There's no owner associated with this channel. This probably
							// means a pooled connection has been closed remotely.
							this.checkForClosure(key);
						} else {
							processor.handleSelectionKeyOperation(key);
						}
					}
				}
			} catch (Exception e) {
				this.handleProcessingException(e);
			}
		}

		log.debug(Thread.currentThread().getName() + " shutting down");

		try {
			this.socketSelector.close();
		} catch (IOException e) {
			log.warn("Error closing socket channel", e);
		}
	}

	private void checkForClosure(SelectionKey key) {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		if (log.logTrace()) {
			log.trace("Checking for possible closure of pooled connection " + System.identityHashCode(socketChannel.socket()));
		}
		
		if (socketChannel.isConnectionPending()) {
			if (log.logTrace()) {
				log.trace("Connection pending on pooled connection" + System.identityHashCode(socketChannel.socket()));
			}
			return;
		}
		
		int numRead;
		boolean closed = false;
		try {
			numRead = socketChannel.read(this.closureTestBuf);
			
			if (numRead <= 0) {
				// Remote entity shut the socket down cleanly. Do the
				// same from our end and cancel the channel.
				// Note: We do this for a read of zero bytes too because this is an
				// unowned pooled connection and we've seen a socket get into a state
				// where it's readable and returns zero from reads continuously.
				// This won't hurt anyone, at worst we close a pooled connection.
				if (log.logTrace()) {
					log.trace("Remote entity has cleanly closed a pooled connection: numRead=" + numRead);
				}
				closed = true;
				try {
					socketChannel.close();
				} catch(IOException e2) {
					log.trace("Error returning the favour (closing socket channel)", e2);
				}
			} else {
				if (log.logTrace()) {
					log.trace("checkForClosure: read " + numRead + " byte(s) from the remote server:\n"
							+ Utils.toHexDump(this.closureTestBuf.array(), 0, numRead));
				}
			}
		} catch (NotYetConnectedException e) {
			if (log.logTrace()) {
				log.trace("Connection pending on pooled connection" + System.identityHashCode(socketChannel.socket()), e);
			}
			return;
		} catch (IOException e) {
			// The remote entity probably forcibly closed the connection.
			if (log.logTrace()) {
				log.trace("Remote entity appears to have forcibly closed a pooled connection", e);
			}
			closed = true;
		}
		
		if (closed) {
			this.resourcePool.notifyUnownedChannelClosure(socketChannel);
			key.cancel();
		}
	}

	private void processPendingSelectorChanges() {
		synchronized (this.mutex) {
			Iterator i = processors.iterator();
			while (i.hasNext()) {
				HttpRpcProcessor processor = (HttpRpcProcessor) i.next();
				try {
					processor.processPendingSelectorChanges();
				} catch (IOException e) {
					processor.handleProcessingException(null, e);
				}
			}
		}
	}

	private void handleProcessingException(Exception e) {
		synchronized (this.mutex) {
			Iterator i = processors.iterator();
			while (i.hasNext()) {
				HttpRpcProcessor processor = (HttpRpcProcessor) i.next();
				processor.handleProcessingException(e);
			}
		}
	}
}
