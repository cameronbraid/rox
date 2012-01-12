package com.flat502.rox.utils;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * This class implements a queue for blocking and releasing
 * threads in a FIFO order.
 * <p>
 *	When a thread calls the {@link #capture()} method it
 * is blocked until {@link #release()} is invoked. Threads are
 * guaranteed to be released in the order in which they were
 * captured.
 * <p>
 * This class is used to ensure fairness when sharing finite
 * resources across multiple threads.
 */
public class ThreadQueue {
	private List captureQueue = new LinkedList();
	private Set pendingRelease = new HashSet();
	private Object mutex;
	
	public ThreadQueue() {
		this(new Object());
	}
	
	public ThreadQueue(Object mutex) {
		this.mutex = mutex;
	}

	// TODO: Document
	public void capture() {
		try {
			this.capture(0);
		} catch (CaptureTimeoutException e) {
			// This should never, ever, happen
			throw (InternalError) new InternalError("capture() caught a timeout exception").initCause(e);
		}
	}

	// TODO: Document
	public void capture(long timeout) throws CaptureTimeoutException {
		synchronized (this.mutex) {
			this.captureQueue.add(Thread.currentThread());
			long start = System.currentTimeMillis();
			while (true) {
				try {
					this.mutex.wait(timeout);
					
					if (this.pendingRelease.remove(Thread.currentThread())) {
						break;
					} else if (timeout > 0) {
						long elapsed = System.currentTimeMillis() - start;
						if (elapsed >= timeout) {
							this.captureQueue.remove(Thread.currentThread());
							throw new CaptureTimeoutException();
						}
					}
				} catch (InterruptedException e) {
				}
			}
		}
	}

	// TODO: Document
	public void release() {
		synchronized (this.mutex) {
			if (this.captureQueue.isEmpty()) {
				return;
			}
			
			this.pendingRelease.add(this.captureQueue.remove(0));
			
			// A more efficient approach might be to block captured threads
			// on their own Object and queue those Objects. However, this
			// requires slightly more complex synchronization which further 
			// complicates the case where a caller wants to share our mutex.
			// Since we're not expecting to deal with more than a few dozen
			// threads this will do until we can prove otherwise.
			this.mutex.notifyAll();
		}
	}

	// For testing
	int size() {
		synchronized (this.mutex) {
			return this.captureQueue.size();
		}
	}

	// For testing
	boolean isEmpty() {
		synchronized (this.mutex) {
			return this.captureQueue.isEmpty();
		}
	}
}
