package com.group.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.group.data.model.User;

public interface UserRepository extends MongoRepository<User, String> {
	public User findByUserId(String userId);
}
