package com.group.data.model;

import java.util.Date;

import org.springframework.data.annotation.Id;

public class Order {

	@Id
	public int orderId;

	public Date orderDate;
	
	public int billId;
	
	public boolean shipped;
	
	public boolean delivered;
	
	

	public Order() {
	}
}
