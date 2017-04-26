package com.group.data.model;

import java.util.List;

import org.springframework.data.annotation.Id;

public class User {
	@Id
	public int userId;

	public String userName;

	public String userEmail;

	public String userPassword;

	public String userAddress;
	
	public List<Order> orders;
	
	public WishList withList;

	public User() {
	}
}
