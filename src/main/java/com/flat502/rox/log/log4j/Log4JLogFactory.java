package com.flat502.rox.log.log4j;

import com.flat502.rox.log.Log;
import com.flat502.rox.log.CachingLogFactory;

public class Log4JLogFactory extends CachingLogFactory {
	public Log newLog(String name) {
		return new Log4JAdapter(name);
	}
}
