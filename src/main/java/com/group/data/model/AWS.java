package com.group.data.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "aws")
public class AWS {

    @Id
    public String id;
    public String dns;
    public String arn;

    public AWS() {
    }
}

