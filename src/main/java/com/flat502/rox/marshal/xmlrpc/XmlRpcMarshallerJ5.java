package com.flat502.rox.marshal.xmlrpc;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.util.Map;

import com.flat502.rox.marshal.ClassDescriptor;
import com.flat502.rox.marshal.FieldNameEncoder;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.utils.XmlPrinter;

/**
 * A sub-class of {@link com.flat502.rox.marshal.xmlrpc.XmlRpcMarshaller}
 * providing support for Java 5.
 * <p>
 * This support essentially boils down to handling {@link java.lang.Enum}
 * types and generically typed collections.
 */
public class XmlRpcMarshallerJ5 extends XmlRpcMarshaller {
	protected XmlRpcMarshallerJ5(FieldNameEncoder fieldNameEncoder) {
		super(fieldNameEncoder);
	}
	
	@Override
	protected void marshalObject(XmlPrinter out, int depth, Object param) throws MarshallingException, IOException {
		Class paramClass = param.getClass();
		if (param instanceof Enum) {
			// Enumerated types are marshalled as string values
			String tag = this.getStringTagName();
			if (tag != null) {
				out.openTag(tag);
				this.marshalValue(out, ((Enum)param).name());
				out.closeTag(tag);
			} else {
				this.marshalValue(out, (String) param);
			}
		} else {
			super.marshalObject(out, depth, param);
		}
	}
}
