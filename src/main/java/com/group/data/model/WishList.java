package com.group.data.model;

import java.util.List;

import org.springframework.data.annotation.Id;

public class WishList {
	@Id
	public int listId;

	public List<Product> products;

	public WishList() {
	}

}
