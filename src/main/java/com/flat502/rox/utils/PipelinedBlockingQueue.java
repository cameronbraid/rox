package com.flat502.rox.utils;

import java.util.*;

/**
 * This class provides a pipelined implementation of the core functionality
 * of Java 1.5's <code>BlockingQueue</code> interface.
 * <p>
 * Elements are enqueued along with the "origin". Pipelining happens
 * within the context of each origin. Simply put, a thread calling
 * {@link #take()} will never dequeue an item that shares an origin
 * with another item that was dequeued by a second thread that has not
 * returned to the queue.
 */
public class PipelinedBlockingQueue {
	private Object mutex = new Object();
	private List<ElementWithOrigin> queue = new LinkedList<ElementWithOrigin>();
	private Set<Object> ownedOrigins = new HashSet<Object>();

	// Maps threads to the origin of the last element they processed
	private Map<Thread, Object> owners = new HashMap<Thread, Object>();

	private class ElementWithOrigin {
		public Object element;
		public Object origin;

		public ElementWithOrigin(Object element, Object origin) {
			this.element = element;
			this.origin = origin;
		}
	}

	/**
	 * Adds the specified element to this queue
	 * @param element 
	 * 	The element to enqueue
	 * @param origin
	 * 	The origin associated with this element.
	 * @throws NullPointerException 
	 * 	if the specified element is <code>null</code>
	 */
	public void add(Object element, Object origin) {
		if (element == null) {
			throw new NullPointerException();
		}

		synchronized (this.mutex) {
			this.queue.add(new ElementWithOrigin(element, origin));
			this.mutex.notify();
		}
	}

	/**
	 * Retrieves and removes the free element on this queue.
	 * <p>
	 * An element is considered free if the origin it is associated 
	 * with is not shared by another element that was previously
	 * dequeued by a thread that has not yet "returned" to this queue
	 * @return the first free element on this queue
	 * @throws InterruptedException 
	 * 	if interrupted while waiting.
	 */
	public Object take() throws InterruptedException {
		Thread caller = Thread.currentThread();
		synchronized (this.mutex) {
			// Note that the caller has returned to the queue
			Object callerOrigin = this.owners.get(caller);

			if (callerOrigin != null) {
				this.ownedOrigins.remove(callerOrigin);
			}

			ElementWithOrigin nextFree;
			while ((nextFree = this.nextFreeElement()) == null) {
				this.mutex.wait();
			}

			// And record what they now own
			this.owners.put(caller, nextFree.origin);
			this.ownedOrigins.add(nextFree.origin);

			return nextFree.element;
		}
	}

	private ElementWithOrigin nextFreeElement() {
		if (this.queue.isEmpty()) {
			return null;
		}

		// Find the first free item
		ListIterator<ElementWithOrigin> iter = this.queue.listIterator();
		while (iter.hasNext()) {
			ElementWithOrigin element = (ElementWithOrigin) iter.next();
			if (!this.ownedOrigins.contains(element.origin)) {
				// This is a free element
				iter.remove();
				return element;
			}
		}
		return null;
	}
}
