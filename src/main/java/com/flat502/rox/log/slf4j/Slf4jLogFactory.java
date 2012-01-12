package com.flat502.rox.log.slf4j;

import com.flat502.rox.log.CachingLogFactory;
import com.flat502.rox.log.Log;

public class Slf4jLogFactory extends CachingLogFactory {

	@Override
	public Log newLog(String name) {
		return new Slf4jAdapter(name);
	}
	
	public Log newLog(Class clazz) {
		return new Slf4jAdapter(clazz);
	}
	
}
