/**
 * 
 */
package com.flat502.rox.client;

import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.RpcResponse;

class TestResponseHandler implements AsynchronousResponseHandler {
	private boolean shouldWait = true;
	private Object response;
	private Throwable exception;

	public synchronized void handleResponse(RpcCall call, RpcResponse rsp, RpcResponseContext context) {
		this.response = rsp.getReturnValue();
		this.shouldWait = false;
		this.notify();
	}

	public synchronized void handleException(RpcCall call, Throwable e, RpcResponseContext context) {
		this.exception = e;
		this.shouldWait = false;
		this.notify();
	}
	
	public synchronized Object waitForResponse(long timeout) throws Throwable {
		long start = System.currentTimeMillis();
		while(this.shouldWait) {
			try {
				this.wait(timeout);
				long elapsed = System.currentTimeMillis()-start;
				if (this.shouldWait && elapsed >= timeout) {
					throw new IllegalStateException("Timed out in waitForResponse: " + elapsed + " >= " + timeout);
				}
			} catch (InterruptedException e) {
			}
		}
		
		if (this.response != null) {
			return this.response;
		} else if (this.exception != null) {
			 throw this.exception;
		} else {
			throw new IllegalStateException("What the hell?");
		}
	}
}