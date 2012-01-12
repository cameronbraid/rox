package com.flat502.rox.http;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple wrapper for {@link java.net.URI} for
 * decomposing a URI into the components we expect
 * when mapping a GET request onto an XML-RPC method
 * call handler.
 * <p>
 * A URI is decomposed into three parts:
 * <ol>
 * <li>A {@link #getMountPoint() mount point}</li>
 * <li>A {@link #getMethodName() method name}</li>
 * <li>A {@link #getParameters() parameter} map</li>
 * </ol>
 */
public class MethodCallURI {
	private String mountPoint;
	private String methodName;
	private List parameterNames;
	private Map parameters;

	public MethodCallURI(String uri) throws URISyntaxException {
		this(new URI(uri));
	}

	public MethodCallURI(URI uri) {
		this.decompose(uri);
	}

	/**
	 * The mount point for a URI is derived by extracting the path
	 * component from the URI and <em>removing</em> the last path element.
	 * <p>
	 * Logically this acts as though the path were split on the path
	 * separator (<code>/</code>) and all but the last element of the
	 * resulting list were joined using the same separator. 
	 * <p>
	 * A <code>null</code> or empty path component results in
	 * the mount point being set to a single path separator (<code>"/"</code>).
	 * @return
	 * 	The mount point for the {@link URI} wrapped by this
	 * 	instance.
	 */
	public String getMountPoint() {
		return this.mountPoint;
	}

	/**
	 * The method name for a URI is derived by extracting the path
	 * component from the URI and <em>retaining</em> the last path element.
	 * <p>
	 * Logically this acts as though the path were split on the path
	 * separator (<code>/</code>) and only the last element of the
	 * resulting list was retained. 
	 * <p>
	 * A <code>null</code> or empty path component results in the method name
	 * being set to an empty String.
	 * @return
	 * 	The method name for the {@link URI} wrapped by this
	 * 	instance.
	 */
	public String getMethodName() {
		return this.methodName;
	}

	/**
	 * The parameters for a URI are derived by extracting the query
	 * component from the URI and constructing a {@link Map} from it.
	 * <p>
	 * This {@link Map} is never <code>null</code> and is always an instance
	 * of {@link LinkedHashMap}. Key insertion order matches the order in
	 * which query keys are specified in the URI. 
	 * <p>
	 * The value for a given key in the returned {@link Map} is one of
	 * three values:
	 * <ol>
	 * <li>A <code>null</code> value if the key was defined without a value in the URI (<code>...&amp;foo&amp;...</code>).</li>
	 * <li>A {@link String} value if the key was defined once, and with a value, in the URI (<code>...&amp;foo&amp;...</code>).</li>
	 * <li>A {@link List} instance if a key appears multiple times (<code>...&amp;foo=1&amp;...&amp;foo=2&amp;...</code>). 
	 * The {@link List} contents will reflect the values assigned to the key in the order in which they appear in the URI. 
	 * If a key appears multiple times and at least once occurrence appears with a value (<code>...&amp;foo=1&amp;...&amp;foo&amp;...</code>), 
	 * then any occurrences without a value will be reflected by a <code>null</code>
	 * value within the {@link List} instance.</li>
	 * </ol>
	 * @return
	 * 	A {@link Map} instance representing the parameters specified in the
	 * 	query component of the URI. If the query portion is <code>null</code>
	 * 	an empty {@link Map} is returned.
	 */
	public Map getParameters() {
		return this.parameters;
	}

	/**
	 * The parameter names for a URI are derived by extracting the query
	 * component from the URI and constructing an array from it.
	 * <p>
	 * This method provides additional information that may be lost in
	 * the translation from a query string to a {@link Map}. Specifically,
	 * if the same key is specified multiple times as part of the URI then
	 * even an ordered {@link Map} provides no means to determine exact parameter
	 * ordering.
	 * @return
	 * 	An array of parameter names in the order in which they occur
	 * 	in the query component. If the query component is <code>null</code>
	 * 	or empty a zero-length array is returned.
	 */
	public String[] getParameterNames() {
		return (String[]) this.parameterNames.toArray(new String[0]);
	}

	private void decompose(URI uri) {
		this.mountPoint = uri.getPath();
		if (this.mountPoint != null) {
			Matcher m = Pattern.compile("^(.*)/").matcher(this.mountPoint);
			if (m.find()) {
				this.mountPoint = m.group(1);
			}
		}
		if (this.mountPoint == null || this.mountPoint.length() == 0) {
			this.mountPoint = "/";
		}
		String[] parts = Pattern.compile("/").split(uri.getPath(), -1);
		this.methodName = parts.length > 0 ? parts[parts.length - 1] : "";
		this.parameterNames = new ArrayList();
		this.parameters = new LinkedHashMap();
		if (uri.getQuery() != null) {
			parts = uri.getQuery().split("&");
			try {
				for (int i = 0; i < parts.length; i++) {
					String[] keyVal = parts[i].split("=", 2);
					String key = URLDecoder.decode(keyVal[0], "ASCII");
					String val = keyVal.length > 1 ? URLDecoder.decode(keyVal[1], "ASCII") : null;
					this.parameterNames.add(key);
					Object prevVal = this.parameters.get(key);
					if (prevVal != null) {
						if (prevVal instanceof List) {
							((List) prevVal).add(val);
						} else {
							ArrayList list = new ArrayList();
							list.add(prevVal);
							list.add(val);
							this.parameters.put(key, list);
						}
					} else {
						this.parameters.put(key, val);
					}
				}
			} catch (UnsupportedEncodingException e) {
				// Since we're hardcoding ASCII this should never happen.
				throw (InternalError) new InternalError("ASCII encoding not installed?").initCause(e);
			}
		}
	}
}
