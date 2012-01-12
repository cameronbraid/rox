package com.flat502.rox.processing;

import com.flat502.rox.http.HttpMessageBuffer;
import com.flat502.rox.http.HttpMessageException;
import com.flat502.rox.http.ProcessingException;
import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.utils.BlockingQueue;

/**
 * An abstract base class used by the default client and server
 * implementation to process complete HTTP messages.
 */
public abstract class HttpMessageHandler implements Runnable {
	private static Log log = LogFactory.getLog(HttpMessageHandler.class);

	private BlockingQueue queue;
	private Object terminateMutex = new Object();
	private boolean shouldTerminate;

	private Thread thisThread;

	/**
	 * Constructs a new instance coupled to a
	 * {@link BlockingQueue}.
	 * @param queue
	 * 	The queue from which this instance should
	 * 	fetch new work items.
	 */
	protected HttpMessageHandler(BlockingQueue queue) {
		this.queue = queue;
		this.shouldTerminate = false;
	}

	/**
	 * Processes work items dequeued from the {@link BlockingQueue} associated
	 * with this instance.
	 * <p>
	 * The default implementation of {@link HttpRpcProcessor}
	 * enqueues two types of items on the underlying queue:
	 * <ol>
	 * <li>{@link HttpMessageBuffer} instances, representing complete
	 * HTTP messages received from a remote entity. These are passed to
	 * {@link #handleMessage(HttpMessageBuffer)} for processing.</li>
	 * <li>{@link HttpMessageException} instances when an error occurs
	 * while compiling an HTTP message. These are unpacked and passed to
	 * {@link #handleHttpMessageException(HttpMessageBuffer, Throwable)} for processing.</li>
	 * </ol>
	 */
	public void run() {
		synchronized(this.terminateMutex) {
			if (this.shouldTerminate) {
				return;
			}
			this.thisThread = Thread.currentThread();
		}
		
		while (true) {
			if (this.shouldTerminate) {
				return;
			}
			
			try {
				Object o = this.queue.take();
				if (o instanceof HttpMessageBuffer) {
					this.handleMessage((HttpMessageBuffer) o);
				} else if (o instanceof HttpMessageException) {
					HttpMessageException exception = (HttpMessageException) o;
					this.handleHttpMessageException(exception.getMsg(), exception.getCause());
				} else if (o instanceof ProcessingException) {
					ProcessingException exception = (ProcessingException) o;
					this.handleProcessingException(exception);
				}
			} catch (InterruptedException e) {
				if (shouldTerminate) {
					break;
				}
				continue;
			} catch (Exception e) {
				log.error("HTTP message handler run() loop caught exception", e);
			}
		}
	}

	/**
	 * Stops this instance.
	 * <p>
	 * If this instance is busy with a work item it is 
	 * completed.
	 */
	public void stop() {
		synchronized(this.terminateMutex) {
			if (this.shouldTerminate) {
				return;
			}
			this.shouldTerminate = true;
		}
		if (this.thisThread != null) {
			this.thisThread.interrupt();
		}
	}

	/**
	 * Called to handle complete HTTP messages.
	 * @param msg
	 * 	The complete HTTP message.
	 * @throws Exception
	 * 	Implementations may raise an exception if
	 * 	an error occurs during processing.
	 */
	protected abstract void handleMessage(HttpMessageBuffer msg)
			throws Exception;

	/**
	 * Called when an error occurs while handling an HTTP
	 * message.
	 * <p>
	 * The default implementation raises an
	 * {@link IllegalStateException}.
	 * @param msg
	 * 	The HTTP message that was being processed
	 * 	when the exception was raised.
	 * @param exception
	 * 	The exception that was raised.
	 */
	protected void handleHttpMessageException(HttpMessageBuffer msg, Throwable exception) {
		throw (IllegalStateException)new IllegalStateException("Queue exception was unhandled").initCause(exception);
	}

	/**
	 * Called when an error occurs that is not related to
	 * an HTTP message.
	 * <p>
	 * The default implementation raises an
	 * {@link IllegalStateException}.
	 * @param exception
	 * 	The exception that was raised.
	 */
	protected void handleProcessingException(ProcessingException exception) {
		throw (IllegalStateException)new IllegalStateException("Queue exception was unhandled").initCause(exception);
	}
}
