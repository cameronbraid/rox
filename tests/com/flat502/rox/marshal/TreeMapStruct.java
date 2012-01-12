/**
 * 
 */
package com.flat502.rox.marshal;

import java.util.TreeMap;

public class TreeMapStruct {
	public TreeMap publicMap;
	private TreeMap privateMap;

	public TreeMapStruct(TreeMap publicMap, TreeMap privateMap) {
		this.publicMap = publicMap;
		this.privateMap = privateMap;
	}

	public TreeMapStruct() {
	}

	public TreeMap getPrivateMap() {
		return this.privateMap;
	}

	public void setPrivateMap(TreeMap v) {
		this.privateMap = v;
	}
}