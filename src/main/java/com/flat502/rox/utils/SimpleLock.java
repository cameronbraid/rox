package com.flat502.rox.utils;

/**
 * An exclusive re-entrant lock.
 * <p>
 * This lock implementation provides exactly the same functionality as
 * Java's <code>synchronization</code> block construct except that it
 * isn't block-oriented.
 * <p>
 * A thread may acquire this lock multiple times and must release
 * it once for each acquisition.
 */
// TODO: Name?
// TODO: Document
// TODO: Do we even want to expose this?
public class SimpleLock {
	private int depth;
	private Thread owner;

	public SimpleLock() {
	}

	public synchronized void lock() {
		Thread currentThread = Thread.currentThread();
		if (currentThread == owner) {
			depth++;
		} else {
			while (depth > 0) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
			owner = Thread.currentThread();
			depth++;
		}
	}

	public synchronized void unlock() throws IllegalMonitorStateException {
		if (owner != Thread.currentThread()) {
			throw new IllegalMonitorStateException(
					"Current thread does not hold the lock");
		}
		depth--;
		if (depth == 0) {
			owner = null;
			notify();
		}
	}
}
