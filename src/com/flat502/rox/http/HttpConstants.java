package com.flat502.rox.http;

/**
 * This interface (and inner-interfaces) collect together 
 * various HTTP related constants.
 */
public interface HttpConstants {
	/**
	 * A subset of the methods defined in 
	 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec5.html#sec5.1.1">section 5.1.1</a>
	 * of RFC 2616.
	 */
	public static interface Methods {
		public static final String GET = "GET";
		public static final String POST = "POST";
		public static final String PUT = "PUT";
		public static final String DELETE = "DELETE";
	}
	
	/**
	 * A subset of the header fields as defined in 
	 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14">section 14</a>
	 * of RFC 2616.
	 */
	public static interface Headers {
		public static final String ACCEPT_ENCODING = "Accept-Encoding";
		public static final String ALLOW = "Allow";
		public static final String CONTENT_LENGTH = "Content-Length";
		public static final String CONTENT_TYPE = "Content-Type";
		public static final String CONTENT_ENCODING = "Content-Encoding";
		public static final String HOST = "Host";
		public static final String DATE = "Date";
		public static final String SERVER = "Server";
		public static final String CONNECTION = "Connection";
		public static final String USER_AGENT = "User-Agent";
	}
	
	/**
	 * Status codes as defined in
	 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html#sec6.1.1">section 6.1.1</a>
	 * of RFC 2616.
	 */
	public static interface StatusCodes {
		public static final int _100_CONTINUE = 100;
		public static final int _101_SWITCHING_PROTOCOLS = 101;

		public static final int _200_OK = 200;
		public static final int _201_CREATED = 201;
		public static final int _202_ACCEPTED = 202;
		public static final int _203_NON_AUTHORITATIVE_INFORMATION = 203;
		public static final int _204_NO_CONTENT = 204;
		public static final int _205_RESET_CONTENT = 205;
		public static final int _206_PARTIAL_CONTENT = 206;

		public static final int _300_MULTIPLE_CHOICES = 300;
		public static final int _301_MOVED_PERMANENTLY = 301;
		public static final int _302_FOUND = 302;
		public static final int _303_SEE_OTHER = 303;
		public static final int _304_NOT_MODIFIED = 304;
		public static final int _305_USE_PROXY = 305;
		public static final int _307_TEMPORARY_REDIRECT = 307;
		public static final int _400_BAD_REQUEST = 400;
		public static final int _401_UNAUTHORIZED = 401;
		public static final int _402_PAYMENT_REQUIRED = 402;
		public static final int _403_FORBIDDEN = 403;
		public static final int _404_NOT_FOUND = 404;
		public static final int _405_METHOD_NOT_ALLOWED = 405;
		public static final int _406_NOT_ACCEPTABLE = 406;
		public static final int _407_PROXY_AUTH_REQUIRED = 407;
		public static final int _408_REQUEST_TIMEOUT = 408;
		public static final int _409_CONFLICT = 409;
		public static final int _410_GONE = 410;
		public static final int _411_LENGTH_REQUIRED = 411;
		public static final int _412_PRECONDITION_FAILED = 412;
		public static final int _413_REQUEST_ENTITY_TOO_LARGE = 413;
		public static final int _414_REQUEST_URI_TOO_LARGE = 414;
		public static final int _415_UNSUPPORTED_MEDIA_TYPE = 415;
		public static final int _416_REQUEST_RANGE_NOT_SATISFIABLE = 416;
		public static final int _417_EXPECTATION_FAILED = 417;
		
		public static final int _500_INTERNAL_SERVER_ERROR = 500;
		public static final int _501_NOT_IMPLEMENTED = 501;
		public static final int _502_BAD_GATEWAY = 502;
		public static final int _503_SERVICE_UNAVAILABLE = 503;
		public static final int _504_GATEWAY_TIMEOUT = 504;
		public static final int _505_HTTP_VERSION_NOT_SUPPORTED = 505;
	}
	
	/**
	 * Content codings as defined in
	 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.5">section 3.5</a>
	 * of RFC 2616.
	 */
	public static interface ContentEncoding {
		public static final String IDENTITY = "identity";
		public static final String X_GZIP = "x-gzip";
		public static final String GZIP = "gzip";
		public static final String X_COMPRESS = "x-compress";
		public static final String COMPRESS = "compress";
		public static final String DEFLATE = "deflate";
	}
}