package com.flat502.rox.marshal.xmlrpc;

import java.util.HashSet;
import java.util.Set;

/**
 * Various constants defined by the XML-RPC specification.
 */
public interface XmlRpcConstants {
	/**
	 * Constants defining the tag names used for XML-RPC
	 * values.
	 */
	public static interface Types {
		public static final String STRING = "string";
		public static final String INT1 = "int";
		public static final String INT2 = "i4";
		public static final String DOUBLE = "double";
		public static final String BOOLEAN = "boolean";
		public static final String ISO8601DATE = "dateTime.iso8601";
		public static final String BASE64 = "base64";
		public static final String STRUCT = "struct";
		public static final String ARRAY = "array";
	}

	public static class Tags {
		public static final String METHOD_CALL = "methodCall";
		public static final String METHOD_NAME = "methodName";
		public static final String PARAMS = "params";
		public static final String PARAM = "param";
		public static final String VALUE = "value";
		public static final String INT = Types.INT1;
		public static final String I4 = Types.INT2;
		public static final String BOOLEAN = Types.BOOLEAN;
		public static final String DOUBLE = Types.DOUBLE;
		public static final String STRING = Types.STRING;
		public static final String DATETIME = Types.ISO8601DATE;
		public static final String BASE64 = Types.BASE64;
		public static final String STRUCT = "struct";
		public static final String MEMBER = "member";
		public static final String NAME = "name";
		public static final String ARRAY = "array";
		public static final String DATA = "data";
		public static final String METHOD_RESPONSE = "methodResponse";
		public static final String FAULT = "fault";

		private static final Set<String> VALID_TAGS = new HashSet<String>();

		static {
			VALID_TAGS.add(METHOD_CALL);
			VALID_TAGS.add(METHOD_NAME);
			VALID_TAGS.add(PARAMS);
			VALID_TAGS.add(PARAM);
			VALID_TAGS.add(VALUE);
			VALID_TAGS.add(INT);
			VALID_TAGS.add(I4);
			VALID_TAGS.add(BOOLEAN);
			VALID_TAGS.add(DOUBLE);
			VALID_TAGS.add(STRING);
			VALID_TAGS.add(DATETIME);
			VALID_TAGS.add(BASE64);
			VALID_TAGS.add(STRUCT);
			VALID_TAGS.add(MEMBER);
			VALID_TAGS.add(NAME);
			VALID_TAGS.add(ARRAY);
			VALID_TAGS.add(DATA);
			VALID_TAGS.add(METHOD_RESPONSE);
			VALID_TAGS.add(FAULT);
		}
		
		public static boolean isValid(String name) {
			return VALID_TAGS.contains(name);
		}
	}
	
	/**
	 * Various constants defined miscellaneous formatting
	 * conventions defined by the XML-RPC specification.
	 */
	public static interface Formats {
		public static final String DATE_FORMAT = "yyyyMMdd'T'HH:mm:ss";
	}

	public static interface ContentTypes {
		public static final String TEXT_XML = "text/xml";
	}
}
