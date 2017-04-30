package com.group.data.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "admin")
public class Admin {

	@Id
	public String id;
	public String email;
	public String password;

	public Admin() {
	}
}
