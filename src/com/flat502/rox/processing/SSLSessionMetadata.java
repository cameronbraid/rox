package com.flat502.rox.processing;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.TimerTask;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLException;

import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.utils.Utils;

class SSLSessionMetadata {
	private static Log log = LogFactory.getLog(SSLSessionMetadata.class);

	public final SSLEngine engine;
	public final ByteBuffer netBuffer;
	public final ByteBuffer appBuffer;
	private TimerTask handshakeTimerTask;

	private HttpRpcProcessor processor;
	private Socket socket;
	private boolean handshakeTimeout;

	public SSLSessionMetadata(HttpRpcProcessor processor, SSLEngine engine, Socket socket) {
		this.processor = processor;
		this.engine = engine;
		this.socket = socket;
		this.netBuffer = ByteBuffer.allocate(engine.getSession().getPacketBufferSize());
		this.appBuffer = ByteBuffer.allocate(engine.getSession().getApplicationBufferSize());
	}
	
	public TimerTask newHandshakeTimerTask() {
		return(this.handshakeTimerTask = new HandshakeTimerTask());
	}
	
	public void cancelHandshakeTimer() {
		if (this.handshakeTimerTask != null) {
			this.handshakeTimerTask.cancel();
		}
	}
	
	public boolean handshakeTimeout() {
		return this.handshakeTimeout;
	}
	
	private class HandshakeTimerTask extends TimerTask {
		public void run() {
			if (log.logDebug()) {
				log.debug(processor.getClass().getSimpleName() + ": SSL handshake timer expired on " + Utils.toString(socket));
			}
			
			handshakeTimeout = true;
			
			processor.handleTimeout(socket, new SSLException("Handshake timeout"));
//			try {
//				socket.close();
//			} catch (IOException e) {
//				log.trace("Handshake timeout: close() failed on " + Utils.toString(socket), e);
//			}
		}
	}
}
