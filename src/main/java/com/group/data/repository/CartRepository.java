package com.group.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.group.data.model.Cart;

import java.util.Optional;

public interface CartRepository extends MongoRepository<Cart, String> {
	public Cart findByUserId(String userId);
	public Optional<Cart> findById(String id);
}
