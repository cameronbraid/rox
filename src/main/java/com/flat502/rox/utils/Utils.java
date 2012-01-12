package com.flat502.rox.utils;

import java.io.*;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;
import java.security.Principal;
import java.security.cert.CertificateEncodingException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.x500.X500Principal;
import javax.security.cert.CertificateException;

/**
 * This class provides various static utility routines used by the rest of
 * this library.
 */
public class Utils {
	private static final Pattern HTTP_CONTENT_TYPE = Pattern.compile("^\\s*(\\S+);?");
	private static final Pattern HTTP_CONTENT_CHARSET = Pattern.compile("charset=(\\S+)");
	private static final Pattern XML_ENCODING = Pattern.compile("encoding\\s*=\\s*\"(\\S+)\"");

	private static final float JAVA_RUNTIME_VERION;
	
	static {
		JAVA_RUNTIME_VERION = Float.parseFloat(System.getProperty("java.specification.version"));
	}
	
	/**
	 * Parses the value of an HTTP <code>Content-Type</code>
	 * header and returns the specified character set.
	 * <p>
	 * If no character set is specified then <code>ISO-8859-1</code>
	 * is returned as per 
	 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7.1">section 3.7.1</a>
	 * of RFC 2616.
	 * @param contentType
	 * 	The value of the <code>Content-Type</code> header.
	 * @return
	 * 	The character set specified by the value or 
	 * 	<code>ISO-8859-1</code> if none was found.
	 */
	public static Charset extractContentCharset(String contentType) {
		Matcher m = HTTP_CONTENT_CHARSET.matcher(contentType);
		if (!m.find()) {
			// HTTP 1.1 spec (RFC 2616 section 3.7.1)
			return Charset.forName("ISO-8859-1");
		}
		return Charset.forName(m.group(1));
	}

	/**
	 * Parses the value of an HTTP <code>Content-Type</code>
	 * header and returns the Internet Media Type
	 * type and sub-type separated by a forwards-slash.
	 * <p>
	 * Any additional parameters (such as character set
	 * information) is discarded.
	 * <p>
	 * An example of a return value would be <code>text/xml</code>.
	 * @param contentType
	 * 	The value of the <code>Content-Type</code> header.
	 * @return
	 * 	The content type.
	 * @throws IllegalArgumentException
	 * 	if the value is malformed.
	 */
	public static String extractContentType(String contentType) {
		Matcher m = HTTP_CONTENT_TYPE.matcher(contentType);
		if (!m.find()) {
			throw new IllegalArgumentException("Malformed Content-Type: [" + contentType + "]");
		}
		String type = m.group(1);
		if (type.endsWith(";")) {
			return type.substring(0, type.length() - 1);
		}
		return type;
	}

	// TODO: Document
	public static String normalizeURIPath(String uri) {
		try {
			uri = new URI(uri).normalize().getPath();
		} catch (URISyntaxException e) {
			throw (IllegalArgumentException)new IllegalArgumentException("Malformed URI [" + uri + "]").initCause(e);
		}
		if (uri.equals("")) {
			uri = "/";
		} else if (!uri.equals("/") && uri.endsWith("/")) {
			uri = uri.substring(0, uri.length() - 1);
		}
		return uri;
	}

	/**
	 * Converts an {@link java.io.InputStream} containing XML into a
	 * {@link Reader} using the encoding specified in the XML header.
	 * <p>
	 * The encoding is determined as specified by
	 * <a href="http://www.w3.org/TR/1998/REC-xml-19980210.html#sec-guessing">appendix F</a>
	 * of version 1.0 of the XML specification.
	 * @param in
	 * 	The input stream the XML document should be read from.
	 * @param hint
	 * 	An optional hint (may be <code>null</code>) if the caller
	 * 	has an idea of what the encoding may be.
	 * @return
	 * 	A {@link Reader} instance backed by the given
	 * 	{@link java.io.InputStream} using the encoding specified
	 * 	by the XML document within the stream. 
	 * @throws Exception
	 * 	if the underlying encoding cannot be determined, or
	 * 	if an error occurs while constructing a reader to return.
	 */
	public static Reader newXmlReader(InputStream in, Charset hint) throws Exception {
		BufferedInputStream bufferedIn = new BufferedInputStream(in);
		bufferedIn.mark(4);
		int byteOne = bufferedIn.read();
		int byteTwo = bufferedIn.read();
		if (byteOne == -1 || byteTwo == -1) {
			throw new IOException("Unable to determine stream encoding: insufficient data");
		}

		Charset charSet = null;
		if (byteOne == 0xFE && byteTwo == 0xFF) {
			// FE FF: UTF-16, big-endian
			charSet = Charset.forName("UTF-16BE");
		} else if (byteOne == 0xFE && byteTwo == 0xFF) {
			// FF FE: UTF-16, little-endian
			charSet = Charset.forName("UTF-16LE");
		} else {
			int byteThree = bufferedIn.read();
			int byteFour = bufferedIn.read();
			if (byteThree == -1 || byteFour == -1) {
				throw new IOException("Unable to determine stream encoding: insufficient data");
			}
			switch (byteOne) {
			case 0x00:
				if (byteTwo == 0x00 && byteThree == 0x00 && byteFour == 0x3C) {
					// 00 00 00 3C: UCS-4, big-endian machine (1234 order)
					throw new UnsupportedEncodingException("UCS-4, big-endian");
				} else if (byteTwo == 0x00 && byteThree == 0x3C && byteFour == 0x00) {
					// 00 00 3C 00: UCS-4, unusual octet order (2143)
					throw new UnsupportedEncodingException("UCS-4, 2134 octet ordering");
				} else if (byteTwo == 0x3C && byteThree == 0x00 && byteFour == 0x00) {
					// 00 3C 00 00: UCS-4, unusual octet order (3412)
					throw new UnsupportedEncodingException("UCS-4, 3412 octet ordering");
				} else if (byteTwo == 0x00 && byteThree == 0x00 && byteFour == 0x3F) {
					// 00 3C 00 3F: UTF-16, big-endian, no Byte Order Mark (and thus, strictly speaking, in error)
					charSet = Charset.forName("UTF-16BE");
				}
				break;
			case 0x3C:
				if (byteTwo == 0x00 && byteThree == 0x00 && byteFour == 0x00) {
					// 3C 00 00 00: UCS-4, little-endian machine (4321 order)
					throw new UnsupportedEncodingException("UCS-4, little-endian");
				} else if (byteTwo == 0x00 && byteThree == 0x3F && byteFour == 0x00) {
					// 3C 00 3F 00: UTF-16, little-endian, no Byte Order Mark (and thus, strictly speaking, in error)
					charSet = Charset.forName("UTF-16LE");
				} else if (byteTwo == 0x3F && byteThree == 0x78 && byteFour == 0x6D) {
					// 3C 3F 78 6D: UTF-8, ISO 646, ASCII, some part of ISO 8859, Shift-JIS, EUC, or any other 7-bit, 8-bit, or mixed-width encoding which ensures that the characters of ASCII have their normal positions, width, and values; the actual encoding declaration must be read to detect which of these applies, but since all of these encodings use the same bit patterns for the ASCII characters, the encoding declaration itself may be read reliably
					charSet = Charset.forName("UTF-8");
				}
				break;
			case 0x4C:
				// 4C 6F A7 94: EBCDIC (in some flavor; the full encoding declaration must be read 
				// to tell which code page is in use)
				if (byteTwo == 0x6F && byteThree == 0xA7 && byteFour == 0x94) {
					throw new UnsupportedEncodingException("EBCDIC");
				}
				break;
			default:
				// other: UTF-8 without an encoding declaration, 
				// or else the data stream is corrupt, fragmentary, 
				// or enclosed in a wrapper of some kind
				charSet = Charset.forName("UTF-8");
				break;
			}
		}
		bufferedIn.reset();

		if (charSet == null) {
			if (hint != null) {
				// Use the hint and check if it works
				charSet = hint;
			}
		}

		// Now that we have a starting point, decode the XML header and
		// check what encoding it specifies.
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bufferedIn, charSet));
		bufferedReader.mark(200);
		String line = readXMLHeader(bufferedReader, 200);
		Matcher m = XML_ENCODING.matcher(line);
		if (m.find()) {
			charSet = Charset.forName(m.group(1));
		}
		bufferedReader.reset();

		if (charSet == null) {
			throw new IOException("Unable to determine stream encoding");
		}

		return bufferedReader;
	}

	private static String readXMLHeader(BufferedReader bufferedReader, int maxChars) throws IOException {
		StringBuffer lineBuf = new StringBuffer();
		char prevCh = 0;
		char ch = 0;
		while (true) {
			ch = (char) bufferedReader.read();
			lineBuf.append(ch);
			if (prevCh == '?' && ch == '>') {
				// We're done
				return lineBuf.toString();
			}
			prevCh = ch;
			if (--maxChars == 0) {
				throw new UnsupportedEncodingException("Unable to locate XML header in inputstream within 200 chars");
			}
		}
	}

	private static long startTime = System.currentTimeMillis();

	/**
	 * A very simple utility routine to aid debugging.
	 * <p>
	 * The message is printed to stdout, prefixed with the
	 * name of the calling thread and the interval in milliseconds
	 * since this class was loaded. This is handy for simple "println"
	 * style debugging when relative timing is a factor.
	 * @param msg
	 * 	The message to display.
	 */
	public static void dbgPrintln(String msg) {
		long ts = System.currentTimeMillis() - startTime;
		System.out.println(ts + ": " + Thread.currentThread().getName() + ": " + msg);
	}
	
	/**
	 * Normalizes an HTTP header name.
	 * <p>
	 * Normalization produces the logical equivalent
	 * of the following algorithm.
	 * <p>
	 * The name is split into words using the
	 * '-' character. Each word is then normalized
	 * by converting the first character to uppercase
	 * and all remaining characters to lowercase.
	 * The resulting words are joined again using
	 * the '-' character. The result is returned. 
	 * @param name
	 * 	The header name.
	 * @return
	 * 	The normalized header name.
	 */
	public static String normalizeHttpHeaderName(String name) {
		char[] chars = name.toCharArray();
		int newWord = -1;
		for (int i = 0; i < chars.length; i++) {
			if (chars[i] == '-') {
				newWord = i;
			}
			if (i == newWord + 1) {
				newWord = -1;
				chars[i] = Character.toUpperCase(chars[i]);
			} else {
				chars[i] = Character.toLowerCase(chars[i]);
			}
		}
		return new String(chars);
	}
	
	public static String join(String delim, Object[] values) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < values.length; i++) {
			sb.append(values[i]);
			if (i < values.length-1) {
				sb.append(delim);
			}
		}
		return sb.toString();
	}
	
	public static String join(String delim, Iterator iter) {
		StringBuffer sb = new StringBuffer();
		if (iter.hasNext()) {
			sb.append(iter.next());
		}
		while (iter.hasNext()) {
			sb.append(delim);
			sb.append(iter.next());
		}
		return sb.toString();
	}
	
	public static Object toArray(Class targetType, Object value) {
		if (targetType == Object[].class) {
			return ((List) value).toArray();
		} else if (targetType == int[].class) {
			return toIntArray((List) value);
		} else if (targetType == long[].class) {
			return toLongArray((List) value);
		} else if (targetType == boolean[].class) {
			return toBooleanArray((List) value);
		} else if (targetType == float[].class) {
			return toFloatArray((List) value);
		} else if (targetType == double[].class) {
			return toDoubleArray((List) value);
		}
		// value is a List with members of the appropriate type
		Object[] arrayTemplate = (Object[]) Array.newInstance(targetType.getComponentType(), 0);
		return ((List) value).toArray(arrayTemplate);
	}

	public static int[] toIntArray(List list) {
		int[] array = new int[list.size()];
		Iterator ints = list.iterator();
		int idx = 0;
		while (ints.hasNext()) {
			Integer value = (Integer) ints.next();
			array[idx++] = value.intValue();
		}
		return array;
	}

	public static long[] toLongArray(List list) {
		long[] array = new long[list.size()];
		Iterator ints = list.iterator();
		int idx = 0;
		while (ints.hasNext()) {
			Integer value = (Integer) ints.next();
			array[idx++] = value.intValue();
		}
		return array;
	}

	public static boolean[] toBooleanArray(List list) {
		boolean[] array = new boolean[list.size()];
		Iterator ints = list.iterator();
		int idx = 0;
		while (ints.hasNext()) {
			Boolean value = (Boolean) ints.next();
			array[idx++] = value.booleanValue();
		}
		return array;
	}

	public static float[] toFloatArray(List list) {
		float[] array = new float[list.size()];
		Iterator ints = list.iterator();
		int idx = 0;
		while (ints.hasNext()) {
			Double value = (Double) ints.next();
			array[idx++] = value.floatValue();
		}
		return array;
	}

	public static double[] toDoubleArray(List list) {
		double[] array = new double[list.size()];
		Iterator ints = list.iterator();
		int idx = 0;
		while (ints.hasNext()) {
			Double value = (Double) ints.next();
			array[idx++] = value.doubleValue();
		}
		return array;
	}

	public static Object coerce(Object value, Class targetType) {
		Class valueClass = value.getClass();
		if (targetType.isAssignableFrom(value.getClass())) {
			// Already compatible
			return value;
		} else
		if (valueClass == Integer.class) {
			if (targetType == Integer.class || targetType == Integer.TYPE) {
				return value;
			}
			if (targetType == Long.class || targetType == Long.TYPE) {
				return new Long(((Integer) value).intValue());
			}
		} else if (valueClass == Double.class) {
			if (targetType == Double.class || targetType == Double.TYPE) {
				return value;
			}
			if (targetType == Float.class || targetType == Float.TYPE) {
				return new Float(((Double) value).floatValue());
			}
		} else if (valueClass == Boolean.class) {
			if (targetType == Boolean.class || targetType == Boolean.TYPE) {
				return value;
			}
		} else if (valueClass == String.class) {
			if (targetType == String.class) {
				return value;
			}
			if (targetType == char[].class) {
				String valueAsString = (String) value;
				return valueAsString.toCharArray();
			}
			if (targetType == Character[].class) {
				String valueAsString = (String) value;
				Character[] chars = new Character[valueAsString.length()];
				for (int i = 0; i < chars.length; i++) {
					chars[i] = new Character(valueAsString.charAt(i));
				}
				return chars;
			}
		} else if (valueClass == byte[].class) {
			if (targetType == byte[].class) {
				return value;
			}
		} else if (valueClass == Date.class) {
			if (targetType == Date.class) {
				return value;
			}
		} else if (List.class.isAssignableFrom(valueClass)) {
			if (targetType.isArray()) {
				return toArray(targetType, value);
			}
			return Utils.convert(value, targetType);
		} else if (valueClass == targetType) {
			return value;
		}
	
		return null;
	}

	public static Object convert(Object value, Class targetType) {
		if (value == null) {
			return null;
		}
		if (targetType == value.getClass()) {
			return value;
		}
		if (targetType == Integer.class || targetType == Integer.TYPE) {
			return Integer.valueOf(Integer.parseInt((String)value));
		}
		if (targetType == Long.class || targetType == Long.TYPE) {
			return Long.valueOf(Long.parseLong((String)value));
		}
		if (targetType == Double.class || targetType == Double.TYPE) {
			return Double.valueOf(Double.parseDouble((String)value));
		}
		if (targetType == Float.class || targetType == Float.TYPE) {
			return Float.valueOf(Float.parseFloat((String)value));
		}
		if (targetType == Boolean.class || targetType == Boolean.TYPE) {
			return Boolean.valueOf(Boolean.parseBoolean((String)value));
		}
		if (targetType == char[].class) {
			return ((String)value).toCharArray();
		}
		if (targetType == Character[].class) {
			String valueStr = (String)value;
			Character[] chars = new Character[valueStr.length()];
			for (int i = 0; i < chars.length; i++) {
				chars[i] = new Character(valueStr.charAt(i));
			}
			return chars;
		}
		if (targetType.isArray()) {
			return convertToArray(targetType, value);
		}
		return value;
	}
	
	public static Object convertToArray(Class targetType, Object value) {
		if (targetType == Object[].class) {
			return ((List) value).toArray();
		} else if (targetType == int[].class) {
			return convertToIntArray((List) value);
		} else if (targetType == long[].class) {
			return convertToLongArray((List) value);
		} else if (targetType == boolean[].class) {
			return convertToBooleanArray((List) value);
		} else if (targetType == float[].class) {
			return convertToFloatArray((List) value);
		} else if (targetType == double[].class) {
			return convertToDoubleArray((List) value);
		}
		// value is a List with members of the appropriate type
		Object[] arrayTemplate = (Object[]) Array.newInstance(targetType.getComponentType(), 0);
		return ((List) value).toArray(arrayTemplate);
	}

	public static int[] convertToIntArray(List list) {
		int[] array = new int[list.size()];
		Iterator ints = list.iterator();
		int idx = 0;
		while (ints.hasNext()) {
			array[idx++] = Integer.parseInt((String) ints.next());
		}
		return array;
	}

	public static long[] convertToLongArray(List list) {
		long[] array = new long[list.size()];
		Iterator ints = list.iterator();
		int idx = 0;
		while (ints.hasNext()) {
			array[idx++] = Long.parseLong((String) ints.next());
		}
		return array;
	}

	public static boolean[] convertToBooleanArray(List list) {
		boolean[] array = new boolean[list.size()];
		Iterator ints = list.iterator();
		int idx = 0;
		while (ints.hasNext()) {
			array[idx++] = Boolean.parseBoolean((String) ints.next());
		}
		return array;
	}

	public static float[] convertToFloatArray(List list) {
		float[] array = new float[list.size()];
		Iterator ints = list.iterator();
		int idx = 0;
		while (ints.hasNext()) {
			array[idx++] = Float.parseFloat((String) ints.next());
		}
		return array;
	}

	public static double[] convertToDoubleArray(List list) {
		double[] array = new double[list.size()];
		Iterator ints = list.iterator();
		int idx = 0;
		while (ints.hasNext()) {
			array[idx++] = Double.parseDouble((String) ints.next());
		}
		return array;
	}

	public static String toHexDump(byte[] data) {
		return toHexDump(data, 0, data.length);
	}
	
	public static String toHexDump(byte[] data, int offset, int length) {
		StringBuffer out = new StringBuffer(data.length * 4);
		String three_spaces = "   ";
		int old_offset = 0;
		int end_offset = offset + length;
		int i;
		while (offset < end_offset) {
			StringBuffer offsetStr = new StringBuffer(String.valueOf(offset));
			while (offsetStr.length() < 4) {
				offsetStr.insert(0, '0');
			}
			out.append(offsetStr + "(" + toHexString(offset) + ")  ");
			old_offset = offset;
			for (i = 0; (i < 16); i++, offset++) {
				if (offset < end_offset) {
					out.append(toHexString(data[offset]) + " ");
				} else {
					out.append(three_spaces);
				}
				if (i == 7) {
					out.append(" ");
				}
			}
			offset = old_offset;
			out.append("  ");
			for (i = 0; (i < 16) && (offset < end_offset); i++, offset++) {
				if (data[offset] < 0x20) {
					out.append(".");
				} else {
					out.append(new String(data, offset, 1));
				}
			}
			out.append("\n");
		}

		return new String(out);
	}

	private static String toHexString(int value, int width) {
		StringBuffer s = new StringBuffer(Integer.toHexString(value).toUpperCase());
		int pad = width - s.length();
		for (int i = 0; i < pad; i++) {
			s.insert(0, "0");
		}
		return s.toString();
	}

	private static String toHexString(int value) {
		return toHexString(value, 4);
	}

	private static String toHexString(byte value) {
		if (value < 0) {
			return toHexString(256 + value, 2);
		}
		return toHexString(value, 2);
	}

	public static String toString(int[] list) {
		StringBuffer sb = new StringBuffer("[");
		for (int i = 0; i < list.length; i++) {
			sb.append(list[i]);
			if (i < list.length-1) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	public static String toString(boolean[] list) {
		StringBuffer sb = new StringBuffer("[");
		for (int i = 0; i < list.length; i++) {
			sb.append(list[i]);
			if (i < list.length-1) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	public static String toString(boolean[][] list) {
		StringBuffer sb = new StringBuffer("[");
		for (int i = 0; i < list.length; i++) {
			sb.append(toString(list[i]));
			if (i < list.length-1) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	public static String toString(Object[] list) {
		StringBuffer sb = new StringBuffer("[");
		for (int i = 0; i < list.length; i++) {
			sb.append(list[i]);
			if (i < list.length-1) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}
	
	public static String toString(Socket socket) {
		if (socket == null) {
			return "socket[null]";
		}
		return "socket[" + System.identityHashCode(socket) + "]";
	}
	
	public static String toString(ServerSocket socket) {
		if (socket == null) {
			return "server-socket[null]";
		}
		return "server-socket[" + System.identityHashCode(socket) + "]";
	}
	
	public static String toString(SelectionKey key) {
		if (key == null) {
			return "selection-key[null]";
		}
		
		try {
			String v = key.isValid() ? "valid" : "invalid";
			String a = key.isAcceptable() ? "acceptable" : "not acceptable";
			String r = key.isReadable() ? "readable" : "not readable";
			String w = key.isWritable() ? "writable" : "not writable";
			return "selection-key[" + v + ", " + a + ", " + r + ", " + w + "]";
		} catch (CancelledKeyException e) {
			return "selection-key[cancelled]";
		}
	}
	
	public static String toString(SSLSession session) {
		if (session == null) {
			return "session[null]";
		}
		String peer;
		try {
			peer = toString(session.getPeerPrincipal());
		} catch (SSLPeerUnverifiedException e) {
			peer = "unverified";
		}
		return "session[identity=" + toString(session.getLocalPrincipal()) + ", peer="+peer+"]";
	}
	
	public static String toString(Principal p) {
		if (p == null) {
			return "null";
		}

		if (p instanceof X500Principal) {
			X500Principal x = (X500Principal) p;
			return "(x500:"+x.getName(X500Principal.CANONICAL)+")";
		}
		
		return "(" + p + ")";
	}
	
	public static float getJavaRuntimeVersion() {
		return JAVA_RUNTIME_VERION;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T[] resize(T[] src, int newsize) {
		if (newsize == src.length) {
			return src;
		}
		
		T[] dst = (T[]) new Object[newsize];
		System.arraycopy(src, 0, dst, 0, Math.min(src.length, newsize));
		return dst;
	}
	
   public static java.security.cert.X509Certificate convert(javax.security.cert.X509Certificate cert) throws javax.security.cert.CertificateEncodingException, java.security.cert.CertificateException {
   	byte[] encoded = cert.getEncoded();
		ByteArrayInputStream bis = new ByteArrayInputStream(encoded);
		java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
		return (java.security.cert.X509Certificate) cf.generateCertificate(bis);
   }
   
   // Converts to javax.security
   public static javax.security.cert.X509Certificate convert(java.security.cert.X509Certificate cert) throws CertificateEncodingException, CertificateException {
   	byte[] encoded = cert.getEncoded();
		return javax.security.cert.X509Certificate.getInstance(encoded);
	}
   
   public static String[] splitAt(String input, String marker) {
	   int index = input.indexOf(marker);
	   if (index == -1) {
		   return new String[]{input, ""};
	   }
	   return new String[]{input.substring(0, index), input.substring(index)};
	}

	public static void main(String[] args) {
		System.out.println(toHexString((byte) 0x15));
		System.out.println(toHexString((byte) 0x6A));
		System.out.println(toHexString((byte) 0x8A));
		System.out.println(toHexString((byte) 0xED));
		System.out.println(toHexDump(new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 }));
	}
}
