package com.flat502.rox.log;

public interface ILogFactory {

	public Log getLog(Class clazz);
	public Log getLog(String name);

}
