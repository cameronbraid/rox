package com.flat502.rox.utils;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

import junit.framework.TestCase;

public class Test_PipelinedBlockingQueue extends TestCase {
	public void testTakeOrder() throws Exception {
		PipelinedBlockingQueue q = new PipelinedBlockingQueue();
		String originA = "OriginA";
		String originB = "OriginB";
		q.add("First", originA);
		q.add("Second", originA);
		q.add("Third", originA);
		q.add("Last", originB);
		
		DequeueThread t1 = new DequeueThread("T1", q);
		DequeueThread t2 = new DequeueThread("T2", q);
		t1.start();
		t2.start();
		
		assertEquals("First", t1.next());
		assertEquals("Last", t2.next());
		assertEquals("Second", t1.next());
		assertEquals("Third", t1.next());
	}
	
	private class DequeueThread extends Thread {
		private PipelinedBlockingQueue queue;
		private boolean takeNext = false;
		private Object nextItem;
		private boolean shutdown;

		public DequeueThread(String name, PipelinedBlockingQueue queue) {
			this.setName(name);
			this.queue = queue;
		}
		
		public Object next() {
			synchronized (this) {
				this.nextItem = null;
				this.takeNext = true;
				this.notifyAll();
			}
			while(this.nextItem == null) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			return this.nextItem;
		}
		
		public synchronized void shutdown() {
			this.shutdown = true;
			this.notifyAll();
		}
		
		@Override
		public void run() {
			while(true) {
				synchronized (this) {
					while(!takeNext && !shutdown) {
						try {
							this.wait();
						} catch (InterruptedException e) {
						}
					}
					
					try {
						this.nextItem = this.queue.take();
						this.takeNext = false;
					} catch (InterruptedException e) {
					}
				}
			}
		}
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_PipelinedBlockingQueue.class);
	}
}
