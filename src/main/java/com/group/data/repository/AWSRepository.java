package com.group.data.repository;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.group.data.model.AWS;

public interface AWSRepository extends MongoRepository<AWS, String> {
    public AWS findById(String id);
}