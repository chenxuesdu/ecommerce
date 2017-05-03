package com.group.data.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "user")
public class User {
	@Id
	public int id;

	public String userId;

	// "admin", "local", "google", "facebook"
	public String type;

	// for Google or Facebook
	public String token;

	public String email;

	public String name;

	public String profilePhoto;

	// for local or admin
	public String password;

	public ShippingAddress shippingInfo;

	public User() {
	}
}
