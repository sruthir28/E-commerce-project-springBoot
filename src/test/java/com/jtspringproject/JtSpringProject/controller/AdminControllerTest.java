package com.jtspringproject.JtSpringProject.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jtspringproject.JtSpringProject.models.Category;
import com.jtspringproject.JtSpringProject.models.Product;
import com.jtspringproject.JtSpringProject.models.User;
import com.jtspringproject.JtSpringProject.services.categoryService;
import com.jtspringproject.JtSpringProject.services.productService;
import com.jtspringproject.JtSpringProject.services.userService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private categoryService categoryService;

	@Autowired
	private productService productService;

	@Autowired
	private userService userService;

	private Product persistProduct(String name) {
		Category category = categoryService.addCategory("admin-cat-" + name);
		Product product = new Product();
		product.setName(name);
		product.setCategory(category);
		product.setImage("img.png");
		product.setPrice(10);
		product.setWeight(1);
		product.setQuantity(1);
		product.setDescription("desc");
		return productService.addProduct(product);
	}

	private User persistAdmin() {
		User user = new User();
		user.setUsername("admin-" + UUID.randomUUID());
		user.setEmail("admin@example.com");
		user.setPassword("adminpw");
		user.setRole("ROLE_ADMIN");
		user.setAddress("HQ");
		return userService.addUser(user);
	}

	@Test
	@WithMockUser(username = "boss", roles = "ADMIN")
	void indexExposesUsername() throws Exception {
		mockMvc.perform(get("/admin/index"))
				.andExpect(status().isOk())
				.andExpect(view().name("index"))
				.andExpect(model().attribute("username", "boss"));
	}

	@Test
	@WithMockUser(username = "boss", roles = "ADMIN")
	void dashboardExposesAdminName() throws Exception {
		mockMvc.perform(get("/admin/Dashboard"))
				.andExpect(status().isOk())
				.andExpect(view().name("adminHome"))
				.andExpect(model().attribute("admin", "boss"));

		mockMvc.perform(get("/admin/"))
				.andExpect(status().isOk())
				.andExpect(view().name("adminHome"));
	}

	@Test
	void loginPageShowsErrorMessageOnlyWhenRequested() throws Exception {
		mockMvc.perform(get("/admin/login"))
				.andExpect(status().isOk())
				.andExpect(model().attributeDoesNotExist("msg"));

		mockMvc.perform(get("/admin/login").param("error", "true"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("msg", "Invalid username or password. Please try again."));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void categoryLifecycleThroughController() throws Exception {
		String name = "cat-" + UUID.randomUUID();

		mockMvc.perform(post("/admin/categories").with(csrf()).param("categoryname", name))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("categories"));

		Category created = categoryService.getCategories().stream()
				.filter(c -> name.equals(c.getName())).findFirst().orElseThrow();

		mockMvc.perform(get("/admin/categories"))
				.andExpect(status().isOk())
				.andExpect(view().name("categories"))
				.andExpect(model().attributeExists("categories"));

		mockMvc.perform(post("/admin/categories/update").with(csrf())
				.param("categoryid", String.valueOf(created.getId()))
				.param("categoryname", name + "-renamed"))
				.andExpect(redirectedUrl("/admin/categories"));
		assertEquals(name + "-renamed", categoryService.getCategory(created.getId()).getName());

		mockMvc.perform(post("/admin/categories/delete").with(csrf())
				.param("id", String.valueOf(created.getId())))
				.andExpect(redirectedUrl("/admin/categories"));
		assertNull(categoryService.getCategory(created.getId()));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void productsPageListsProducts() throws Exception {
		Product product = persistProduct("listed");

		MvcResult result = mockMvc.perform(get("/admin/products"))
				.andExpect(status().isOk())
				.andExpect(view().name("products"))
				.andExpect(model().attributeExists("products"))
				.andReturn();

		@SuppressWarnings("unchecked")
		List<Product> products = (List<Product>) result.getModelAndView().getModel().get("products");
		assertTrue(products.stream().anyMatch(p -> p.getId() == product.getId()));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void addProductPageAndSubmission() throws Exception {
		Category category = categoryService.addCategory("gadgets");

		mockMvc.perform(get("/admin/products/add"))
				.andExpect(status().isOk())
				.andExpect(view().name("productsAdd"))
				.andExpect(model().attributeExists("categories"));

		String name = "product-" + UUID.randomUUID();
		mockMvc.perform(post("/admin/products/add").with(csrf())
				.param("name", name)
				.param("categoryid", String.valueOf(category.getId()))
				.param("price", "99")
				.param("weight", "3")
				.param("quantity", "7")
				.param("description", "shiny")
				.param("productImage", "img.png"))
				.andExpect(redirectedUrl("/admin/products"));

		Product created = productService.getProducts().stream()
				.filter(p -> name.equals(p.getName())).findFirst().orElseThrow();
		assertEquals(99, created.getPrice());
		assertEquals(7, created.getQuantity());
		assertEquals(category.getId(), created.getCategory().getId());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void updateProductPageAndSubmission() throws Exception {
		Product product = persistProduct("updatable");

		mockMvc.perform(get("/admin/products/update/" + product.getId()))
				.andExpect(status().isOk())
				.andExpect(view().name("productsUpdate"))
				.andExpect(model().attributeExists("categories"))
				.andExpect(model().attributeExists("product"));

		mockMvc.perform(post("/admin/products/update/" + product.getId()).with(csrf())
				.param("name", "updated-name")
				.param("categoryid", String.valueOf(product.getCategory().getId()))
				.param("price", "555")
				.param("weight", "9")
				.param("quantity", "2")
				.param("description", "changed")
				.param("productImage", "new.png"))
				.andExpect(redirectedUrl("/admin/products"));

		Product updated = productService.getProduct(product.getId());
		assertEquals("updated-name", updated.getName());
		assertEquals(555, updated.getPrice());
		assertEquals("new.png", updated.getImage());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void deleteProductRemovesIt() throws Exception {
		Product product = persistProduct("deletable");

		mockMvc.perform(post("/admin/products/delete").with(csrf())
				.param("id", String.valueOf(product.getId())))
				.andExpect(redirectedUrl("/admin/products"));

		assertNull(productService.getProduct(product.getId()));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void postToProductsRedirectsToCategories() throws Exception {
		mockMvc.perform(post("/admin/products").with(csrf()))
				.andExpect(redirectedUrl("/admin/categories"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void customersPageListsUsers() throws Exception {
		User admin = persistAdmin();

		MvcResult result = mockMvc.perform(get("/admin/customers"))
				.andExpect(status().isOk())
				.andExpect(view().name("displayCustomers"))
				.andReturn();

		@SuppressWarnings("unchecked")
		List<User> customers = (List<User>) result.getModelAndView().getModel().get("customers");
		assertTrue(customers.stream().anyMatch(u -> u.getId() == admin.getId()));
	}

	@Test
	void profileDisplayShowsExistingUser() throws Exception {
		User admin = persistAdmin();

		mockMvc.perform(get("/admin/profileDisplay").with(user(admin.getUsername()).roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(view().name("updateProfile"))
				.andExpect(model().attribute("userid", admin.getId()))
				.andExpect(model().attribute("username", admin.getUsername()))
				.andExpect(model().attribute("email", "admin@example.com"))
				.andExpect(model().attribute("password", ""))
				.andExpect(model().attribute("address", "HQ"));
	}

	@Test
	@WithMockUser(username = "ghost-admin", roles = "ADMIN")
	void profileDisplayReportsUnknownUser() throws Exception {
		mockMvc.perform(get("/admin/profileDisplay"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("msg", "User not found"))
				.andExpect(model().attributeDoesNotExist("userid"));
	}

	@Test
	void updateUserPersistsChangesAndRedirects() throws Exception {
		User admin = persistAdmin();
		String newName = admin.getUsername() + "-new";

		mockMvc.perform(post("/admin/updateuser").with(csrf()).with(user(admin.getUsername()).roles("ADMIN"))
				.param("userid", String.valueOf(admin.getId()))
				.param("username", newName)
				.param("email", "changed@example.com")
				.param("password", "")
				.param("address", "Elsewhere"))
				.andExpect(redirectedUrl("index"));

		User reloaded = userService.getUserById(admin.getId());
		assertEquals(newName, reloaded.getUsername());
		assertEquals("changed@example.com", reloaded.getEmail());
		assertEquals("Elsewhere", reloaded.getAddress());
		assertNotNull(reloaded.getPassword());
		assertFalse(reloaded.getPassword().isEmpty());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void updateUserForUnknownIdStillRedirects() throws Exception {
		mockMvc.perform(post("/admin/updateuser").with(csrf())
				.param("userid", String.valueOf(Integer.MAX_VALUE))
				.param("username", "nobody")
				.param("email", "n@example.com")
				.param("password", "")
				.param("address", "nowhere"))
				.andExpect(redirectedUrl("index"));
	}

	@Test
	@WithMockUser(roles = "USER")
	void accessDeniedPageRenders() throws Exception {
		mockMvc.perform(get("/403"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/views/403.jsp"));
	}
}
