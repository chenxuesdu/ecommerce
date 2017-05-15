package com.group.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
	public List<Product> getProduct(@PathVariable String name) {
		Product product = this.productRepository.findByName(name);
		List<Product> list = new ArrayList<>();
		list.add(product);
		return list;
	}

	@RequestMapping(value = "/product/id/{id}", method = RequestMethod.GET, produces = "application/json")
	public List<Product> getProductById(@PathVariable String id) {
		Product product = this.productRepository.findById(id).get();
		List<Product> list = new ArrayList<>();
		list.add(product);
		return list;
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
		List<Product> result = this.getProduct(product.name);
		System.out.println("************" + result.get(0));
		if (result == null || result.size() == 0 || result.get(0) == null) {
			this.productRepository.save(product);
		} else {
			// update
			result.get(0).category = product.category;
			result.get(0).description = product.description;
			result.get(0).inventory = product.inventory;
			result.get(0).price = product.price;
			this.productRepository.save(result.get(0));
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
		Product product = this.productRepository.findById(id).get();
		if (product != null) {
			this.productRepository.delete(product);
		}
	}

	/**
	 * save Or update Order
	 * 
	 * @param order
	 * @return
	 */
	@RequestMapping(value = "/order/save", method = RequestMethod.POST, consumes = "application/json")
	public int saveOrder(@RequestBody Order order) {
		Order o = this.orderRepository.findById(order.id).get();
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
	public List<Order> getOrder(@PathVariable String userId) {
		Order order = this.orderRepository.findByUserId(userId);
		List<Order> list = new ArrayList<>();
		list.add(order);
		return list;
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
		Order order = this.orderRepository.findById(id).get();
		if (order != null) {
			this.orderRepository.delete(order);
		}
	}

	/**
	 * Get Cart by userId
	 * 
	 * @param userId
	 * @return
	 */
	@RequestMapping(value = "/cart/{userId}", method = RequestMethod.GET, produces = "application/json")
	public List<Cart> getCart(@PathVariable String userId) {
		Cart cart = this.cartRepositry.findByUserId(userId);
		List<Cart> list = new ArrayList<>();
		list.add(cart);
		return list;
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
		Cart cart = this.cartRepositry.findByUserId(id);
		if (cart != null) {
			this.cartRepositry.delete(cart);
		}
	}

	/**
	 * Get user by userId
	 *
	 * @param userId
	 * @return
	 */
	@RequestMapping(value = "/user/{userId}", method = RequestMethod.GET, produces = "application/json")
	public List<User> getUser(@PathVariable String userId) {
		User user = this.userRepository.findById(userId).get();
		List<User> list = new ArrayList<>();
		list.add(user);
		return list;
		/**/
	}

	/**
	 * Get user by email and type
	 * @param userEmail
	 * @param userType
	 * @return
	 */
	@RequestMapping(value = "/user/{userEmail}/{userType}", method = RequestMethod.GET, produces = "application/json")
	public List<User> getUserByField(@PathVariable String userEmail, @PathVariable String userType) {
		User user = this.userRepository.findByEmailAndType(userEmail, userType);
		List<User> list = new ArrayList<>();
		list.add(user);
		return list;
	}

	@RequestMapping(value = "/user/all", method = RequestMethod.GET, produces = "application/json")
	public List<User> getUserByField() {
		List<User> list = this.userRepository.findAll();
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).type.equals("admin")) {
				User admin = list.get(i);
				list.remove(admin);
			}
		}
		return list;
	}


	/**
	 * Save or Update userId
	 * 
	 * @param user
	 * @return
	 */
	@RequestMapping(value = "/user/save", method = RequestMethod.POST, consumes = "application/json")
	public String saveUser(@RequestBody User user) {
		System.out.println("+++++++++" + user.id);
		if (user.id == null) {
			this.userRepository.save(user);
			return user.id;
		} else {
			User u = this.userRepository.findById(user.id).get();
			if (u != null) {
				user.id = u.id;
			}
			this.userRepository.save(user);
			return user.id;
		}

	}

	/**
	 * Delete a user by id
	 * 
	 * @param id
	 */
	@RequestMapping(value = "/user/delete/{id}", method = RequestMethod.DELETE)
	public void deleteUser(@PathVariable String id) {
		User user = this.userRepository.findById(id).get();
		if (user != null) {
			this.userRepository.delete(user);
		}
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
		AWS a = this.awsRepository.findById(aws.id).get();
		if (a != null) {
			aws.id = a.id;
		}
		this.awsRepository.save(aws);
		return aws.id;
	}
}
