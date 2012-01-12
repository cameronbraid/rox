package com.flat502.rox.marshal.xmlrpc;

import java.util.Iterator;

/**
 * An extremely thin interface representing the information the marshaller
 * needs from an XML parser.
 */
public interface XmlNode {
	public String getFullName();
	public String getContent();
	public int getChildrenCount();
	public XmlNode getChildAtIndex(int index);
	public Iterator enumerateChildren();
}
