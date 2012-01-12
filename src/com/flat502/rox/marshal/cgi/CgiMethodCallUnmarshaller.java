package com.flat502.rox.marshal.cgi;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;

import com.flat502.rox.http.MethodCallURI;
import com.flat502.rox.marshal.ClassDescriptor;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.MethodCallUnmarshallerAid;
import com.flat502.rox.marshal.RpcCall;
import com.flat502.rox.marshal.xmlrpc.XmlRpcConstants;
import com.flat502.rox.utils.DateFormatThreadLocal;
import com.flat502.rox.utils.UTCSimpleDateFormat;
import com.flat502.rox.utils.Utils;

public class CgiMethodCallUnmarshaller {
	private static final DateFormatThreadLocal DATE_FORMAT = new DateFormatThreadLocal(new UTCSimpleDateFormat(
			XmlRpcConstants.Formats.DATE_FORMAT));

	public RpcCall unmarshal(MethodCallURI uri, MethodCallUnmarshallerAid aid) throws Exception {
		Object[] params = this.unpackParams(uri, aid);
		return new CgiRpcMethodCall(uri.getMethodName(), params);
	}

	private Object[] unpackParams(MethodCallURI uri, MethodCallUnmarshallerAid aid) throws Exception {
		Map parameters = uri.getParameters();
		if (parameters.isEmpty()) {
			return new Object[0];
		}

		// The first decision is whether or not we really have key-value
		// pairs. If all values are null then we'll just use the keys
		// as a list of parameter values.
		if (this.allValuesNull(parameters.values().iterator())) {
			String[] names = uri.getParameterNames();
			Object[] keys = new Object[uri.getParameterNames().length];
			for (int i = 0; i < keys.length; i++) {
				keys[i] = names[i];
				if (aid != null) {
					Class type = aid.getType(uri.getMethodName(), i);
					if (type != null) {
						keys[i] = this.convert(names[i], type);
					}
				}
			}
			return keys;
		}

		// Some of the values are non-null so we'll treat this as a method
		// call with a single struct parameter. This may still require some
		// type coercion.
		Class structClass = aid == null ? null : aid.getType(uri.getMethodName(), 0);
		if (structClass == null || Map.class.isAssignableFrom(structClass)) {
			return new Object[] { parameters };
		}

		if (List.class.isAssignableFrom(structClass)) {
			// This will never work.
			throw new MarshallingException("CGI parameter map cannot be coerced to a List type: " + structClass.getName());
		}

		// We have some kind of structClass and map. Try to populate an instance of
		// structClass based on the map contents.
		ClassDescriptor cd = ClassDescriptor.getInstance(structClass);
		Object structObject = this.newStructObject(structClass);

		Iterator iter = parameters.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			if (entry.getValue() != null) {
				String key = (String) entry.getKey();
				Class clazz = cd.getSetterType(key);
				cd.setValue(structObject, key, this.convert(entry.getValue(), clazz));
			}
		}

		return new Object[] { structObject };
	}

	private Object convert(Object value, Class targetType) throws ParseException {
		if (targetType == Date.class) {
			return DATE_FORMAT.getFormatter().parse((String) value);
		}
		return Utils.convert(value, targetType);
	}

	private Object newStructObject(Class structClass) throws MarshallingException {
		if (structClass == Map.class) {
			return new HashMap();
		}

		try {
			return structClass.newInstance();
		} catch (Exception e) {
			throw new MarshallingException("Couldn't instantiate  " + structClass.getName(), e);
		}
	}

	private boolean allValuesNull(Iterator iter) {
		while (iter.hasNext()) {
			if (iter.next() != null) {
				return false;
			}
		}
		return true;
	}
}
