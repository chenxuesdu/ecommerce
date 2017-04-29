package com.group.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.group.data.model.Product;
import com.group.data.repository.ProductRepository;

@RestController
@RequestMapping("db")
public class DBController {

	@Autowired
	private ProductRepository productRepository;

	@RequestMapping(value = "/product/{name}", method = RequestMethod.GET, produces = "application/json")
	public Product getProduct(@PathVariable String name) {
		Product product = productRepository.findByName(name);
		return product;
	}

	@RequestMapping(value = "/product/all", method = RequestMethod.GET, produces = "application/json")
	public List<Product> getAllProduct() {
		List<Product> list = productRepository.findAll();
		return list;
	}

	@RequestMapping(value = "/product/save", method = RequestMethod.POST, consumes = "application/json")
	public int save(@RequestBody Product product) {
		productRepository.save(product);
		return 0;
	}

}
