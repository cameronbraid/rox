package com.flat502.rox.marshal.xmlrpc;

import java.lang.reflect.Constructor;
import java.text.DateFormat;

import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.marshal.FieldNameCodec;
import com.flat502.rox.marshal.FieldNameDecoder;
import com.flat502.rox.marshal.FieldNameEncoder;
import com.flat502.rox.marshal.MethodUnmarshaller;
import com.flat502.rox.utils.DateFormatThreadLocal;
import com.flat502.rox.utils.Utils;

/**
 * Centralizes utility routines for this package.
 */
class XmlRpcUtils {
	private static Log log = LogFactory.getLog(XmlRpcUtils.class);
	
	private static DateFormatThreadLocal[] dateFormats = new DateFormatThreadLocal[2];

	public static DateFormatThreadLocal getDateFormatProvider(Class user, DateFormat initialFormat) {
		// This may well be called by multiple threads. Without
		// synchronization the worst is that we'll create more than
		// one DateFormatThreadLocal instance (and even SimpleDateFormat).
		// That will quickly end though and the up front cost is preferable
		// to an ongoing cost of synchronization since we'll be in here 
		// a lot (because we allow sub-classes to provide their own
		// formatter we must initialize the relevant thread local formatter
		// provider using the result from an instance method).
		//
		// In addition we use an if-else ladder as a (very) poor man's hashtable
		// since we control all of the callers of this method. This obviously
		// doesn't scale (in terms of maintenance) but will do for now.
		if (MethodUnmarshaller.class.isAssignableFrom(user) ||
				SaxUnmarshaller.class.isAssignableFrom(user)) {
			if (dateFormats[0] == null) {
				dateFormats[0] = new DateFormatThreadLocal(initialFormat);
			}
			return dateFormats[0];
		}

		if (XmlRpcMarshaller.class.isAssignableFrom(user)) {
			if (dateFormats[1] == null) {
				dateFormats[1] = new DateFormatThreadLocal(initialFormat);
			}
			return dateFormats[1];
		}

		throw new IllegalArgumentException("Unsupported user class: " + user.getName());
	}
	
	public static XmlRpcMarshaller newMarshaller(FieldNameEncoder fieldNameEncoder) {
		if (Utils.getJavaRuntimeVersion() > 1.4) {
			// Dodge the 1.4 compiler
			try {
				Class clazz = Class.forName(XmlRpcMarshaller.class.getName() + "J5");
				Constructor cons = clazz.getDeclaredConstructor(new Class[]{FieldNameEncoder.class});
				return (XmlRpcMarshaller) cons.newInstance(new Object[]{fieldNameEncoder});
			} catch (Exception e) {
				log.warn("Failed to instantiate Java 1.5 marshaller: falling back to 1.4 marshaller", e);
				return new XmlRpcMarshaller(fieldNameEncoder);
			}
		}
		return new XmlRpcMarshaller(fieldNameEncoder);
	}
	
	public static DomUnmarshaller newDomUnmarshaller(FieldNameCodec fieldNameCodec) {
		return new DomUnmarshaller(fieldNameCodec);
	}
	
	public static SaxUnmarshaller newSaxUnmarshaller(FieldNameCodec fieldNameCodec) {
		return new SaxUnmarshaller(fieldNameCodec);
	}
}
