package com.group.data.model;

import java.util.Date;

import org.springframework.data.annotation.Id;

public class Bill {

	@Id
	public int billId;

	public Date billDate;

	public Bill() {
	}

}
