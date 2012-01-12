/**
 * 
 */
package com.flat502.rox.marshal;

import java.util.Date;
import java.util.List;

public class ListStruct {
	public List publicList;
	private List privateList;

	public ListStruct(List publicList, List privateList) {
		this.publicList = publicList;
		this.privateList = privateList;
	}

	public ListStruct() {
	}

	public List getPrivateList() {
		return this.privateList;
	}

	public void setPrivateList(List v) {
		this.privateList = v;
	}
}