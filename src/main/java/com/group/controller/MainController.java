package com.group.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {
	@RequestMapping("/")
	public String home() {
		return "Welcome to our ecommerce system!";
	}
}
