package com.group.data.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "google")
public class Google {

	@Id
	public String id;

	public String token;

	public String email;

	public String name;

	public String profilePhoto;

	public Google() {
	}
}
