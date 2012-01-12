package com.flat502.rox.demo.validation;

import java.net.URL;
import java.util.HashMap;

import com.flat502.rox.client.XmlRpcClient;
import com.flat502.rox.log.Level;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.log.SimpleLogFactory;
import com.flat502.rox.log.StreamLog;
import com.flat502.rox.marshal.NopFieldNameCodec;
import com.flat502.rox.marshal.xmlrpc.DomMethodResponseUnmarshaller;

public class ValidationSuiteClient {
	/**
	 * Call each of the exposed methods on the remote demo server.
	 * @param args
	 * 	A list of parameters. Only the first is used and if
	 * 	present must be the URL of the remote server. This 
	 * 	defaults to <code>http://localhost:8080/</code> if
	 * 	not specified.
	 */
	public static void main(String[] args) {
		try {
			String url = "http://localhost:8080/";

			if (args != null && args.length > 0) {
				url = args[0];
			}
			System.out.println("Connecting to "+url);

			LogFactory.configure(new SimpleLogFactory(new StreamLog(System.out, Level.INFO)));
			XmlRpcClient client = new XmlRpcClient(new URL(url));
			
			// We swap in a new unmarshaller (actually it's the same unmarshaller used
			// by default, we're just initializing it with a different FieldNameCodec)
			// since the XML-RPC validation suite uses a camelCase naming convention for
			// fields, which is what RoX expects on the Java side. So the codec we install
			// does a NOP (NO Operation) on the field name, returning unchanged.
			NopFieldNameCodec fieldNameCodec = new NopFieldNameCodec();
			client.setUnmarshaller(new DomMethodResponseUnmarshaller(fieldNameCodec));
			
			IValidationSuite server = (IValidationSuite) client.proxyObject(
					"validator1.", IValidationSuite.class);

			HashMap map = new HashMap();
			map.put("foo", "bar");
			
			System.out.println(server.countTheEntities("<><><>").ctLeftAngleBrackets);
			System.out.println(server.arrayOfStructsTest(new MoeLarryAndCurly[]{new MoeLarryAndCurly()}));
			System.out.println(server.echoStructTest(map));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
