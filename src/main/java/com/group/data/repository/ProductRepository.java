package com.group.data.repository;

import com.group.data.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {
	public Product findByName(String name);
	public Optional<Product> findById(String id);
	public List<Product> findByCategory(String category);
}
