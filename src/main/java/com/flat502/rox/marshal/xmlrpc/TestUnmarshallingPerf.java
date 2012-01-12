package com.flat502.rox.marshal.xmlrpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import com.flat502.rox.marshal.RpcResponse;


public class TestUnmarshallingPerf {
	@SuppressWarnings("unchecked")
	public static String initXML() throws Exception {
		List list = new ArrayList();
		for(int ami = 0; ami < 100; ami++) {
			Properties props = new Properties();
			for(int i = 0; i < 30; i++) {
				int len = (int)(Math.random()*250) + 50;
				if (i == 0) {
					// x509 cert
					len = 2000;
				}
				StringBuffer sb = new StringBuffer();
				for(int j = 0; j < len; j++) {
					sb.append(j % 10);
				}
				props.setProperty("property-name-"+i, sb.toString());
			}
			
			Map struct = new HashMap();
			struct.put("ami-id", "ami-1234"+ami);
			struct.put("second", "something");
			struct.put("third", "something-else");
			struct.put("properties", props);
			
			list.add(struct);
		}
		XmlRpcMethodResponse rsp = new XmlRpcMethodResponse(list);
		return new String(rsp.marshal(), "UTF-8");
	}
	
	public static RpcResponse domUnmarshal(String xml) {
		try {
			return new DomMethodResponseUnmarshaller().unmarshal(xml);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static RpcResponse saxUnmarshal(String xml) {
		try {
			return new SaxMethodResponseUnmarshaller().unmarshal(xml);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private static class Exec {
		public Runnable init;
		public Runnable oper;

		public Exec(Runnable init, Runnable oper) {
			this.init = init;
			this.oper = oper;
		}
	}
	
	public static final int WARMUP = 5;
	public static final int ITERS = 20;
	public static final int LOOPS = 5;
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		final String xml = initXML();
		System.out.println(xml.length());

		Map<String, Exec> map = new LinkedHashMap<String, Exec>();
//		map.put("SAX(XP)     ",
//				new Exec(
//						new Runnable() {public void run() { System.getProperties().setProperty("javax.xml.parsers.SAXParserFactory", "com.flat502.rox.test.XPSaxParserFactory"); SaxParserPool.reset(); }},
//						new Runnable() {public void run() { saxUnmarshal(xml); }}
//						)
//						);
//		map.put("SAX(Orig)   ",
//				new Exec(
//						new Runnable() {public void run() { System.getProperties().remove("javax.xml.parsers.SAXParserFactory"); SaxParserPool.reset(); }},
//						new Runnable() {public void run() { saxUnmarshal(xml); }}
//						)
//						);
		map.put("SAX(JDK1.5) ",
				new Exec(
						new Runnable() {public void run() { System.getProperties().setProperty("javax.xml.parsers.SAXParserFactory", "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl"); SaxParserPool.reset(); }},
						new Runnable() {public void run() { saxUnmarshal(xml); }}
						)
						);
//		map.put("SAX(Xerces) ",
//				new Exec(
//						new Runnable() {public void run() { System.getProperties().setProperty("javax.xml.parsers.SAXParserFactory", "org.apache.xerces.jaxp.SAXParserFactoryImpl"); SaxParserPool.reset(); }},
//						new Runnable() {public void run() { saxUnmarshal(xml); }}
//						)
//						);
//		map.put("SAX(Piccolo)",
//				new Exec(
//						new Runnable() {public void run() { System.getProperties().setProperty("javax.xml.parsers.SAXParserFactory", "com.bluecast.xml.JAXPSAXParserFactory"); SaxParserPool.reset(); }},
//						new Runnable() {public void run() { saxUnmarshal(xml); }}
//						)
//						);
//		map.put("DOM", new Runnable() {public void run() { domUnmarshal(xml); }});
		Set<Entry<String, Exec>> entries = map.entrySet();
		
		for(int loop = 0; loop < LOOPS; loop++) {
			System.out.println("Loop " + (loop + 1));
			for (Map.Entry<String, Exec> entry : entries) {
				entry.getValue().init.run();
				Runnable oper = entry.getValue().oper;
				for(int iters = 0; iters < WARMUP; iters++) {
					oper.run();
				}
				long now = System.currentTimeMillis();
				for(int iters = 0; iters < ITERS; iters++) {
					oper.run();
				}
				long duration = System.currentTimeMillis()-now;
				
				System.out.println("  "+entry.getKey() + " : " + ((double) duration / ITERS) + "ms");
			}
		}

//		System.out.println(domUnmarshal(xml).getReturnValue().getClass());
//		System.out.println(saxUnmarshal(xml, true).getReturnValue().getClass());
//		System.out.println(saxUnmarshal(xml, false).getReturnValue().getClass());
	}
}
