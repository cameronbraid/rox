package com.flat502.rox.marshal;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

import junit.framework.TestCase;

import com.flat502.rox.utils.XmlPlainPrinter;

public class Test_HyphenatedFieldNameCodec extends TestCase {
	public void testDecodeSingleWord() throws Exception {
		HyphenatedFieldNameCodec codec = new HyphenatedFieldNameCodec();
		assertEquals("single",  codec.decodeFieldName("single"));
	}

	public void testEncodeSingleWord() throws Exception {
		HyphenatedFieldNameCodec codec = new HyphenatedFieldNameCodec();
		assertEquals("single",  codec.encodeFieldName("single"));
	}

	public void testDecodeDoubleWord() throws Exception {
		HyphenatedFieldNameCodec codec = new HyphenatedFieldNameCodec();
		assertEquals("doubleWord",  codec.decodeFieldName("double-word"));
	}

	public void testEncodeDoubleWord() throws Exception {
		HyphenatedFieldNameCodec codec = new HyphenatedFieldNameCodec();
		assertEquals("double-word",  codec.encodeFieldName("doubleWord"));
	}

	public void testDecodeCapitalsRun() throws Exception {
		HyphenatedFieldNameCodec codec = new HyphenatedFieldNameCodec();
		assertEquals("aBCD",  codec.decodeFieldName("a-b-c-d"));
	}

	public void testEncodeCapitalsRun() throws Exception {
		HyphenatedFieldNameCodec codec = new HyphenatedFieldNameCodec();
		assertEquals("a-b-c-d",  codec.encodeFieldName("aBCD"));
	}

	public void testDecodeSingleLetterLowercase() throws Exception {
		HyphenatedFieldNameCodec codec = new HyphenatedFieldNameCodec();
		assertEquals("a",  codec.decodeFieldName("a"));
	}

	public void testEncodeSingleLetterLowercase() throws Exception {
		HyphenatedFieldNameCodec codec = new HyphenatedFieldNameCodec();
		assertEquals("a",  codec.encodeFieldName("a"));
	}

	public void testDecodeSingleLetterUppercase() throws Exception {
		HyphenatedFieldNameCodec codec = new HyphenatedFieldNameCodec();
		assertEquals("a",  codec.decodeFieldName("A"));
	}

	public void testEncodeSingleLetterUppercase() throws Exception {
		HyphenatedFieldNameCodec codec = new HyphenatedFieldNameCodec();
		assertEquals("a",  codec.encodeFieldName("A"));
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_HyphenatedFieldNameCodec.class);
	}
}
