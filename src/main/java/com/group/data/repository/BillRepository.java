package com.group.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.group.data.model.Bill;

public interface BillRepository extends MongoRepository<Bill, String> {
	public Bill findByBillId(int billId);
	}

