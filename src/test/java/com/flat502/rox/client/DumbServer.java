package com.flat502.rox.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import com.flat502.rox.log.Log;
import com.flat502.rox.log.LogFactory;
import com.flat502.rox.utils.Utils;

// A server that will either never accept or never read
// from a connection.
public class DumbServer extends Thread {
	private static Log log = LogFactory.getLog(DumbServer.class);
	
	private int port;
	private boolean accept;
	private byte[] rsp;

	private ServerSocket serverSocket;
	private IOException exception;

	public DumbServer(int port, boolean accept) {
		this(port, accept, null);
	}
	
	public DumbServer(int port, boolean accept, byte[] rsp) {
		this.port = port;
		this.accept = accept;
		this.rsp = rsp;
		log.trace("DumbServer: init: accept="+accept+", rsp="+(rsp != null));
		start();
	}

	public void run() {
		try {
			this.serverSocket = new ServerSocket(this.port);
			log.trace("DumbServer: listening on "+serverSocket);
			
			if (this.accept) {
				Socket socket = this.serverSocket.accept();
				log.trace("DumbServer: accepted "+socket);
				
				if (this.rsp != null) {
					InputStream is = socket.getInputStream();
					byte[] req = new byte[32768];
					
					log.trace("DumbServer: reading ...");
					int nread = is.read(req);

					log.trace("DumbServer: recv "+nread+" byte(s):\n" + Utils.toHexDump(req, 0, nread));
					
					OutputStream os = socket.getOutputStream();
					os.write(this.rsp);
					os.flush();
					
					log.trace("DumbServer: wrote "+this.rsp.length+" byte(s):\n" + Utils.toHexDump(rsp));
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			this.exception = e;
		}
	}

	public void shutdown() throws IOException {
		if (this.exception != null) {
			throw this.exception;
		}
		while (this.serverSocket == null)
			Thread.yield();
		this.serverSocket.close();
	}
}
