package com.flat502.rox.utils;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class UTCSimpleDateFormat extends SimpleDateFormat {
	public UTCSimpleDateFormat(String pattern) {
		super(pattern);
		this.setTimeZone(TimeZone.getTimeZone("UTC"));
	}
}
