package com.flat502.rox.client;

import java.io.IOException;

import com.flat502.rox.http.HttpResponseBuffer;
import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;

class SynchronousNotifier implements Notifiable {
	private static Log log = LogFactory.getLog(SynchronousNotifier.class);

	private HttpResponseBuffer rsp;
	private boolean timeOut = false;
	private Throwable exception;

	public synchronized HttpResponseBuffer waitForResponse() throws IOException {
		while (true) {
			if (this.rsp != null) {
				return rsp;
			} else if (this.timeOut) {
				throw new RpcCallTimeoutException(this.exception);
			} else if (this.exception != null) {
				throw new RpcCallFailedException(this.exception);
			}

			try {
				this.wait();
			} catch (InterruptedException e) {
				if (log.logDebug()) {
					log.debug("waitForResponse() was interrupted",
							e);
				}
				continue;
			}
		}
	}

	public void notify(HttpResponseBuffer rsp, RpcResponseContext context) {
		synchronized (this) {
			this.rsp = rsp;
			this.notify();
		}
	}

	public void notify(Throwable e, RpcResponseContext context) {
		synchronized (this) {
			this.exception = e;
			this.notify();
		}
	}
	
	public void notifyTimedOut(Throwable cause, RpcResponseContext context) {
		synchronized (this) {
			this.timeOut = true;
			this.exception = cause;
			this.notify();
		}
	}
}
