/**
 * 
 */
package com.flat502.rox.marshal;

import java.util.LinkedList;

public class LinkedListStruct {
	public LinkedList publicList;
	private LinkedList privateList;

	public LinkedListStruct(LinkedList publicList, LinkedList privateList) {
		this.publicList = publicList;
		this.privateList = privateList;
	}

	public LinkedListStruct() {
	}

	public LinkedList getPrivateList() {
		return this.privateList;
	}

	public void setPrivateList(LinkedList v) {
		this.privateList = v;
	}
}