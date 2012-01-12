package com.flat502.rox.log;

public class NullLogFactory implements ILogFactory {
	private static final Log NULL_LOG = new NullLog();
	
	public static NullLogFactory INSTANCE = new NullLogFactory();
	
	@Override
	public Log getLog(Class clazz) {
		return NULL_LOG;
	}
	
	@Override
	public Log getLog(String name) {
		return NULL_LOG;
	}

}
