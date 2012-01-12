package com.flat502.rox.log;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A {@link com.flat502.rox.log.Log} implementation backed by an
 * a {@link java.io.OutputStream}.
 */
public class StreamLog extends AbstractLog {
	private static final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm:ss.SSS");
	
	private PrintWriter out;

	public StreamLog(OutputStream os, Level level) {
		super(level);
		this.out = new PrintWriter(os);
	}

	protected void traceImpl(String msg, Throwable e) {
		this.out.println(this.format(now()+" [trace]", msg));
		if (e != null) {
			e.printStackTrace(this.out);
		}
		this.out.flush();
	}

	protected void debugImpl(String msg, Throwable e) {
		this.out.println(this.format(now()+" [debug]", msg));
		if (e != null) {
			e.printStackTrace(this.out);
		}
		this.out.flush();
	}

	protected void infoImpl(String msg, Throwable e) {
		this.out.println(this.format(now()+" [info]", msg));
		if (e != null) {
			e.printStackTrace(this.out);
		}
		this.out.flush();
	}

	protected void warnImpl(String msg, Throwable e) {
		this.out.println(this.format(now()+" [warn]", msg));
		if (e != null) {
			e.printStackTrace(this.out);
		}
		this.out.flush();
	}

	protected void errorImpl(String msg, Throwable e) {
		this.out.println(this.format(now()+" [error]", msg));
		if (e != null) {
			e.printStackTrace(this.out);
		}
		this.out.flush();
	}
	
	private String now() {
		return sdf.format(new Date());
	}

	private String format(String prefix, String msg) {
		StringBuffer sb = new StringBuffer(prefix);
		sb.append(' ');
		while (sb.length() < 20) {
			sb.append(' ');
		}
		sb.append(msg);
		return sb.toString();
	}
}
