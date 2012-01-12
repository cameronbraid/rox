package com.flat502.rox.encoding;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.flat502.rox.http.HttpConstants;

public class GZipEncoding implements Encoding {
	public String getName() {
		return HttpConstants.ContentEncoding.GZIP;
	}

	public InputStream getDecoder(InputStream in) throws IOException {
		return new GZIPInputStream(in);
	}

	public OutputStream getEncoder(OutputStream out) throws IOException {
		return new GZIPOutputStream(out);
	}
}
