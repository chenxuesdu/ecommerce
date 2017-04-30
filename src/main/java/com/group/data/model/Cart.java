package com.group.data.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "cart")
public class Cart {
	@Id
	public String id;

	public String userId;

	public List<String> product;

	public Cart() {
	}

}
