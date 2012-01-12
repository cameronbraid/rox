package com.flat502.rox.marshal.xmlrpc;

import java.io.*;
import java.util.*;
import java.net.URI;
import java.net.URL;

import org.xml.sax.*;
import javax.xml.parsers.*;

/**
 * Simple XML parser, for internal XML RPC usage. Doesn't support much.
 */
public class XmlRpcSaxParser extends SAXParser implements Parser {
    private static class EmptyAttributeList implements AttributeList {
	public int getLength () { return 0; }
	public String getName (int i) { return null; }
	public String getType (int i) { return null; }
	public String getValue (int i) { return null; }
	public String getType (String name) { return null; }
	public String getValue (String name) { return null; }
    }

    private static final int BUF_SIZE = 64*1024;

    private static final AttributeList EMPTY_ATTRS = new EmptyAttributeList();
    
    private static final char[] EMPTY_CHARS = new char[0];

    private static final String METHOD_CALL     = "methodCall";
    private static final String METHOD_RESPONSE = "methodResponse";

    private static final String[] XML_RPC_ELEMENTS = {
	METHOD_CALL,
	"methodCall",
	"methodName",
	"params",
	"param",
	"value",
	"int",
	"i4",
	"boolean",
	"double",
	"string",
	"dateTime.iso8601",
	"base64",
	"struct",
	"member",
	"name",
	"array",
	"data",
	METHOD_RESPONSE,
	"fault",
    };

    // ASCII-range name characters
    private static final boolean[] NAME_CHARS = new boolean[256];
    static {
	for( int c = '0'; c <= '9'; c++ ) { NAME_CHARS[c] = true; }
	for( int c = 'a'; c <= 'z'; c++ ) { NAME_CHARS[c] = true; }
	for( int c = 'A'; c <= 'Z'; c++ ) { NAME_CHARS[c] = true; }
	NAME_CHARS['_'] = true;
	NAME_CHARS['-'] = true;
	NAME_CHARS['.'] = true;
    }

    // CDATA
    private static final boolean[] CDATA_CHARS = new boolean[256];
    static {
	for( int i = 0; i < CDATA_CHARS.length; i++ ) { CDATA_CHARS[i] = true; }
	CDATA_CHARS['<'] = false;
	CDATA_CHARS['&'] = false;
    }

    private static final int STATE_CDATA            = 1;
    private static final int STATE_CDATA_CONTINUE   = 2;
    private static final int STATE_OPEN             = 3;
    private static final int STATE_PI               = 4;
    private static final int STATE_COMMENT          = 5;
    /*
    private static final int STATE_ELEMENT;
    private static final int STATE_ELEMENT_WS;
    private static final int STATE_ATTR_NAME_WS;
    private static final int STATE_ATTR_NAME_WS;
    private static final int STATE_ATTR_NAME_WS;
    private static final int STATE_ATTR_VAL_WS;
    */

    private EntityResolver entityResolver;
    private DTDHandler dtdHandler;
    private DocumentHandler documentHandler;
    private ErrorHandler errorHandler;
    private char[] buf = new char[BUF_SIZE];
    private int pos, limit;
    private boolean isReset;
    private int state;
    private boolean isComplete;

    public XmlRpcSaxParser() {
	reset();
    }

    private boolean readBuf( Reader reader ) throws IOException {
	int from = isReset ? limit : 0;
	isReset = false;
	int nRead = reader.read( this.buf, from, this.buf.length - from );
	if( nRead == -1 ) { return false; }
	this.limit = from + nRead;
	return true;
    }

    // set up the buffer to reparse partial elements 
    private void resetBuf( int start, int len ) throws SAXException {
	// check we don't have a nasty edge condition (buffer full with partial element)
	if( start == 0 && len == buf.length ) {
	    throw new SAXException( "Partially parsed item is too long starting with \"" + new String( buf, start, 20 ) + "...\"" );
	}
	if( start != 0 ) {
	    // copy the partially parsed item to the front of the buffer
	    System.arraycopy( buf, start, buf, 0, len );
	}
	this.pos = 0;
	this.limit = len;
	this.isReset = true;
    }

    // set up the buffer to reparse partially deref'ed but incomplete CDATA
    private void resetBuf( int cdataStart, int cdataLen, int start, int len ) throws SAXException {
	// check we don't have a nasty edge condition (buffer full with partial element)
	if( cdataStart == 0 && start+len == buf.length ) {
	    throw new SAXException( "Partially parsed CDATA is too long starting with \"" + new String( buf, start, 20 ) + "...\"" );
	}
	if( cdataStart != 0 ) {
	    // copy the deref'ed partial CDATA to the front of the buffer
	    System.arraycopy( buf, cdataStart, buf, 0, cdataLen );
	}
	if( len != 0 ) {
	    // copy the partially parsed item to the front of the buffer
	    System.arraycopy( buf, start, buf, cdataLen, len );
	}
	this.pos = cdataLen;
	this.limit = cdataLen+len;
	this.isReset = true;
    }

    /// Translate a reference into a raw character
    private char deref( int start, int len ) throws SAXException {
	char c1 = buf[start];

	if( c1 == 'a' ) {
	    return len == 4 ? '&' : '\'';
	}
	else if( c1 == 'l' ) {
	    return '<';
	}
	else if( c1 == 'g' ) {
	    return '>';
	}
	else if( c1 == 'q' ) {
	    return '"';
	}
	else if( c1 == '#' ) {
	    // numeric ref
	    try {
		if( buf[start+1] == 'x' ) {
		    return (char) Integer.parseInt( new String( buf, start+2, len-3 ), 16 );
		}
		else {
		    return (char) Integer.parseInt( new String( buf, start+1, len-2 ) );
		}
	    }
	    catch( NumberFormatException e ) { /*ignore*/ }
	}
	throw new SAXException( "Unrecognised character reference \"" + new String( buf, start-1, len+1 ) + "\"" );
    }

    private boolean equalChars( String element, int start, int len ) {
	char[] buf = this.buf;
	for( int i = 1; i < len; i++ ) {
	    if( buf[start+i] != element.charAt(i) ) { return false; }
	}
	return true;
    }

    /// Get an element - it must be one of the recognised ones
    private String getElement( int start, int len ) throws SAXException {
	char c0 = buf[start];	    
	for( int i = 0; i < XML_RPC_ELEMENTS.length; i++ ) {
	    String element = XML_RPC_ELEMENTS[i];
	    if( element.length() == len && c0 == element.charAt(0) && equalChars( element, start, len ) ) {
		return element;
	    }
	}
	throw new SAXException( "Unrecognised element <" + new String( buf, start, len ) + ">" );
    }

    /// Parse CDATA overwriting the buffer in-place with the compacted raw characters with references translated to raw characters.
    /// If the buffer ends in the middle of a reference, we 
    /// @return the end of the in-place dereferenced CDATA chars
    private void parseCdata() throws SAXException {
	int pos = this.pos, limit = this.limit, start = pos, copyPos = pos;
	// are we continuing a partial CDATA?
	if( state == STATE_CDATA_CONTINUE ) {
	    start = 0;
	    state = STATE_CDATA;
	}
	char[] buf = this.buf;
	int c = -1;
	while(true) {
	    while( pos < limit && CDATA_CHARS[(c = buf[pos])] ) {
		buf[copyPos++] = (char)c;
		pos++; 
	    }
	    this.pos = pos;

	    if( limit <= pos ) {
		// restart next time with the already-deref'ed chars ready to roll
		resetBuf( start, copyPos-start, 0, 0 );
		state = STATE_CDATA_CONTINUE;
		return;
	    }

	    if( c == '<' ) { break; }

	    // parse and interpret a reference - at this stage it must be a '&'
	    int startRef = pos+1;
	    pos++;
	    while( pos < limit && (c = buf[pos++]) != ';' ) { /*continue*/ }
	    // did we find the terminator?
	    if( c == ';' ) {
		buf[copyPos++] = deref( startRef, pos-startRef );
	    }
	    else {
		// ran out of buffer before end of ref - reparse it at the start of the next buffer chunk
		resetBuf( start, copyPos-start, startRef, pos-startRef );
		state = STATE_CDATA_CONTINUE;
		return;
	    }
	}
	// report the characters
	if( start < copyPos && documentHandler != null ) { documentHandler.characters( buf, start, copyPos-start ); }

	state = STATE_OPEN;

	this.pos = pos;
    }

    // Parse and ignore a PI
    private void parsePi() throws SAXException {
	int pos = this.pos, limit = this.limit;
	char[] buf = this.buf;
	int c = -1;

	while( pos < limit ) {
	    // look for PI terminator - TODO embedded strings!
	    while( pos < limit && (c = buf[pos]) != '?' ) { pos++; }

	    if( c == '?' ) {
		if( limit <= pos ) {
		    // we can't tell yet, so reparse the '?' at the start of the next chunk
		    resetBuf( pos, 1 );
		    return;
		}
		else {
		    pos++;
		    if( buf[pos++] == '>' ) {
			// we're done - expect some CDATA
			this.state = STATE_CDATA;
			this.pos = pos;
			return;
		    }
		}
	    }
	}
	this.pos = pos;
    }

    // Parse and ignore a comment
    private void parseComment() throws SAXException {
	int pos = this.pos, limit = this.limit;
	char[] buf = this.buf;
	int c = -1;

	while( pos < limit ) {
	    // look for PI terminator - TODO embedded strings?
	    while( pos < limit && (c = buf[pos]) != '-' ) { pos++; }

	    if( c == '-' ) {
		if( limit-1 <= pos ) {
		    // we can't tell yet, so reparse the '-' at the start of the next chunk
		    resetBuf( pos, limit-pos );
		    return;
		}
		else {
		    pos++;
		    if( buf[pos] == '-' && buf[pos+1] == '>' ) {
			pos += 2;
			// we're done - expect some CDATA
			this.state = STATE_CDATA;
			this.pos = pos;
			return;
		    }
		}
	    }
	}
	this.pos = pos;
    }

    private void parseFromOpen() throws SAXException {
	int pos = this.pos+1, limit = this.limit;
	char[] buf = this.buf;
	int c;

	if( limit <= pos ) {
	    // we can't tell yet, so reparse the '<' at the start of the next chunk
	    resetBuf( pos-1, limit-(pos-1) );
	    this.state = STATE_CDATA;
	    return;
	}

	// TODO CDATA sections
	c = buf[pos];
	if( NAME_CHARS[c] ) {
	    this.pos = pos;
	    parseStartElement();
	    return;
	}
	else if( c == '/' ) {
	    this.pos = pos+1;
	    parseEndElement();
	    return;
	}
	else if( c == '?' ) {
	    this.pos = pos+1;
	    this.state = STATE_PI;
	    return;
	}
	else if( c == '!' ) {
	    if( limit-2 <= pos ) {
		// we can't tell yet, so reparse the '<!' at the start of the next chunk
		resetBuf( pos-1, limit-(pos-1) );
		this.state = STATE_CDATA;
		return;
	    }
	    if( buf[pos+1] == '-' && buf[pos+2] == '-' ) {
		this.pos = pos+3;
		this.state = STATE_COMMENT;
		return;
	    }
	}
	throw new SAXException( "Unrecognised item starting with \"" + new String( buf, pos-1, Math.min( 5, limit-(pos-1) ) ) + "...\"" );
    }

    private void parseStartElement() throws SAXException {
	int pos = this.pos, limit = this.limit, start = pos;
	char[] buf = this.buf;
	int c = -1;

	while( pos < limit && NAME_CHARS[ (c = buf[pos]) ] ) { pos++; }

	if( limit <= pos ) {
	    // end of buffer in the middle of an element - reparse as part of the new chunk
	    resetBuf( start-1, limit-(start-1) );
	    this.state = STATE_CDATA;
	    return;
	}
	
	// record end of tag name and consume remaining whitespace within this tag
	int nameEnd = pos;
	while( pos < limit && (c = buf[pos]) == ' ' ) { pos++; }

	// TODO allow whitespace and attributes!
	if( c == '>' ) {
	    // report the element
	    if( documentHandler != null ) {
		String element = getElement( start, nameEnd-start );
		documentHandler.startElement( element, EMPTY_ATTRS );
	    }
	    this.pos = pos+1;
	    this.state = STATE_CDATA;
	}
	else if( c == '/' ) {
	    if( limit-1 <= pos ) {
		// we can't tell yet, so reparse the element at the start of the next chunk
		resetBuf( start-1, limit-(start-1) );
		this.state = STATE_CDATA;
		return;
	    }
	    if( buf[pos+1] == '>' ) {
		// report the element (along with an empty value)
		if( documentHandler != null ) {
		    String element = getElement( start, nameEnd-start );
		    documentHandler.startElement( element, EMPTY_ATTRS );
		    documentHandler.characters(EMPTY_CHARS, 0, 0);
		    documentHandler.endElement(element);
		}
		this.pos = pos+2;
		this.state = STATE_CDATA;
	    }
	    else {
		throw new SAXException( "Illegal character '" + (char)buf[pos] + "' after '/' in element starting \"" + new String( buf, start-1, pos-(start-1) ) + "\"" );
	    }
	}
	else {
	    throw new SAXException( "Illegal character '" + (char)buf[pos] + "' in element starting \"" + new String( buf, start-1, pos-(start-1) ) + "\"" );
	}
    }

    private void parseEndElement() throws SAXException {
	int pos = this.pos, limit = this.limit, start = pos;
	char[] buf = this.buf;
	int c = -1;

	while( pos < limit && NAME_CHARS[ (c = buf[pos]) ] ) { pos++; }

	if( limit <= pos ) {
	    // end of buffer in the middle of an close element - reparse as part of the new chunk
	    resetBuf( start-2, limit-(start-2) );
	    this.state = STATE_CDATA;
	    return;
	}

	// TODO allow whitespace and attributes!
	if( c == '>' ) {
	    String element = getElement( start, pos-start );
	    // take note if we have a plausibly complete document
	    if( element == METHOD_CALL || element == METHOD_RESPONSE ) {
		this.isComplete = true;
	    }
	    // report the element
	    if( documentHandler != null ) { documentHandler.endElement(element); }
	    this.pos = pos+1;
	    this.state = STATE_CDATA;
	}
	else {
	    throw new SAXException( "Illegal character '" + (char)buf[pos] + "' in close element starting \"" + new String( buf, start-2, pos-(start-2) ) + "...\"" );
	}
    }

    private void parse( Reader reader ) throws SAXException, IOException {

	// we're starting to parse
	if( documentHandler != null ) { documentHandler.startDocument(); }

	try {
	    try {
		this.isComplete = false;

		// read the next chunk of input
		this.pos = 0;
		this.state = STATE_CDATA;
		
		while( readBuf(reader) ) {
		    // parse the next chunk
		    while( !isReset && pos < limit ) {
			switch(state) {
			case STATE_CDATA:
			case STATE_CDATA_CONTINUE:
			    parseCdata();
			    break;
			case STATE_OPEN:
			    parseFromOpen();
			    break;
			case STATE_PI:
			    parsePi();
			    break;
			case STATE_COMMENT:
			    parseComment();
			    break;
			default:
			    throw new SAXException( "Invalid internal state " + state );
			}
		    }
		    if(!isReset) {
			resetBuf(0,0);
		    }
		}
		// STATE_CDATA_CONTINUE is OK cos we can ignore CDATA at the end of the document
		if( state != STATE_CDATA && state != STATE_CDATA_CONTINUE ) {
		    throw new SAXException( "Unexpected end of document." );
		}
		else if( !isComplete ) {
		    throw new SAXException( "Incomplete document - top-level end element not found" );
		}
	    }
	    catch( ArrayIndexOutOfBoundsException e ) {
		throw (SAXException) new SAXException( "Non-ASCII characters are not yet supported." ).initCause(e);
	    }
	}
	finally {
	    if( documentHandler != null ) { documentHandler.endDocument(); }
	}

    }

    ///////////////////////////////////////// SAXParser Impl /////////////////////////////////////////

    public void reset() {
	this.pos = this.limit = 0;
	this.isReset = false;
	this.state = STATE_CDATA;
    }

    public Parser getParser() throws SAXException { return this; }

    public XMLReader getXMLReader() throws SAXException {
	throw new SAXException("XMLReader not yet supported");
    }
    
    public boolean isNamespaceAware() { return false; }

    public boolean isValidating() { return false; }

    public void setProperty(String name, Object value) throws SAXNotRecognizedException, SAXNotSupportedException {
	throw new SAXNotSupportedException("setProperty is not supported");
    }

    public Object getProperty(String name) throws SAXNotRecognizedException, SAXNotSupportedException {
	throw new SAXNotSupportedException("getProperty is not supported");
    }

    ///////////////////////////////////////// Parser Impl ///////////////////////////////////////////

    public void setLocale (Locale locale) throws SAXException {
	throw new UnsupportedOperationException();
    }
    
    public void setEntityResolver (EntityResolver resolver) {
	this.entityResolver = resolver;
    }
    
    public void setDTDHandler (DTDHandler handler) {
	this.dtdHandler = handler;
    }
    
    
    public void setDocumentHandler (DocumentHandler handler) {
	this.documentHandler = handler;
    }
    
    public void setErrorHandler (ErrorHandler handler) {
	this.errorHandler = handler;
    }

    private String getEncodingOrUtf8( InputSource source ) {
	String encoding = source.getEncoding();
	return encoding == null ? "UTF-8" : encoding;
    }
    
    // all roads lead to this function
    public void parse (InputSource source) throws SAXException, IOException {
	Reader reader = source.getCharacterStream();
	if( reader == null ) {
	    InputStream is = source.getByteStream();
	    if( is != null ) {
		reader = new InputStreamReader( is, getEncodingOrUtf8(source) );
	    }
	    else {
		String uri = source.getSystemId();
		if( uri != null ) {
		    try {
			reader = new InputStreamReader( new URI(uri).toURL().openStream(), getEncodingOrUtf8(source) );
		    }
		    catch( java.net.URISyntaxException e ) {
			throw (SAXException) new SAXException( "Invalid URI \"" + uri + "\"" ).initCause(e);
		    }
		}
		else {
		    throw new SAXException( "No input source found." );
		}
	    }
	}
	parse(reader);
    }
    
    public void parse (String systemId) throws SAXException, IOException {
	parse( new InputSource(systemId) );
    }
    
}
