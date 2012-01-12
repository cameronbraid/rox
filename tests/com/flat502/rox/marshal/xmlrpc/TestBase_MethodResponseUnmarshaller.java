package com.flat502.rox.marshal.xmlrpc;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;

import com.flat502.rox.marshal.*;

public abstract class TestBase_MethodResponseUnmarshaller extends TestBase_Unmarshaller {
	public TestBase_MethodResponseUnmarshaller(String name) {
		super(name);
	}
	
	protected abstract RpcResponse unmarshal(String xml, Class type) throws Exception;
	protected abstract RpcResponse unmarshal(InputStream xml, Class types) throws Exception;

	public void testStringValue() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodResponse>",
				"	<params>",
				"		<param>",
				"			<value><string>foo</string></value>",
				"		</param>",
				"	</params>",
				"</methodResponse>" };
		String xml = toString(xmlLines);

		RpcResponse rsp = this.unmarshal(xml, null);
		assertTrue(rsp.getReturnValue() instanceof String);
		assertEquals("foo", rsp.getReturnValue());
	}
	
	public void testFault() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodResponse>",
				"	<fault>",
				"		<value>",
				"			<struct>",
				"				<member>",
				"					<name>faultCode</name>",
				"					<value><int>42</int></value>",
				"				</member>",
				"				<member>",
				"					<name>faultString</name>",
				"					<value><string>Life, the Universe and Everything</string></value>",
				"				</member>",
				"			</struct>",
				"		</value>",
				"	</fault>",
				"</methodResponse>" };
		String xml = toString(xmlLines);

		RpcResponse rsp = this.unmarshal(xml, null);
		assertTrue(rsp instanceof RpcFault);
		RpcFault fault = (RpcFault)rsp;
		assertEquals(42, fault.getFaultCode());
		assertEquals("Life, the Universe and Everything", fault.getFaultString());
	}
	
	public void testExplicitStringEnumValue() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodResponse>",
				"	<params>",
				"		<param>",
				"			<value><string>BAR</string></value>",
				"		</param>",
				"	</params>",
				"</methodResponse>" };
		String xml = toString(xmlLines);

		RpcResponse rsp = this.unmarshal(xml, EnumConstants.class);
		assertTrue(rsp.getReturnValue() instanceof EnumConstants);
		assertSame(EnumConstants.BAR, rsp.getReturnValue());
	}

	public void testImplicitStringEnumValue() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodResponse>",
				"	<params>",
				"		<param>",
				"			<value>BAR</value>",
				"		</param>",
				"	</params>",
				"</methodResponse>" };
		String xml = toString(xmlLines);

		RpcResponse rsp = this.unmarshal(xml, EnumConstants.class);
		assertTrue(rsp.getReturnValue() instanceof EnumConstants);
		assertSame(EnumConstants.BAR, rsp.getReturnValue());
	}

	@SuppressWarnings("unchecked")
	public void testListOfCharsAsString() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodResponse>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<array>",
				"					<data>",
				"						<value>h</value>",
				"						<value>e</value>",
				"						<value>l</value>",
				"						<value>l</value>",
				"						<value>o</value>",
				"						<value>!</value>",
				"					</data>",
				"				</array>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodResponse>" };
		String xml = toString(xmlLines);

		try {
			this.unmarshal(xml, String.class);
		} catch(MarshallingException e) {
			assertTrue(e.getMessage().contains("java.lang.String is not a List implementation"));
		}
	}

	@SuppressWarnings("unchecked")
	public void testTypedList() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodResponse>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<array>",
				"					<data>",
				"						<value>first</value>",
				"						<value>second</value>",
				"					</data>",
				"				</array>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodResponse>" };
		String xml = toString(xmlLines);

		RpcResponse rsp = this.unmarshal(xml, new ArrayList<String>().getClass());
		assertTrue(rsp.getReturnValue() instanceof ArrayList);
		ArrayList<String> retVal = (ArrayList<String>) rsp.getReturnValue();
		assertEquals(2, retVal.size());
		assertEquals("first", retVal.get(0));
		assertEquals("second", retVal.get(1));
	}

	public void testTypedListFieldMember() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodResponse>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>public-list</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value>first</value>",
				"									<value>second</value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodResponse>" };
		String xml = toString(xmlLines);

		RpcResponse call = this.unmarshal(xml, GenericsStructJ5.class );
		assertNotNull(call.getReturnValue());
		assertTrue(call.getReturnValue() instanceof GenericsStructJ5);
		GenericsStructJ5 struct = (GenericsStructJ5) call.getReturnValue();
		assertNotNull(struct.publicList);
		assertEquals(2, struct.publicList.size());
		assertEquals("first", struct.publicList.get(0));
		assertEquals("second", struct.publicList.get(1));
	}

	public void testTypedListMethodMember() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodResponse>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>private-list</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value>first</value>",
				"									<value>second</value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodResponse>" };
		String xml = toString(xmlLines);

		RpcResponse call = this.unmarshal(xml, GenericsStructJ5.class );
		assertNotNull(call.getReturnValue());
		assertTrue(call.getReturnValue() instanceof GenericsStructJ5);
		GenericsStructJ5 struct = (GenericsStructJ5) call.getReturnValue();
		assertNotNull(struct.getPrivateList());
		assertEquals(2, struct.getPrivateList().size());
		assertEquals("first", struct.getPrivateList().get(0));
		assertEquals("second", struct.getPrivateList().get(1));
	}

	public void testIncorrectlyTypedListFieldMember() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodResponse>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>public-list</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value><int>42</int></value>",
				"									<value><int>43</int></value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodResponse>" };
		String xml = toString(xmlLines);

		RpcResponse call = this.unmarshal(xml, GenericsStructJ5.class );
		assertNotNull(call.getReturnValue());
		assertTrue(call.getReturnValue() instanceof GenericsStructJ5);
		GenericsStructJ5 struct = (GenericsStructJ5) call.getReturnValue();
		assertNotNull(struct.publicList);
		assertEquals(2, struct.publicList.size());
		try {
			assertEquals("first", struct.publicList.get(0));
			assertEquals("second", struct.publicList.get(1));
			fail();
		} catch(ClassCastException e) {
			// The joys of type erasure
		}
	}

	public void testObjectAsReturnType() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodResponse>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>foo</name>",
				"						<value>bar</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodResponse>" };
		String xml = toString(xmlLines);

		RpcResponse rsp = this.unmarshal(xml, Object.class);
		assertTrue(rsp.getReturnValue() instanceof Map);
		assertEquals("bar", ((Map)rsp.getReturnValue()).get("foo"));
	}

	public void testMismatchedReturnType() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodResponse>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>foo</name>",
				"						<value>bar</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodResponse>" };
		String xml = toString(xmlLines);

		try {
			this.unmarshal(xml, String.class);
			fail();
		} catch(MarshallingException e) {
			assertTrue(e.getMessage().contains("Can't find member 'foo'"));
		}
	}

	public void testMismatchedFieldTypeStructToString() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodResponse>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>string-member</name>",
				"						<value>",
				"							<struct>",
				"								<member>",
				"									<name>key</name>",
				"									<value>value</value>",
				"								</member>",
				"							</struct>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodResponse>" };
		String xml = toString(xmlLines);

		try {
			this.unmarshal(xml, SimpleStruct.class);
			fail();
		} catch(MarshallingException e) {
			assertTrue(e.getMessage().contains("Can't find member 'key'"));
		}
	}

	public void testMismatchedFieldTypeArrayToString() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodResponse>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>string-member</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value><int>42</int></value>",
				"									<value><int>43</int></value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodResponse>" };
		String xml = toString(xmlLines);

		try {
			this.unmarshal(xml, SimpleStruct.class);
			fail();
		} catch(MarshallingException e) {
		}
	}
}
