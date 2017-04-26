package com.group.data.model;

import org.springframework.data.annotation.Id;

public class Product {
	@Id
	public int productId;

	public String puoductName;

	public String productDesc;

	public float productPrice;

	public Product() {
	}

}
