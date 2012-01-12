package com.flat502.rox.log;

import java.util.HashMap;
import java.util.Map;

public abstract class LogFactoryImpl {
	private Map logCache = new HashMap();

	public Log getLog(Class clazz) {
		// The caller has synchronized
		// We "reimplement" this logic rather than just deferring to
		// getLog(String) so that subclasses can differentiate between
		// the two forms of "context" in newLog(*)
		Log log = (Log)this.logCache.get(clazz.getName());
		if (log == null) {
			log = this.newLog(clazz);
			this.logCache.put(clazz.getName(), log);
		}
		return log;
	}
	
	public Log getLog(String name) {
		// The caller has synchronized
		Log log = (Log)this.logCache.get(name);
		if (log == null) {
			log = this.newLog(name);
			this.logCache.put(name, log);
		}
		return log;
	}
	
	public Log newLog(Class clazz) {
		return this.newLog(clazz.getName());
	}
	
	public abstract Log newLog(String name);
}
