package com.group.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.group.data.model.Google;

public interface GoogleRepository extends MongoRepository<Google, String> {
	public Google getByEmail(String email);
}
