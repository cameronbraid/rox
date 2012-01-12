package com.flat502.rox.log;


// TODO: Document
public class LogFactory {
	private static final Log NULL_LOG = new NullLog();
	private static LogFactoryImpl impl;
	
	private LogFactory() {
	}
	
	public static synchronized LogFactoryImpl configure(LogFactoryImpl impl) {
		LogFactoryImpl oldImpl = LogFactory.impl;
		LogFactory.impl = impl;
		return oldImpl;
	}
	
	public static synchronized Log getLog(Class clazz) {
		if (impl == null) {
			return NULL_LOG;
		}
		return impl.getLog(clazz.getName());
	}

	public static synchronized Log getLog(String name) {
		if (impl == null) {
			return NULL_LOG;
		}
		return impl.getLog(name);
	}
}
