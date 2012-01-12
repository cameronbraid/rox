package com.flat502.rox.utils;

import java.util.Collection;

/**
 * Lock supporting Conditions as per 1.5's java.util.concurrent.locks package.
 */
public class Lock {
    private SimpleLock lock;
    private int depth;
    private Thread owner;

    public Lock() {
	this.lock = new SimpleLock();
    }

    public void lock() throws InterruptedException {
	lock.lock();
	try {
	    lock(1);
	}
	finally {
	    lock.unlock();
	}
    }

    public void unlock() throws InterruptedException, IllegalMonitorStateException {
	lock.lock();
	try {
	    if( owner != Thread.currentThread() ) { throw new IllegalMonitorStateException("Current thread does not hold the lock"); }
	    synchronized(this) {
		depth--;
		if( depth == 0 ) {
		    owner = null;
		    notify();
		}
	    }
	}
	finally {
	    lock.unlock();
	}
    }

    public Condition newCondition() {
	return new Condition();
    }

    /// Wait for any one of a number of conditions to be signalled
    public void waitOnConditions( Collection conditions ) {
	throw new RuntimeException( "Manana" );
    }

    /// Must be called with lock held
    private int unlockAll() {
	int depth = this.depth;
	this.depth = 0;
	this.owner = null;
	return depth;
    }

    /// Must be called with lock held
    private void lock( int depth ) throws InterruptedException {
	Thread currentThread = Thread.currentThread();
	if( currentThread == owner ) {
	    this.depth += depth;
	}
	else {
	    while( this.depth > 0 ) {
		synchronized(this) {
		    lock.unlock();
		    wait();
		}
		lock.lock();
	    }
	    owner = Thread.currentThread();
	    this.depth = depth;
	}
    }

    /// Condition supporting only one waiter.
    public class Condition {

	public Condition() {}

	public Lock getLock() { return Lock.this; }

	public void await() throws InterruptedException, IllegalMonitorStateException {
	    lock.lock();
	    boolean gotLock = true;
	    try {
		int lockDepth = 0;
		if( owner != Thread.currentThread() ) { throw new IllegalMonitorStateException("Current thread does not hold the lock"); }
		if( lockDepth != 0 )                  { throw new IllegalMonitorStateException("Another thread is already waiting on this condition"); }
		try {
		    synchronized(this) {
			lockDepth = unlockAll();
			lock.unlock();
			gotLock = false;
			wait();
		    }
		}
		finally {
		    lock.lock();
		    gotLock = true;
		    // we MUST regain the lock here - even if interrupted - I think
		    boolean done = false;
		    while(!done) {
			try {
			    lock(lockDepth);
			    done = true;
			}
			catch( InterruptedException e ) {
			    /*ignore - we can't exit here at all without regaining the lock*/
			}
		    }
		}
	    }
	    finally {
		if(gotLock) { lock.unlock(); }
	    }
	}

	/// Wake up some arbitrary thread waiting on this condition
	public void signal() throws InterruptedException, IllegalMonitorStateException {
	    lock.lock();
	    try {
		if( owner != Thread.currentThread() ) { throw new IllegalMonitorStateException("Current thread does not hold the lock"); }

		synchronized(this) {
		    notify();
		}
	    }
	    finally {
		lock.unlock();
	    }
	}

	/// Wake up all threads waiting on this condition
	public void signalAll() throws InterruptedException, IllegalMonitorStateException {
	    lock.lock();
	    try {
		if( owner != Thread.currentThread() ) { throw new IllegalMonitorStateException("Current thread does not hold the lock"); }

		synchronized(this) {
		    notifyAll();
		}
	    }
	    finally {
		lock.unlock();
	    }
	}
    }
}
