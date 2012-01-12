package com.flat502.rox.marshal.xmlrpc;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.n3.nanoxml.IXMLElement;
import net.n3.nanoxml.IXMLParser;
import net.n3.nanoxml.IXMLReader;
import net.n3.nanoxml.StdXMLReader;
import net.n3.nanoxml.XMLParserFactory;

import com.flat502.rox.marshal.*;
import com.flat502.rox.marshal.ClassDescriptor;
import com.flat502.rox.marshal.FieldNameCodec;
import com.flat502.rox.marshal.FieldNameEncoder;
import com.flat502.rox.marshal.HyphenatedFieldNameCodec;
import com.flat502.rox.marshal.MarshallingException;
import com.flat502.rox.marshal.MethodUnmarshaller;
import com.flat502.rox.marshal.UnmarshallerAid;
import com.flat502.rox.utils.DateFormatThreadLocal;
import com.flat502.rox.utils.UTCSimpleDateFormat;
import com.flat502.rox.utils.Utils;

/**
 * Abstract base class containing common unmarshalling logic
 * for the DOM based unmarshaller.
 */
public class DomUnmarshaller extends XmlRpcMethodUnmarshaller {
	private static final int SIMPLE_TYPE = 0;

	private static final int COMPLEX_TYPE_STRUCT = 1;

	private static final int COMPLEX_TYPE_ARRAY = 2;

	/**
	 * Centralizes common initialization.
	 * <p>
	 * This constructor calls {@link #newDateFormat()} 
	 * to initialize the underlying date formatter.
	 * @param fieldNameCodec
	 * 	An implementation of {@link FieldNameCodec} used when
	 * 	struct members are unmarshalled. May be <code>null</code>.
	 */
	protected DomUnmarshaller(FieldNameCodec fieldNameCodec) {
		super(fieldNameCodec);
	}

	protected XmlNode parse(Reader in) throws Exception {
		IXMLParser parser = XMLParserFactory.createDefaultXMLParser();
		IXMLReader reader = new StdXMLReader(in);
		parser.setReader(reader);
		IXMLElement root = (IXMLElement) parser.parse();
		return new NanoXmlNode(root);
	}

	/**
	 * Assert that the given node is named as expected.
	 * 
	 * @param node
	 *            The node to check.
	 * @param expected
	 *            The expected name.
	 * @throws MarshallingException
	 *             If the assertion fails.
	 */
	protected void expectTag(XmlNode node, String expected) throws MarshallingException {
		if (!node.getFullName().equals(expected)) {
			throw new MarshallingException("Expected '" + expected + "', found '" + node.getFullName() + "'");
		}
	}

	protected Object parseParam(XmlNode node, Class structClass, UnmarshallerAid aid) throws MarshallingException {
		expectTag(node, "param");
		if (node.getChildrenCount() != 1) {
			throw new MarshallingException("Expected exactly 1 value under '" + node.getFullName() + "', found "
					+ node.getChildrenCount());
		}

		return this.parseValue(node.getChildAtIndex(0), structClass, aid);
	}

	protected Object parseValue(XmlNode value, UnmarshallerAid aid) throws MarshallingException {
		return this.parseValue(value, null/*HashMap.class*/, aid);
	}

	/**
	 * Called to parse an XML-RPC &lt;value&gt; tag.
	 * <p>
	 * If the current XML-RPC value is a struct and <code>structClass</code>
	 * is any concrete implementation of {@link Map}, then values are stored using
	 * {@link Map#put(Object, Object)}. If <code>structClass</code> is not a
	 * map it is initialized by mapping the struct members onto public fields
	 * and accessor methods.
	 * 
	 * @param value
	 *            The root of the XML-RPC value sub-document.
	 * @param structClass
	 *            The {@link Class} to instantiate if this value is a struct.
	 * @return An Object representing the value within the XML sub-document
	 *         rooted at <code>value</code>.
	 * @throws MarshallingException
	 *             If the XML document is malformed or an error occurs
	 *             unmarshalling the current value or a member value (in the
	 *             case of complex structures).
	 */
	protected Object parseValue(XmlNode value, Class structClass, UnmarshallerAid aid) throws MarshallingException {
		expectTag(value, "value");
		if (value.getChildrenCount() == 0) {
			// Implicit string type
			return this.parseString(value.getContent(), structClass);
		}

		if (value.getChildrenCount() != 1) {
			throw new MarshallingException("Expected exactly 1 type under '" + value.getFullName() + "', found "
					+ value.getChildrenCount());
		}
		XmlNode type = value.getChildAtIndex(0);
		String typeName = type.getFullName();
		if (typeName.equals(Types.INT1) || typeName.equals(Types.INT2)) {
			return parseInt(type.getContent());
		}
		if (typeName.equals(Types.BOOLEAN)) {
			return parseBoolean(type.getContent());
		}
		if (typeName.equals(Types.STRING)) {
			return parseString(type.getContent(), structClass);
		}
		if (typeName.equals(Types.DOUBLE)) {
			return parseDouble(type.getContent());
		}
		if (typeName.equals(Types.ISO8601DATE)) {
			return parseDate(type.getContent());
		}
		if (typeName.equals(Types.BASE64)) {
			return parseBase64(type.getContent());
		}
		if (typeName.equals(Types.STRUCT)) {
			if (structClass == null) {
				structClass = HashMap.class;
			}
			
			Object structObject = this.newStructObject(structClass);
			return parseStruct(type, structObject, aid);
		}
		if (typeName.equals(Types.ARRAY)) {
			if (type.getChildrenCount() != 1) {
				throw new MarshallingException("Expected exactly 1 data list under '" + type.getFullName() + "', found "
						+ type.getChildrenCount());
			}
			return parseArrayData(type.getChildAtIndex(0), structClass, aid);
		}

		// Currently this only allows for the addition of new
		// simple types
		return this.parseUnknownType(typeName, type.getContent());
	}

	protected Object parseUnknownType(String typeName, String typeValue) throws MarshallingException {
		throw new MarshallingException("Unsupported value type: '" + typeName + "'");
	}
	protected Object parseStruct(XmlNode struct, UnmarshallerAid aid) throws MarshallingException {
		return this.parseStruct(struct, null, aid);
	}

	protected Object parseStruct(XmlNode struct, Object structObject, UnmarshallerAid aid) throws MarshallingException {
		expectTag(struct, "struct");

		Map structMap = null;
		if (structObject instanceof Map) {
			structMap = (Map) structObject;
		}

		Iterator members = struct.enumerateChildren();
		while (members.hasNext()) {
			XmlNode member = (XmlNode) members.next();
			if (member.getChildrenCount() != 2) {
				throw new MarshallingException("Expected exactly 2 children under '" + member.getFullName() + "', found "
						+ member.getChildrenCount());
			}

			String name = this.parseMemberName(member.getChildAtIndex(0));
			if (structMap != null) {
				Object value = this.parseValue(member.getChildAtIndex(1), aid);
				structMap.put(name, value);
			} else {
				Class fieldClass = null;//HashMap.class;
				XmlNode valueElement = member.getChildAtIndex(1);
				switch (this.nextValueType(valueElement)) {
				case COMPLEX_TYPE_STRUCT:
					// The value we're about to parse is a struct,
					// so we need to figure out what type it is.
					fieldClass = this.getStructMemberType(structObject, this.decodeFieldName(name));
					break;
				case COMPLEX_TYPE_ARRAY:
					// The value we're about to parse is an array,
					// so we need to figure out what component type it is.
					Class testFieldClass = this.getStructMemberType(structObject, this.decodeFieldName(name));
					if (!List.class.isAssignableFrom(testFieldClass)) {
						// For List or Object[] we have to use a Map. It's not
						// one of those so make sure it's an array.
						if (!testFieldClass.isArray()) {
							throw new MarshallingException("Expected a List or an array for member " + name + ", found type "
									+ testFieldClass.getName());
						}
						fieldClass = testFieldClass;
					} else {
						// It's a concrete List type
						fieldClass = testFieldClass;
					}
					break;
				}

				// Parse the value and then assign it to the struct.
				// We do it in this order so we can use the type of
				// the value to assist in locating a setter if required.
				Object value = this.parseValue(valueElement, fieldClass, aid);
				this.setObjectMember(structObject, name, value, aid);
			}
		}
		return structMap != null ? structMap : structObject;
	}

	protected String parseMemberName(XmlNode name) throws MarshallingException {
		expectTag(name, "name");
		return name.getContent();
	}

	protected Object parseArrayData(XmlNode data, Class structClass, UnmarshallerAid aid) throws MarshallingException {
		expectTag(data, "data");
		
		Class structClassComponent = null;
		if (structClass == null) {
			structClass = ArrayList.class;
		} else {
			structClassComponent = structClass.getComponentType();
		}
		
//		if (structClassComponent == null) {
//			structClassComponent = HashMap.class;
//		}

		List arrayData;
		if (!structClass.isInterface() && List.class.isAssignableFrom(structClass)) {
			// It's a concrete List implementation
			try {
				arrayData = (List) structClass.newInstance();
			} catch(Exception e) {
				throw new MarshallingException("Failed to instantiate concrete List type: "+structClass.getName(), e);
			}
		} else {
			// It's of type List or it's an array (we'll coerce it
			// afterwards.
			arrayData = new ArrayList();
		} 
		Iterator values = data.enumerateChildren();
		while (values.hasNext()) {
			Object value = this.parseValue((XmlNode) values.next(), structClassComponent, aid);
			arrayData.add(value);
		}

		// Check if the list we've just constructed needs to be coerced into
		// an array of some kind.
		if (structClass.isArray()) {
			if (structClass == Object[].class) {
				return arrayData.toArray();
			} else if (structClass == int[].class) {
				return Utils.toArray(structClass, arrayData);
			} else if (structClass == long[].class) {
				return Utils.toLongArray(arrayData);
			} else if (structClass == boolean[].class) {
				return Utils.toBooleanArray(arrayData);
			} else if (structClass == float[].class) {
				return Utils.toFloatArray(arrayData);
			} else if (structClass == double[].class) {
				return Utils.toDoubleArray(arrayData);
			}

			// Target is an array of user-defined objects.
			// value is a List with members of the appropriate type
			Object[] arrayTemplate = (Object[]) Array.newInstance(structClass.getComponentType(), 0);
			return arrayData.toArray(arrayTemplate);
		}

		return arrayData;
	}

	private int nextValueType(XmlNode value) throws MarshallingException {
		expectTag(value, "value");
		if (value.getChildrenCount() == 0) {
			// Implicit string type
			return SIMPLE_TYPE;
		}

		if (value.getChildrenCount() != 1) {
			throw new MarshallingException("Expected exactly 1 type under '" + value.getFullName() + "', found "
					+ value.getChildrenCount());
		}
		XmlNode type = value.getChildAtIndex(0);
		String typeName = type.getFullName();
		if (typeName.equals("struct")) {
			return COMPLEX_TYPE_STRUCT;
		}
		if (typeName.equals("array")) {
			return COMPLEX_TYPE_ARRAY;
		}

		return SIMPLE_TYPE;
	}

	private XmlRpcMethodResponse parseResponseParams(XmlNode params,
			MethodResponseUnmarshallerAid aid) throws MarshallingException {
		expectTag(params, "params");

		if (params.getChildrenCount() != 1) {
			throw new MarshallingException("Expected exactly 1 param under '"
					+ params.getFullName() + "', found " + params.getChildrenCount());
		}

		XmlNode param = params.getChildAtIndex(0);
		Class retClass = aid == null ? null : aid.getReturnType();
		if (retClass == Object.class) {
			retClass = null;
		}
		return new XmlRpcMethodResponse(this.parseParam(param, retClass, aid), this.getDefaultFieldNameCodec());
	}

	protected XmlRpcMethodResponse parseMethodResponse(XmlNode node,
			MethodResponseUnmarshallerAid aid) throws MarshallingException {
		expectTag(node, "methodResponse");

		if (node.getChildrenCount() != 1) {
			throw new MarshallingException("Expected exactly 1 child under '"
					+ node.getFullName() + "', found " + node.getChildrenCount());
		}

		XmlNode child = node.getChildAtIndex(0);
		if (child.getFullName().equals("fault")) {
			return this.parseFault(child, aid);
		} else {
			Iterator children = node.enumerateChildren();
			return this
					.parseResponseParams((XmlNode) children.next(), aid);
		}
	}

	private XmlRpcMethodFault parseFault(XmlNode faultNode, UnmarshallerAid aid)
			throws MarshallingException {
		expectTag(faultNode, "fault");

		if (faultNode.getChildrenCount() != 1) {
			throw new MarshallingException("Expected exactly 1 value under '"
					+ faultNode.getFullName() + "', found "
					+ faultNode.getChildrenCount());
		}

		Fault fault = (Fault) this.parseValue(faultNode.getChildAtIndex(0),
				Fault.class, aid);
		return new XmlRpcMethodFault(fault.faultCode, fault.faultString);
	}
}
