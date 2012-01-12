package com.flat502.rox.utils;

public interface Profiler {
	public void begin(long id, String operation);
	public void end(long id, String operation);
	public void count(long id, String operation);
}
