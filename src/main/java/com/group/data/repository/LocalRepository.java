package com.group.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.group.data.model.Local;

public interface LocalRepository extends MongoRepository<Local, String> {
	public Local getByEmail(String email);
}
