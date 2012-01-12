package com.flat502.rox.processing;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.*;

import com.flat502.rox.utils.BlockingQueue;
import com.flat502.rox.utils.Profiler;
import com.flat502.rox.utils.ProfilerCollection;

// TODO: Document
public abstract class ResourcePool {
	private Thread processingThread;
	private ChannelSelector channelSelector;
	private Timer timer;
	private BlockingQueue queue;
	private List workers = new LinkedList();
	private boolean hasFirstWorker;
	private Map namedMutexes = new HashMap();
	
	private ProfilerCollection profilers = new ProfilerCollection();
	
	protected ResourcePool() {
		this.queue = this.newQueue();
	}

	protected ChannelSelector getChannelSelector() throws IOException {
		synchronized(workers) {
			if (this.channelSelector == null) {
				this.channelSelector = this.newChannelSelector();
			}
		}
		return this.channelSelector;
	}
	
	protected Timer getTimer() {
		synchronized(workers) {
			// Create this Timer lazily so if callers never use timeouts they 
			// don't have the overhead of an extra thread per client instance.
			if (this.timer == null) {
				this.timer = new Timer(true);
			}
		}
		return this.timer;
	}
	
	protected void startProcessingThread() {
		synchronized(workers) {
			if (this.processingThread != null) {
				return;
			}
			
			this.processingThread = this.newProcessingThread(this.channelSelector);
		}
		this.processingThread.start();
	}
	
	public BlockingQueue getQueue() {
		return this.queue;
	}
	
	public void shutdown() {
		synchronized(workers) {
			if (this.getWorkerCount() > 0) {
				while (this.removeWorker() > 0) {
				}
			}
			this.channelSelector.shutdown();
			if (this.timer != null) {
				this.timer.cancel();
			}
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
		synchronized (workers) {
			if (this.workers.size() == 1 && !this.hasFirstWorker) {
				this.hasFirstWorker = true;
				return this.workers.size();
			}
			
			HttpMessageHandler worker = this.newWorker();
			Thread workerThread = new Thread(worker);
			workerThread.setName(worker.getClass().getName() + "-" + System.identityHashCode(workerThread));
			workerThread.setDaemon(true);
			workerThread.start();
			this.workers.add(worker);
			return this.workers.size();
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
		for (int i = 0; i < count; i++) {
			this.addWorker();
		}

		return this.workers.size();
	}

	/**
	 * Get the number of worker threads currently
	 * responsible for this instance.
	 * @return
	 * 	The number of worker threads backing this instance.
	 */
	public int getWorkerCount() {
		return this.workers.size();
	}
	
	public int removeWorker() {
		synchronized (workers) {
			if (this.workers.isEmpty()) {
				throw new IllegalStateException("No workers to remove");
			}
			HttpMessageHandler worker;
			worker = (HttpMessageHandler) this.workers.remove(this.workers.size() - 1);
			worker.stop();
			return workers.size();
		}
	}
	
	public void registerProfiler(Profiler p) {
		synchronized (workers) {
			this.profilers.addProfiler(p);
		}
	}
	
	protected ProfilerCollection getProfilers() {
		return this.profilers;
	}

	protected abstract HttpMessageHandler newWorker();

	/**
	 * A factory method for creating the central HTTP processing
	 * thread.
	 * <p>
	 * Sub-classes may override this method if an alternative
	 * implementation is required, or to alter properties of 
	 * the thread that is created by default, but they should
	 * not invoke the {@link Thread#start()} method.
	 * @param target
	 * @return
	 * 	This implementation returns a new instance of
	 * 	{@link HttpRpcProcessor}.
	 */
	protected Thread newProcessingThread(Runnable target) {
		return new Thread(target, HttpRpcProcessor.class.getName());
	}
	
	protected ChannelSelector newChannelSelector() throws IOException {
		return new ChannelSelector(this);
	}
	
	protected void detach(HttpRpcProcessor processor) throws IOException {
		synchronized (workers) {
			this.channelSelector.deregister(processor);
		}
	}
	
	protected void notifyUnownedChannelClosure(SocketChannel channel) {
	}

	protected BlockingQueue newQueue() {
		return new BlockingQueue();
	}
}
