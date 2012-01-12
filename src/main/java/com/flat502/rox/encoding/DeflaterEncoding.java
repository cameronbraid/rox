package com.flat502.rox.encoding;

import java.io.*;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

import com.flat502.rox.http.HttpConstants;

public class DeflaterEncoding implements Encoding {
	public String getName() {
		return HttpConstants.ContentEncoding.DEFLATE;
	}

	public InputStream getDecoder(InputStream in) throws IOException {
		return new InflaterInputStream(in);
	}

	public OutputStream getEncoder(OutputStream out) throws IOException {
		return new DeflaterOutputStream(out);
	}
}
