package com.flat502.rox;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Exposes version information for Rox.
 * <p>
 * The version information in this Class is captured at build time and
 * is available for programmatic access in addition to being used to
 * construct the default values for the HTTP UserAgent and Server headers.
 */
public class Version {
	private static final Pattern REV = Pattern.compile("\\$Rev:\\s*(\\d+)\\s*\\$");
	
	// These are updated at build time.
	private static final int major = 1/*MAJOR*/;
	private static final int minor = 2/*MINOR*/;
	private static final int rev;
	private static final String author = "James Greenfield"/*AUTHOR*/;
	
	static {
		Matcher m = REV.matcher("$Rev: 16644 $");
		if (m.find()) {
			rev = Integer.parseInt(m.group(1));
		} else {
			rev = 0;
		}
	}

	private static final String os = getOsString();
	private static final String name = "RoX/" + major + "." + minor + "." + rev + " " + os;

	private static String getOsString() {
		return System.getProperty("os.name") + " "
				+ System.getProperty("os.version") + " ("
				+ System.getProperty("os.arch") + ")";
	}
	
	public static int getMajorVersion() {
		return major;
	}

	public static int getMinorVersion() {
		return minor;
	}
	
	public static int getRevision() {
		return rev;
	}
	
	public static String getDescription() {
		return name;
	}

	public static void main(String[] args) {
		System.out.println(name);
	}
}
