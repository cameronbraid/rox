/**
 * 
 */
package com.flat502.rox.marshal;

import java.util.List;

public class GenericsStructJ5 {
	public List<String> publicList;
	private List<String> privateList;

	public GenericsStructJ5(List<String> publicMap, List<String> privateMap) {
		this.publicList = publicMap;
		this.privateList = privateMap;
	}

	public GenericsStructJ5() {
	}

	public List<String> getPrivateList() {
		return this.privateList;
	}

	public void setPrivateList(List<String> v) {
		this.privateList = v;
	}
}