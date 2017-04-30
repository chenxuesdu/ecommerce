package com.group.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.group.data.model.Admin;

public interface AdminRepository extends MongoRepository<Admin, String> {
	public Admin findByEmail(String email);
}
