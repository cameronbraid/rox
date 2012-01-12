package com.flat502.rox.client;

import com.flat502.rox.http.HttpResponseBuffer;

interface Notifiable {
	public void notify(HttpResponseBuffer rsp, RpcResponseContext context);
	public void notify(Throwable e, RpcResponseContext context);
	public void notifyTimedOut(Throwable cause, RpcResponseContext context);
}
