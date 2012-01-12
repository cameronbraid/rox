package com.flat502.rox.utils;

import java.util.LinkedList;
import java.util.List;

/**
 * This class provides a simple implementation of the core functionality
 * of Java 1.5's <code>BlockingQueue</code> interface.
 * <p>
 * The intention is to be able to use this project on Java 1.4.
 */
// TODO: Can we write this so the most recently unblocked worker
// is unblocked again (i.e. stack them?) This will reduce paging
// issues.
public class BlockingQueue {
	private Object mutex = new Object();
	private List queue = new LinkedList();

	/**
	 * Adds the specified element to this queue
	 * @param o the element
	 * @throws NullPointerException if the specified element is <code>null</code>
	 */
	public void add(Object o) {
		if (o == null) {
			throw new NullPointerException();
		}

		synchronized (this.mutex) {
			this.queue.add(o);
			this.mutex.notify();
		}
	}

	/**
	 * Retrieves and removes the head of this queue, waiting
	 * if no elements are present on this queue.
	 * @return the head of this queue
	 * @throws InterruptedException if interrupted while waiting.
	 */
	public Object take() throws InterruptedException {
		synchronized (this.mutex) {
			while (this.queue.isEmpty()) {
				this.mutex.wait();
			}
			return this.queue.remove(0);
		}
	}
}
