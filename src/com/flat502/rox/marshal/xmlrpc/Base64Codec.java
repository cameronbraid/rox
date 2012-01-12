package com.flat502.rox.marshal.xmlrpc;

/**
 * An RFC 1521 compliant base 64 encoder/decoder.
 */
class Base64Codec {
	// Map 6-bit nibbles to base 64 chars
	private static char[] nibbleMap = new char[64];

	// Map base 64 chars to 6-bit nibbles
	private static byte[] charMap = new byte[128];

	public static String encode(byte[] in) {
		int unpaddedLen = (in.length * 4 + 2) / 3;
		int paddedLen = ((in.length + 2) / 3) * 4;
		char[] out = new char[paddedLen];
		int inIdx = 0;
		int outIdx = 0;
		while (inIdx < in.length) {
			int i0 = in[inIdx++] & 0xff;
			int i1 = inIdx < in.length ? in[inIdx++] & 0xff : 0;
			int i2 = inIdx < in.length ? in[inIdx++] & 0xff : 0;
			int o0 = i0 >>> 2;
			int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
			int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
			int o3 = i2 & 0x3F;
			out[outIdx++] = nibbleMap[o0];
			out[outIdx++] = nibbleMap[o1];
			out[outIdx] = outIdx < unpaddedLen ? nibbleMap[o2] : '=';
			outIdx++;
			out[outIdx] = outIdx < unpaddedLen ? nibbleMap[o3] : '=';
			outIdx++;
		}
		return new String(out);
	}

	public static byte[] decode(char[] in) {
	    return decode( in, 0, in.length );
	}

    public static byte[] decode(char[] in, int inIdx, int inLen) {
		if (inLen % 4 != 0) {
			throw new IllegalArgumentException(
					"Base 64 encoded string length not divisible by 4: " + in.length);
		}

		int endIdx = inIdx + inLen - 1;
		while (inLen > 0 && in[endIdx] == '=') {
			inLen--;
			endIdx--;
		}
		int outLen = (inLen * 3) / 4;
		byte[] out = new byte[outLen];
		int outIdx = 0;
		while (inIdx <= endIdx) {
			int i0 = in[inIdx++];
			int i1 = in[inIdx++];
			int i2 = inIdx <= endIdx ? in[inIdx++] : 'A';
			int i3 = inIdx <= endIdx ? in[inIdx++] : 'A';
			if (i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127)
				throw new IllegalArgumentException(
						"Base 64 encoded data contains an illegal character");
			int b0 = charMap[i0];
			int b1 = charMap[i1];
			int b2 = charMap[i2];
			int b3 = charMap[i3];
			if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0)
				throw new IllegalArgumentException(
						"Base 64 encoded data contains an illegal character");
			int o0 = (b0 << 2) | (b1 >>> 4);
			int o1 = ((b1 & 0xf) << 4) | (b2 >>> 2);
			int o2 = ((b2 & 3) << 6) | b3;
			out[outIdx++] = (byte) o0;
			if (outIdx < outLen)
				out[outIdx++] = (byte) o1;
			if (outIdx < outLen)
				out[outIdx++] = (byte) o2;
		}
		return out;
	}

	static {
		// Initialize our mapping tables
		int i = 0;
		for (char c = 'A'; c <= 'Z'; c++)
			nibbleMap[i++] = c;
		for (char c = 'a'; c <= 'z'; c++)
			nibbleMap[i++] = c;
		for (char c = '0'; c <= '9'; c++)
			nibbleMap[i++] = c;
		nibbleMap[i++] = '+';
		nibbleMap[i++] = '/';

		for (i = 0; i < charMap.length; i++)
			charMap[i] = -1;
		for (i = 0; i < 64; i++)
			charMap[nibbleMap[i]] = (byte) i;
	}
}
