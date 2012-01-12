package com.flat502.rox.client;

import com.flat502.rox.marshal.MethodResponseUnmarshallerAid;

class ReturnTypeMapper extends MethodResponseUnmarshallerAid {
	private Class retClass;

	// retClass may be null
	public ReturnTypeMapper(Class retClass) {
		this.retClass = retClass;
	}
	
	public Class getReturnType() {
		return this.retClass;
	}
}
