package com.flat502.rox.server;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.flat502.rox.http.HttpConstants;
import com.flat502.rox.http.HttpResponseException;
import com.flat502.rox.marshal.*;
import com.flat502.rox.processing.SSLSession;
import com.flat502.rox.utils.Utils;

/**
 * This class dynamically proxies a plain old Java object (POJO) by
 * mapping RPC method calls onto methods onto a target
 * object using reflection.
 * <p>
 * The {@link com.flat502.rox.server.ProxyingRequestHandler}
 * class makes use of this to provide a proxying synchronous
 * request handler.
 * <p>
 * The mapping is relatively unsophisticated and is based entirely
 * on the method name. As a result, method overloading is not supported.
 * Overloading methods results in undefined behaviour, as
 * {@link java.lang.Class#getMethods()} makes no guarantees regarding
 * the order in which methods are returned. You're free to overload
 * methods (i.e. this implementation will not prevent it)
 * but you do so at your own risk.
 * <p>
 * This class also extends 
 * {@link com.flat502.rox.marshal.MethodCallUnmarshallerAid} 
 * and uses the {@link java.lang.reflect.Method#getParameterTypes()}
 * to determine type mappings dynamically when RPC structs
 * are involved.
 * <p>
 * Methods that accept RPC arrays must be defined to
 * accept {@link java.util.List} instances
 * for those parameters. Type coercion is as per
 * {@link java.lang.reflect.Method#invoke(java.lang.Object, java.lang.Object[])}.
 */
public abstract class RpcMethodProxy extends MethodCallUnmarshallerAid {
	private Pattern namePattern;
	private Object target;

	private Map<String, Class<?>[]> methodTypeMap = new HashMap<String, Class<?>[]>();
	private Map<String, Method> methodMap = new HashMap<String, Method>();

	public RpcMethodProxy(String namePattern, Object target) {
		this.namePattern = Pattern.compile(namePattern);
		this.target = target;
		this.initMethodMap(target);
	}

	/**
	 * Retired method.
	 * <p>
	 * This method has been retired and has an empty method body.
	 * Overriding implementations will still be delegated to but
	 * direct calls to this method will simply return
	 * <code>null</code> (unless it is overridden).
	 * @param call
	 * @return <code>null</code>
	 * @throws Exception
	 * @deprecated Override {@link #invoke(RpcCall, RpcCallContext)} instead.
	 */
	public RpcResponse invoke(RpcCall call) throws Exception {
		return null;
	}
	
	public RpcResponse invoke(RpcCall call, RpcCallContext context) throws Exception {
		// Defer to the previous handler method for backwards compatibility.
		RpcResponse rsp = this.invoke(call);
		if (rsp != null) {
			// This is probably an instance of a subclasss that predates
			// the introduction of the SecureSyncHandler interface.
			return rsp;
		}
		
		Matcher m = this.namePattern.matcher(call.getName());
		if (!m.matches()) {
			return this.newRpcFault(new NoSuchMethodError("Name pattern [" + this.namePattern.pattern()
					+ "] didn't match method name [" + call.getName() + "]"));
		}
		String matchedName;
		try {
			matchedName = this.decodeRpcMethodName(m.group(1));
		} catch (IndexOutOfBoundsException e) {
			return this.newRpcFault(e);
		}
		Method method = (Method) this.methodMap.get(matchedName);
		if (method == null) {
			throw new HttpResponseException(HttpConstants.StatusCodes._404_NOT_FOUND, "Not Found (" + matchedName + ")");
		}
		Object returnValue;
		try {
			// Start with the parameters from the RPC call.
			Object[] params = call.getParameters();
			
			Class[] targetParams = (Class[]) this.methodTypeMap.get(matchedName);
			if (params.length == targetParams.length-1) {
				if (RpcCallContext.class.isAssignableFrom(targetParams[targetParams.length-1])) {
					params = Utils.resize(params, params.length + 1);
					params[params.length-1] = context;
				}
			}
			
			returnValue = method.invoke(this.target, params);
		} catch (IllegalArgumentException e) {
			throw new HttpResponseException(HttpConstants.StatusCodes._400_BAD_REQUEST, "Bad Request (illegal argument: "
					+ e.getMessage() + ")", e);
		} catch (IllegalAccessException e) {
			throw new HttpResponseException(HttpConstants.StatusCodes._405_METHOD_NOT_ALLOWED, "Method Not Allowed ("
					+ matchedName + ")", e);
		}
		return this.newRpcResponse(returnValue);
	}

	public Class getType(String methodName, int index) {
		Matcher m = this.namePattern.matcher(methodName);
		if (!m.find()) {
			throw new IllegalStateException("No match on [" + methodName + "] using pattern ["
					+ this.namePattern.pattern() + "]");
		}
		if (m.groupCount() == 0) {
			throw new IllegalStateException("No group matched on [" + methodName + "] using pattern ["
					+ this.namePattern.pattern() + "]");
		}

		String lookupName = this.decodeRpcMethodName(m.group(1));
		Class[] types = (Class[]) this.methodTypeMap.get(lookupName);
		if (types == null || types.length <= index) {
			return null;
		}
		return types[index];
	}

	private void initMethodMap(Object target) {
		Method[] methods = target.getClass().getMethods();
		for (int i = 0; i < methods.length; i++) {
			if (methods[i].getDeclaringClass() == Object.class) {
				// Skip methods declared on java.lang.Object
				continue;
			}
			String name = this.encodeJavaMethodName(methods[i].getName());
			this.methodTypeMap.put(name, methods[i].getParameterTypes());
			Method m = (Method) this.methodMap.put(name, methods[i]);
			if (m != null) {
				throw new IllegalArgumentException("Ambiguous method name: " + m.getName()
						+ " (method overloading is not supported");
			}
		}
	}

	// TODO: Document
	protected String decodeRpcMethodName(String name) {
		return this.massageGeneralMethodName(name);
	}

	// TODO: Document
	protected String encodeJavaMethodName(String name) {
		return this.massageGeneralMethodName(name);
	}

	private String massageGeneralMethodName(String name) {
		StringBuffer sb = new StringBuffer();
		char[] chars = name.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			switch (chars[i]) {
			case '-':
			case '_':
				break;
			default:
				sb.append(Character.toLowerCase(chars[i]));
			}
		}
		return sb.toString();
	}

	/**
	 * @return
	 * 	this implementation always returns <code>null</code>.
	 */
	public FieldNameCodec getFieldNameCodec(String methodName) {
		return null;
	}

	public abstract RpcFault newRpcFault(Throwable e);

	protected abstract RpcResponse newRpcResponse(Object returnValue);
}
