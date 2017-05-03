package com.group.data.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "local")
public class Local {
	@Id
	public String id;
	public String email;
	public String name;

	public String profilePhoto;

	public Local() {
	}
}
