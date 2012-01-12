package com.flat502.rox.utils;

import java.text.DateFormat;

// TODO: Document
public class DateFormatThreadLocal {
	private ThreadLocal threadLocal;

	public DateFormatThreadLocal(final DateFormat formatter) {
		this.threadLocal = new ThreadLocal() {
			protected Object initialValue() {
				return formatter.clone();
			};
		};
	}
	
	public DateFormat getFormatter() {
		return (DateFormat) this.threadLocal.get();
	}
}
