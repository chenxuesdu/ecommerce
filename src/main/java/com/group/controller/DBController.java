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

	/**
	 * Find a product by product name
	 * 
	 * @param name
	 * @return
	 */
	@RequestMapping(value = "/product/{name}", method = RequestMethod.GET, produces = "application/json")
	public Product getProduct(@PathVariable String name) {
		Product product = this.productRepository.findByName(name);
		return product;
	}

	/**
	 * List all products
	 * 
	 * @return
	 */
	@RequestMapping(value = "/product/all", method = RequestMethod.GET, produces = "application/json")
	public List<Product> getAllProduct() {
		List<Product> list = this.productRepository.findAll();
		return list;
	}

	/**
	 * save a product, if product name exists, update the product.
	 * 
	 * @param product
	 * @return
	 */
	@RequestMapping(value = "/product/save", method = RequestMethod.POST, consumes = "application/json")
	public int save(@RequestBody Product product) {
		Product result = this.getProduct(product.name);
		if (result == null) {
			this.productRepository.save(product);
		} else {
			result.inventory = product.inventory;
			result.price = product.price;
			this.productRepository.save(result);
		}
		return 0;
	}

	/**
	 * delete a product by id
	 * 
	 * @param id
	 */
	@RequestMapping(value = "/product/delete/{id}", method = RequestMethod.DELETE)
	public void delete(@PathVariable String id) {
		this.productRepository.delete(id);
	}

}
