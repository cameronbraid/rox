package com.flat502.rox.utils;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;

/**
 * An implementation of {@link com.flat502.rox.utils.XmlPrinter}
 * that produces XML formatted for readability.
 * <p>
 * This implementation uses line breaks and indentation to format
 * XML in a way that is more easily consumed by mere mortals.
 * As a result this implementation produces somewhat inefficient
 * XML.
 */
public class XmlPrettyPrinter implements XmlPrinter {
	private PrintStream out;
	private int closeCount = 0;
	private int depth = 0;
	private String currentTag;
	private String currentValue;

	public XmlPrettyPrinter(OutputStream out) {
		this(new PrintStream(out));
	}

	public XmlPrettyPrinter(PrintStream out) {
		this.out = out;
	}

	public void writeHeader(String version, Charset charSet) {
		this.out.println("<?xml version=\"" + version + "\" encoding=\""
				+ charSet.name() + "\"?>");
	}

	public void openTag(String name) {
		if (this.currentTag != null) {
			this.out
					.println(this.indent(this.depth) + "<" + this.currentTag + ">");
			if (this.currentValue != null) {
				throw new IllegalStateException(
						"I don't know how to cope with values inside non-leaf elements: "
								+ name);
			}
			this.depth++;
		}
		this.currentTag = name;
		this.closeCount = 0;
	}

	public void writeValue(String value) {
		if (this.currentTag != null) {
			this.out.print(this.indent(this.depth) + "<" + this.currentTag + ">");
		}
		this.out.print(value);
		this.currentValue = value;
	}

	public void closeTag(String name) {
		if (this.closeCount > 0) {
			this.depth--;
		}
		this.closeCount++;

		if (this.currentValue != null) {
			this.out.println("</" + name + ">");
		} else {
			if (name.equals(this.currentTag)) {
				this.out.println(this.indent(this.depth) + "<" + name + "/>");
			} else {
				this.out.println(this.indent(this.depth) + "</" + name + ">");
			}
		}
		this.currentValue = null;
		this.currentTag = null;
	}

	public void finishDocument() {
	}

	private String indent(int depth) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < depth; i++) {
			sb.append("   ");
		}
		return sb.toString();
	}

	public static void main(String[] args) {
		XmlPrettyPrinter pp = new XmlPrettyPrinter(System.out);
		pp.openTag("methodCall");
		pp.openTag("methodName");
		pp.writeValue("somevalue");
		pp.closeTag("methodName");
		pp.openTag("params");
		pp.openTag("param");
		pp.writeValue("somevalue");
		pp.closeTag("param");
		pp.openTag("list");
		pp.openTag("value");
		pp.writeValue("somevalue");
		pp.closeTag("value");
		pp.openTag("value");
		pp.writeValue("somevalue");
		pp.closeTag("value");
		pp.openTag("value");
		pp.writeValue("somevalue");
		pp.closeTag("value");
		pp.closeTag("list");
		pp.closeTag("params");
		pp.openTag("empty");
		pp.closeTag("empty");
		pp.closeTag("methodCall");
	}
}
