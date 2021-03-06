package com.group.data.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "product")
public class Product {
	@Id
	public String id;

	public String category;

	public String name;

	public String inventory;

	public float price;

	public String[] productPhotos;

	public String description;

	public Product() {
	}
}
