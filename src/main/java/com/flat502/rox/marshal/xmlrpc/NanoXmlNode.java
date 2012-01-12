package com.flat502.rox.marshal.xmlrpc;

import java.util.Enumeration;
import java.util.Iterator;

import net.n3.nanoxml.IXMLElement;

class NanoXmlNode implements XmlNode {
	private IXMLElement element;

	public NanoXmlNode(IXMLElement element) {
		this.element = element;
	}

	public String getFullName() {
		return this.element.getFullName();
	}

	public String getContent() {
		String content = this.element.getContent();
		if (content == null) {
			return "";
		}
		return content;
	}

	public int getChildrenCount() {
		return this.element.getChildrenCount();
	}

	public XmlNode getChildAtIndex(int index) {
		return new NanoXmlNode(this.element.getChildAtIndex(index));
	}

	public Iterator enumerateChildren() {
		return new NanoIterator(this.element.enumerateChildren());
	}

	private class NanoIterator implements Iterator {
		private Enumeration enumeration;

		public NanoIterator(Enumeration enumeration) {
			this.enumeration = enumeration;
		}

		public boolean hasNext() {
			return this.enumeration.hasMoreElements();
		}

		public Object next() {
			return new NanoXmlNode((IXMLElement) this.enumeration.nextElement());
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
