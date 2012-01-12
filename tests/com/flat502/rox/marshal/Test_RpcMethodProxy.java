package com.flat502.rox.marshal;

import junit.framework.TestCase;

import com.flat502.rox.marshal.xmlrpc.XmlRpcMethodCall;
import com.flat502.rox.marshal.xmlrpc.XmlRpcMethodFault;
import com.flat502.rox.marshal.xmlrpc.XmlRpcMethodResponse;
import com.flat502.rox.processing.SSLSession;
import com.flat502.rox.server.RpcCallContext;
import com.flat502.rox.server.RpcMethodProxy;

public class Test_RpcMethodProxy extends TestCase {
	public void testHyphentatedMethodNameMapping() throws Exception {
		ProxyObject target = new ProxyObject();
		MockRpcMethodProxy proxy = new MockRpcMethodProxy("prefix\\.(foo-bar)", target);
		proxy.invoke(new XmlRpcMethodCall("prefix.foo-bar", new String[] { "param" }), null);

		assertEquals("fooBar", target.lastMethod);
		assertEquals("param", target.lastParams[0]);
	}

	public void testObjectReturnType() throws Exception {
		ProxyObject target = new ProxyObject();
		MockRpcMethodProxy proxy = new MockRpcMethodProxy("prefix\\.(returns-object)", target);
		proxy.invoke(new XmlRpcMethodCall("prefix.returns-object", new String[] { "param" }), null);

		assertEquals("returnsObject", target.lastMethod);
		assertEquals("param", target.lastParams[0]);
	}

	private static class MockRpcMethodProxy extends RpcMethodProxy {
		public MockRpcMethodProxy(String namePattern, Object target) {
			super(namePattern, target);
		}

		public RpcFault newRpcFault(Throwable e) {
			return new XmlRpcMethodFault(e);
		}

		protected RpcResponse newRpcResponse(Object returnValue) {
			return new XmlRpcMethodResponse(returnValue);
		}
	}

	public void testOptionalRpcCallParameter() throws Exception {
		ContextualProxyObject target = new ContextualProxyObject();
		MockRpcMethodProxy proxy = new MockRpcMethodProxy("prefix\\.(foo-bar)", target);
		RpcCallContext context = new RpcCallContext(null, null, null);
		proxy.invoke(new XmlRpcMethodCall("prefix.foo-bar", new String[] { "param" }), context);

		assertEquals("fooBar", target.lastMethod);
		assertEquals("param", target.lastParams[0]);
		assertTrue(target.lastParams[1] instanceof RpcCallContext);
	}

	public static void main(String[] args) {
		junit.textui.TestRunner.run(Test_RpcMethodProxy.class);
	}
}
