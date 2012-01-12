package com.flat502.rox.processing;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.regex.Pattern;

import javax.net.ssl.*;

import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;

public class SSLConfiguration {
	public static enum ClientAuth {
		NONE,
		REQUEST,
		REQUIRE
	};
	
	private static Log log = LogFactory.getLog(SSLConfiguration.class);
	
	/**
	 * A regular expression that matches only cipher suites that
	 * allow for anonymous key exchange.
	 */
	public static final String ANON_CIPHER_SUITES = "_DH_anon_(?i)";

	/**
	 * A regular expression that matches all cipher suites.
	 */
	public static final String ALL_CIPHER_SUITES = ".*";
	
	/**
	 * A regular expression that matches all protocols.
	 */
	public static final String ALL_PROTOCOLS = ".*";
	
	/**
	 * A regular expression that matches all TLS protocols.
	 */
	public static final String TLS_PROTOCOLS = "^TLS";
	
	// The pattern used to select cipher suites
	private Pattern cipherSuitePattern;

	private Pattern protocolPattern;
	
	// Default to 10 seconds
	private int handshakeTimeout = 10000;
	
	private KeyStore keyStore;
	private KeyStore trustStore;
	private String keyStorePassphrase;

	private SecureRandom rng;

	private ClientAuth clientAuth = ClientAuth.NONE;

	private PrivateKey explicitPrivateKey;
	private X509Certificate[] explicitCertChain;

	private String keystoreName;
	private String truststoreName;

	private SSLContext explicitContext;

	public SSLConfiguration() {
		this.setCipherSuitePattern(ALL_CIPHER_SUITES);
		this.setProtocolPattern(ALL_PROTOCOLS);
	}
	
	public SSLConfiguration(SSLContext context) {
		this();
		this.explicitContext = context;
	}
	
	public SSLConfiguration(Properties props) throws GeneralSecurityException, IOException {
		this();
		String ts = getProperty(props, "javax.net.ssl.trustStore", null);
		String tsp = getProperty(props, "javax.net.ssl.trustStorePassword", null);
		String tst = getProperty(props, "javax.net.ssl.trustStoreType", "JKS");
		if (ts != null && tsp != null && tst != null) {
			this.setTrustStore(ts, tsp, tst);
		}

		String ks = getProperty(props, "javax.net.ssl.keyStore", null);
		String ksp = getProperty(props, "javax.net.ssl.keyStorePassword", null);
		String kst = getProperty(props, "javax.net.ssl.keyStoreType", "JKS");
		if (ks != null && ksp != null && kst != null) {
			this.setKeyStore(ks, ksp, ksp, kst);
		}
	}
	
	public void setRandomNumberGenerator(SecureRandom rng) {
		this.rng = rng;
	}
	
	/**
	 * Configure a timeout value for SSL handshaking.
	 * <p>
	 * If the remote server is not SSL enabled then it falls
	 * to some sort of timeout to determine this, since a non-SSL server
	 * is waiting for a request from a client, which is in turn waiting
	 * for an SSL handshake to be initiated by the server.
	 * <p>
	 * This method controls the length of that timeout.
	 * <p>
	 * This timeout defaults to 10 seconds. 
	 * <p>
	 * The new timeout affects only connections initiated subsequent to the
	 * completion of this method call.
	 * @param timeout
	 * 	The timeout (in milliseconds). A value of 0 indicates no timeout should be
	 * 	enforced (not recommended).
	 * @throws IllegalArgumentException
	 * 	If the timeout provided is negative.
	 */
	public void setHandshakeTimeout(int timeout) {
		if (timeout < 0) {
			throw new IllegalArgumentException("timeout is negative");
		}

		this.handshakeTimeout = timeout;
	}
	
	public int getHandshakeTimeout() {
		return this.handshakeTimeout;
	}

	/**
	 * Set the regular expression used to select the SSL cipher
	 * suites to use during SSL handshaking.
	 * @param cipherSuitePattern
	 * 	A regular expression for selecting the 
	 * 	set of SSL cipher suites. A <code>null</code> value
	 * 	will treated as matching <i>all</i> cipher suites.
	 * @see #ALL_CIPHER_SUITES
	 * @see #ANON_CIPHER_SUITES
	 */
	public void setCipherSuitePattern(String cipherSuitePattern) {
		if (cipherSuitePattern == null) {
			cipherSuitePattern = ALL_CIPHER_SUITES;
		}

		synchronized (this) {
			this.cipherSuitePattern = Pattern.compile(cipherSuitePattern);
		}
	}

	/**
	 * Set the regular expression used to select the SSL protocol
	 * suites to use during SSL handshaking.
	 * @param protocolPattern
	 * 	A regular expression for selecting the 
	 * 	set of SSL protocols. A <code>null</code> value
	 * 	will treated as matching <i>all</i> protocols.
	 * @see #ALL_PROTOCOLS
	 * @see #TLS_PROTOCOLS
	 */
	public void setProtocolPattern(String protocolPattern) {
		if (protocolPattern == null) {
			protocolPattern = ALL_PROTOCOLS;
		}

		synchronized (this) {
			this.protocolPattern = Pattern.compile(protocolPattern);
		}
	}

	public void addTrustedEntities(Collection<X509Certificate> certs) throws GeneralSecurityException, IOException {
		for (X509Certificate certificate : certs) {
			this.addTrustedEntity(certificate);
		}
	}
	
	public void addTrustedEntity(X509Certificate cert) throws GeneralSecurityException, IOException {
		if (this.trustStore == null) {
			this.trustStore = this.initKeyStore();
		}
		String alias = cert.getSubjectDN().getName() + ":" + cert.getSerialNumber();
		this.trustStore.setCertificateEntry(alias, cert);
		this.setTrustStore(this.trustStore);
	}
	
	public void addIdentity(PrivateKey privateKey, X509Certificate[] chain) throws GeneralSecurityException, IOException {
		if (this.keyStore == null) {
			this.keyStore = this.initKeyStore();
		}
		String alias = privateKey.getAlgorithm() + ":" + privateKey.hashCode();
		this.keyStore.setKeyEntry(alias, privateKey, "".toCharArray(), chain);
		this.setKeyStore(this.keyStore, "");
		
		this.explicitPrivateKey = privateKey;
		this.explicitCertChain = chain;
	}
	
	public void setClientAuthentication(ClientAuth auth) {
		this.clientAuth  = auth;
	}
	
	public ClientAuth getClientAuthentication() {
		return this.clientAuth;
	}
	
	// Convenience method
	public void setKeyStore(String storeFile, String storePassphrase, String entryPassphrase, String storeType) throws GeneralSecurityException, IOException {
		KeyStore ks = KeyStore.getInstance(storeType);
		ks.load(new FileInputStream(storeFile), storePassphrase.toCharArray());
		this.setKeyStore(ks, entryPassphrase);

		this.keystoreName = storeFile;
	}
	
	// Keystore.load must have been called.
	public void setKeyStore(KeyStore ks, String passphrase) throws GeneralSecurityException {
		this.keyStore = ks;
		this.keyStorePassphrase = passphrase;
	}
	
	// Convenience method
	public void setTrustStore(String storeFile, String passphrase, String storeType) throws GeneralSecurityException, IOException {
		KeyStore ks = KeyStore.getInstance(storeType);
		ks.load(new FileInputStream(storeFile), passphrase.toCharArray());
		this.setTrustStore(ks);
		
		this.truststoreName = storeFile;
	}
	
	public void setTrustStore(KeyStore ts) throws GeneralSecurityException {
		this.trustStore = ts;
	}
	
	public SSLContext createContext() throws GeneralSecurityException {
		if (this.explicitContext != null) {
			return this.explicitContext;
		}
		
		SSLContext sslContext = SSLContext.getInstance("TLS");
		
		List<KeyManager> keyManagers = new ArrayList<KeyManager>();
		List<TrustManager> trustManagers = new ArrayList<TrustManager>();

		if (this.keyStore != null) {
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(this.keyStore, this.keyStorePassphrase.toCharArray());
			
			KeyManager[] km = kmf.getKeyManagers();
			for (int i = 0; i < km.length; i++) {
				keyManagers.add(km[i]);
			}
		}

		if (this.trustStore != null) {
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(this.trustStore);
	
			TrustManager[] tm = tmf.getTrustManagers();
			for (int i = 0; i < tm.length; i++) {
				trustManagers.add(tm[i]);
			}
		}

		KeyManager[] km = keyManagers.isEmpty() ? null : keyManagers.toArray(new KeyManager[0]);
		TrustManager[] tm = trustManagers.isEmpty() ? null : trustManagers.toArray(new TrustManager[0]);
		
		sslContext.init(km, tm, this.rng);
		return sslContext;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("  client auth=" + this.clientAuth);
		sb.append("\n  handshake timeout=" + this.handshakeTimeout + "ms");
		if (this.explicitPrivateKey != null) {
			sb.append("\n  explicit identity(key)=" + this.explicitPrivateKey);
		}
		if (this.explicitCertChain != null) {
			sb.append("\n  explicit identity(certs)=" + Arrays.toString(this.explicitCertChain));
		}
		if (this.keystoreName != null) {
			sb.append("\n  keystore=" + keystoreName);
		}
		if (this.truststoreName != null) {
			sb.append("\n  truststore=" + truststoreName);
		}
		return sb.toString();
	}

	protected String[] selectCiphersuites(String[] supportedCipherSuites) {
		synchronized (this) {
			if (log.logTrace()) {
				log.trace("Selecting cipher suites using pattern [" + this.cipherSuitePattern + "]");
			}
			List<String> ciphers = new ArrayList<String>(supportedCipherSuites.length);
			for (int i = 0; i < supportedCipherSuites.length; i++) {
				if (this.cipherSuitePattern.matcher(supportedCipherSuites[i]).find()) {
					if (log.logTrace()) {
						log.trace("Matched " + supportedCipherSuites[i]);
					}
					ciphers.add(supportedCipherSuites[i]);
				}
			}
			return ciphers.toArray(new String[0]);
		}
	}

	protected String[] selectProtocols(String[] supportedProtocols) {
		synchronized (this) {
			if (log.logTrace()) {
				log.trace("Selecting protocols using pattern [" + this.protocolPattern + "]");
			}
			List<String> protocols = new ArrayList<String>(supportedProtocols.length);
			for (int i = 0; i < supportedProtocols.length; i++) {
				if (this.protocolPattern.matcher(supportedProtocols[i]).find()) {
					if (log.logTrace()) {
						log.trace("Matched " + supportedProtocols[i]);
					}
					protocols.add(supportedProtocols[i]);
				}
			}
			return protocols.toArray(new String[0]);
		}
	}

	//setProtocolPattern etc
	//
	
	private KeyStore initKeyStore() throws GeneralSecurityException, IOException {
		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(null);
		return ks;
	}

	private String getProperty(Properties props, String name, String defVal) throws SSLException {
		String v = props.getProperty(name, defVal);
		if (v == null) {
			log.warn("No value for property " + name);
		}
		return v;
	}
}
