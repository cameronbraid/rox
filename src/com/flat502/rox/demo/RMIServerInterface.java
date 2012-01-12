package com.flat502.rox.demo;

import java.util.Date;
import java.util.List;

/**
 * A common interface shared between {@link com.flat502.rox.demo.RMIClientDemo},
 * {@link com.flat502.rox.demo.RMIAsyncServerDemo} and {@link com.flat502.rox.demo.RMISyncServerDemo}.
 * <p>
 * A common interface is essential because under the hood the client-side proxying 
 * functionality relies on Java's dynamic proxy functionality 
 * ({@link java.lang.reflect.Proxy}) and this is built around interfaces.
 */
public interface RMIServerInterface {
	public int sum(int[] list);

	public String getVersionInfo(boolean verbose);

	public Date getDate();
}
