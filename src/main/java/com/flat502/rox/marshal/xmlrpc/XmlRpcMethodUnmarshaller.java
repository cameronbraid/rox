package com.flat502.rox.marshal.xmlrpc;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.flat502.rox.marshal.*;
import com.flat502.rox.marshal.FieldNameCodec;
import com.flat502.rox.marshal.HyphenatedFieldNameCodec;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.MethodUnmarshaller;
import com.flat502.rox.utils.DateFormatThreadLocal;
import com.flat502.rox.utils.UTCSimpleDateFormat;
import com.flat502.rox.utils.Utils;

public abstract class XmlRpcMethodUnmarshaller implements MethodUnmarshaller, XmlRpcConstants {
	private static final DateFormat DATE_FORMAT = new UTCSimpleDateFormat(XmlRpcConstants.Formats.DATE_FORMAT);

	private static DateFormatThreadLocal dateFormats;

	private FieldNameCodec fieldNameCodec;

	public XmlRpcMethodUnmarshaller() {
		this(null);
	}
	
	public XmlRpcMethodUnmarshaller(FieldNameCodec fieldNameCodec) {
		dateFormats = XmlRpcUtils.getDateFormatProvider(DomUnmarshaller.class, this.newDateFormat());
		this.fieldNameCodec = fieldNameCodec;
		if (this.fieldNameCodec == null) {
			this.fieldNameCodec = new HyphenatedFieldNameCodec();
		}
	}
	
	protected FieldNameCodec getFieldNameCodec() {
		return this.fieldNameCodec;
	}

	/*
	 *  (non-Javadoc)
	 * @see com.flat502.rox.marshal.MethodUnmarshaller#getFieldNameCodec()
	 */
	// TODO: This seems like an odd method. What was I thinking?
	public FieldNameCodec getDefaultFieldNameCodec() {
		return this.fieldNameCodec;
	}

	protected DateFormat getDateFormat() {
		return dateFormats.getFormatter();
	}

	protected DateFormat newDateFormat() {
		return DATE_FORMAT;
	}

	protected Object parseInt(String value) throws MarshallingException {
		try {
			return new Integer(value);
		} catch (NumberFormatException e) {
			throw new MarshallingException("Invalid integer value: " + value, e);
		}
	}

	protected Object parseBoolean(String value) throws MarshallingException {
		if (!value.equals("0") && !value.equals("1")) {
			throw new MarshallingException("Invalid boolean value: " + value);
		}
		return value.equals("1") ? Boolean.TRUE : Boolean.FALSE;
	}

	protected Object parseString(String value, Class structClass) throws MarshallingException {
		if (structClass != null && Enum.class.isAssignableFrom(structClass)) {
			return this.parseEnum(value, structClass);
		}

		if (structClass != null && structClass != Object.class && structClass != String.class) {
			throw new MarshallingException("Incompatible class (" + structClass.getName() + ") for string field '" + value
					+ "'");
		}
		
		return this.parseString(value);
	}

	protected Object parseString(String value) {
		// We must not unescape XML entities because the XML parse
		// should have done that for us.
		return value;
	}

	protected Object parseDouble(String value) throws MarshallingException {
		try {
			return new Double(value);
		} catch (NumberFormatException e) {
			throw new MarshallingException("Invalid integer value: " + value, e);
		}
	}

	protected Object parseDate(String value) throws MarshallingException {
		try {
			return this.getDateFormat().parse(value);
		} catch (ParseException e) {
			throw (MarshallingException) new MarshallingException("Invalid date literal: " + value).initCause(e);
		}
	}

	protected Object parseBase64(String value) {
		return Base64Codec.decode(value.toCharArray());
	}

	@SuppressWarnings("unchecked")
	protected Object parseEnum(String value, Class structClass) throws MarshallingException {
		return Enum.valueOf(structClass, value);
	}

	protected Object newStructObject(Class structClass) throws MarshallingException {
		if (structClass == Map.class) {
			return new HashMap();
		}

		if (structClass == List.class || structClass.isArray()) {
			return new ArrayList();
		}

		try {
			return structClass.newInstance();
		} catch (Exception e) {
			throw new MarshallingException("Couldn't instantiate  " + structClass.getName(), e);
		}
	}

	/**
	 * Determine an Object field name from the name of an XML-RPC struct member.
	 * <p>
	 * The default implementation defers to the 
	 * {@link FieldNameEncoder} this instance was initialized with.  
	 * 
	 * @param name
	 *            The XML-RPC struct member name.
	 * @return The name of the field the associated value should be assigned to.
	 */
	protected String decodeFieldName(String name) {
		return this.getFieldNameCodec().decodeFieldName(name);
	}

	protected Class getStructMemberType(Object structObject, String name) throws MarshallingException {
		Class structClass = structObject.getClass();
		ClassDescriptor cDesc;
		try {
			cDesc = ClassDescriptor.getInstance(structClass);
		} catch (IntrospectionException e) {
			throw new MarshallingException("Introspection of " + structClass.getName() + " failed", e);
		}

		// We use the type of the setter and not a getter because 
		// we're creating this object for unmarshalling and we don't 
		// want to impose that a getter be around when all we'll be 
		// doing is initializing the object.
		return cDesc.getSetterType(name);
	}

	/**
	 * Called when unmarshalling an XML-RPC struct as an instance of something
	 * other than {@link Map}.
	 * <p>
	 * This method begins by attempting to locate a public field with a name
	 * that is determined by passing the XML-RPC struct member name to
	 * {@link #decodeFieldName(String)}.
	 * <p>
	 * If a field is found it's value is set. Mappings are as defined under this
	 * {@link XmlRpcMethod class's description}. The following limited
	 * conversions are performed if required:
	 * <ul>
	 * <li>&lt;string&gt; values are converted to <code>char[]</code></li>
	 * <li>&lt;double&gt; values are converted to <code>float</code> or
	 * {@link java.lang.Float}</li>
	 * <li>&lt;array&gt; types are converted to <code>Object[]</code></li>
	 * </ul>
	 * All other coercion is as per
	 * {@link Field#set(java.lang.Object, java.lang.Object)}.
	 * <p>
	 * If an appropriate field is not found, an attempt is made to locate a
	 * public setter method with a name that is determined using JavaBean
	 * introspection (see {@link Introspector}).
	 * <p>
	 * If a setter method is found it is invoked with the value. Mapping and
	 * coercion is as for direct field initialization.
	 * 
	 * @param structObject
	 *            The object the XML-RPC struct is being unmarshalled as.
	 * @param name
	 *            The name of the XML-RPC member.
	 * @param value
	 *            The value of the XML-RPC member.
	 * @throws MarshallingException
	 */
	protected void setObjectMember(Object structObject, String name, Object value, UnmarshallerAid aid) throws MarshallingException {
		Class structClass = structObject.getClass();
		ClassDescriptor cDesc;
		try {
			cDesc = ClassDescriptor.getInstance(structClass);
		} catch (IntrospectionException e) {
			throw new MarshallingException("Introspection of " + structClass.getName() + " failed", e);
		}

		String setterName = name;
		if (!(structObject instanceof Fault)) {
			// For faults we don't need to do anything to the member names (faultCode, faultString)
			// since they already follow our preferred naming convention and we're unmarshalling them
			// into our own type.
			setterName = this.decodeFieldName(name);
		}
		Class targetType = null;
		try {
			targetType = cDesc.getSetterType(setterName);
		} catch(IllegalArgumentException e) {
			if (aid != null && !aid.ignoreMissingFields()) {
				throw new MarshallingException("Can't find member '" + setterName + "'", e);
			}
		}
		if (targetType != null) {
			Object coercedValue = Utils.coerce(value, targetType);
			try {
				cDesc.setValue(structObject, setterName, coercedValue);
			} catch (Exception e2) {
				throw new MarshallingException("Error setting property " + setterName + " for struct member '" + name
						+ "' of " + structObject.getClass().getName(), e2);
			}
		}
	}
}
