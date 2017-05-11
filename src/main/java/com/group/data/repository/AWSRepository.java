package com.group.data.repository;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.group.data.model.AWS;

import java.util.Optional;

public interface AWSRepository extends MongoRepository<AWS, String> {
    public Optional<AWS> findById(String id);
}