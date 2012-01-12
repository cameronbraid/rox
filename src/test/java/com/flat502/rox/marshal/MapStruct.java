/**
 * 
 */
package com.flat502.rox.marshal;

import java.util.Map;

public class MapStruct {
	public Map publicMap;
	private Map privateMap;

	public MapStruct(Map publicMap, Map privateMap) {
		this.publicMap = publicMap;
		this.privateMap = privateMap;
	}

	public MapStruct() {
	}

	public Map getPrivateMap() {
		return this.privateMap;
	}

	public void setPrivateMap(Map v) {
		this.privateMap = v;
	}
}