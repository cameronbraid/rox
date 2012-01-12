package com.flat502.rox.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

/**
 * An implementation of {@link com.flat502.rox.utils.XmlPrinter}
 * that produces compact XML.
 * <p>
 * This implementation avoids uses line breaks and any indentation.
 * As a result this implementation produces relatively efficient
 * XML at the cost of readability.
 */
public class XmlPlainPrinter implements XmlPrinter {
	private OutputStream out;
	private String lastOpened;
	private String lastValue;
	private String charSetName;

	public XmlPlainPrinter(OutputStream out) {
		this.out = out;
	}

	public void writeHeader(String version, Charset charSet) throws IOException {
		this.charSetName = charSet.name();
		this.out.write("<?xml version=\"".getBytes(this.charSetName));
		this.out.write(version.getBytes(this.charSetName));
		this.out.write("\" encoding=\"".getBytes(this.charSetName));
		this.out.write(charSet.name().getBytes(this.charSetName));
		this.out.write('"');
		this.out.write('?');
		this.out.write('>');
	}

	public void openTag(String name) throws IOException {
		if (this.lastOpened != null) {
			this.out.write('<');
			this.out.write(this.lastOpened.getBytes(this.charSetName));
			this.out.write('>');
		}
		this.lastOpened = name;
	}

	public void writeValue(String value) throws IOException {
		this.out.write('<');
		this.out.write(this.lastOpened.getBytes(this.charSetName));
		this.out.write('>');
		this.out.write(value.getBytes(this.charSetName));
		this.lastValue = value;
	}

	public void closeTag(String name) throws IOException {
		if (name.equals(this.lastOpened) && this.lastValue == null) {
			this.out.write('<');
			this.out.write(name.getBytes(this.charSetName));
			this.out.write('/');
			this.out.write('>');
		} else {
			this.out.write('<');
			this.out.write('/');
			this.out.write(name.getBytes(this.charSetName));
			this.out.write('>');
		}
		this.lastOpened = null;
		this.lastValue = null;
	}

	public void finishDocument() throws IOException {
	}
}
