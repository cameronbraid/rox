/**
 * 
 */
package com.flat502.rox.marshal;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class Reservation {
	public String id;
	public Date start;
	public long duration;
	public boolean kill;

	public Map properties;

	public Reservation() {
	}
}