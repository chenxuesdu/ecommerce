package com.group.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.group.data.model.Cart;

public interface CartRepository extends MongoRepository<Cart, String> {
	public Cart findByUserId(String userId);
}
