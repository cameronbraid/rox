package com.flat502.rox.client;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Encapsulates the logic that makes client-side
 * dynamic proxying possible.
 */
class RpcClientProxy implements InvocationHandler {
	private String methodPrefix;
	private Class targetClass;
	private HttpRpcClient client;

	private Object proxiedTarget;

	public RpcClientProxy(Class targetClass, HttpRpcClient client)
			throws Exception {
		this(null, targetClass, client);
	}

	public RpcClientProxy(String methodPrefix, Class targetClass,
			HttpRpcClient client) throws Exception {
		this.methodPrefix = methodPrefix;
		this.targetClass = targetClass;
		this.client = client;
		this.proxiedTarget = this.proxyTarget(targetClass);
	}

	public Object getProxiedTarget() {
		return this.proxiedTarget;
	}

	private Object proxyTarget(Class targetClass) throws Exception {
		Class[] targetInterfaces;
		if (targetClass.isInterface()) {
			targetInterfaces = new Class[] { targetClass };
		} else {
			targetInterfaces = targetClass.getInterfaces();
		}
		return Proxy.newProxyInstance(this.getClass().getClassLoader(),
				targetInterfaces, this);
	}

	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		String methodName = "";
		if (methodPrefix != null) {
			methodName = methodPrefix;
		}
		methodName += method.getName();
		return client.execute(methodName, args, method.getReturnType());
	}
}
