package com.group.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.group.data.model.Cart;
import com.group.data.model.Order;
import com.group.data.model.Product;
import com.group.data.model.User;
import com.group.data.model.AWS;
import com.group.data.repository.CartRepository;
import com.group.data.repository.OrderRepository;
import com.group.data.repository.ProductRepository;
import com.group.data.repository.UserRepository;
import com.group.data.repository.AWSRepository;

@RestController
@RequestMapping("db")
public class DBController {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private CartRepository cartRepositry;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AWSRepository awsRepository;

	/**
	 * Find a product by product name
	 * 
	 * @param name
	 * @return
	 */
	@RequestMapping(value = "/product/name/{name}", method = RequestMethod.GET, produces = "application/json")
	public Product getProduct(@PathVariable String name) {
		Product product = this.productRepository.findByName(name);
		return product;
	}

	@RequestMapping(value = "/product/id/{id}", method = RequestMethod.GET, produces = "application/json")
	public Product getProductById(@PathVariable String id) {
		Product product = this.productRepository.findById(id);
		return product;
	}


	/**
	 * Find products with a same category
	 *
	 * @param category
	 * @return
	 */
	@RequestMapping(value = "/product/category/{category}", method = RequestMethod.GET, produces = "application/json")
	public List<Product> getProductByCategory(@PathVariable String category) {
		List<Product> productList = this.productRepository.findByCategory(category);
		return productList;
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
	public int saveProduct(@RequestBody Product product) {
		Product result = this.getProduct(product.name);
		if (result == null) {
			this.productRepository.save(product);
		} else {
			result.category = product.category;
			result.description = product.description;
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

	/**
	 * save Or update Order
	 * 
	 * @param order
	 * @return
	 */
	@RequestMapping(value = "/order/save", method = RequestMethod.POST, consumes = "application/json")
	public int saveOrder(@RequestBody Order order) {
		Order o = this.orderRepository.getById(order.id);
		if (o != null) {
			order.id = o.id;
		}
		this.orderRepository.save(order);
		return 0;
	}

	/**
	 * Get Order by userId
	 * 
	 * @param userId
	 * @return
	 */
	@RequestMapping(value = "/order/{uerId}", method = RequestMethod.GET, produces = "application/json")
	public Order getOrder(@PathVariable String userId) {
		Order order = this.orderRepository.getByUserId(userId);
		return order;
	}

	/**
	 * Get all Order
	 * 
	 * @return
	 */
	@RequestMapping(value = "/order/all", method = RequestMethod.GET, produces = "application/json")
	public List<Order> getAllOrder() {
		List<Order> list = this.orderRepository.findAll();
		return list;
	}

	@RequestMapping(value = "/order/delete/{id}", method = RequestMethod.DELETE)
	public void deleteOrder(@PathVariable String id) {
		this.orderRepository.delete(id);
	}

	/**
	 * Get Cart by userId
	 * 
	 * @param userId
	 * @return
	 */
	@RequestMapping(value = "/cart/{uerId}", method = RequestMethod.GET, produces = "application/json")
	public Cart getCart(@PathVariable String userId) {
		Cart cart = this.cartRepositry.findByUserId(userId);
		return cart;
	}

	/**
	 * Save or update
	 * 
	 * @param cart
	 * @return
	 */
	@RequestMapping(value = "/cart/save", method = RequestMethod.POST, consumes = "application/json")
	public int saveCart(@RequestBody Cart cart) {
		Cart c = this.cartRepositry.findByUserId(cart.userId);
		if (c != null) {
			cart.id = c.id;
		}
		this.cartRepositry.save(cart);
		return 0;
	}

	/**
	 * Delete a cart by id
	 * 
	 * @param id
	 */
	@RequestMapping(value = "/cart/delete/{id}", method = RequestMethod.DELETE)
	public void deleteCart(@PathVariable String id) {
		this.cartRepositry.delete(id);
	}

	/**
	 * Get user by userId
	 * 
	 * @param userId
	 * @return
	 */
	@RequestMapping(value = "/user/{userId}", method = RequestMethod.GET, produces = "application/json")
	public User getUser(@PathVariable String userId) {
		User user = this.userRepository.findById(userId);
		return user;
	}

	/**
	 * Get user by email and type
	 * @param userEmail
	 * @param userType
	 * @return
	 */
	@RequestMapping(value = "/user/{userEmail}/{userType}", method = RequestMethod.GET, produces = "application/json")
	public User getUserByField(@PathVariable String userEmail, @PathVariable String userType) {
		User user = this.userRepository.findByEmailAndType(userEmail, userType);
		return user;
	}


	/**
	 * Save or Update userId
	 * 
	 * @param user
	 * @return
	 */
	@RequestMapping(value = "/user/save", method = RequestMethod.POST, consumes = "application/json")
	public String saveUser(@RequestBody User user) {
		User u = this.userRepository.findById(user.id);
		if (u != null) {
			user.id = u.id;
		}
		this.userRepository.save(user);
		return user.id;
	}

	/**
	 * Delete a user by id
	 * 
	 * @param id
	 */
	@RequestMapping(value = "/user/delete/{id}", method = RequestMethod.DELETE)
	public void deleteUser(@PathVariable String id) {
		this.userRepository.delete(id);
	}

	/**
	 * Find all aws info
	 * @return
	 */
	@RequestMapping(value="/aws", method = RequestMethod.GET, produces = "application/json")
	public List<AWS> getAWS() {
		List<AWS> allAWSConfig = this.awsRepository.findAll();
		return allAWSConfig;
	}

	/**
	 * Find all aws info
	 * @return
	 */
	@RequestMapping(value="/aws", method = RequestMethod.POST, consumes = "application/json")
	public String saveAWS(@RequestBody AWS aws) {
		AWS a = this.awsRepository.findById(aws.id);
		if (a != null) {
			aws.id = a.id;
		}
		this.awsRepository.save(aws);
		return aws.id;
	}
}
