package com.group.data.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user")
public class User {
	@Id
	public int id;

	public String userId;
	public Admin admin;
	public Local local;
	public Google facebook;
	public Google google;
	public ShippingAddress shippingInfo;

	public User() {
	}
}
