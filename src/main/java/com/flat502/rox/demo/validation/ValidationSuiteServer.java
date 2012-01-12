package com.flat502.rox.demo.validation;

import java.io.FileOutputStream;
import java.net.InetAddress;
import java.util.Date;
import java.util.Map;

import com.flat502.rox.log.Level;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.log.SimpleLogFactory;
import com.flat502.rox.log.StreamLog;
import com.flat502.rox.marshal.NopFieldNameCodec;
import com.flat502.rox.server.XmlRpcProxyingRequestHandler;
import com.flat502.rox.server.XmlRpcServer;

/**
 * An implementation of the suite of methods defined by
 * the <a href="http://www.xmlrpc.com/validator1Docs">XML-RPC validation suite</a>.
 */
public class ValidationSuiteServer implements IValidationSuite {
	public int arrayOfStructsTest(MoeLarryAndCurly[] list) {
		int total = 0;
		for (int i = 0; i < list.length; i++) {
			total += list[i].curly;
		}
		return total;
	}

	public EntityInfo countTheEntities(String str) {
		EntityInfo result = new EntityInfo();
		for (int i = 0; i < str.length(); i++) {
			switch (str.charAt(i)) {
			case '<':
				result.ctLeftAngleBrackets++;
				break;
			case '>':
				result.ctRightAngleBrackets++;
				break;
			case '&':
				result.ctAmpersands++;
				break;
			case '\'':
				result.ctApostrophes++;
				break;
			case '"':
				result.ctQuotes++;
				break;
			}
		}
		return result;
	}

	public int easyStructTest(MoeLarryAndCurly struct) {
		return struct.curly + struct.larry + struct.moe;
	}

	public Map echoStructTest(Map struct) {
		return struct;
	}

	public Object[] manyTypesTest(Integer n, Boolean b, String s, Double d,
			Date dt, byte[] b64) {
		return new Object[] { n, b, s, d, dt, b64 };
	}

	public String moderateSizeArrayCheck(String[] list) {
		return list[0] + list[list.length - 1];
	}

	public int nestedStructTest(Map calendar) {
		Map months = (Map) calendar.get("2000");
		Map days = (Map) months.get("04");
		Map buriedStruct = (Map) days.get("01");
		Integer moe = (Integer) buriedStruct.get("moe");
		Integer larry = (Integer) buriedStruct.get("larry");
		Integer curly = (Integer) buriedStruct.get("curly");
		return moe.intValue() + larry.intValue() + curly.intValue();
	}

	public MultipliedStruct simpleStructReturnTest(int n) {
		return new MultipliedStruct(n);
	}

	/**
	 * Start an instance of this demo server.
	 * @param args
	 * 	A list of parameters indicating
	 * 	the <code>host/address</code> and
	 * 	<code>port</code> to bind to. These default to 
	 * 	<code>localhost</code> and <code>8080</code> if
	 * 	not specified.
	 */
	public static void main(String[] args) {
		try {
			String host = "localhost";
			int port = 8080;

			if (args != null && args.length > 0) {
				host = args[0];
				if (args.length > 1) {
					port = Integer.parseInt(args[1]);
				}
			}
			System.out.println("Starting server on " + host + ":" + port);

			LogFactory.configure(new SimpleLogFactory(new StreamLog(new FileOutputStream("validation.log"), Level.TRACE)));
			XmlRpcServer server = new XmlRpcServer(InetAddress.getByName(host), port);

			// By default RoX expects hyphenated struct member names. The XML-RPC
			// validation suite uses camelCase. This can be changed by passing
			// RoX an unmarshaller configured with a different FieldNameDecoder.
			// 
			// The encoding (marshalling) side is up to the request handler.
			// In this case we're using the proxying request handler provided by
			// Rox, which accepts a FieldNameEncoder as an optional constructor
			// parameter.
			//
			// The NopFieldNameCodec returns the field name unchanged.
			NopFieldNameCodec fieldNameCodec = new NopFieldNameCodec();
			
			// We don't use registerProxyingHandler() here because
			// we want to provide a custom FieldNameCodec.
			// registerProxyingHandler() is just a convenience method
			// consisting of the next few lines of code anyway.
			String namePattern = "^validator1\\.(.*)";
			XmlRpcProxyingRequestHandler proxy = new XmlRpcProxyingRequestHandler(
					namePattern, new ValidationSuiteServer(), fieldNameCodec);
			server.registerHandler(null, namePattern, proxy, proxy);
			server.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
