package com.flat502.rox.utils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import junit.framework.TestCase;

public class Test_ThreadQueue extends TestCase {
	private static final int ITERS = 5;
	
	public void testReleaseEmptyQueue() throws InterruptedException {
		ThreadQueue q = new ThreadQueue();
		q.release();
	}
	
	public void testSingleCaptureReleaseNoTimeout() throws InterruptedException {
		for (int iters = 0; iters < ITERS; iters++) {
			ThreadQueue q = new ThreadQueue();
			List releaseOrder = Collections.synchronizedList(new LinkedList());
			new TestThread("A", q, releaseOrder, false).start();
			while (q.isEmpty())
				;
			q.release();

			waitForRelease(releaseOrder);
			assertEquals("A", releaseOrder.remove(0));
			assertTrue(releaseOrder.isEmpty());
		}
	}

	public void testSingleCaptureReleaseTimeoutDoesNotExpire() throws InterruptedException {
		for (int iters = 0; iters < ITERS; iters++) {
			ThreadQueue q = new ThreadQueue();
			List releaseOrder = Collections.synchronizedList(new LinkedList());
			new TestThread("A", q, releaseOrder, true).start();
			while (q.isEmpty())
				;
			Thread.sleep(750);
			q.release();

			waitForRelease(releaseOrder);
			assertEquals("A", releaseOrder.remove(0));
			assertTrue(releaseOrder.isEmpty());
		}
	}

	public void testSingleCaptureReleaseTimeoutExpires() throws InterruptedException {
		for (int iters = 0; iters < ITERS; iters++) {
			ThreadQueue q = new ThreadQueue();
			List releaseOrder = Collections.synchronizedList(new LinkedList());
			new TestThread("A", q, releaseOrder, true).start();
			while (q.isEmpty())
				;
			Thread.sleep(1250);
			q.release();

			waitForRelease(releaseOrder);
			assertTrue(releaseOrder.remove(0) instanceof CaptureTimeoutException);
			assertTrue(releaseOrder.isEmpty());
		}
	}

	public void testMultipleCaptureReleaseNoTimeout() {
		final int CAPTURE_COUNT = 10;

		for (int iters = 0; iters < ITERS; iters++) {
			ThreadQueue q = new ThreadQueue();
			List releaseOrder = new LinkedList();
			for (int i = 0; i < CAPTURE_COUNT; i++) {
				new TestThread(String.valueOf(i), q, releaseOrder, false).start();
				while (q.size() < i + 1)
					;
			}

			for (int i = 0; i < CAPTURE_COUNT; i++) {
				q.release();
				waitForRelease(releaseOrder);
				assertEquals(String.valueOf(i), releaseOrder.remove(0));
			}
			assertTrue(releaseOrder.isEmpty());
		}
	}

	public void testMultipleCaptureReleaseTimeoutExpires() throws InterruptedException {
		for (int iters = 0; iters < ITERS; iters++) {
			ThreadQueue q = new ThreadQueue();
			List releaseOrder = Collections.synchronizedList(new LinkedList());
			new TestThread("A", q, releaseOrder, false).start();
			while (q.size() < 1)
				;
			new TestThread("B", q, releaseOrder, true).start();
			while (q.size() < 2)
				;
			new TestThread("C", q, releaseOrder, false).start();
			while (q.size() < 3)
				;

			q.release();
			Thread.sleep(1250);
			q.release();
			waitForRelease(releaseOrder, 3);

			assertEquals("A", releaseOrder.remove(0));
			assertTrue(releaseOrder.remove(0) instanceof CaptureTimeoutException);
			assertEquals("C", releaseOrder.remove(0));

			assertTrue(releaseOrder.isEmpty());
		}
	}

	public void testConcurrentReleases() throws InterruptedException {
		for (int iters = 0; iters < ITERS; iters++) {
			ThreadQueue q = new ThreadQueue();
			List releaseOrder = Collections.synchronizedList(new LinkedList());
			new TestThread("A", q, releaseOrder, false).start();
			while (q.size() < 1)
				;
			new TestThread("B", q, releaseOrder, false).start();
			while (q.size() < 2)
				;

			q.release();
			q.release();
			waitForRelease(releaseOrder, 2);
		}
	}

	private void waitForRelease(List releaseOrder) {
		this.waitForRelease(releaseOrder, 1);
	}
	
	private void waitForRelease(List releaseOrder, int size) {
		int slept = 0;
		while (releaseOrder.size() < size) {
			try {
				Thread.sleep(50);
				slept += 50;
			} catch (InterruptedException e) {
			}
			if (slept >= 5000) {
				throw new RuntimeException("Timed out waiting for released thread");
			}
		}
	}

	private class TestThread extends Thread {
		private ThreadQueue q;
		private List recorder;
		private boolean timeout;

		public TestThread(String name, ThreadQueue q, List recorder, boolean timeout) {
			super(name);
			this.q = q;
			this.recorder = recorder;
			this.timeout = timeout;
		}

		public void run() {
			if (!this.timeout) {
				this.q.capture();
				this.recorder.add(this.getName());
			} else {
				try {
					this.q.capture(1000);
					this.recorder.add(this.getName());
				} catch (CaptureTimeoutException e) {
					this.recorder.add(e);
				}
			}				
		}
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_ThreadQueue.class);
	}
}
