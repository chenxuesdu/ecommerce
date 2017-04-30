package com.group.data.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "shippingAddress")
public class ShippingAddress {
	public ShippingAddress() {

	}

	@Id
	public String id;

	public String firstName;

	public String lastName;

	public String streetAddress;

	public String city;

	public String state;

	public String zip;

	public String phoneNumber;
}
