package com.flat502.rox.marshal.xmlrpc;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.*;

import com.flat502.rox.marshal.ClassDescriptor;
import com.flat502.rox.marshal.FieldNameEncoder;
import com.flat502.rox.marshal.HyphenatedFieldNameCodec;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.utils.*;

/**
 * This class supports marshalling of XML-RPC values, <i>not</i>
 * execution. For that functionality see the
 * {@link com.flat502.rox.client.XmlRpcClient} class.
 * <p>
 * The relationship between Java and XML-RPC data types is given in the
 * following table: <table border=1 cellpadding=5>
 * <tr>
 * <th>Java</th>
 * <th>XML-RPC</th>
 * <th>Notes</th>
 * </tr>
 * <tr>
 * <td>Integer or int</td>
 * <td>&lt;int&gt; or &lt;i4&gt;</td>
 * <td>This implementation always uses the &lt;int&gt; form when marshalling
 * integers.</td>
 * </tr>
 * <tr>
 * <td>Boolean or boolean</td>
 * <td>&lt;boolean&gt;</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>String or char[]</td>
 * <td>&lt;string&gt;</td>
 * <td>This implementation always uses an explicit &lt;string&gt; tag when
 * marshalling strings.</td>
 * </tr>
 * <tr>
 * <td>Float, float, Double or double</td>
 * <td>&lt;double&gt;</td>
 * <td>If the target field is a <code>float</code> (or <code>Float</code>)
 * then the value may be truncated.</td>
 * </tr>
 * <tr>
 * <td>java.util.Date</td>
 * <td>&lt;dateTime.iso8601&gt;</td>
 * <td>Date values are interpreted as UTC dates for the purposes of
 * unmarshalling.</td>
 * </tr>
 * <tr>
 * <td>byte[]</td>
 * <td>&lt;base64&gt;</td>
 * <td></td>
 * </tr>
 * <tr>
 * <td>Object or java.util.Map</td>
 * <td>&lt;struct&gt;</td>
 * <td>Objects are marshalled (and unmarshalled) by treating their public
 * fields as keys in a map. The names of the public fields are transformed by
 * calls to the {@link #encodeFieldName(String)} method. 
 * <code>transient</code> fields and fields with a <code>null</code> 
 * value are ignored during marshalling.</td>
 * </tr>
 * <tr>
 * <td>Object[] or java.util.List</td>
 * <td>&lt;array&gt;</td>
 * <td></td>
 * </tr>
 * </table>
 */
public class XmlRpcMarshaller implements XmlRpcConstants {
	private static final DateFormat DATE_FORMAT = new UTCSimpleDateFormat(XmlRpcConstants.Formats.DATE_FORMAT);
	private static DateFormatThreadLocal dateFormats;

	private boolean marshalCompactXml;

	// We cache field name encodings to avoid potentially
	// expensive string manipulation. This hurts the NOP
	// codec case a little but improves the general case
	// fairly dramatically.
	private static Map fieldNameEncodingCache = new HashMap();

	private FieldNameEncoder fieldNameEncoder;

	/**
	 * Initialize a new instance of this class.
	 * <p>
	 * This constructor calls {@link #newDateFormat()} 
	 * to initialize the underlying date formatter.
	 * @param fieldNameEncoder
	 * 	An implementation of {@link FieldNameEncoder} used when
	 * 	struct members are marshalled. May be <code>null</code>.
	 */
	protected XmlRpcMarshaller(FieldNameEncoder fieldNameEncoder) {
		XmlRpcMarshaller.dateFormats = XmlRpcUtils.getDateFormatProvider(XmlRpcMarshaller.class, this.newDateFormat());
		this.marshalCompactXml = true;
		this.fieldNameEncoder = fieldNameEncoder;
		if (this.fieldNameEncoder == null) {
			this.fieldNameEncoder = new HyphenatedFieldNameCodec();
		}
	}

	/**
	 * Configure the compactness of the marshalled form of this instance.
	 * <p>
	 * The marshalled form of instances is compact by default.
	 * 
	 * @param compact
	 *            A flag indicating whether to produce compact XML (<code>true</code>)
	 *            or more readable XML (<code>true</code>).
	 */
	public void setCompactXml(boolean compact) {
		this.marshalCompactXml = compact;
	}

	/**
	 * A factory method invoked to create a new instance of {@link XmlPrinter}.
	 * <p>
	 * Implementations are free to ignore the <code>compact</code> parameter.
	 * It is intended as hint to ease debugging. This implementation "pretty
	 * prints" the XML if <code>compact</code> is <code>false</code>, using
	 * indentation and line terminators to lay the document out in a more
	 * readable form. The result is likely to be a considerably larger XML
	 * document.
	 * 
	 * @param out
	 * 	The {@link java.io.OutputStream} the returned instance should
	 *		write to.
	 * @param charSet
	 * 	The character encoding that should be used when writing
	 * 	textual data to the underlying stream.
	 * @return
	 * 	A new instance of {@link XmlPrinter} that will print
	 * 	to the given stream.
	 * @throws IOException
	 */
	protected XmlPrinter newXmlWriter(OutputStream out, Charset charSet) throws IOException {
		if (this.marshalCompactXml) {
			return new XmlPlainPrinter(out);
		}
		return new XmlPrettyPrinter(new PrintStream(out, false, charSet.name()));
	}

	protected DateFormat getDateFormat() {
		return XmlRpcMarshaller.dateFormats.getFormatter();
	}

	protected String getIntTagName() {
		return Types.INT1;
	}

	protected String getBooleanTagName() {
		return Types.BOOLEAN;
	}

	/**
	 * @return Implementations may return <code>null</code> if strings should
	 *         be encoded without a type tag.
	 */
	protected String getStringTagName() {
		return Types.STRING;
	}

	protected String getDoubleTagName() {
		return Types.DOUBLE;
	}

	protected String getDateTagName() {
		return Types.ISO8601DATE;
	}

	protected String getBase64TagName() {
		return Types.BASE64;
	}

	/**
	 * Initialize a new {@link DateFormat} for use when formatting
	 * XML-RPC date values.
	 * <p>
	 * This method may be invoked multiple times and it <em>must</em> 
	 * return an equivalent formatter on each invocation. Equivalent
	 * formatters always produce the same string literal when formatting
	 * a given {@link Date} instance.
	 * <p>
	 * @return
	 * 	A new date
	 */
	protected DateFormat newDateFormat() {
		return DATE_FORMAT;
	}

	protected void marshalValue(XmlPrinter out, int depth, Object param) throws MarshallingException, IOException {
		if (param == null) {
			throw new NullPointerException("null values are not supported by XML-RPC");
		}
		if (param instanceof Map && param instanceof List) {
			throw new IllegalArgumentException("Parameter implements both Map and List. What must I do?");
		}

		out.openTag("value");

		String tag = null;
		String value = null;
		Class paramClass = param.getClass();
		if (param instanceof Map) {
			// We support this in addition to field introspection
			// for convenience.
			this.marshalMap(out, depth + 1, (Map) param);
		} else if (param instanceof List) {
			this.marshalList(out, depth + 1, (List) param);
		} else if (paramClass == Boolean.TYPE || paramClass == Boolean.class) {
			tag = this.getBooleanTagName();
			out.openTag(tag);
			this.marshalValue(out, (Boolean) param);
			out.closeTag(tag);
		} else if (paramClass == Character.TYPE || paramClass == Character.class) {
			tag = this.getStringTagName();
			out.openTag(tag);
			this.marshalValue(out, (Character) param);
			out.closeTag(tag);
		} else if (Number.class.isAssignableFrom(paramClass) || paramClass.isPrimitive()) {
			if (paramClass == Float.class || paramClass == Float.TYPE || paramClass == Double.class
					|| paramClass == Double.TYPE) {
				tag = this.getDoubleTagName();
			} else {
				tag = this.getIntTagName();
			}
			out.openTag(tag);
			this.marshalValue(out, (Number) param);
			out.closeTag(tag);
		} else if (paramClass == Boolean.TYPE || paramClass == Boolean.class) {
			tag = this.getBooleanTagName();
			out.openTag(tag);
			this.marshalValue(out, (Boolean) param);
			out.closeTag(tag);
		} else if (paramClass == String.class) {
			tag = this.getStringTagName();
			if (tag != null) {
				out.openTag(tag);
				this.marshalValue(out, (String) param);
				out.closeTag(tag);
			} else {
				this.marshalValue(out, (String) param);
			}
		} else if (paramClass == Date.class) {
			tag = this.getDateTagName();
			out.openTag(tag);
			this.marshalValue(out, (Date) param);
			out.closeTag(tag);
		} else if (paramClass.isArray()) {
			if (paramClass == byte[].class) {
				tag = this.getBase64TagName();
				out.openTag(tag);
				this.marshalValue(out, (byte[]) param);
				out.closeTag(tag);
			} else if (paramClass == char[].class) {
				tag = this.getStringTagName();
				if (tag != null) {
					out.openTag(tag);
					this.marshalValue(out, new String((char[]) param));
					out.closeTag(tag);
				} else {
					this.marshalValue(out, new String((char[]) param));
				}
			} else if (paramClass == Character[].class) {
				tag = this.getStringTagName();
				if (tag != null) {
					out.openTag(tag);
					Character[] paramAsArray = (Character[]) param;
					StringBuffer sb = new StringBuffer();
					for (int i = 0; i < paramAsArray.length; i++) {
						sb.append(paramAsArray[i]);
					}
					this.marshalValue(out, sb.toString());
					out.closeTag(tag);
				} else {
					this.marshalValue(out, new String((char[]) param));
				}
			} else if (param instanceof Object[]) {
				this.marshalArray(out, depth + 1, param);
			} else if (paramClass == int[].class) {
				this.marshalArray(out, depth + 1, param);
			} else if (paramClass == long[].class) {
				this.marshalArray(out, depth + 1, param);
			} else if (paramClass == boolean[].class) {
				this.marshalArray(out, depth + 1, param);
			} else if (paramClass == double[].class) {
				this.marshalArray(out, depth + 1, param);
			} else if (paramClass == float[].class) {
				this.marshalArray(out, depth + 1, param);
			} else {
				throw new MarshallingException("Unsupported primitive array type: " + paramClass.getName());
			}
		} else if (paramClass == char[].class) {
			tag = this.getStringTagName();
			if (tag != null) {
				out.openTag(tag);
				this.marshalValue(out, new String((char[]) param));
				out.closeTag(tag);
			} else {
				this.marshalValue(out, new String((char[]) param));
			}
		} else {
			this.marshalObject(out, depth + 1, param);
		}
		out.closeTag("value");
	}

	protected void marshalValue(XmlPrinter out, Boolean val) throws MarshallingException, IOException {
		out.writeValue(val.booleanValue() ? "1" : "0");
	}

	protected void marshalValue(XmlPrinter out, Character val) throws MarshallingException, IOException {
		switch (val.charValue()) {
		case '<':
			out.writeValue("&lt;");
			break;
		case '>':
			out.writeValue("&gt;");
			break;
		case '&':
			out.writeValue("&amp;");
			break;
		default:
			out.writeValue(val.toString());
		}
	}

	protected void marshalValue(XmlPrinter out, String val) throws MarshallingException, IOException {
		val = val.replaceAll("&", "&amp;");
		val = val.replaceAll("<", "&lt;");
		val = val.replaceAll(">", "&gt;");
		out.writeValue(val);
	}

	protected void marshalValue(XmlPrinter out, Number val) throws MarshallingException, IOException {
		if (val.longValue() < Integer.MIN_VALUE || val.longValue() > Integer.MAX_VALUE) {
			throw new MarshallingException("Integer value out of range: " + val);
		}
		out.writeValue(val.toString());
	}

	protected void marshalValue(XmlPrinter out, Date val) throws MarshallingException, IOException {
		out.writeValue(this.getDateFormat().format(val));
	}

	protected void marshalValue(XmlPrinter out, byte[] val) throws MarshallingException, IOException {
		out.writeValue(Base64Codec.encode(val));
	}

	/**
	 * Marshals an {@link Object} that is not one of the standard types in the
	 * mapping table discussed in the
	 * {@link XmlRpcMarshaller description of this class}.
	 * <P>
	 * The object is not a simple type ({@link Integer} etc) and does not
	 * implement either the {@link Map} or {@link List} interface.
	 * <P>
	 * 
	 * @param depth
	 *            Used for pretty printing the output. Implementations are free
	 *            to ignore this parameter.
	 * @param param
	 *            The object to marshal
	 * @throws MarshallingException
	 * @throws IOException 
	 */
	protected void marshalObject(XmlPrinter out, int depth, Object param) throws MarshallingException, IOException {
		ClassDescriptor cDesc;
		try {
			cDesc = ClassDescriptor.getInstance(param.getClass());
		} catch (IntrospectionException e) {
			throw new MarshallingException("introspection failed: " + param.getClass().getName(), e);
		}

		out.openTag(Types.STRUCT);

		Iterator getters = cDesc.getters();
		int memberCount = 0;
		while (getters.hasNext()) {
			String getterName = (String) getters.next();
			try {
				Object paramValue = cDesc.getValue(param, getterName);
				if (paramValue != null) {
					out.openTag("member");
					out.openTag("name");
					out.writeValue(this.encodeFieldName(getterName));
					out.closeTag("name");
					this.marshalValue(out, depth + 2, paramValue);
					out.closeTag("member");
					memberCount++;
				}
			} catch (IllegalAccessException e) {
				throw (IllegalArgumentException)new IllegalArgumentException("access error: " + getterName).initCause(e);
			} catch (IllegalArgumentException e) {
				throw (IllegalArgumentException)new IllegalArgumentException("getter has arguments: " + getterName).initCause(e);
			} catch (InvocationTargetException e) {
				throw (IllegalArgumentException)new IllegalArgumentException("invocation error: " + getterName).initCause(e);
			}
		}

		out.closeTag(Types.STRUCT);

		// Check if we actually rendered any values
		if (memberCount == 0) {
			throw new IllegalArgumentException("Class has no non-null public fields or getters: "
					+ param.getClass().getName());
		}
	}

	protected void marshalMap(XmlPrinter out, int depth, Map param) throws MarshallingException, IOException {
		out.openTag(Types.STRUCT);
		Iterator entries = param.entrySet().iterator();
		while (entries.hasNext()) {
			Map.Entry entry = (Map.Entry) entries.next();
			if (entry.getKey() == null) {
				throw new IllegalArgumentException("Null key encountered");
			}
			if (!(entry.getKey() instanceof String)) {
				throw new IllegalArgumentException("Non-string key encountered");
			}

			out.openTag("member");
			out.openTag("name");
			out.writeValue(entry.getKey().toString());
			out.closeTag("name");
			this.marshalValue(out, depth + 2, entry.getValue());
			out.closeTag("member");
		}
		out.closeTag(Types.STRUCT);
	}

	protected void marshalList(XmlPrinter out, int depth, List list) throws MarshallingException, IOException {
		out.openTag(Types.ARRAY);
		out.openTag("data");
		Iterator iter = list.iterator();
		while (iter.hasNext()) {
			Object element = iter.next();
			this.marshalValue(out, depth + 2, element);
		}
		out.closeTag("data");
		out.closeTag(Types.ARRAY);
	}

	private void marshalArray(XmlPrinter out, int depth, Object array) throws MarshallingException, IOException {
		if (!array.getClass().isArray()) {
			throw new MarshallingException("Expected an array type, got " + array.getClass().getName());
		}

		out.openTag(Types.ARRAY);
		out.openTag("data");
		int length = Array.getLength(array);
		for (int i = 0; i < length; i++) {
			this.marshalValue(out, depth + 2, Array.get(array, i));
		}
		out.closeTag("data");
		out.closeTag(Types.ARRAY);
	}

	/**
	 * Determine the name of an XML-RPC struct member from an Object field name.
	 * <p>
	 * The default implementation defers to the 
	 * {@link FieldNameEncoder} this instance was initialized with.  
	 * @param name
	 *            The name of the Object field.
	 * @return The XML-RPC struct member name the associated value should be
	 *         assigned to.
	 */
	protected String encodeFieldName(String name) {
		String encodedName = (String) fieldNameEncodingCache.get(name);
		if (encodedName != null) {
			return encodedName;
		}

		encodedName = this.fieldNameEncoder.encodeFieldName(name);
		fieldNameEncodingCache.put(name, encodedName);
		return encodedName;
	}
}
