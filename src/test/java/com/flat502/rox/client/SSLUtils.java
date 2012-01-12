package com.flat502.rox.client;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Properties;

import sun.security.x509.CertAndKeyGen;
import sun.security.x509.X500Name;
import sun.security.x509.X509Cert;

import com.flat502.rox.utils.Utils;

public class SSLUtils {
	public static Identity loadIdentity(Properties props, String alias) throws Exception {
		String pwd = props.getProperty("javax.net.ssl.keyStorePassword");
		KeyStore ks = KeyStore.getInstance(props.getProperty("javax.net.ssl.keyStoreType"));
		ks.load(new FileInputStream(props.getProperty("javax.net.ssl.keyStore")), pwd.toCharArray());

		//		Enumeration<String> e = ks.aliases();
		//		while (e.hasMoreElements()) {
		//			String a = (String) e.nextElement();
		//			System.out.println(a);
		//		}

		PrivateKey key = (PrivateKey) ks.getKey(alias, pwd.toCharArray());
		//		System.out.println(key);
		Certificate[] chain = ks.getCertificateChain(alias);
		java.security.cert.X509Certificate[] x509chain = Arrays.asList(chain).toArray(
				new java.security.cert.X509Certificate[0]);
		//		System.out.println(x509chain);

		return new Identity(key, x509chain);
	}

	public static X509Certificate loadTrustedEntity(Properties props, String alias) throws Exception {
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(new FileInputStream(props.getProperty("javax.net.ssl.trustStore")), props.getProperty(
				"javax.net.ssl.trustStorePassword").toCharArray());

		return (X509Certificate) ks.getCertificate(alias);
	}

	public static class Identity {
		public PrivateKey privateKey;
		public X509Certificate[] chain;

		public Identity(PrivateKey kr, X509Certificate[] chain) throws Exception {
			this.privateKey = kr;
			this.chain = chain;
		}

		public String toString() {
			return "identity[key=" + privateKey + ", chain=" + Arrays.toString(chain) + "]";
		}
	}

	@SuppressWarnings("deprecation")
	public static Identity generateIdentity(String name) throws Exception {
		CertAndKeyGen cakg = new CertAndKeyGen("RSA", "MD5WithRSA");
		cakg.generate(1024);

		PublicKey publicKey = cakg.getPublicKey();
		//		System.out.println(publicKey);

		PrivateKey privateKey = cakg.getPrivateKey();
		//		System.out.println(privateKey);

		X500Name x500name = new X500Name(name, "", "", "", "", "");
		//		System.out.println(x500name);

		X509Cert cert = cakg.getSelfCert(x500name, 2000000);
		//		System.out.println("cert: " + cert);

		javax.security.cert.X509Certificate certificate = javax.security.cert.X509Certificate.getInstance(cert
				.getSignedCert());
		certificate.checkValidity();
		//		System.out.println("Issuer DN .......... " + certificate.getIssuerDN());
		//		System.out.println("Not after .......... " + certificate.getNotAfter());
		//		System.out.println("Not before ......... " + certificate.getNotBefore());
		//		System.out.println("Serial No. ......... " + certificate.getSerialNumber());
		//		System.out.println("Signature Alg. ..... " + certificate.getSigAlgName());
		//		System.out.println("Signature Alg. OID . " + certificate.getSigAlgOID());
		//		System.out.println("Subject DN ......... " + certificate.getSubjectDN());

		return new Identity(privateKey, new X509Certificate[] { Utils.convert(certificate) });
	}

	public static void main(String[] args) {
		System.setProperty("javax.net.ssl.trustStore",
				"D:/work/java/xmlrpc.sslengine/src/com/flat502/rox/test/amazon-combined.truststore");
		System.setProperty("javax.net.ssl.trustStorePassword", "ipmanager");

		System.setProperty("javax.net.ssl.keyStore",
				"D:/work/java/xmlrpc.sslengine/src/com/flat502/rox/test/keystore.p12");
		System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
		System.setProperty("javax.net.ssl.keyStoreType", "pkcs12");

		try {
			//			generateIdentity("fred");
			Identity ipman = loadIdentity(System.getProperties(), "1");
			System.out.println(ipman.chain[0].getSubjectDN());
			X509Certificate ec2ra = loadTrustedEntity(System.getProperties(), "ipmanager"); // .2 through .5
			System.out.println(ec2ra.getSubjectDN());
			X509Certificate ec2ca = loadTrustedEntity(System.getProperties(), "ipmanager.2"); // .2 through .5
			System.out.println(ec2ca.getSubjectDN());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
