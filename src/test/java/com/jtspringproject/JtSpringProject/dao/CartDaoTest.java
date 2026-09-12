package com.jtspringproject.JtSpringProject.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.jtspringproject.JtSpringProject.models.Cart;
import com.jtspringproject.JtSpringProject.models.CartProduct;
import com.jtspringproject.JtSpringProject.models.Category;
import com.jtspringproject.JtSpringProject.models.Product;
import com.jtspringproject.JtSpringProject.models.User;

@SpringBootTest
@ActiveProfiles("test")
class CartDaoTest {

	@Autowired
	private cartDao cartDao;

	@Autowired
	private cartProductDao cartProductDao;

	@Autowired
	private userDao userDao;

	@Autowired
	private productDao productDao;

	@Autowired
	private categoryDao categoryDao;

	private User newUser() {
		User user = new User();
		user.setUsername("cart-user-" + UUID.randomUUID());
		user.setEmail("cart@example.com");
		user.setPassword("secret");
		user.setRole("ROLE_USER");
		user.setAddress("addr");
		return userDao.saveUser(user);
	}

	private Product newProduct(String name) {
		Category category = categoryDao.addCategory("cart-cat-" + name);
		Product product = new Product();
		product.setName(name);
		product.setImage("img.png");
		product.setCategory(category);
		product.setQuantity(1);
		product.setPrice(10);
		product.setWeight(1);
		product.setDescription("desc");
		return productDao.addProduct(product);
	}

	private Cart newCart(User customer) {
		Cart cart = new Cart();
		cart.setCustomer(customer);
		return cartDao.addCart(cart);
	}

	@Test
	void addCartPersistsCartForCustomer() {
		User customer = newUser();
		Cart cart = newCart(customer);

		assertTrue(cart.getId() > 0);
		assertTrue(cartDao.getCarts().stream()
				.anyMatch(c -> c.getId() == cart.getId() && c.getCustomer().getId() == customer.getId()));
	}

	@Test
	void updateCartReassignsCustomer() {
		Cart cart = newCart(newUser());
		User other = newUser();
		cart.setCustomer(other);

		cartDao.updateCart(cart);

		Cart loaded = cartDao.getCarts().stream().filter(c -> c.getId() == cart.getId()).findFirst().orElseThrow();
		assertEquals(other.getId(), loaded.getCustomer().getId());
	}

	@Test
	void deleteCartRemovesCart() {
		Cart cart = newCart(newUser());

		cartDao.deleteCart(cart);

		assertFalse(cartDao.getCarts().stream().anyMatch(c -> c.getId() == cart.getId()));
	}

	@Test
	void cartProductLinksCartAndProductThroughCompositeKey() {
		Cart cart = newCart(newUser());
		Product first = newProduct("first");
		Product second = newProduct("second");

		cartProductDao.addCartProduct(new CartProduct(cart, first));
		cartProductDao.addCartProduct(new CartProduct(cart, second));

		List<Product> products = cartProductDao.getProductByCartID(cart.getId());
		assertEquals(2, products.size());
		assertTrue(products.stream().anyMatch(p -> p.getId() == first.getId()));
		assertTrue(products.stream().anyMatch(p -> p.getId() == second.getId()));
		assertTrue(cartProductDao.getCartProducts().stream()
				.anyMatch(cp -> cp.getCart().getId() == cart.getId() && cp.getProduct().getId() == first.getId()));
	}

	@Test
	void getProductByCartIDReturnsEmptyListForEmptyCart() {
		Cart cart = newCart(newUser());

		assertTrue(cartProductDao.getProductByCartID(cart.getId()).isEmpty());
	}

	@Test
	void deleteCartProductRemovesLink() {
		Cart cart = newCart(newUser());
		Product product = newProduct("removable");
		CartProduct link = cartProductDao.addCartProduct(new CartProduct(cart, product));

		cartProductDao.deleteCartProduct(link);

		assertTrue(cartProductDao.getProductByCartID(cart.getId()).isEmpty());
	}
}
