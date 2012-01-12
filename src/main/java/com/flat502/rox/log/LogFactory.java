package com.flat502.rox.log;

import java.lang.reflect.InvocationTargetException;


// TODO: Document
public class LogFactory {
	private static ILogFactory impl = tryCreateUsingReflection();
	
	private LogFactory() {
	}
	
	private static ILogFactory tryCreateUsingReflection() {
		String[] classNames = {"com.flat502.rox.log.slf4j.Slf4jLogFactory", "com.flat502.rox.log.log4j.Log4JLogFactory"};
		for (String className : classNames) {
			try {
				Class<?> c = Class.forName(className);
				return (CachingLogFactory)c.getConstructor().newInstance();
			}
			catch (ClassNotFoundException e) {
				// try next one
			} catch (IllegalArgumentException e) {
				// try next one
			} catch (SecurityException e) {
				// try next one
			} catch (InstantiationException e) {
				// try next one
			} catch (IllegalAccessException e) {
				// try next one
			} catch (InvocationTargetException e) {
				// try next one
			} catch (NoSuchMethodException e) {
				// try next one
			}
		}
		return NullLogFactory.INSTANCE;
	}
	
	
	public static synchronized ILogFactory configure(ILogFactory impl) {
		ILogFactory oldImpl = LogFactory.impl;
		LogFactory.impl = impl;
		return oldImpl;
	}
	
	public static synchronized Log getLog(Class clazz) {
		return impl.getLog(clazz.getName());
	}

	public static synchronized Log getLog(String name) {
		return impl.getLog(name);
	}
}
