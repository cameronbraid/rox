package com.flat502.rox.marshal.xmlrpc;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.flat502.rox.demo.validation.MoeLarryAndCurly;
import com.flat502.rox.marshal.*;
import com.flat502.rox.utils.Utils;

public abstract class TestBase_MethodCallUnmarshaller extends TestBase_Unmarshaller {
	public TestBase_MethodCallUnmarshaller(String name) {
		super(name);
	}

	protected abstract RpcCall unmarshal(String xml, Class[] types) throws Exception;
	protected abstract RpcCall unmarshalWithAid(String xml, MethodCallUnmarshallerAid aid) throws Exception;

	protected abstract RpcCall unmarshal(InputStream xml, Class[] types) throws Exception;

	public void testMethodName() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertEquals("testMethod", call.getName());
	}

	public void testNoParams() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
	}

	public void testMalformedXMLRPC() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>server.delay</methodName>",
				"	<params>",
				"			<value><int>1000</int></value>",
				"		<param>",
				"		</param>",
				"	</params>",
				"</methodCall>"};
		String xml = toString(xmlLines);

		try {
			this.unmarshal(xml, null);
			fail();
		} catch(MarshallingException e) {
		}
	}

	public void testDuplicateMethodName() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>server.delay</methodName>",
				"	<methodName>server.broken</methodName>",
				"	<params>",
				"		<param>",
				"			<value><int>1000</int></value>",
				"			<value><string>first call</string></value>",
				"		</param>",
				"	</params>",
				"</methodCall>"};
		String xml = toString(xmlLines);

		try {
			this.unmarshal(xml, null);
			fail();
		} catch(MarshallingException e) {
		}
	}

	public void testDuplicateParam() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>server.delay</methodName>",
				"	<params>",
				"		<param>",
				"			<value><int>1000</int></value>",
				"			<value><string>first call</string></value>",
				"		</param>",
				"	</params>",
				"</methodCall>"};
		String xml = toString(xmlLines);

		try {
			this.unmarshal(xml, null);
			fail();
		} catch(MarshallingException e) {
		}
	}

	public void testDuplicateValue() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>server.delay</methodName>",
				"	<params>",
				"		<param>",
				"			<value><int>1000</int><string>first call</string></value>",
				"		</param>",
				"	</params>",
				"</methodCall>"};
		String xml = toString(xmlLines);

		try {
			this.unmarshal(xml, null);
			fail();
		} catch(MarshallingException e) {
		}
	}

	public void testDuplicateStruct() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>key</name><value>value</value>",
				"					</member>",
				"				</struct>",
				"				<struct>",
				"					<member>",
				"						<name>key</name><value>value</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		try {
			this.unmarshal(xml, null);
			fail();
		} catch(MarshallingException e) {
		}
	}

	public void testDuplicateArray() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<array>",
				"					<data><value>string value</value></data>",
				"				</array>",
				"				<array>",
				"					<data><value>string value</value></data>",
				"				</array>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		try {
			this.unmarshal(xml, null);
			fail();
		} catch(MarshallingException e) {
		}
	}

	public void testDuplicateStructNested() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
				"<methodCall>",
				"   <methodName>testName</methodName>",
				"   <params>",
				"      <param>",
				"         <value>",
				"            <struct>",
				"               <member>",
				"                  <name>public-second-level</name>",
				"                  <value>",
				"                     <struct>",
				"                        <member>",
				"                           <name>name</name><value>public</value>",
				"                        </member>",
				"                     </struct>",
				"                     <struct>",
				"                        <member>",
				"                           <name>name</name><value>public</value>",
				"                        </member>",
				"                     </struct>",
				"                  </value>",
				"               </member>",
				"            </struct>",
				"         </value>",
				"      </param>",
				"   </params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		try {
			this.unmarshal(xml, null);
			fail();
		} catch(MarshallingException e) {
		}
	}

	public void testEmptyImplicitStringValueTag() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value />",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertEquals("", call.getParameters()[0]);
	}

	public void testEmptyExplicitStringValueTag() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value><string/></value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertEquals("", call.getParameters()[0]);
	}

	public void testEmptyIntValueTags() throws Exception {
		try {
			this.testEmptyTypedValueTags("int");
			fail();
		} catch (MarshallingException e) {
		}
	}

	public void testEmptyI4ValueTags() throws Exception {
		try {
			this.testEmptyTypedValueTags("i4");
			fail();
		} catch (MarshallingException e) {
		}
	}

	public void testEmptyBooleanValueTags() throws Exception {
		try {
			this.testEmptyTypedValueTags("boolean");
			fail();
		} catch (MarshallingException e) {
		}
	}

	public void testEmptyDoubleValueTags() throws Exception {
		try {
			this.testEmptyTypedValueTags("double");
			fail();
		} catch (MarshallingException e) {
		}
	}

	public void testEmptyDateValueTags() throws Exception {
		try {
			this.testEmptyTypedValueTags("dateTime.iso8601");
			fail();
		} catch (MarshallingException e) {
		}
	}

	public void testEmptyBase64ValueTags() throws Exception {
		this.testEmptyTypedValueTags("base64");
	}

	// Template test method for testing an empty parameter tag with an 
	// explicit type (of the form <type/>)
	private void testEmptyTypedValueTags(String type) throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value><" + type + "/></value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		this.unmarshal(xml, null);
	}

	public void testSingleLongLine() throws Exception {
		String[] xmlLines = {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"		<param>",
				"			<value>test</value>",
				"		</param>",
				"	</params>" + "</methodCall>" };
		String xml = toString(xmlLines);

		InputStream xmlStream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
		RpcCall call = this.unmarshal(xmlStream, null);
		// Just check there's no exception
	}

	public void testMultipleParams() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value><int>1</int></value>",
				"		</param>",
				"		<param>",
				"			<value><int>2</int></value>",
				"		</param>",
				"		<param>",
				"			<value><int>3</int></value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(3, call.getParameters().length);
	}

	public void testInvalidParamType() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value><invalid>42</invalid></value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		try {
			RpcCall call = this.unmarshal(xml, null);
			fail();
		} catch (MarshallingException e) {
		}
	}

	public void testIntParam_int() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value><int>42</int></value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof Integer);
		assertEquals(new Integer(42), call.getParameters()[0]);
	}

	public void testIntParam_i4() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value><i4>42</i4></value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof Integer);
		assertEquals(new Integer(42), call.getParameters()[0]);
	}

	public void testStringParam_implicit() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>string value</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof String);
		assertEquals("string value", call.getParameters()[0]);
	}

	public void testStringParam_explicit() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value><string>string value</string></value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof String);
		assertEquals("string value", call.getParameters()[0]);
	}

	public void testStringParamEscaped() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value><string>&lt;div&gt;foo&amp;bar&lt;/div&gt;</string></value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof String);
		assertEquals("<div>foo&bar</div>", call.getParameters()[0]);
	}

	public void testMultilineString() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<string>three\nblind\nmice</string>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof String);
		assertEquals("three\nblind\nmice", call.getParameters()[0]);
	}
	
	public void testStringParam_64K() throws Exception {
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < 700; i++) {
			sb.append("0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789");
		}
		
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value><string>"+sb.toString()+"</string></value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof String);
		assertEquals(70000, ((String)call.getParameters()[0]).length());
	}

	public void testDoubleParam() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value><double>42.5</double></value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof Double);
		assertEquals(new Double(42.5), call.getParameters()[0]);
	}

	public void testDateParam() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value><dateTime.iso8601>19980717T14:08:55</dateTime.iso8601></value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof Date);
		assertEquals(newDate(1998, 7, 17, 14, 8, 55), call.getParameters()[0]);
	}

	public void testBase64Param() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value><base64>eW91IGNhbid0IHJlYWQgdGhpcyE=</base64></value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof byte[]);
		assertEquals("you can't read this!".getBytes("UTF-8"), (byte[]) call.getParameters()[0]);
	}

	public void testStructParam() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>string member</name>",
				"						<value>string value</value>",
				"					</member>",
				"					<member>",
				"						<name>int member</name>",
				"						<value><int>42</int></value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof Map);
		Map paramMap = (Map) call.getParameters()[0];
		assertEquals(2, paramMap.size());
		assertEquals("string value", paramMap.get("string member"));
		assertEquals(new Integer(42), paramMap.get("int member"));
	}

	public void testEmptyStructParam() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof Map);
		Map paramMap = (Map) call.getParameters()[0];
		assertEquals(0, paramMap.size());
	}

	public void testStructWithEmptyStructParam() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>empty struct</name>",
				"						<value><struct></struct></value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof Map);
		Map paramMap = (Map) call.getParameters()[0];
		assertEquals(1, paramMap.size());
		assertTrue(paramMap.get("empty struct") instanceof Map);
		assertEquals(0, ((Map) paramMap.get("empty struct")).size());
	}

	public void testTypedStructWithEmptyStructParam() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>empty struct</name>",
				"						<value><struct></struct></value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof Map);
		Map paramMap = (Map) call.getParameters()[0];
		assertEquals(1, paramMap.size());
		assertTrue(paramMap.get("empty struct") instanceof Map);
		assertEquals(0, ((Map) paramMap.get("empty struct")).size());
	}

	public void testArrayParam() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<array>",
				"					<data>",
				"						<value>string value</value>",
				"						<value><int>42</int></value>",
				"					</data>",
				"				</array>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof List);
		List paramList = (List) call.getParameters()[0];
		assertEquals(2, paramList.size());
		assertEquals("string value", paramList.get(0));
		assertEquals(new Integer(42), paramList.get(1));
	}

	public void testTypedArrayField() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>public-typed-array</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value>",
				"										<struct>",
				"											<member>",
				"												<name>string-member</name>",
				"												<value>hit me baby</value>",
				"											</member>",
				"										</struct>",
				"									</value>",
				"									<value>",
				"										<struct>",
				"											<member>",
				"												<name>string-member</name>",
				"												<value>one more time</value>",
				"											</member>",
				"										</struct>",
				"									</value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { TypedArrayStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof TypedArrayStruct);
		TypedArrayStruct struct = (TypedArrayStruct) call.getParameters()[0];
		assertNotNull(struct.publicTypedArray);
		assertEquals(2, struct.publicTypedArray.length);
		assertEquals("hit me baby", struct.publicTypedArray[0].stringMember);
		assertEquals("one more time", struct.publicTypedArray[1].stringMember);
	}

	public void testTypedArrayMethod() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>private-typed-array</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value>",
				"										<struct>",
				"											<member>",
				"												<name>string-member</name>",
				"												<value>hit me baby</value>",
				"											</member>",
				"										</struct>",
				"									</value>",
				"									<value>",
				"										<struct>",
				"											<member>",
				"												<name>string-member</name>",
				"												<value>one more time</value>",
				"											</member>",
				"										</struct>",
				"									</value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { TypedArrayStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof TypedArrayStruct);
		TypedArrayStruct struct = (TypedArrayStruct) call.getParameters()[0];
		assertNotNull(struct.getPrivateTypedArray());
		assertEquals(2, struct.getPrivateTypedArray().length);
		assertEquals("hit me baby", struct.getPrivateTypedArray()[0].stringMember);
		assertEquals("one more time", struct.getPrivateTypedArray()[1].stringMember);
	}

	public void testPrimitiveIntArrayField() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>public-int-array</name>",
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
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { PrimitiveArraysStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof PrimitiveArraysStruct);
		PrimitiveArraysStruct struct = (PrimitiveArraysStruct) call.getParameters()[0];
		assertNotNull(struct.publicIntArray);
		assertEquals(2, struct.publicIntArray.length);
		assertEquals(42, struct.publicIntArray[0]);
		assertEquals(43, struct.publicIntArray[1]);
	}

	public void testPrimitiveIntArrayMethod() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>private-int-array</name>",
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
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { PrimitiveArraysStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof PrimitiveArraysStruct);
		PrimitiveArraysStruct struct = (PrimitiveArraysStruct) call.getParameters()[0];
		assertNotNull(struct.getPrivateIntArray());
		assertEquals(2, struct.getPrivateIntArray().length);
		assertEquals(42, struct.getPrivateIntArray()[0]);
		assertEquals(43, struct.getPrivateIntArray()[1]);
	}

	public void testPrimitiveDoubleArrayField() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>public-double-array</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value><double>3.14</double></value>",
				"									<value><double>6.28</double></value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { PrimitiveArraysStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof PrimitiveArraysStruct);
		PrimitiveArraysStruct struct = (PrimitiveArraysStruct) call.getParameters()[0];
		assertNotNull(struct.publicDoubleArray);
		assertEquals(2, struct.publicDoubleArray.length);
		assertEquals(3.14, struct.publicDoubleArray[0], 0.1);
		assertEquals(6.28, struct.publicDoubleArray[1], 0.1);
	}

	public void testPrimitiveDoubleArrayMethod() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>private-double-array</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value><double>3.14</double></value>",
				"									<value><double>6.28</double></value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { PrimitiveArraysStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof PrimitiveArraysStruct);
		PrimitiveArraysStruct struct = (PrimitiveArraysStruct) call.getParameters()[0];
		assertNotNull(struct.getPrivateDoubleArray());
		assertEquals(2, struct.getPrivateDoubleArray().length);
		assertEquals(3.14, struct.getPrivateDoubleArray()[0], 0.1);
		assertEquals(6.28, struct.getPrivateDoubleArray()[1], 0.1);
	}

	public void testPrimitiveBooleanArrayField() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>public-boolean-array</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value><boolean>1</boolean></value>",
				"									<value><boolean>1</boolean></value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { PrimitiveArraysStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof PrimitiveArraysStruct);
		PrimitiveArraysStruct struct = (PrimitiveArraysStruct) call.getParameters()[0];
		assertNotNull(struct.publicBooleanArray);
		assertEquals(2, struct.publicBooleanArray.length);
		assertEquals(true, struct.publicBooleanArray[0]);
		assertEquals(true, struct.publicBooleanArray[1]);
	}

	public void testPrimitiveBooleanArrayMethod() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>private-boolean-array</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value><boolean>1</boolean></value>",
				"									<value><boolean>1</boolean></value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { PrimitiveArraysStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof PrimitiveArraysStruct);
		PrimitiveArraysStruct struct = (PrimitiveArraysStruct) call.getParameters()[0];
		assertNotNull(struct.getPrivateBooleanArray());
		assertEquals(2, struct.getPrivateBooleanArray().length);
		assertEquals(true, struct.getPrivateBooleanArray()[0]);
		assertEquals(true, struct.getPrivateBooleanArray()[1]);
	}

	public void testIntegerArrayField() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>public-integer-array</name>",
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
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { IntegerArraysStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof IntegerArraysStruct);
		IntegerArraysStruct struct = (IntegerArraysStruct) call.getParameters()[0];
		assertNotNull(struct.publicIntegerArray);
		assertEquals(2, struct.publicIntegerArray.length);
		assertEquals(new Integer(42), struct.publicIntegerArray[0]);
		assertEquals(new Integer(43), struct.publicIntegerArray[1]);
	}

	public void testIntegerArrayMethod() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>private-int-array</name>",
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
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { PrimitiveArraysStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof PrimitiveArraysStruct);
		PrimitiveArraysStruct struct = (PrimitiveArraysStruct) call.getParameters()[0];
		assertNotNull(struct.getPrivateIntArray());
		assertEquals(2, struct.getPrivateIntArray().length);
		assertEquals(42, struct.getPrivateIntArray()[0]);
		assertEquals(43, struct.getPrivateIntArray()[1]);
	}

	public void testNestedListField() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>public-list</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value>",
				"										<array>",
				"											<data>",
				"												<value><int>42</int></value>",
				"												<value><int>43</int></value>",
				"											</data>",
				"										</array>",
				"									</value>",
				"									<value>",
				"										<array>",
				"											<data>",
				"												<value><int>44</int></value>",
				"												<value><int>45</int></value>",
				"												<value><int>46</int></value>",
				"											</data>",
				"										</array>",
				"									</value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { ListStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof ListStruct);
		ListStruct struct = (ListStruct) call.getParameters()[0];
		assertNotNull(struct.publicList);
		assertEquals(2, struct.publicList.size());
		assertTrue(struct.publicList.get(0) instanceof List);
		assertEquals(new Integer(42), ((List) struct.publicList.get(0)).get(0));
		assertEquals(new Integer(43), ((List) struct.publicList.get(0)).get(1));
		assertTrue(struct.publicList.get(1) instanceof List);
		assertEquals(new Integer(44), ((List) struct.publicList.get(1)).get(0));
		assertEquals(new Integer(45), ((List) struct.publicList.get(1)).get(1));
		assertEquals(new Integer(46), ((List) struct.publicList.get(1)).get(2));
	}

	public void testNestedListMethod() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>private-list</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value>",
				"										<array>",
				"											<data>",
				"												<value><int>42</int></value>",
				"												<value><int>43</int></value>",
				"											</data>",
				"										</array>",
				"									</value>",
				"									<value>",
				"										<array>",
				"											<data>",
				"												<value><int>44</int></value>",
				"												<value><int>45</int></value>",
				"												<value><int>46</int></value>",
				"											</data>",
				"										</array>",
				"									</value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { ListStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof ListStruct);
		ListStruct struct = (ListStruct) call.getParameters()[0];
		assertNotNull(struct.getPrivateList());
		assertEquals(2, struct.getPrivateList().size());
		assertTrue(struct.getPrivateList().get(0) instanceof List);
		assertEquals(new Integer(42), ((List) struct.getPrivateList().get(0)).get(0));
		assertEquals(new Integer(43), ((List) struct.getPrivateList().get(0)).get(1));
		assertTrue(struct.getPrivateList().get(1) instanceof List);
		assertEquals(new Integer(44), ((List) struct.getPrivateList().get(1)).get(0));
		assertEquals(new Integer(45), ((List) struct.getPrivateList().get(1)).get(1));
		assertEquals(new Integer(46), ((List) struct.getPrivateList().get(1)).get(2));
	}

	public void testListField() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
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
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { ListStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof ListStruct);
		ListStruct struct = (ListStruct) call.getParameters()[0];
		assertNotNull(struct.publicList);
		assertEquals(2, struct.publicList.size());
		assertEquals(new Integer(42), struct.publicList.get(0));
		assertEquals(new Integer(43), struct.publicList.get(1));
	}

	public void testListMethod() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>private-list</name>",
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
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { LinkedListStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof LinkedListStruct);
		LinkedListStruct struct = (LinkedListStruct) call.getParameters()[0];
		assertNotNull(struct.getPrivateList());
		assertEquals(2, struct.getPrivateList().size());
		assertEquals(new Integer(42), struct.getPrivateList().get(0));
		assertEquals(new Integer(43), struct.getPrivateList().get(1));
	}

	public void testLinkedListField() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
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
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { LinkedListStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof LinkedListStruct);
		LinkedListStruct struct = (LinkedListStruct) call.getParameters()[0];
		assertNotNull(struct.publicList);
		assertEquals(2, struct.publicList.size());
		assertEquals(new Integer(42), struct.publicList.get(0));
		assertEquals(new Integer(43), struct.publicList.get(1));
	}

	public void testLinkedListMethod() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>private-list</name>",
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
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { ListStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof ListStruct);
		ListStruct struct = (ListStruct) call.getParameters()[0];
		assertNotNull(struct.getPrivateList());
		assertEquals(2, struct.getPrivateList().size());
		assertEquals(new Integer(42), struct.getPrivateList().get(0));
		assertEquals(new Integer(43), struct.getPrivateList().get(1));
	}

	public void testMapField() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>public-map</name>",
				"						<value>",
				"							<struct>",
				"								<member>",
				"									<name>string member</name>",
				"									<value>string value</value>",
				"								</member>",
				"							</struct>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { MapStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof MapStruct);
		MapStruct struct = (MapStruct) call.getParameters()[0];
		assertNotNull(struct.publicMap);
		assertEquals(1, struct.publicMap.size());
		assertEquals("string value", struct.publicMap.get("string member"));
	}

	public void testMapMethod() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>private-map</name>",
				"						<value>",
				"							<struct>",
				"								<member>",
				"									<name>string member</name>",
				"									<value>string value</value>",
				"								</member>",
				"							</struct>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { MapStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		MapStruct struct = (MapStruct) call.getParameters()[0];
		assertTrue(call.getParameters()[0] instanceof MapStruct);
		assertNotNull(struct.getPrivateMap());
		assertEquals(1, struct.getPrivateMap().size());
		assertEquals("string value", struct.getPrivateMap().get("string member"));
	}

	public void testTreeMapField() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>public-map</name>",
				"						<value>",
				"							<struct>",
				"								<member>",
				"									<name>string member</name>",
				"									<value>string value</value>",
				"								</member>",
				"							</struct>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { TreeMapStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof TreeMapStruct);
		TreeMapStruct struct = (TreeMapStruct) call.getParameters()[0];
		assertNotNull(struct.publicMap);
		assertEquals(1, struct.publicMap.size());
		assertEquals("string value", struct.publicMap.get("string member"));
	}

	public void testTreeMapMethod() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>private-map</name>",
				"						<value>",
				"							<struct>",
				"								<member>",
				"									<name>string member</name>",
				"									<value>string value</value>",
				"								</member>",
				"							</struct>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { TreeMapStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		TreeMapStruct struct = (TreeMapStruct) call.getParameters()[0];
		assertTrue(call.getParameters()[0] instanceof TreeMapStruct);
		assertNotNull(struct.getPrivateMap());
		assertEquals(1, struct.getPrivateMap().size());
		assertEquals("string value", struct.getPrivateMap().get("string member"));
	}

	public void testJaggedIntArrayField() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>public-int-array</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value>",
				"										<array>",
				"											<data>",
				"												<value><int>42</int></value>",
				"												<value><int>43</int></value>",
				"											</data>",
				"										</array>",
				"									</value>",
				"									<value>",
				"										<array>",
				"											<data>",
				"												<value><int>44</int></value>",
				"												<value><int>45</int></value>",
				"												<value><int>46</int></value>",
				"											</data>",
				"										</array>",
				"									</value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { JaggedIntArraysStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof JaggedIntArraysStruct);
		JaggedIntArraysStruct struct = (JaggedIntArraysStruct) call.getParameters()[0];
		assertNotNull(struct.publicIntArray);
		assertEquals(2, struct.publicIntArray.length);
		assertEquals(2, struct.publicIntArray[0].length);
		assertEquals(42, struct.publicIntArray[0][0]);
		assertEquals(43, struct.publicIntArray[0][1]);
		assertEquals(3, struct.publicIntArray[1].length);
		assertEquals(44, struct.publicIntArray[1][0]);
		assertEquals(45, struct.publicIntArray[1][1]);
		assertEquals(46, struct.publicIntArray[1][2]);
	}

	public void testJaggedCustomTypeArrayField() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>public-custom-array</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value>",
				"										<array>",
				"											<data>",
				"												<value>",
				"													<struct>",
				"														<member>",
				"															<name>string-member</name>",
				"															<value>hit me baby</value>",
				"														</member>",
				"													</struct>",
				"												</value>",
				"												<value>",
				"													<struct>",
				"														<member>",
				"															<name>string-member</name>",
				"															<value>one more time</value>",
				"														</member>",
				"													</struct>",
				"												</value>",
				"											</data>",
				"										</array>",
				"									</value>",
				"									<value>",
				"										<array>",
				"											<data>",
				"												<value>",
				"													<struct>",
				"														<member>",
				"															<name>string-member</name>",
				"															<value>hit me baby</value>",
				"														</member>",
				"													</struct>",
				"												</value>",
				"												<value>",
				"													<struct>",
				"														<member>",
				"															<name>string-member</name>",
				"															<value>one more time</value>",
				"														</member>",
				"													</struct>",
				"												</value>",
				"												<value>",
				"													<struct>",
				"														<member>",
				"															<name>string-member</name>",
				"															<value>ooh ooh</value>",
				"														</member>",
				"													</struct>",
				"												</value>",
				"											</data>",
				"										</array>",
				"									</value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { JaggedCustomTypeArraysStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof JaggedCustomTypeArraysStruct);
		JaggedCustomTypeArraysStruct struct = (JaggedCustomTypeArraysStruct) call.getParameters()[0];
		assertNotNull(struct.publicCustomArray);
		assertEquals(2, struct.publicCustomArray.length);
		assertEquals(2, struct.publicCustomArray[0].length);
		assertEquals("hit me baby", struct.publicCustomArray[0][0].stringMember);
		assertEquals("one more time", struct.publicCustomArray[0][1].stringMember);
		assertEquals(3, struct.publicCustomArray[1].length);
		assertEquals("hit me baby", struct.publicCustomArray[1][0].stringMember);
		assertEquals("one more time", struct.publicCustomArray[1][1].stringMember);
		assertEquals("ooh ooh", struct.publicCustomArray[1][2].stringMember);
	}

	public void testJaggedCustomTypeArrayMethod() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>private-custom-array</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value>",
				"										<array>",
				"											<data>",
				"												<value>",
				"													<struct>",
				"														<member>",
				"															<name>string-member</name>",
				"															<value>hit me baby</value>",
				"														</member>",
				"													</struct>",
				"												</value>",
				"												<value>",
				"													<struct>",
				"														<member>",
				"															<name>string-member</name>",
				"															<value>one more time</value>",
				"														</member>",
				"													</struct>",
				"												</value>",
				"											</data>",
				"										</array>",
				"									</value>",
				"									<value>",
				"										<array>",
				"											<data>",
				"												<value>",
				"													<struct>",
				"														<member>",
				"															<name>string-member</name>",
				"															<value>hit me baby</value>",
				"														</member>",
				"													</struct>",
				"												</value>",
				"												<value>",
				"													<struct>",
				"														<member>",
				"															<name>string-member</name>",
				"															<value>one more time</value>",
				"														</member>",
				"													</struct>",
				"												</value>",
				"												<value>",
				"													<struct>",
				"														<member>",
				"															<name>string-member</name>",
				"															<value>ooh ooh</value>",
				"														</member>",
				"													</struct>",
				"												</value>",
				"											</data>",
				"										</array>",
				"									</value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { JaggedCustomTypeArraysStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof JaggedCustomTypeArraysStruct);
		JaggedCustomTypeArraysStruct struct = (JaggedCustomTypeArraysStruct) call.getParameters()[0];
		assertNotNull(struct.getPrivateCustomArray());
		assertEquals(2, struct.getPrivateCustomArray().length);
		assertEquals(2, struct.getPrivateCustomArray()[0].length);
		assertEquals("hit me baby", struct.getPrivateCustomArray()[0][0].stringMember);
		assertEquals("one more time", struct.getPrivateCustomArray()[0][1].stringMember);
		assertEquals(3, struct.getPrivateCustomArray()[1].length);
		assertEquals("hit me baby", struct.getPrivateCustomArray()[1][0].stringMember);
		assertEquals("one more time", struct.getPrivateCustomArray()[1][1].stringMember);
		assertEquals("ooh ooh", struct.getPrivateCustomArray()[1][2].stringMember);
	}

	public void testJaggedObjectArrayField() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>public-object-array</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value>",
				"										<array>",
				"											<data>",
				"												<value><string>foo</string></value>",
				"												<value><int>42</int></value>",
				"											</data>",
				"										</array>",
				"									</value>",
				"									<value>",
				"										<array>",
				"											<data>",
				"												<value><double>3.14</double></value>",
				"												<value><boolean>1</boolean></value>",
				"											</data>",
				"										</array>",
				"									</value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { JaggedObjectArraysStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof JaggedObjectArraysStruct);
		JaggedObjectArraysStruct struct = (JaggedObjectArraysStruct) call.getParameters()[0];
		assertNotNull(struct.publicObjectArray);
		assertEquals(2, struct.publicObjectArray.length);
		assertEquals(2, struct.publicObjectArray[0].length);
		assertTrue(struct.publicObjectArray[0][0] instanceof String);
		assertEquals("foo", struct.publicObjectArray[0][0]);
		assertTrue(struct.publicObjectArray[0][1] instanceof Integer);
		assertEquals(new Integer(42), struct.publicObjectArray[0][1]);
		assertEquals(2, struct.publicObjectArray[1].length);
		assertTrue(struct.publicObjectArray[1][0] instanceof Double);
		assertEquals(new Double(3.14), struct.publicObjectArray[1][0]);
		assertTrue(struct.publicObjectArray[1][1] instanceof Boolean);
		assertEquals(Boolean.TRUE, struct.publicObjectArray[1][1]);
	}

	public void testJaggedObjectArrayMethod() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>private-object-array</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value>",
				"										<array>",
				"											<data>",
				"												<value><string>foo</string></value>",
				"												<value><int>42</int></value>",
				"											</data>",
				"										</array>",
				"									</value>",
				"									<value>",
				"										<array>",
				"											<data>",
				"												<value><double>3.14</double></value>",
				"												<value><boolean>1</boolean></value>",
				"											</data>",
				"										</array>",
				"									</value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { JaggedObjectArraysStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof JaggedObjectArraysStruct);
		JaggedObjectArraysStruct struct = (JaggedObjectArraysStruct) call.getParameters()[0];
		assertNotNull(struct.getPrivateObjectArray());
		assertEquals(2, struct.getPrivateObjectArray().length);
		assertEquals(2, struct.getPrivateObjectArray()[0].length);
		assertTrue(struct.getPrivateObjectArray()[0][0] instanceof String);
		assertEquals("foo", struct.getPrivateObjectArray()[0][0]);
		assertTrue(struct.getPrivateObjectArray()[0][1] instanceof Integer);
		assertEquals(new Integer(42), struct.getPrivateObjectArray()[0][1]);
		assertEquals(2, struct.getPrivateObjectArray()[1].length);
		assertTrue(struct.getPrivateObjectArray()[1][0] instanceof Double);
		assertEquals(new Double(3.14), struct.getPrivateObjectArray()[1][0]);
		assertTrue(struct.getPrivateObjectArray()[1][1] instanceof Boolean);
		assertEquals(Boolean.TRUE, struct.getPrivateObjectArray()[1][1]);
	}

	public void testStructParamMethod() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>string-object</name>",
				"						<value>string object</value>",
				"					</member>",
				"					<member>",
				"						<name>char-array</name>",
				"						<value>char array</value>",
				"					</member>",
				"					<member>",
				"						<name>int-object</name>",
				"						<value><int>42</int></value>",
				"					</member>",
				"					<member>",
				"						<name>int-val</name>",
				"						<value><int>24</int></value>",
				"					</member>",
				"					<member>",
				"						<name>double-object</name>",
				"						<value><double>3.14</double></value>",
				"					</member>",
				"					<member>",
				"						<name>double-val</name>",
				"						<value><double>6.28</double></value>",
				"					</member>",
				"					<member>",
				"						<name>float-object</name>",
				"						<value><double>3.24</double></value>",
				"					</member>",
				"					<member>",
				"						<name>float-val</name>",
				"						<value><double>6.48</double></value>",
				"					</member>",
				"					<member>",
				"						<name>boolean-val</name>",
				"						<value><boolean>1</boolean></value>",
				"					</member>",
				"					<member>",
				"						<name>boolean-object</name>",
				"						<value><boolean>1</boolean></value>",
				"					</member>",
				"					<member>",
				"						<name>date</name>",
				"						<value><dateTime.iso8601>19980717T14:08:55</dateTime.iso8601></value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { TestObject.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof TestObject);
		TestObject object = (TestObject) call.getParameters()[0];
		assertEquals("string object", object.getStringObject());
		assertEquals("char array", new String(object.getCharArray()));
		assertEquals(new Integer(42), object.getIntObject());
		assertEquals(24, object.getIntVal());
		assertEquals(new Double(3.14), object.getDoubleObject());
		assertEquals(6.28, object.getDoubleVal(), 0.001);
		assertEquals(new Float(3.24), object.getFloatObject());
		assertEquals(6.48, object.getFloatVal(), 0.001);
		assertEquals(Boolean.TRUE, object.getBooleanObject());
		assertEquals(true, object.getBooleanVal());
		assertEquals(newDate(1998, 7, 17, 14, 8, 55), object.getDate());
	}

	public void testStructParamField() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>string-object</name>",
				"						<value>string object</value>",
				"					</member>",
				"					<member>",
				"						<name>char-array</name>",
				"						<value>char array</value>",
				"					</member>",
				"					<member>",
				"						<name>int-object</name>",
				"						<value><int>42</int></value>",
				"					</member>",
				"					<member>",
				"						<name>int-val</name>",
				"						<value><int>24</int></value>",
				"					</member>",
				"					<member>",
				"						<name>double-object</name>",
				"						<value><double>3.14</double></value>",
				"					</member>",
				"					<member>",
				"						<name>double-val</name>",
				"						<value><double>6.28</double></value>",
				"					</member>",
				"					<member>",
				"						<name>float-object</name>",
				"						<value><double>3.24</double></value>",
				"					</member>",
				"					<member>",
				"						<name>float-val</name>",
				"						<value><double>6.48</double></value>",
				"					</member>",
				"					<member>",
				"						<name>boolean-val</name>",
				"						<value><boolean>1</boolean></value>",
				"					</member>",
				"					<member>",
				"						<name>boolean-object</name>",
				"						<value><boolean>1</boolean></value>",
				"					</member>",
				"					<member>",
				"						<name>date</name>",
				"						<value><dateTime.iso8601>19980717T14:08:55</dateTime.iso8601></value>",
				"					</member>",
				"					<member>",
				"						<name>object-array</name>",
				"						<value>",
				"							<array>",
				"								<data>",
				"									<value>",
				"										<string>ObjectOne</string>",
				"									</value>",
				"									<value>",
				"										<string>ObjectTwo</string>",
				"									</value>",
				"								</data>",
				"							</array>",
				"						</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { TestStruct.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof TestStruct);
		TestStruct struct = (TestStruct) call.getParameters()[0];
		assertEquals("string object", struct.stringObject);
		assertEquals("char array", new String(struct.charArray));
		assertEquals(new Integer(42), struct.intObject);
		assertEquals(24, struct.intVal);
		assertEquals(new Double(3.14), struct.doubleObject);
		assertEquals(6.28, struct.doubleVal, 0.001);
		assertEquals(new Float(3.24), struct.floatObject);
		assertEquals(6.48, struct.floatVal, 0.001);
		assertEquals(Boolean.TRUE, struct.booleanObject);
		assertEquals(true, struct.booleanVal);
		assertEquals(newDate(1998, 7, 17, 14, 8, 55), struct.date);
		assertTrue(Arrays.equals(new Object[] { "ObjectOne", "ObjectTwo" }, struct.objectArray));
	}

	public void testStructParamNested() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
				"<methodCall>",
				"   <methodName>testName</methodName>",
				"   <params>",
				"      <param>",
				"         <value>",
				"            <struct>",
				"               <member>",
				"                  <name>public-second-level</name>",
				"                  <value>",
				"                     <struct>",
				"                        <member>",
				"                           <name>name</name>",
				"                           <value>",
				"                              <string>public</string>",
				"                           </value>",
				"                        </member>",
				"                     </struct>",
				"                  </value>",
				"               </member>",
				"               <member>",
				"                  <name>private-second-level</name>",
				"                  <value>",
				"                     <struct>",
				"                        <member>",
				"                           <name>name</name>",
				"                           <value>",
				"                              <string>private</string>",
				"                           </value>",
				"                        </member>",
				"                     </struct>",
				"                  </value>",
				"               </member>",
				"            </struct>",
				"         </value>",
				"      </param>",
				"   </params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { FirstLevel.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof FirstLevel);
		FirstLevel object = (FirstLevel) call.getParameters()[0];
		assertEquals("public", object.publicSecondLevel.name);
		assertEquals("private", object.getPrivateSecondLevel().name);
	}

	public void testFailOnMissingFieldWithSetterMethod() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>string-object</name>",
				"						<value>string object</value>",
				"					</member>",
				"					<member>",
				"						<name>missing-field</name>",
				"						<value>missing field value</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		try {
			this.unmarshal(xml, new Class[] { TestObject.class });
			fail();
		} catch(MarshallingException e) {
			assertTrue(e.getMessage().contains("Can't find member 'missingField'"));
		}
	}

	public void testIgnoreMissingFieldWithSetterMethod() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>string-object</name>",
				"						<value>string object</value>",
				"					</member>",
				"					<member>",
				"						<name>missing-field</name>",
				"						<value>missing field value</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		MethodCallUnmarshallerAid aid = new MethodCallUnmarshallerAid() {
			public Class getType(String methodName, int index) {
				return TestObject.class;
			}
			
			public boolean ignoreMissingFields() {
				return true;
			}
		};
		RpcCall call = this.unmarshalWithAid(xml, aid);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof TestObject);
		TestObject object = (TestObject) call.getParameters()[0];
		assertEquals("string object", object.getStringObject());
	}


	public void testFailOnMissingFieldWithFieldAccess() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>string-object</name>",
				"						<value>string object</value>",
				"					</member>",
				"					<member>",
				"						<name>missing-field</name>",
				"						<value>missing field value</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		try {
			this.unmarshal(xml, new Class[] { TestStruct.class });
			fail();
		} catch(MarshallingException e) {
			assertTrue(e.getMessage().contains("Can't find member 'missingField'"));
		}
	}

	public void testIgnoreMissingFieldWithFieldAccess() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>string-object</name>",
				"						<value>string object</value>",
				"					</member>",
				"					<member>",
				"						<name>missing-field</name>",
				"						<value>missing field value</value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		MethodCallUnmarshallerAid aid = new MethodCallUnmarshallerAid() {
			public Class getType(String methodName, int index) {
				return TestStruct.class;
			}
			
			public boolean ignoreMissingFields() {
				return true;
			}
		};
		RpcCall call = this.unmarshalWithAid(xml, aid);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof TestStruct);
		TestStruct object = (TestStruct) call.getParameters()[0];
		assertEquals("string object", object.stringObject);
	}

	// testValidation* are based on the XML-RPC validation suite.

	public void testValidationSuiteArrayOfStructsTest() throws Exception {
		InputStream is = this.getClass().getResourceAsStream("arrayOfStructsTest.xml");
		RpcCall call = this.unmarshal(is, new Class[] { MoeLarryAndCurly[].class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof MoeLarryAndCurly[]);
	}

	public void testValidationSuiteCountTheEntities() throws Exception {
		InputStream is = this.getClass().getResourceAsStream("countTheEntities.xml");
		RpcCall call = this.unmarshal(is, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof String);
		assertEquals("'b<<&'r&z'n'is'ujqe<&'&xcgo<>&<\"dv'<mltw<fakp<&'''hy\"", call.getParameters()[0]);
	}

	public void testValidationSuiteEasyStructTest() throws Exception {
		InputStream is = this.getClass().getResourceAsStream("easyStructTest.xml");
		RpcCall call = this.unmarshal(is, new Class[] { MoeLarryAndCurly.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof MoeLarryAndCurly);
	}

	public void testValidationSuiteEchoStructTest() throws Exception {
		InputStream is = this.getClass().getResourceAsStream("echoStructTest.xml");
		RpcCall call = this.unmarshal(is, null);
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof Map);
	}

	public void testValidationSuiteManyTypesTest() throws Exception {
		InputStream is = this.getClass().getResourceAsStream("manyTypesTest.xml");
		RpcCall call = this.unmarshal(is, null);
		assertNotNull(call.getParameters());
		assertEquals(6, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof Integer);
		assertTrue(call.getParameters()[1] instanceof Boolean);
		assertTrue(call.getParameters()[2] instanceof String);
		assertTrue(call.getParameters()[3] instanceof Double);
		assertTrue(call.getParameters()[4] instanceof Date);
		assertTrue(call.getParameters()[5] instanceof byte[]);
	}

	public void testValidationModerateSizeArrayCheck() throws Exception {
		InputStream is = this.getClass().getResourceAsStream("moderateSizeArrayCheck.xml");
		RpcCall call = this.unmarshal(is, new Class[] { String[].class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof String[]);
		String[] strings = (String[]) call.getParameters()[0];
		assertEquals(148, strings.length);
		assertEquals("Illinois", strings[0]);
		assertEquals("Arkansas", strings[147]);
	}

	public void testValidationSuiteNestedStructTest() throws Exception {
		InputStream is = this.getClass().getResourceAsStream("nestedStructTest.xml");
		RpcCall call = this.unmarshal(is, null);
	}

	public void testValidationSuiteSimpleStructReturnTest() throws Exception {
		InputStream is = this.getClass().getResourceAsStream("simpleStructReturnTest.xml");
		RpcCall call = this.unmarshal(is, null);
	}

    public static class Testes {
	public int i;
	public boolean b;
	public double d;
	public String s;
	public Object o1;
	public Object o2;
	public Object o3;
	public Object o4;
	public Object o5;
	public Object o6;
	public String s2;
	public int[] ai;
	public boolean[][] aab;
	public Object[] oi;
	public int intValue;
	public boolean booleanValue;
	public double doubleValue;
	public Testes2 innerTestes;
	public Testes2[] innerTestesArray;

	public Testes() {}

	public String toString() { return "Testes: " + i + ", " + b + ", " + d + ", \"" + s + "\", \"" + s2 + "\", " + o1 + ", " + o2 + ", " + o3 + ", " + o4 + ", " + o5 + ", " + o6 + ", " + Utils.toString(ai) + ", " + Utils.toString(aab) + ", " + Utils.toString(oi) + ", (" + innerTestes + "), " + Utils.toString(innerTestesArray) ; }
    }

    public static class Testes2 {
	public int i;
	public boolean b;
	public double d;
	public int intValue;
	public boolean booleanValue;
	public double doubleValue;

	public Testes2() {}

	public String toString() { return "Testes2: " + i + ", " + b + ", " + d + ", " + intValue + ", " + booleanValue + ", " + doubleValue; }
    }

    public void testValidationSuiteComplicatedNestedStructsAndArrays() throws Exception {
	// TODO flesh out complexNestedStruct.xml - RPJ lost the original
		InputStream is = this.getClass().getResourceAsStream("complexNestedStruct.xml");
		RpcCall call = this.unmarshal(is, new Class[] { Testes.class } );
    }

	// Testcase for a bug in the SaxUnmarshaller
	public void testReservation() throws Exception {
		String[] xmlLines = new String[] {
				"<?xml version=\"1.0\"?>",
				"<methodCall>",
				"	<methodName>testMethod</methodName>",
				"	<params>",
				"		<param>",
				"			<value>",
				"				<struct>",
				"					<member>",
				"						<name>properties</name>",
				"						<value>",
				"							<struct>",
				"								<member>",
				"									<name>foo</name>",
				"									<value>1234</value>",
				"								</member>",
				"							</struct>",
				"						</value>",
				"					</member>",
				"					<member>",
				"						<name>id</name>",
				"						<value>string object</value>",
				"					</member>",
				"					<member>",
				"						<name>kill</name>",
				"						<value><boolean>1</boolean></value>",
				"					</member>",
				"					<member>",
				"						<name>start</name>",
				"						<value><dateTime.iso8601>19980717T14:08:55</dateTime.iso8601></value>",
				"					</member>",
				"				</struct>",
				"			</value>",
				"		</param>",
				"	</params>",
				"</methodCall>" };
		String xml = toString(xmlLines);

		RpcCall call = this.unmarshal(xml, new Class[] { Reservation.class });
		assertNotNull(call.getParameters());
		assertEquals(1, call.getParameters().length);
		assertTrue(call.getParameters()[0] instanceof Reservation);
		Reservation struct = (Reservation) call.getParameters()[0];
		assertEquals("string object", struct.id);
		assertEquals(true, struct.kill);
		assertEquals(newDate(1998, 7, 17, 14, 8, 55), struct.start);
	}
}
