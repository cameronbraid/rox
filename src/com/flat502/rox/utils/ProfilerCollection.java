package com.flat502.rox.utils;

import java.util.HashSet;
import java.util.Set;

public class ProfilerCollection implements Profiler {
	private Set<Profiler> profilers = null;

	public void addProfiler(Profiler p) {
		if (this.profilers == null) {
			this.profilers = new HashSet<Profiler>();
		}
		this.profilers.add(p);
	}

	public void begin(long id, String operation) {
		if (this.profilers == null) {
			return;
		}
		for (Profiler p : this.profilers) {
			p.begin(id, operation);
		}
	}

	public void end(long id, String operation) {
		if (this.profilers == null) {
			return;
		}
		for (Profiler p : this.profilers) {
			p.end(id, operation);
		}
	}

	public void count(long id, String operation) {
		if (this.profilers == null) {
			return;
		}
		for (Profiler p : this.profilers) {
			p.count(id, operation);
		}
	}
}
