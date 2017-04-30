package com.group.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.group.data.model.ShippingAddress;

public interface ShippingAddressRepository extends MongoRepository<ShippingAddress, String> {

}
