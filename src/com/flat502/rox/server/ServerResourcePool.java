/**
 * 
 */
package com.flat502.rox.server;

import com.flat502.rox.processing.HttpMessageHandler;
import com.flat502.rox.processing.ResourcePool;

public class ServerResourcePool extends ResourcePool {
	protected HttpMessageHandler newWorker() {
		return new HttpRequestHandler(this.getQueue());
	}
}