package com.flat502.rox.log;


public class SimpleLogFactory extends CachingLogFactory {
	private Log log;

	public SimpleLogFactory(Log log) {
		this.log = log;
	}
	
	public Log newLog(String name) {
		return this.log;
	}
}
