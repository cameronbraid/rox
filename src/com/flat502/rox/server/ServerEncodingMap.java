package com.flat502.rox.server;

import java.util.HashMap;
import java.util.Map;

import com.flat502.rox.encoding.Encoding;
import com.flat502.rox.encoding.EncodingMap;

class ServerEncodingMap implements EncodingMap {
	private Map map = new HashMap();
	
	public Encoding addEncoding(Encoding encoding) {
		return (Encoding) this.map.put(encoding.getName(), encoding);
	}
	
	public Encoding getEncoding(String name) {
		return (Encoding) this.map.get(name);
	}
}
