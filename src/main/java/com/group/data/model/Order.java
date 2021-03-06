package com.group.data.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "order")
public class Order {

	@Id
	public String id;

	public String userId;

	public List<String> productList;

	public String timestamp;

	public ShippingAddress shippingAddress;

	public float totalPrice;

	public Order() {
	}
}
