package com.flat502.rox.marshal.xmlrpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.AttributeList;
import org.xml.sax.HandlerBase;
import org.xml.sax.SAXException;

import com.flat502.rox.marshal.*;
import com.flat502.rox.utils.Utils;

/**
 * Marshal an XML-RPC method call using easy (v1) SAX.
 */
public class SaxUnmarshaller extends XmlRpcMethodUnmarshaller {
	private static enum State {
		ROOT, METHOD_CALL {
			public String tagName() {
				return Tags.METHOD_CALL;
			}
		},
		METHOD_NAME {
			public String tagName() {
				return Tags.METHOD_NAME;
			}
		},
		PARAMS {
			public String tagName() {
				return Tags.PARAMS;
			}
		},
		PARAM {
			public String tagName() {
				return Tags.PARAM;
			}
		},

		METHOD_RSP {
			public String tagName() {
				return Tags.METHOD_RESPONSE;
			}
		},

		FAULT {
			public String tagName() {
				return Tags.FAULT;
			}
		},

		VALUE {
			public String tagName() {
				return Tags.VALUE;
			}
		},

		STRING {
			public String tagName() {
				return Tags.STRING;
			}
		},
		INT {
			public String tagName() {
				return Tags.INT;
			}
		},
		I4 {
			public String tagName() {
				return Tags.I4;
			}
		},
		BOOLEAN {
			public String tagName() {
				return Tags.BOOLEAN;
			}
		},
		DOUBLE {
			public String tagName() {
				return Tags.DOUBLE;
			}
		},
		DATETIME {
			public String tagName() {
				return Tags.DATETIME;
			}
		},
		BASE64 {
			public String tagName() {
				return Tags.BASE64;
			}
		},

		STRUCT {
			public String tagName() {
				return Tags.STRUCT;
			}
		},
		MEMBER {
			public String tagName() {
				return Tags.MEMBER;
			}
		},
		NAME {
			public String tagName() {
				return Tags.NAME;
			}
		},

		ARRAY {
			public String tagName() {
				return Tags.ARRAY;
			}
		},
		DATA {
			public String tagName() {
				return Tags.DATA;
			}
		};

		public String tagName() {
			throw new IllegalStateException();
		}
	};

	private static Map<String, State> rootTransitions() {
		Map<String, State> nextState = new HashMap<String, State>();
		nextState.put(Tags.METHOD_CALL, State.METHOD_CALL);
		nextState.put(Tags.METHOD_RESPONSE, State.METHOD_RSP);
		return nextState;
	}

	private static Map<String, State> methodCallTransitions() {
		Map<String, State> nextState = new HashMap<String, State>();
		nextState.put(Tags.METHOD_NAME, State.METHOD_NAME);
		nextState.put(Tags.PARAMS, State.PARAMS);
		return nextState;
	}

	private static Map<String, State> paramsTransitions() {
		Map<String, State> nextState = new HashMap<String, State>();
		nextState.put(Tags.PARAM, State.PARAM);
		return nextState;
	}

	private static Map<String, State> paramTransitions() {
		Map<String, State> nextState = new HashMap<String, State>();
		nextState.put(Tags.VALUE, State.VALUE);
		return nextState;
	}

	private static Map<String, State> valueTransitions() {
		Map<String, State> nextState = new HashMap<String, State>();
		nextState.put(Tags.STRING, State.STRING);
		nextState.put(Tags.BASE64, State.BASE64);
		nextState.put(Tags.INT, State.INT);
		nextState.put(Tags.I4, State.I4);
		nextState.put(Tags.DOUBLE, State.DOUBLE);
		nextState.put(Tags.BOOLEAN, State.BOOLEAN);
		nextState.put(Tags.DATETIME, State.DATETIME);
		nextState.put(Tags.STRUCT, State.STRUCT);
		nextState.put(Tags.ARRAY, State.ARRAY);
		return nextState;
	}

	private static Map<String, State> structTransitions() {
		Map<String, State> nextState = new HashMap<String, State>();
		nextState.put(Tags.MEMBER, State.MEMBER);
		return nextState;
	}

	private static Map<String, State> memberTransitions() {
		Map<String, State> nextState = new HashMap<String, State>();
		nextState.put(Tags.NAME, State.NAME);
		nextState.put(Tags.VALUE, State.VALUE);
		return nextState;
	}

	private static Map<String, State> arrayTransitions() {
		Map<String, State> nextState = new HashMap<String, State>();
		nextState.put(Tags.DATA, State.DATA);
		return nextState;
	}

	private static Map<String, State> dataTransitions() {
		Map<String, State> nextState = new HashMap<String, State>();
		nextState.put(Tags.VALUE, State.VALUE);
		return nextState;
	}

	private static Map<String, State> methodRspTransitions() {
		Map<String, State> nextState = new HashMap<String, State>();
		nextState.put(Tags.PARAMS, State.PARAMS);
		nextState.put(Tags.FAULT, State.FAULT);
		return nextState;
	}

	private static Map<String, State> faultTransitions() {
		Map<String, State> nextState = new HashMap<String, State>();
		nextState.put(Tags.VALUE, State.VALUE);
		return nextState;
	}

	private static final Map<State, Map<String, State>> TRANSITIONS = new HashMap<State, Map<String, State>>();

	static {
		TRANSITIONS.put(State.ROOT, rootTransitions());
		TRANSITIONS.put(State.METHOD_CALL, methodCallTransitions());
		TRANSITIONS.put(State.PARAMS, paramsTransitions());
		TRANSITIONS.put(State.PARAM, paramTransitions());
		TRANSITIONS.put(State.VALUE, valueTransitions());
		TRANSITIONS.put(State.STRUCT, structTransitions());
		TRANSITIONS.put(State.MEMBER, memberTransitions());
		TRANSITIONS.put(State.ARRAY, arrayTransitions());
		TRANSITIONS.put(State.DATA, dataTransitions());
		TRANSITIONS.put(State.METHOD_RSP, methodRspTransitions());
		TRANSITIONS.put(State.FAULT, faultTransitions());
	}

	private static final StructInfo SENTINEL_STRUCT = new StructInfo((Object) null);

	///////////////////////////////////////////////// Instance ////////////////////////////////////////////////

	private UnmarshallerAid aid;
	private MethodCallUnmarshallerAid callAid;
	private MethodResponseUnmarshallerAid rspAid;

	// true to parse request, false to parse response
	private boolean expectRequest;

	private boolean isMethodCall;
	private StringBuilder methodName;
	private StringBuilder memberName;
	private StringBuilder implicitStringValue;
	private StringBuilder stringValue;
	private List<Object> params;
	private boolean isFault;
	private Fault fault;

	// Stack of nested states and saved values. The top elements is always a state (STATE_XXXX).
	private Stack stateStack;
	private State currentState;

	// Stack of nested struct objects
	private Stack structStack;
	private StructInfo currentStruct;

	// The struct whose members we are busy parsing
	private static class StructInfo {
		public Object value;

		// Non-null if this represents a map
		public Map<String, Object> asMap;

		// Non-null if this represents a list
		public List<Object> asList;

		// Track info for the member we're currently handling in this struct
		public String memberName;
		public Class memberClass;

		public StructInfo(Map<String, Object> map) {
			this.value = this.asMap = map;
		}

		public StructInfo(List<Object> list) {
			this.value = this.asList = list;
		}

		public StructInfo(Object obj) {
			this.value = obj;
			if (obj instanceof Map) {
				this.asMap = (Map) obj;
			}
			if (obj instanceof List) {
				this.asList = (List) obj;
			}
		}
	}

	// The value we have just parsed
	private Object value;
	
	private HandlerBase saxHandler;

	////////////////////////////////////////// pubic interface ///////////////////////////////////////////////

	public SaxUnmarshaller(FieldNameCodec codec) {
		super(codec);
		this.saxHandler = new Handler();
	}

	public void setCallAid(MethodCallUnmarshallerAid callAid) {
		this.aid = this.callAid = callAid;
	}

	/**
	 * @param rspAid An unmarshaller aid implementation.
	 */
	public void setResponseAid(MethodResponseUnmarshallerAid rspAid) {
		this.aid = this.rspAid = rspAid;
	}

	public String getMethodName() {
		return this.methodName.toString();
	}

	public Object[] getParams() {
		return params.toArray();
	}

	public Object getResponse() {
		return params.isEmpty() ? null : params.get(0);
	}

	public Fault getFault() {
		return fault;
	}

	// clean up internal state
	private void reset() {
		this.methodName = new StringBuilder();
		this.stringValue = null;
		this.implicitStringValue = null;
		this.params = new ArrayList<Object>();
		this.stateStack = new Stack();
		this.value = null;
		this.isFault = false;
		this.fault = null;

		// Initialize the struct stack with a sentinel
		this.structStack = new Stack();
		pushStruct(SENTINEL_STRUCT);
	}
	
	private void pushState(State s) {
		this.currentState = s;
		stateStack.push(s);
	}

	private State popState() {
		State s = (State) stateStack.pop();
		currentState = (State) stateStack.peek();
		return s;
	}

	private State currentState() {
		return currentState;
	}

	private void pushStruct(StructInfo s) {
		this.currentStruct = s;
		structStack.push(s);
	}

	private StructInfo popStruct() {
		StructInfo s = (StructInfo) structStack.pop();
		currentStruct = (StructInfo) structStack.peek();
		return s;
	}

	private StructInfo currentStruct() {
		return (StructInfo) structStack.peek();
	}

	private void info(Object msg) {
		System.out.println(msg);
	}

	private void debug(Object msg) {
		System.out.println(msg);
	}

	///////////////////////////////////// Set to parse Request/Response //////////////////////////////////

	/// @param expectRequest true iff we must parse a request, false iff we must parse a response
	public void expectRequest(boolean expectRequest) {
		this.expectRequest = expectRequest;
	}

	//////////////////////////////////////// DocumentHandler ////////////////////////////////////////////

	protected Class getStructMemberType(Object structObject, String name) throws MarshallingException {
		if (!this.isFault) {
			name = this.decodeFieldName(name);
		}
		try {
			return super.getStructMemberType(structObject, name);
		} catch (IllegalArgumentException e) {
			if (aid != null && !aid.ignoreMissingFields()) {
				throw new MarshallingException("Can't find member '" + name + "'", e);
			}
		}
		return null;
	}

	//////////////////////////////////////// DocumentHandler ////////////////////////////////////////////

	public void startDocument() throws SAXException {
//		info("");
		this.reset();
		pushState(State.ROOT);
	}

	public void endDocument() throws SAXException {
		// Validation
		if (this.isMethodCall) {
			if (this.methodName.length() == 0) {
				throw new SAXException("Missing method name");
			}
		} else {
			if (this.params.size() > 1) {
				throw new SAXException("Responses may only include one parameter value");	
			}
		}
	}

	public void startElement(String element, AttributeList attrs) throws SAXException {
		if (!Tags.isValid(element)) {
			throw new SAXException("Unexpected tag [" + element + "]");
		}

		State curState = currentState();
		Map<String, State> nextStateMap = TRANSITIONS.get(curState);
		if (nextStateMap == null) {
			throw new SAXException("No state transitions defined for state [" + curState + "]");
		}

		State nextState = nextStateMap.get(element);
		if (nextState == null) {
			throw new SAXException("No transition for [" + curState + ", " + element + "]");
		}

//		debug("start[" + element + "] transitions from " + curState + " to " + nextState);

		Class structClass;
		StructInfo struct;
		try {
			switch (nextState) {
			case METHOD_CALL:
				if (!this.expectRequest) {
					throw new SAXException("Method response expected");
				}
				this.isMethodCall = true;
				break;
			case METHOD_RSP:
				if (this.expectRequest) {
					throw new SAXException("Method call expected");
				}
				this.isMethodCall = false;
				getType();
				break;
			case METHOD_NAME:
				if (this.methodName.length() > 0) {
					throw new SAXException("Already have a method name");
				}
				break;
			case FAULT:
				this.isFault = true;
				break;
			case STRUCT:
				if (this.value != null) {
					throw new SAXException("Repeated struct");
				}
				if (structStack.size() == 1) {
					if (this.isFault) {
						structClass = Fault.class;
					} else {
						// This is a top level struct
						structClass = getType();
					}
				} else {
					structClass = currentStruct().memberClass;
				}
	
				if (structClass == null) {
					struct = new StructInfo(new HashMap<String, Object>());
				} else {
					struct = new StructInfo(newStructObject(structClass));
				}
				
				if (this.isFault) {
					this.fault = (Fault) struct.value;
				}
	
				pushStruct(struct);
	//			info("Created struct of type " + struct.value.getClass() + ": " + System.identityHashCode(struct.value));
				break;
			case MEMBER:
				this.memberName = new StringBuilder();
				break;
			case VALUE:
				if (this.value != null) {
					throw new SAXException("Repeated value");
				}
				
				this.value = null;
				this.stringValue = null;
				this.implicitStringValue = null;
				break;
			case ARRAY:
				if (this.value != null) {
					throw new SAXException("Repeated array");
				}
				if (structStack.size() == 1) {
					structClass = getType();
				} else {
					structClass = currentStruct().memberClass;
				}
	
				if (structClass == null) {
					struct = new StructInfo(new ArrayList<Object>());
				} else if (structClass.isArray()) {
					struct = new StructInfo(new ArrayList<Object>());
					if (structClass.isArray()) {
						struct.memberClass = structClass.getComponentType();
					}
				} else {
					if (!List.class.isAssignableFrom(structClass)) {
						throw new SAXException(structClass.getName() + " is not a List implementation");
					}
					
					// This handles the specific List implementations. Everything else we use our
					// own ArrayList for and coerce/convert afterwards.
					struct = new StructInfo(newStructObject(structClass));
				}
	
				pushStruct(struct);
	//			info("Created list of type " + struct.value.getClass() + ": " + System.identityHashCode(struct.value));
				break;
			}
		} catch(MarshallingException e) {
			throw (SAXException) new SAXException(e).initCause(e);
		}

		pushState(nextState);
	}
	
	private Class getType() {
		Class type;
		if (this.isMethodCall) {
			type = callAid == null ? null : callAid.getType(this.getMethodName(), this.params.size());
		} else {
			type = rspAid == null ? null : rspAid.getReturnType();
		}
		
		if (type == List.class || type == Map.class || type == Object.class) {
			return null;
		}
		
		return type;
	}
	
	private Object finalizeString(StringBuilder value) throws MarshallingException {
		String v = this.toString(value);
		if (structStack.size() == 1 && getType() != null) {
			return this.parseString(v, getType());
		}
		return this.parseString(v);
	}

	private String toString(StringBuilder value) {
		return (value == null ? "" : value.toString());
	}
	
	public void endElement(String element) throws SAXException {
		State curState = currentState();
		if (!element.equals(curState.tagName())) {
			throw new SAXException("Expected </" + curState.tagName() + ">, got </" + element + ">");
		}
		
		State prevState = popState();
		curState = currentState();
//		debug("end[" + element + "] transitions from " + prevState + " to " + curState);
		
		StructInfo curStruct;
		try {
			switch(prevState) {
			case METHOD_NAME:
				// This is a quirk of the way dispatch is done in Rox. Suffice to say,
				// once we know the method name here we need to call back into the
				// UnmarshallerAid with it so that the dispatcher can use that and
				// set up the correct internal handler. Nasty, but it's Saturday
				// and I'm running out of time.
				getType();
				break;
			case STRING:
				this.storeValue(this.finalizeString(this.stringValue));
				break;
			case INT:
			case I4:
				this.storeValue(this.parseInt(this.toString(this.stringValue)));
				break;
			case BOOLEAN:
				this.storeValue(this.parseBoolean(this.toString(this.stringValue)));
				break;
			case DOUBLE:
				this.storeValue(this.parseDouble(this.toString(this.stringValue)));
				break;
			case BASE64:
				this.storeValue(this.parseBase64(this.toString(this.stringValue)));
				break;
			case DATETIME:
				this.storeValue(this.parseDate(this.toString(this.stringValue)));
				break;
			case STRUCT:
				this.value = popStruct().value;
				break;
			case MEMBER:
				curStruct = this.currentStruct();
	//			info("Setting struct member [" + curStruct.memberName + "] to [" + this.value + "]");
				if (curStruct.asMap != null) {
					curStruct.asMap.put(curStruct.memberName.toString(), this.value);
				} else {
					this.setObjectMember(curStruct.value, curStruct.memberName, this.value, this.callAid);
				}
				this.value = null;
				break;
			case NAME:
				// Determine the type of this member from the current struct
				curStruct = this.currentStruct();
				curStruct.memberName = this.memberName.toString();
				if (curStruct.asMap != null || curStruct.asList != null) {
					// It's just a map or list, there's no type info here.
					curStruct.memberClass = null;
				} else {
					// It's a user-defined class, use the field type info
					curStruct.memberClass = this.getStructMemberType(curStruct.value, curStruct.memberName);
				}
				break;
			case ARRAY:
				this.value = popStruct().value;
				
				Class paramClass = getType();
				if (structStack.size() == 1 && paramClass != null) {
					this.value = Utils.coerce(this.value, paramClass);
				}
				break;
			case DATA:
				break;
			case VALUE:
				if (this.value == null) {
					// Special case for implicit string value
					this.value = this.finalizeString(this.implicitStringValue);
				}
	
				curStruct = this.currentStruct();
				if (curStruct.asList != null) {
	//				info("Appending array element [" + this.value + "]");
					if (curStruct.memberClass != null) {
						curStruct.asList.add(Utils.coerce(this.value, curStruct.memberClass));
					} else {
						curStruct.asList.add(this.value);
					}
					this.value = null;
				}
				break;
			case PARAM:
	//			info("Adding parameter of type " + this.value.getClass().getName());
				this.params.add(this.value);
				this.value = null;
				break;
			case FAULT:
	//			info("Adding parameter of type " + this.value.getClass().getName());
				this.params.add(this.value);
				break;
			}
		} catch(MarshallingException e) {
			throw (SAXException) new SAXException(e).initCause(e);
		}
	}
	
	private void storeValue(Object v) throws SAXException {
		if (this.value != null) {
			throw new SAXException("Repeated values");
		}
		this.value = v;
	}
	
	public HandlerBase getSaxHandler() {
		return this.saxHandler;
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
//		debug("chars[" + new String(ch, start, length) + "] in state " + currentState());

		State curState = currentState();
		switch (curState) {
		case METHOD_NAME:
			this.methodName.append(new String(ch, start, length));
			break;
		case NAME:
			this.memberName.append(new String(ch, start, length));
			break;
		case VALUE: // This is an implicit string value
			if (this.implicitStringValue == null) {
				this.implicitStringValue = new StringBuilder();
			}
			this.implicitStringValue.append(new String(ch, start, length));
			break;
		case STRING:
		case INT:
		case I4:
		case BOOLEAN:
		case BASE64:
		case DATETIME:
		case DOUBLE:
			if (this.stringValue == null) {
				this.stringValue = new StringBuilder();
			}
			this.stringValue.append(new String(ch, start, length));
			break;
		}
	}
	
	private class Handler extends HandlerBase {
		public void startDocument() throws SAXException {
			SaxUnmarshaller.this.startDocument();
		}
		
		public void endDocument() throws SAXException {
			SaxUnmarshaller.this.endDocument();
		}
		
		public void startElement(String name, AttributeList attributes) throws SAXException {
			SaxUnmarshaller.this.startElement(name, attributes);
		}
		
		public void endElement(String name) throws SAXException {
			SaxUnmarshaller.this.endElement(name);
		}
		
		public void characters(char[] ch, int start, int length) throws SAXException {
			SaxUnmarshaller.this.characters(ch, start, length);
		}
	}
}
