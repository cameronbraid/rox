package com.flat502.rox.marshal;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * An abstract implementation of the {@link com.flat502.rox.marshal.RpcCall}
 * interface.
 * <p>
 * This interface handles the "drudgery" of storing the method name
 * and parameters
 * and the implementation of {@link com.flat502.rox.marshal.RpcCall#getName()}
 * and {@link com.flat502.rox.marshal.RpcCall#getParameters()}.
 */
public abstract class AbstractRpcCall implements RpcCall {
	private String name;
	private Object[] params;

	/**
	 * Initializes a new instance of this class
	 * <p>
	 * If <code>params</code> is null a new zero-length
	 * array is created and stored in place of it.
	 * @param name
	 * 	The name of the method. May not be <code>null</code>.
	 * @param params
	 * 	The method parameters. May be <code>null</code>.
	 */
	public AbstractRpcCall(String name, Object[] params) {
		if (name == null) {
			throw new NullPointerException();
		}
		
		this.name = name;
		this.params = params;
		if (this.params == null) {
			this.params = new Object[0];
		}
	}

	public String getName() {
		return this.name;
	}

	public Object[] getParameters() {
		return this.params;
	}
}
