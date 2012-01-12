package com.flat502.rox.marshal;

import java.sql.Timestamp;

public class DateStruct {
	public Timestamp dateMember;

	public DateStruct(Timestamp timestamp) {
		this.dateMember = timestamp;
	}
}
