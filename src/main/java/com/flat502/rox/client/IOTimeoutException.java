package com.flat502.rox.client;

import java.io.IOException;

public class IOTimeoutException extends IOException {
	public IOTimeoutException(String message) {
		super(message);
	}
}
