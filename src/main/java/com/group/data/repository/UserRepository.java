package com.group.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.group.data.model.User;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
	public Optional<User> findById(String id);
	public User findByEmailAndType(String email, String type);
}
