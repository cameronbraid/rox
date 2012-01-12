package com.flat502.rox.marshal;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

import junit.framework.TestCase;

import com.flat502.rox.utils.XmlPlainPrinter;
 
public class Test_ClassDescriptor extends TestCase {
	public void testWrongDescriptor() throws Exception {
		ClassDescriptor cd = ClassDescriptor.getInstance(TwoStrings.class);

		SimpleStruct struct = new SimpleStruct();
		try {
			cd.getValue(struct, "nullString");
			fail();
		} catch (IllegalArgumentException e) {
		}

		try {
			cd.setValue(struct, "nullString", "Test");
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	public void testTwoStrings() throws Exception {
		ClassDescriptor cd = ClassDescriptor.getInstance(TwoStrings.class);
		assertEquals(String.class, cd.getGetterType("nullString"));
		assertEquals(String.class, cd.getGetterType("nonNullString"));

		TwoStrings struct = new TwoStrings();
		assertNull(cd.getValue(struct, "nullString"));
		assertEquals("Not Null", cd.getValue(struct, "nonNullString"));

		cd.setValue(struct, "nullString", "Test");
		assertEquals("Test", cd.getValue(struct, "nullString"));
	}

	public void testTestObject() throws Exception {
		ClassDescriptor cd = ClassDescriptor.getInstance(TestObject.class);
		assertEquals(Integer.class, cd.getGetterType("intObject"));
		assertEquals(Integer.TYPE, cd.getGetterType("intVal"));

		TestObject struct = new TestObject();
		assertNull(cd.getValue(struct, "intObject"));
		assertEquals(new Integer(0), cd.getValue(struct, "intVal"));

		cd.setValue(struct, "intObject", new Integer(42));
		assertEquals(new Integer(42), cd.getValue(struct, "intObject"));
		cd.setValue(struct, "intObject", new Integer(43));
		assertEquals(new Integer(43), cd.getValue(struct, "intObject"));
	}

	public void testPublicFieldWithMethods() throws Exception {
		ClassDescriptor cd = ClassDescriptor.getInstance(PublicFieldWithMethods.class);

		PublicFieldWithMethods struct = new PublicFieldWithMethods("Hello World");
		assertEquals("Hello World", cd.getValue(struct, "stringMember"));
		assertTrue(struct.getterCalled);

		cd.setValue(struct, "stringMember", "Goodbye World");
		assertTrue(struct.getterCalled);
		assertEquals("Goodbye World", cd.getValue(struct, "stringMember"));
	}

	public void testTransientFieldSkipped() throws Exception {
		ClassDescriptor cd = ClassDescriptor.getInstance(Modifiers.class);

		Modifiers struct = new Modifiers();
		assertEquals("NORMAL", cd.getValue(struct, "normalString"));
		try {
			assertEquals("TRANSIENT", cd.getValue(struct, "transientString"));
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	public void testStaticFieldSkipped() throws Exception {
		ClassDescriptor cd = ClassDescriptor.getInstance(Modifiers.class);

		Modifiers struct = new Modifiers();
		assertEquals("NORMAL", cd.getValue(struct, "normalString"));
		try {
			assertEquals("STATIC", cd.getValue(struct, "staticString"));
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	public void testFinalFieldSkipped() throws Exception {
		ClassDescriptor cd = ClassDescriptor.getInstance(Modifiers.class);

		Modifiers struct = new Modifiers();
		assertEquals("NORMAL", cd.getValue(struct, "normalString"));
		try {
			assertEquals("FINAL", cd.getValue(struct, "finalString"));
			fail();
		} catch (IllegalArgumentException e) {
		}
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_ClassDescriptor.class);
	}
}
