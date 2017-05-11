package com.group.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.group.data.model.Order;

import java.util.Optional;

public interface OrderRepository extends MongoRepository<Order, String> {
	public Order findByUserId(String userId);
	public Optional<Order> findById(String id);
}
