package com.flat502.rox.marshal;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * An abstract implementation of the {@link com.flat502.rox.marshal.RpcResponse}
 * interface.
 * <p>
 * This interface handles the "drudgery" of storing a return value
 * and the implementation of {@link com.flat502.rox.marshal.RpcResponse#getReturnValue()}.
 */
public abstract class AbstractRpcResponse implements RpcResponse {
	private Object returnValue;

	public AbstractRpcResponse(Object returnValue) {
		this.returnValue = returnValue;
	}

	public Object getReturnValue() {
		return this.returnValue;
	}
}
