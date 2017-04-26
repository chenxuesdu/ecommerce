package com.group.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.group.data.model.Order;

public interface OrderRepository extends MongoRepository<Order, String> {
	public Order findByOrderId(int orderId);
}