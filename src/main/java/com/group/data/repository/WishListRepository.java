package com.group.data.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.group.data.model.WishList;

public interface WishListRepository extends MongoRepository<WishList, String> {
	public WishList getByListId(int listId);
}
