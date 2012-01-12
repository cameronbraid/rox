package com.flat502.rox.processing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.regex.Pattern;

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import com.flat502.rox.http.HttpMessageBuffer;
import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.server.SSLSessionPolicy;
import com.flat502.rox.utils.BlockingQueue;
import com.flat502.rox.utils.Profiler;
import com.flat502.rox.utils.ProfilerCollection;
import com.flat502.rox.utils.Utils;

/**
 * This abstract base class encapsulates all of the generic logic common
 * to both client and server side communication using RPC over HTTP.
 * <p>
 * This particular implementation uses a single {@link java.lang.Thread}
 * (this class) to manage all I/O on the underlying collection of
 * sockets. I/O is managed using a {@link java.nio.channels.Selector}
 * instance. This thread may be shared with other instances by means
 * of a {@link com.flat502.rox.processing.ResourcePool}.
 * <p>
 * I/O writes are queued and written whenever the target socket becomes
 * available for writing. I/O reads are buffered in an instance of
 * {@link com.flat502.rox.http.HttpMessageBuffer} until a complete
 * message is available (as determined by the
 * {@link com.flat502.rox.http.HttpMessageBuffer#isComplete()} method.
 * <p>
 * When a complete message has been received it is placed on a shared
 * queue serviced by a pool of {@link #addWorker() worker} threads,
 * themselves instances of 
 * {@link com.flat502.rox.processing.HttpMessageHandler} created
 * through calls to the {@link com.flat502.rox.processing.ResourcePool#newWorker()} factory method
 * on the underlying {@link com.flat502.rox.processing.ResourcePool}.
 * A worker pool may be provided when an instance of this class is created.
 * This allows multiple instances to share an underlying worker pool.
 * <p>
 * All I/O and any state changes on the underlying 
 * {@link java.nio.channels.Selector} (like updates to interest operations on
 * channels, or new channel registrations) are handled directly by the
 * thread backing this instance to avoid unexpected blocking due to platform 
 * inconsistencies in the underlying NIO implementation.
 */
public abstract class HttpRpcProcessor {
	private static Log log = LogFactory.getLog(HttpRpcProcessor.class);
	
	/**
	 * A regular expression that matches only cipher suites that
	 * allow for anonymous key exchange.
	 * @deprecated use {@link SSLConfiguration#ANON_CIPHER_SUITES} instead.
	 */
	public static final String ANON_CIPHER_SUITES = SSLConfiguration.ANON_CIPHER_SUITES;

	/**
	 * A regular expression that matches all cipher suites.
	 * @deprecated use {@link SSLConfiguration#ALL_CIPHER_SUITES} instead.
	 */
	public static final String ALL_CIPHER_SUITES = SSLConfiguration.ALL_CIPHER_SUITES;

	private static final String CLOSE_AFTER_WRITE = "CloseAfterWrite";

	// Implementation note: Win32 NIO implementations have had 
	// problems in the past if OP_READ and OP_WRITE are set at
	// the same time. Interleaving them solves the problem.
	private static final Integer OP_WRITE = new Integer(SelectionKey.OP_WRITE);
	private static final Integer OP_READ = new Integer(SelectionKey.OP_READ);

	// A local buffer for all non-blocking I/O read operations.
	private ByteBuffer nonBlockingReadBuf = ByteBuffer.allocate(8192);

	// A local buffer for all blocking I/O read operations.
	private byte[] blockingReadBuf = new byte[8192];

	// An empty buffer used as the source buffer for wrap() operations during
	// SSL handshakes.
	private ByteBuffer BLANK = ByteBuffer.allocate(0);

	// The worker pool for this instance. null until one is
	// configured or addWorker is called (in which case a default
	// is used).
	private Object workerPoolMutex = new Object();
	private boolean sharedWorkerPool;
	private ResourcePool resourcePool;

	private ChannelSelector channelSelector;
	
	private SSLSessionPolicy sslSessionPolicy;
	
	private ProfilerCollection profilers = new ProfilerCollection();

	// The shared queue events should be delivered via.
	private BlockingQueue queue;
	
	// The Selector worker threads wait on for
	// socket events.
	private Selector socketSelector;

	// Maps Socket to OP_WRITE or OP_READ.
	private Map pendingInterestOps = new LinkedHashMap();

	private Set pendingRegistrations = new HashSet();
	private Set pendingCancellations = new HashSet();

	private Map sslEngineMap = new HashMap();
	private SSLContext sslContext;

	// true if we're using SSL
	private boolean useHttps = true;

	// Non-null if we're using SSL
	private SSLConfiguration sslConfig;
	
	private boolean started;

	private boolean shouldShutdown;

	/**
	 * Initializes a new instance of this class.
	 * @param useHttps
	 * 	A flag indicating whether or not the underlying
	 * 	transport should use HTTPS (HTTP over SSL).
	 * @see #setCipherSuitePattern(String)
	 */
	// TODO: Document extra param
	protected HttpRpcProcessor(boolean useHttps, ResourcePool workerPool) throws IOException {
		this(useHttps, workerPool, null);
	}
	
	protected HttpRpcProcessor(boolean useHttps, ResourcePool workerPool, SSLConfiguration sslCfg) throws IOException {
		this.useHttps = useHttps;
		if (sslCfg != null) {
			this.initHttps(sslCfg);
		}
		
		if (workerPool == null) {
			this.resourcePool = this.newWorkerPool();
			this.sharedWorkerPool = false;
		} else {
			this.resourcePool = workerPool;
			this.sharedWorkerPool = true;
		}
		
		this.queue = this.resourcePool.getQueue();
	}
	
	private void initHttps(SSLConfiguration config) throws SSLException {
		this.sslConfig = config;
		
		if (log.logDebug()) {
			log.debug("Initializing SSL:\n" + config);
		}

		try {
			this.sslContext = this.sslConfig.createContext();
		} catch (Exception e) {
			throw (SSLException) new SSLException("Failed to initialize SSL context").initCause(e);
		}
	}

	/**
	 * Create a new worker instance and add it to the underlying
	 * thread pool.
	 * <p>
	 * Worker threads are responsible for handling complete
	 * HTTP messages. All I/O is handled by this thread instance
	 * alone.
	 * <p>
	 * If an instance of this class is constructed and started
	 * without this method having been invoked it will be invoked
	 * before processing begins.
	 * @return
	 * 	The number of worker threads backing this instance.
	 */
	public int addWorker() {
		synchronized(this.workerPoolMutex) {
			return this.resourcePool.addWorker();
		}
	}

	/**
	 * A convenience method for adding multiple worker threads
	 * in a single call.
	 * @param count
	 * 	The number of worker threads to add.
	 * @return
	 * 	The number of worker threads backing this instance.
	 */
	public int addWorkers(int count) {
		synchronized(this.workerPoolMutex) {
			return this.resourcePool.addWorkers(count);
		}
	}

	/**
	 * Get the number of worker threads currently
	 * responsible for this instance.
	 * @return
	 * 	The number of worker threads backing this instance.
	 */
	public int getWorkerCount() {
		synchronized(this.workerPoolMutex) {
			return this.resourcePool.getWorkerCount();
		}
	}
	
	/**
	 * Removes a worker thread from the thread pool.
	 * <p>
	 * It's possible to reduce the size of the thread
	 * pool to zero, stopping all HTTP message
	 * processing. Incoming data will still be accepted,
	 * as will new connections (if this applies), but
	 * no processing will occur until the thread pool
	 * size is increased again.
	 * @throws IllegalStateException
	 * 	if this method is invoked while the thread pool
	 * 	is empty.
	 * @return
	 * 	The number of worker threads backing this instance.
	 */
	public int removeWorker() {
		synchronized(this.workerPoolMutex) {
			return this.resourcePool.removeWorker();
		}
	}

	/**
	 * Set the regular expression used to select the SSL cipher
	 * suites to use for all connections from this point on.
	 * @param cipherSuitePattern
	 * 	A regular expression for selecting the 
	 * 	set of SSL cipher suites. A <code>null</code> value
	 * 	will treated as matching <i>all</i> cipher suites.
	 * @throws IllegalStateException
	 * 	If this instance was not configured to use HTTPS.
	 * @see #ALL_CIPHER_SUITES
	 * @see #ANON_CIPHER_SUITES
	 */
	public void setCipherSuitePattern(String cipherSuitePattern) {
		if (!this.useHttps) {
			throw new IllegalStateException("This instance is not configured to use HTTPS");
		}

		this.sslConfig.setCipherSuitePattern(cipherSuitePattern);
	}

	/**
	 * Configure a timeout value for SSL handshaking.
	 * <p>
	 * If the remote server is not SSL enabled then it falls
	 * to some sort of timeout to determine this, since a non-SSL server
	 * is waiting for a request from a client, which is in turn waiting
	 * for an SSL handshake to be initiated by the server.
	 * <p>
	 * This method controls the length of that timeout.
	 * <p>
	 * This timeout defaults to 10 seconds. 
	 * <p>
	 * The new timeout affects only connections initiated subsequent to the
	 * completion of this method call.
	 * @param timeout
	 * 	The timeout (in milliseconds). A value of 0 indicates no timeout should be
	 * 	enforced (not recommended).
	 * @throws IllegalArgumentException
	 * 	If the timeout provided is negative.
	 */
	public void setSSLHandshakeTimeout(int timeout) {
		if (!this.useHttps) {
			throw new IllegalStateException("This instance is not configured to use HTTPS");
		}

		this.sslConfig.setHandshakeTimeout(timeout);
	}

//	public void configureSSL(SSLConfiguration config) throws SSLException {
//		if (!this.useHttps) {
//			throw new IllegalStateException("This instance is not configured to use HTTPS");
//		}
//		
//		this.initHttps(config);
//	}
	
	public SSLConfiguration getSSLConfiguration() {
		return this.sslConfig;
	}
	
	protected SSLSession newSSLSession(Socket socket) {
		javax.net.ssl.SSLSession session = this.getSSLSession(socket);
		if (session == null) {
			return null;
		}
		return new SSLSession(session);
	}

	protected javax.net.ssl.SSLSession getSSLSession(Socket socket) {
		SSLSessionMetadata sessionMetadata = (SSLSessionMetadata) this.sslEngineMap.get(socket);
		if (sessionMetadata == null) {
			return null;
		}
		return sessionMetadata.engine.getSession();
	}

	protected boolean isStarted() {
		synchronized(this.workerPoolMutex) {
			return this.started;
		}
	}

	public void start() throws IOException {
		synchronized(this.workerPoolMutex) {
			if (this.started) {
				throw new IllegalStateException("Already started");
			}

			if (this.useHttps && this.sslConfig == null) {
				// Use a default configuration
				try {
					this.initHttps(new SSLConfiguration(System.getProperties()));
				} catch (SSLException e) {
					throw (IOException)new IOException("Default SSL Initialization Failed").initCause(e);
				} catch (GeneralSecurityException e) {
					throw (IOException)new IOException("Default SSL Initialization Failed").initCause(e);
				}
			}
			
			// Always make sure there's at least one worker. The
			// WorkerPool class will "swallow" the first addWorker()
			// to provide the "at least one but however many you said
			// if you said any" semantics.
			if (this.getWorkerCount() == 0) {
				this.addWorker();
			}

			this.resourcePool.startProcessingThread();
			this.started = true;
		}
	}

	// TODO: Document delayed resource release:
	// There is a small delay between stopping the server and the listening socket
	// actually being released to the OS. It only happens when the selecting thread
	// performs it's next select() operation, which is almost immediately but asynchronous.
	// See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5073504
	public void stop() throws IOException {
		synchronized(this.workerPoolMutex) {
			if (!this.sharedWorkerPool) {
				this.resourcePool.shutdown();
			}
			// This will call channelSelector.deregister() for client instances
			// (indirectly, as part of detaching from the resource pool).
			this.stopImpl();

			this.shouldShutdown = true;
			
			// But this is still requires for server instances
			// TODO: Move into stopImpl on the server?
			this.channelSelector.deregister(this);
			
			// Wake up the selecting thread so it notices this channel should
			// be deregistered.
			this.socketSelector.wakeup();
		}
	}

	protected abstract void stopImpl() throws IOException;
	
	protected boolean shouldUseHTTPS() {
		return this.useHttps;
	}
	
	void processPendingSelectorChanges() throws IOException {
		// Process any queued channel registrations
		synchronized (this.pendingRegistrations) {
			if (this.pendingRegistrations.size() > 0) {
				Iterator channels = this.pendingRegistrations.iterator();
				while (channels.hasNext()) {
					SocketChannel channel = (SocketChannel) channels.next();
					boolean wq = isWriteQueued(channel.socket());
					if (log.logTrace()) {
						log.trace(
								"Registering channel (wq=" + wq + ") for " + Utils.toString(channel.socket()));
					}
					if (channel.isConnected()) {
						// A registration is re-queued after an SSL I/O 
						// operation to avoid a CancelledKeyException.
						// In that case we are interested in reads, not
						// connections.
						if (log.logTrace()) {
							log.trace("Interest ops change to OP_READ for " + Utils.toString(channel.socket()));
						}
						channel.register(this.getSocketSelector(), SelectionKey.OP_READ);
					} else {
						if (log.logTrace()) {
							log.trace("Interest ops change to OP_CONNECT for " + Utils.toString(channel.socket()));
						}
						channel.register(this.getSocketSelector(), SelectionKey.OP_CONNECT);
					}
				}
				this.pendingRegistrations.clear();
			}
		}

		// Process any queued interestOps updates.
		synchronized (this.pendingInterestOps) {
			if (this.pendingInterestOps.size() > 0) {
				Iterator entries = this.pendingInterestOps.entrySet().iterator();
				while (entries.hasNext()) {
					Map.Entry entry = (Map.Entry) entries.next();
					Socket socket = (Socket) entry.getKey();
					SelectionKey sk = socket.getChannel().keyFor(this.getSocketSelector());
					if (socket.getChannel().isConnected()) {
						// Only update the interest ops set if we're not
						// waiting to complete the connection (otherwise we
						// disable the OP_CONNECT interest op and never see
						// the connection complete).
						if (sk != null && sk.isValid()) {
							int ops = ((Integer) entry.getValue()).intValue();
							if (log.logTrace()) {
								log.trace("Interest ops change for " + Utils.toString(socket) + ": " + (ops == OP_READ ? "OP_READ" : "OP_WRITE"));
							}
							sk.interestOps(ops);
						}
					}
				}
				this.pendingInterestOps.clear();
			}
		}

		// Process any queued channel cancellations
		synchronized (this.pendingCancellations) {
			if (this.pendingCancellations.size() > 0) {
				Iterator channels = this.pendingCancellations.iterator();
				while (channels.hasNext()) {
					SelectableChannel channel = (SelectableChannel) channels.next();
					boolean client = channel instanceof SocketChannel;
					boolean connected = (client && ((SocketChannel)channel).isConnected());
					if (!client || connected) {
						channel.close();
						SelectionKey key = channel.keyFor(this.getSocketSelector());
						if (key != null) {
							key.cancel();
						}
					}
					if (log.logTrace()) {
						if (client) {
							log.trace("Cancellation on socket " + Utils.toString(((SocketChannel)channel).socket()));
						} else {
							log.trace("Cancellation on serverSocket " + Utils.toString(((ServerSocketChannel)channel).socket()));
						}
					}

				}
				this.pendingCancellations.clear();
			}
		}
	}

	void processSelectionKey(SelectionKey key) throws IOException {
		try {
			this.handleSelectionKeyOperation(key);
		} catch (IOException e) {
			SocketChannel socketChannel = (SocketChannel) key.channel();
			this.deregisterSocket(socketChannel.socket());
			//		this.queueCancellation(socketChannel);
			key.cancel();
			socketChannel.close();

			this.handleProcessingException(socketChannel.socket(), e);
		}
	}
	
	// TODO: Better name to differentiate from overloaded method?
	void handleProcessingException(Exception e) {
		if (e instanceof ClosedSelectorException) {
			if (!this.shouldShutdown) {
				this.handleProcessingException(null, e);
			}
		} else {
			this.handleProcessingException(null, e);
		}
	}

	/**
	 * Initializes this implementation.
	 * <p>
	 * Sub-classes <i>must</i> invoke this method after their
	 * constructor has completed it's initialization.
	 * <p>
	 * This implementation invokes {@link #initSelector(Selector)}
	 * to initialize the underlying {@link Selector}.
	 * @throws IOException
	 * 	if an error occurs during initialization.
	 */
	protected void initialize() throws IOException {
		this.channelSelector = this.resourcePool.getChannelSelector();
		this.channelSelector.register(this);
		this.socketSelector = this.channelSelector.getSocketSelector();
		this.initSelector(this.socketSelector);
	}

	/**
	 * Queue's a new {@link SocketChannel} for registration
	 * with the underlying {@link Selector}.
	 * <p>
	 * The update is queued internally and the selecting thread is
	 * awoken to apply the change. This removes any risk of platform
	 * specific NIO implementation discrepancies from blocking
	 * indefinitely.
	 * @param channel
	 * 	The {@link SocketChannel} to register.
	 */
	protected void queueRegistration(SocketChannel channel) {
		synchronized (this.pendingRegistrations) {
			this.pendingRegistrations.add(channel);
		}
		this.getSocketSelector().wakeup();
	}

	protected void queueCancellation(AbstractSelectableChannel channel) {
		synchronized (this.pendingCancellations) {
			this.pendingCancellations.add(channel);
		}
		this.getSocketSelector().wakeup();
	}
	
	protected Timer getTimer() {
		synchronized(this.workerPoolMutex) {
			return this.resourcePool.getTimer();
		}
	}
	
	/**
	 * Returns a handle to the shared queue used
	 * by worker threads.
	 * @return
	 * 	A handle to the shared queue.
	 */
	protected BlockingQueue getQueue() {
		return this.queue;
	}

	/**
	 * Returns a handle to the {@link Selector} this
	 * thread is using for all I/O.
	 * @return
	 * 	A handle to the {@link Selector}.
	 */
	protected Selector getSocketSelector() {
		return this.socketSelector;
	}

	/**
	 * Central dispatch routine for handling I/O
	 * events on the underlying {@link Selector}.
	 * <p>
	 * This implementation defers to the 
	 * {@link #read(SelectionKey)} or {@link #write(SelectionKey)}
	 * method appropriately.
	 * <p>
	 * If a sub-class overrides this method it should defer to
	 * it if it is not interested in the {@link SelectionKey}
	 * presented.
	 * @param key
	 * 	The {@link SelectionKey} for the socket on which
	 * 	an I/O operation is pending.
	 * @throws IOException
	 * 	if an error occurs while processing the pending event.
	 */
	protected void handleSelectionKeyOperation(SelectionKey key) throws IOException {
		if (key.isValid() && key.isReadable()) {
			this.read(key);
		} else if (key.isValid() && key.isWritable()) {
			this.write(key);
		}
	}

	/**
	 * Writes any pending data to the socket indicated by
	 * the given {@link SelectionKey}.
	 * <p>
	 * This implementation checks for data using the
	 * {@link #getWriteBuffer(Socket)} method. If data
	 * is available as much as possible is written
	 * to the socket.
	 * @param key
	 * 	The {@link SelectionKey} indicating the socket
	 * 	available for writing.
	 * @throws IOException
	 * 	If an error occurs while attempting to write to
	 * 	the indicated socket.
	 */
	protected void write(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		Socket socket = socketChannel.socket();
		ByteBuffer buf = null;

		// If we're using HTTPS and handshaking is still happening then we need to call 
		// SSLEngine.wrap() which will write the next chunk of handshake data.
		if (this.useHttps && this.isHandshaking(socket)) {
			if (log.logTrace()) {
				log.trace(this.getClass().getSimpleName() + ": write(): still handshaking for " + Utils.toString(socket));
			}

			try {
				this.progressSSLHandshake(key);
			} catch(SSLException e) {
				safeClose(key, socketChannel, "SSL handshake error during write()", e);
				this.handleSSLHandshakeFailed(socket);
				handleProcessingException(socket, e);
			}

			return;
		}

		// Get the next chunk of application data to write, if we're using
		// HTTPS we'll encrypt it a little further down.
		while(true) {
			buf = this.getWriteBuffer(socket);
			
			if (buf == null) {
				// A second OP_WRITE was queued at some point. This happens
				// because multiple threads (the selector and the caller's
				// original "write" thread) can both request an OP_WRITE
				// interest op change. Rather than trying to coordinate their
				// efforts we gracefully handle the case where this happens
				// and just ignore the fact that there's nothing to write.
				if (log.logTrace()) {
					log.trace(this.getClass().getSimpleName() + ": Ignoring write() call with no write buffer queued on ["
							+ Utils.toString(socket) + "]");
				}
	
				key.interestOps(SelectionKey.OP_READ);
				return;
			}
			
			if (this.useHttps) {
				buf = this.encryptWriteBuffer(socket, buf);
			}
	
			try {
				if (writeBuffer(key, socketChannel, buf)) {
					// All data was successsfully written
					this.processDataWritten(key, socket);
				}
			} catch(IOException e) {
				// An error occurred
				safeClose(key, socketChannel, "write() failed", e);
			}
		}
	}

	private boolean writeBuffer(SelectionKey key, SocketChannel socketChannel, ByteBuffer buf) throws IOException {
		Socket socket = socketChannel.socket();
		
		if (log.logTrace()) {
			byte[] d = buf.array();
			log.trace(this.getClass().getSimpleName() +
					": Writing " + buf.remaining() + " byte(s) on " + Utils.toString(socket)
							+ ":\n" + Utils.toHexDump(d, buf.position(), buf.remaining()));
		}

		int numWritten = socketChannel.write(buf);
		if (log.logTrace()) {
			log.trace(this.getClass().getSimpleName() + ": Wrote " + numWritten + " byte(s) on " + Utils.toString(socket)
					+ ", " + buf.remaining() + " remaining");
		}
		
		return (buf.remaining() == 0);
	}

	/**
	 * Reads any pending data from the socket indicated by
	 * the given {@link SelectionKey}.
	 * <p>
	 * This implementation retrieves an
	 * {@link HttpMessageBuffer} instance for the 
	 * indicated socket using the {@link #getReadBuffer(Socket)}
	 * method. Data present on the socket is added to
	 * this buffer and if a complete HTTP message has
	 * been received it is enqueued on the 
	 * {@link #getQueue() shared queue}.
	 * @param key
	 * 	The {@link SelectionKey} indicating the socket
	 * 	available for writing.
	 * @throws IOException
	 * 	If an error occurs while attempting to read from
	 * 	the indicated socket.
	 */
	protected void read(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		Socket socket = socketChannel.socket();

		if (this.useHttps && this.isHandshaking(socket)) {
			if (log.logTrace()) {
				log.trace(this.getClass().getSimpleName() + ": read(): still handshaking for " + Utils.toString(socket));
			}
			
			try {
				this.progressSSLHandshake(key);
			} catch(SSLException e) {
				safeClose(key, socketChannel, "SSL handshake error during read()", e);
				handleProcessingException(socket, e);
			}
			
			return;
		}

		HttpMessageBuffer httpMsg = this.getReadBuffer(socket);
		
		ByteBuffer readBuf = this.nonBlockingReadBuf;

		int numRead;
		try {
			numRead = readBuffer(key, socketChannel, readBuf);
		} catch(Exception e) {
			readBuf.clear();
			this.handleMessageException(httpMsg, e);
			return;
		}
		
		if (numRead == 0) {
			readBuf.clear();
			return;
		}

		if (this.useHttps) {
			ByteBuffer decryptedBuf = null;
			try {
				while (readBuf.hasRemaining()) {
					// unwrap() (decrypt) the message
					decryptedBuf = this.decryptReadBuffer(socket, readBuf);
					this.processReadData(httpMsg, decryptedBuf.array(), decryptedBuf.remaining());
				}
			} catch (SSLException e) {
				if (decryptedBuf != null) {
					decryptedBuf.clear();
				}
	
				// The remote entity probably forcibly closed the connection.
				// Nothing to see here. Move on.
				safeClose(key, socketChannel, "SSL decryption failed", e);
				
				this.handleMessageException(httpMsg, e);
				return;
			}
		} else {		
			this.processReadData(httpMsg, readBuf.array(), readBuf.remaining());
		}
		
		// Clear our read buffer after we've handled the data instead of before
		// we read so that during the SSL handshake we can deal with the BUFFER_UNDERFLOW
		// case by simple waiting for more data (which will be appended into this buffer).
		readBuf.clear();
	}
	
	private int readBuffer(SelectionKey key, SocketChannel socketChannel, ByteBuffer readBuf) throws IOException {
		Socket socket = socketChannel.socket();

		int numRead;
		try {
			log.trace(this.getClass().getSimpleName() + ": ENTERING read(): "+readBuf+": "+socketChannel.isBlocking());
			numRead = socketChannel.read(readBuf);
			log.trace(this.getClass().getSimpleName() + ": EXITING read() = "+numRead);
		} catch (IOException e) {
			// The remote entity probably forcibly closed the connection.
			// Nothing to see here. Move on.
			safeClose(key, socketChannel, "SocketChannel.read() exception cancels connection", e);
			
			throw e;
		}
		
		if (numRead == -1) {
			// Remote entity shut the socket down cleanly. Do the
			// same from our end and cancel the channel.
			safeClose(key, socketChannel, "SocketChannel.read() returned EOF", null);

			// The caller needs to be notifed. Although this is
			// a "clean" close from the caller's perspective this
			// is unexpected. So we manufacture an exception.
			throw new RemoteSocketClosedException("Remote entity closed connection");
		}
		
		readBuf.flip();
		
		// Make sure the limit on the buffer reflects what we read off the wire.
		// It's not enough to simply flip the buffer, since if the position was at x > 0
		// and we read nothing off the wire we end up with pos = 0 and limit = x
		readBuf.limit(numRead);
		
		if (log.logTrace()) {
			byte[] d = readBuf.array();
			log.trace(this.getClass().getSimpleName() + 
					": Read " + numRead + " byte(s) on " + Utils.toString(socket)
							+ ":\n" + Utils.toHexDump(d, readBuf.position(), readBuf.remaining()));
		}
		
		return numRead;
	}
	
	private void safeClose(SelectionKey key, SocketChannel socketChannel, String traceMsg, Exception cause) {
		if (log.logTrace()) {
			log.trace(traceMsg, cause);
		}
		
		//this.queueCancellation(socketChannel);
		key.cancel();
		try {
			socketChannel.close();
		} catch(IOException e2) {
			log.trace(traceMsg + ": close() failed", e2);
		}
		
		// This will clean up associated buffers and SSL engines too
		this.deregisterSocket(socketChannel.socket());
	}
	
	private ByteBuffer encryptWriteBuffer(Socket socket, ByteBuffer buffer) throws SSLException {
		if (log.logTrace()) {
			log.trace("Encrypting " + buffer.remaining() + " byte(s) for " + Utils.toString(socket));
		}
		
		SSLSessionMetadata sessionMetadata = (SSLSessionMetadata) this.sslEngineMap.get(socket);
		
		sessionMetadata.netBuffer.clear();
		SSLEngineResult result = sessionMetadata.engine.wrap(buffer, sessionMetadata.netBuffer);
		sessionMetadata.netBuffer.flip();

		if (log.logTrace()) {
			log.trace("Encryption produced " + sessionMetadata.netBuffer.remaining() + " byte(s) for "
					+ Utils.toString(socket) + ": " + result);
		}
		
		return sessionMetadata.netBuffer;
	}

	private ByteBuffer decryptReadBuffer(Socket socket, ByteBuffer buffer) throws SSLException {
		if (log.logTrace()) {
			log.trace("Decrypting " + buffer.remaining() + " byte(s) for " + Utils.toString(socket));
		}
		
		SSLSessionMetadata sessionMetadata = (SSLSessionMetadata) this.sslEngineMap.get(socket);
		
		sessionMetadata.appBuffer.clear();
		SSLEngineResult result = sessionMetadata.engine.unwrap(buffer, sessionMetadata.appBuffer);
		sessionMetadata.appBuffer.flip();

		if (log.logTrace()) {
			log.trace("Decryption produced " + sessionMetadata.appBuffer.remaining() + " byte(s) for "
					+ Utils.toString(socket) + ": " + result + "(read buffer has " + buffer.remaining()
					+ " byte(s) remaining)");
		}
		
		return sessionMetadata.appBuffer;
	}

	/**
	 * Queue's data to be written on the indicated {@link Socket}.
	 * <p>
	 * The data is queued internally and the interest operations
	 * set on the associated {@link SocketChannel} is updated
	 * to indicate that a write operation is desired.
	 * @param socket
	 * 	The socket to which the data should be written.
	 * @param data
	 * 	The data to be written
	 * @param close
	 * 	The socket should be closed after the write completes
	 */
	protected void queueWrite(Socket socket, byte[] data, boolean close) {
		SelectionKey key = socket.getChannel().keyFor(this.getSocketSelector());
		if (log.logTrace()) {
			boolean pendReg = this.pendingRegistrations.contains(socket.getChannel());
			log.trace(
					"Queuing " + data.length + " byte(s) (close=" + close + ", socket=" + Utils.toString(socket)
							+ ", key=" + key + ", pr=" + pendReg + "):\n" + Utils.toHexDump(data, 0, data.length));
		}

		ByteBuffer buf = ByteBuffer.wrap(data);
		this.putWriteBuffer(socket, buf);

		if (key == null) {
			// This channel isn't registered yet. We've queued the
			// write. When the channel is connected the selector
			// thread will notice and set the interest Ops to OP_WRITE
			if (log.logTrace()) {
				log.trace("Queued write for unregistered channel on " + Utils.toString(socket));
			}
		} else {
			if (close) {
				// Signal that we want a close after the write completes.
				// This should never be required on an unregistered
				// SelectionKey since it's really only used to shut down
				// a socket in the event of bad data or to shut down
				// a socket after responding to a pre-1.1 client.
				key.attach(CLOSE_AFTER_WRITE);
			}
		}

		// Indicate that we're interested in writing on this socket. The socket 
		// itself may not be connected (or even registered) at this point, but at 
		// the very least a registration has been queued (otherwise there would 
		// be no socket to begin with). If so then the registration will be 
		// processed and this OP_WRITE will be ignored (if the socket is not
		// yet connected) or applied (if it is).
		//
		// We have to do this because the connection operation may finish on this 
		// socket before this method is called, in which case this write would
		// never happen.
		this.queueInterestOpsUpdate(socket, OP_WRITE);
	}

	/**
	 * Create and initialize a {@link Selector} for all I/O
	 * operations.
	 * @param selector
	 * @throws IOException
	 * 	If an error occurs while attempting to create the
	 * 	{@link Selector}.
	 */
	protected void initSelector(Selector selector) throws IOException {
	}

	/**
	 * Factory method for new worker pools.
	 * <p>
	 * This is invoked from the constructor to create
	 * a new worker pool when one is not provided.
	 * @return
	 * 	A new {@link ResourcePool}.
	 */
	protected abstract ResourcePool newWorkerPool();
	
	protected boolean isSharedWorkerPool() {
		return this.sharedWorkerPool;
	}

	/**
	 * An error handler invoked when an attempt to determine
	 * if an HTTP message buffer constitutes a complete HTTP
	 * message.
	 * @param msg
	 * 	The message buffer the exception is associated with.
	 * @param e
	 * 	The exception that was raised.
	 * @throws IOException
	 * 	Implementations may raise an IOException, since
	 * 	there may be a requirement to notify the remote
	 * 	party of the error (which in turn will require
	 * 	network I/O).
	 */
	protected abstract void handleMessageException(HttpMessageBuffer msg, Exception e) throws IOException;

	/**
	 * An error handler invoked when an error occurs within
	 * the main processing loop.
	 * @param socket
	 * 	The socket the exception is associated with. This
	 * 	may be <code>null</code> if the exception is not
	 * 	specific to a particular socket.
	 * @param e
	 * 	The exception that was raised.
	 */
	protected abstract void handleProcessingException(Socket socket, Exception e);
	
	protected abstract void handleTimeout(Socket socket, Exception cause);

	/**
	 * Called when a complete HTTP message has been identified.
	 * @param socket
	 * 	The socket on which a complete message has been
	 * 	received.
	 */
	protected abstract void removeReadBuffer(Socket socket);

	/**
	 * Called when a a socket is deregistered and any buffers
	 * associated with it should be released.
	 * @param socket
	 * 	The socket on which a complete message has been
	 * 	received.
	 */
	protected abstract void removeReadBuffers(Socket socket);

	/**
	 * Called when data is available on a socket.
	 * <p>
	 * Implementations must return a buffer, even if this
	 * means creating a new instance. The same buffer should
	 * be returned for a given socket until the
	 * {@link #removeReadBuffer(Socket)} method is invoked,
	 * ensuring that message fragmentation is correctly handled.
	 * @param socket
	 * 	The socket on which data has arrived.
	 * @return
	 * 	A message buffer for the given socket.
	 */
	protected abstract HttpMessageBuffer getReadBuffer(Socket socket);

	/**
	 * Called when data is available to be written to a socket.
	 * @param socket
	 * 	The socket on which the data should be sent.
	 * @param data
	 * 	The data to be written.
	 */
	protected abstract void putWriteBuffer(Socket socket, ByteBuffer data);

	/**
	 * Called to find out if data is queued to be written to a socket.
	 * @param socket
	 * 	The socket on which the data should be sent.
	 * @return
	 * 	<code>true</code> if there is a buffer waiting to be written on
	 * 	the given socket
	 */
	protected abstract boolean isWriteQueued(Socket socket);

	/**
	 * Called when a all of the data in a pending write buffer 
	 * has been written to a socket.
	 * @param socket
	 * 	The socket the buffer was associated with.
	 */
	protected abstract void removeWriteBuffer(Socket socket);

	/**
	 * Called when a a socket is deregistered and any buffers
	 * associated with it should be released.
	 * @param socket
	 * 	The socket the buffer was associated with.
	 */
	protected abstract void removeWriteBuffers(Socket socket);

	/**
	 * Called when a socket becomes available for writing.
	 * <p>
	 * Implementations should return a buffer only if one
	 * has been added using {@link #putWriteBuffer(Socket, ByteBuffer)}.
	 * The same buffer should be returned for a given socket until the
	 * {@link #removeWriteBuffer(Socket)} method is invoked,
	 * ensuring that large messages that must be written in
	 * multiple fragments are correctly handled.
	 * @param socket
	 * 	The socket that is available for writing.
	 * @return
	 * 	A data buffer for the given socket.
	 */
	protected abstract ByteBuffer getWriteBuffer(Socket socket);

	/**
	 * Request an update to a given {@link Socket}'s interest 
	 * operation set.
	 * <p>
	 * The update is queued internally and the selecting thread is
	 * awoken to apply the change. This removes any risk of platform
	 * specific NIO implementation discrepancies from blocking
	 * indefinitely.
	 * @param socket
	 * 	The {@link Socket} the change is intended for.
	 * @param interestOp
	 * 	The new interest operation.
	 */
	private void queueInterestOpsUpdate(Socket socket, Integer interestOp) {
		synchronized (this.pendingInterestOps) {
			this.pendingInterestOps.put(socket, interestOp);
		}
		this.getSocketSelector().wakeup();
	}
	
	protected void queueRead(Socket socket) {
		this.queueInterestOpsUpdate(socket, OP_READ);
	}

	protected void queueWrite(Socket socket) {
		this.queueInterestOpsUpdate(socket, OP_WRITE);
	}

	public void registerSSLSessionPolicy(SSLSessionPolicy policy) {
		this.sslSessionPolicy = policy;
	}

	public void registerProfiler(Profiler p) {
		synchronized(this.profilers) {
			this.profilers.addProfiler(p);
		}
		synchronized (this.workerPoolMutex) {
			this.resourcePool.registerProfiler(p);
		}
	}
	
	// TODO: Comment to describe why this differes to registerSocket
	protected void registerChannel(SelectableChannel channel) {
		this.channelSelector.addChannel(this, channel);
	}
	
	protected void deregisterChannel(SelectableChannel channel) {
		this.channelSelector.removeChannel(channel);
	}
	
	protected void registerSocket(Socket socket, String host, int port, boolean client) throws IOException {
	}

	protected void deregisterSocket(Socket socket) {
		this.removeReadBuffers(socket);
		this.removeWriteBuffers(socket);
		
		this.deregisterChannel(socket.getChannel());
	}
	
	private SSLSessionMetadata initSocketSSLHandshake(Socket socket) throws SSLException {
		// Create the engine
		SSLEngine engine = this.initSocketSSLEngine(socket);
		
		// Create SSL metadata container (this will initialize relevant buffers too)
		SSLSessionMetadata metadata = new SSLSessionMetadata(this, engine, socket);
		
		// Kick off the SSL handshake
		engine.beginHandshake();
		
		int timeout = this.sslConfig.getHandshakeTimeout();
		if (timeout != 0) {
			if (log.logTrace()) {
				log.trace("Starting " + timeout + "ms timer for SSL handshake on " + Utils.toString(socket));
			}
			resourcePool.getTimer().schedule(metadata.newHandshakeTimerTask(), timeout);
		}
		
		// Record the new engine so it get's closed when the socket is shut down.
		this.sslEngineMap.put(socket, metadata);
		
		return metadata;
	}
	
	protected SSLEngine initSocketSSLEngine(Socket socket) throws SSLException {
		SSLEngine engine = this.sslContext.createSSLEngine();
		engine.setEnabledCipherSuites(this.sslConfig.selectCiphersuites(engine.getSupportedCipherSuites()));
		engine.setEnabledProtocols(this.sslConfig.selectProtocols(engine.getSupportedProtocols()));
		return engine;
	}
	
	private boolean isHandshaking(Socket socket) {
		SSLSessionMetadata sessionMetadata = (SSLSessionMetadata) this.sslEngineMap.get(socket);
		return (sessionMetadata == null) || (sessionMetadata.engine.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING);
	}

	private void progressSSLHandshake(SelectionKey key) throws IOException {
		SocketChannel socketChannel = (SocketChannel) key.channel();
		Socket socket = socketChannel.socket();

		// Make sure an engine is initialized for this socket
		SSLSessionMetadata sessionMetadata = (SSLSessionMetadata) this.sslEngineMap.get(socket);
		if (sessionMetadata == null) {
			if (log.logTrace()) {
				log.trace(this.getClass().getSimpleName() + ": Initializing SSL engine for " + this.getClass().getSimpleName() + " on "
						+ Utils.toString(socket));
			}
			sessionMetadata = this.initSocketSSLHandshake(socket);
			this.handleSSLHandshakeStarted(socket, sessionMetadata);
		}
		
		SSLEngine engine = sessionMetadata.engine;
		if (engine.getHandshakeStatus() == HandshakeStatus.NOT_HANDSHAKING) {
			// This is an error condition since we never call this method
			// after we finish handshaking.
			throw new SSLException("SSLEngine is not handshaking for " + Utils.toString(socket));
		}
		
		SSLEngineResult result;
		while(true) {
			if (log.logTrace()) {
				log.trace(this.getClass().getSimpleName() + ": SSL handshake status for " + Utils.toString(socket) + ": "
						+ engine.getHandshakeStatus());
			}
			
			switch(engine.getHandshakeStatus()) {
			case FINISHED:
			case NOT_HANDSHAKING:
				this.handleSSLHandshakeFinished(socket, sessionMetadata);
				return;
			case NEED_TASK:
				this.delegateSSLEngineTasks(socket, engine);
				break;
			case NEED_UNWRAP:
				// Since the handshake needs an unwrap() and we're only in here because of either
				// a read and a write, we assume(!) we're in here because of a read and that
				// data is available.
				
				int numRead;
				try {
					if (log.logTrace()) {
						log.trace(this.getClass().getSimpleName() + ": NEED_UNWRAP for " + Utils.toString(socket) + ": calling readBuffer()");
					}
					numRead = this.readBuffer(key, socketChannel, this.nonBlockingReadBuf);
				} catch(RemoteSocketClosedException e) {
					// The remote guy shut us out during the handshak
					throw new SSLException("Handshake aborted by remote entity (socket closed)", e);
				} catch(IOException e) {
					if (sessionMetadata.handshakeTimeout()) {
						throw new SSLException("Handshake aborted (timeout)", e);
					}
					throw new SSLException("Handshake aborted by remote entity (read error)", e);
				}
				
				if (numRead == 0 && engine.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP) {
					// Bail so we go back to blocking the selector
					if (log.logTrace()) {
						log.trace(this.getClass().getSimpleName() + ": NEED_UNWRAP for " + Utils.toString(socket)
								+ ": readBuffer() returned 0, unwrap() pending, returning");
					}
					
					// Since we're in here the channel is already registered for OP_READ.
					// Don't requeue it since that will needlessly wake up the selecting
					// thread.
					this.nonBlockingReadBuf.clear();
					return;
				}
				
				while(this.nonBlockingReadBuf.hasRemaining()) {
					sessionMetadata.appBuffer.clear();
					result = engine.unwrap(this.nonBlockingReadBuf, sessionMetadata.appBuffer);
					sessionMetadata.appBuffer.flip();
					// A handshake never produces data for us to consume.
					if (sessionMetadata.appBuffer.hasRemaining()) {
						throw new SSLException("Handshake produced application data (unexpected)");
					}

					if (log.logTrace()) {
						log.trace(this.getClass().getSimpleName() + ": unwrap() result for " + Utils.toString(socket) + ": " + result);
					}
					
					if (result.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
						// Need more data
						this.queueInterestOpsUpdate(socket, OP_READ);
						break;
					}

					if (engine.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
						this.delegateSSLEngineTasks(socket, engine);
					}
				}
				
				this.nonBlockingReadBuf.clear();
				
				break;
			case NEED_WRAP:
				// The engine wants to give us data to send to the remote party to advance
				// the handshake. Let it :-)

				ByteBuffer sslBuf;
				if (sessionMetadata.netBuffer.position() == 0) {
					// We have no outstanding data to write for the handshake (from a previous wrap())
					// so ask the engine for more.
					result = engine.wrap(BLANK, sessionMetadata.netBuffer);
					sessionMetadata.netBuffer.flip();
					
					if (log.logTrace()) {
						log.trace(this.getClass().getSimpleName() + ": wrap() result for "
								+ Utils.toString(socket)
								+ ": "
								+ result
								+ ": produced data\n"
								+ Utils.toHexDump(sessionMetadata.netBuffer.array(), sessionMetadata.netBuffer.position(),
										sessionMetadata.netBuffer.remaining()));
					}
				} else {
					// There's data remaining from the last wrap() call, fall through and try to write it
				}

				// Write the data away
				try {
					if (log.logTrace()) {
						log.trace(this.getClass().getSimpleName() + ": NEED_WRAP for " + Utils.toString(socket) + ": calling writeBuffer()");
					}
					if (!this.writeBuffer(key, socketChannel, sessionMetadata.netBuffer)) {
						// Only some of the data was written Still have data to write, make sure we are alerted to write availability on the socket
						this.queueInterestOpsUpdate(socket, OP_WRITE);
						// And return since we have to wait for the socket to become available.
						return;
					}
				} catch(IOException e) {
					if (sessionMetadata.handshakeTimeout()) {
						throw new SSLException("Handshake aborted (timeout)", e);
					}

					// FIXME: what do we do here?
					log.error("SSL write failed: ARRRRGH", e);
					throw e;
				}

				// All the data was written away, clear the buffer out
				sessionMetadata.netBuffer.clear();
				
				if (engine.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP) {
					if (log.logTrace()) {
						log.trace(this.getClass().getSimpleName() + ": After wrap() SSL handshake expects unwrap for " + Utils.toString(socket));
					}
					
					// We need more data (to pass to unwrap(), signal we're interested
					// in reading on the socket
					this.queueInterestOpsUpdate(socket, OP_READ);
					
					// And return since we have to wait for the socket to become available.
					return;
				}
				
				// For all other cases fall through so we can check what the next step is.
				// This ensures we handle delegated tasks, and handshake completion neatly.
				break;
			}
		}
	}
	
	private void delegateSSLEngineTasks(Socket socket, SSLEngine engine) {
		Runnable task;
		while ((task = engine.getDelegatedTask()) != null) {
			// TODO: We could use a thread pool and hand these out. Later.
			if (log.logTrace()) {
				log.trace(this.getClass().getSimpleName() + ": Delegating SSL task " + task);
			}
			task.run();
		}
		if (task != null && log.logTrace()) {
			log.trace(this.getClass().getSimpleName() + ": All SSL delegated tasks complete: "
					+ engine.getHandshakeStatus());
		}
	}

	private void handleSSLHandshakeStarted(Socket socket, SSLSessionMetadata sessionMetadata) {
		synchronized(this.profilers) {
			this.profilers.begin(sessionMetadata.hashCode(), "ssl.handshake");
		}
	}

	private void handleSSLHandshakeFailed(Socket socket) {
		SSLSessionMetadata metadata = (SSLSessionMetadata) this.sslEngineMap.get(socket);
		if (metadata == null) {
			log.warn("SSL handshake meta-data not found for " + Utils.toString(socket));
			return;
		}
		
		synchronized(this.profilers) {
			this.profilers.count(metadata.hashCode(), "ssl.handshake.failed");
			this.profilers.end(metadata.hashCode(), "ssl.handshake");
		}
	}
	
	private void handleSSLHandshakeFinished(Socket socket, SSLSessionMetadata metadata) throws SSLException {
		SSLEngine engine = metadata.engine;
		if (log.logDebug()) {
			log.debug(this.getClass().getSimpleName() + ": SSL handshake finished for " + Utils.toString(socket) + ": " + Utils.toString(engine.getSession()));
		}
		
		metadata.cancelHandshakeTimer();
		
		// Assign to a local so we don't have to synchronize access to this
		// guy.
		SSLSessionPolicy policy = this.sslSessionPolicy;
		SSLSession session = new SSLSession(engine.getSession());
		if (policy != null && !policy.shouldRetain(socket.getChannel(), session)) {
			throw new SSLException("SSL Session rejected by installed policy: " + policy);
		}

		synchronized(this.profilers) {
			this.profilers.count(metadata.hashCode(), "ssl.handshake.complete");
			this.profilers.end(metadata.hashCode(), "ssl.handshake");
		}
		
		this.handleSSLHandshakeFinished(socket, engine);
	}
	
	protected abstract void handleSSLHandshakeFinished(Socket socket, SSLEngine engine);

	private void processReadData(HttpMessageBuffer httpMsg, byte[] data, int numRead) throws IOException {
		if (log.logTrace()) {
			log.trace("Read " + numRead + " byte(s):\n" + Utils.toHexDump(data, 0, numRead));
		}

		try {
			Socket socket = httpMsg.getSocket();
			int excess = httpMsg.addBytes(data, 0, numRead);
			// FIXME: To really support pipelining we need to guarantee
			// response order. The way I see it this means "pipelining" only
			// really means "all requests received in a single read". If we
			// assume that then we could collect them all together here
			// before dispatching them and have the handler threads know how
			// to deal with a single "batch" call.
			while (excess >= 0) {
				// Clear this socket's request buffer
				this.removeReadBuffer(socket);
				this.getQueue().add(httpMsg);
				
				if (excess > 0) {
					// There's still data, start a new message
					httpMsg = this.getReadBuffer(socket);
					excess = httpMsg.addBytes(data, excess, numRead-excess);
				} else {
					// We have a complete message and no more data
					// break out of the loop
					break;
				}
			}
		} catch (Exception e) {
			this.handleMessageException(httpMsg, e);
		}
	}

	private boolean processDataWritten(SelectionKey key, Socket socket) throws IOException {
		this.removeWriteBuffer(socket);

		if (key.attachment() == CLOSE_AFTER_WRITE) {
			key.cancel();
			socket.getChannel().close();
			this.deregisterSocket(socket);
			return false;
		} else {
			key.interestOps(SelectionKey.OP_READ);
			return true;
		}
	}
}
