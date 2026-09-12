package com.jtspringproject.JtSpringProject.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jtspringproject.JtSpringProject.models.Category;
import com.jtspringproject.JtSpringProject.models.Product;
import com.jtspringproject.JtSpringProject.models.User;
import com.jtspringproject.JtSpringProject.services.categoryService;
import com.jtspringproject.JtSpringProject.services.productService;
import com.jtspringproject.JtSpringProject.services.userService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private userService userService;

	@Autowired
	private productService productService;

	@Autowired
	private categoryService categoryService;

	private void persistProduct() {
		Category category = categoryService.addCategory("user-cat");
		Product product = new Product();
		product.setName("visible");
		product.setCategory(category);
		product.setImage("img.png");
		product.setPrice(1);
		product.setWeight(1);
		product.setQuantity(1);
		product.setDescription("desc");
		productService.addProduct(product);
	}

	private User persistUser() {
		User user = new User();
		user.setUsername("shopper-" + UUID.randomUUID());
		user.setEmail("shopper@example.com");
		user.setPassword("pw");
		user.setRole("ROLE_NORMAL");
		user.setAddress("Home");
		return userService.addUser(user);
	}

	@Test
	void loginPageShowsErrorMessageOnlyWhenRequested() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(model().attributeDoesNotExist("msg"));

		mockMvc.perform(get("/login").param("error", "true"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("msg", "Please enter correct email and password"));
	}

	@Test
	@WithMockUser(username = "shopper", roles = "USER")
	void indexPageListsProductsForUser() throws Exception {
		persistProduct();

		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(view().name("index"))
				.andExpect(model().attribute("username", "shopper"))
				.andExpect(model().attributeExists("products"));
	}

	@Test
	@WithMockUser(roles = "USER")
	void userProductsPageListsProducts() throws Exception {
		persistProduct();

		mockMvc.perform(get("/user/products"))
				.andExpect(status().isOk())
				.andExpect(view().name("uproduct"))
				.andExpect(model().attributeExists("products"));
	}

	@Test
	@WithMockUser(roles = "USER")
	void buyPageRenders() throws Exception {
		mockMvc.perform(get("/buy"))
				.andExpect(status().isOk())
				.andExpect(view().name("buy"));
	}

	@Test
	void registerNewUserEncodesPasswordAndAssignsRole() throws Exception {
		String username = "fresh-" + UUID.randomUUID();

		mockMvc.perform(post("/newuserregister").with(csrf())
				.param("username", username)
				.param("email", "fresh@example.com")
				.param("password", "plain")
				.param("address", "somewhere"))
				.andExpect(status().isOk())
				.andExpect(view().name("userLogin"));

		User created = userService.getUserByUsername(username);
		assertNotNull(created);
		assertEquals("ROLE_NORMAL", created.getRole());
		assertTrue(created.getPassword().startsWith("$2a$"));
	}

	@Test
	void registerExistingUsernameReturnsToRegisterWithMessage() throws Exception {
		User existing = persistUser();

		mockMvc.perform(post("/newuserregister").with(csrf())
				.param("username", existing.getUsername())
				.param("email", "dup@example.com")
				.param("password", "pw")
				.param("address", "dup"))
				.andExpect(status().isOk())
				.andExpect(view().name("register"))
				.andExpect(model().attribute("msg",
						existing.getUsername() + " is taken. Please choose a different username."));
	}

	@Test
	void profileDisplayShowsExistingUser() throws Exception {
		User user = persistUser();

		mockMvc.perform(get("/profileDisplay").with(user(user.getUsername()).roles("USER")))
				.andExpect(status().isOk())
				.andExpect(view().name("updateProfile"))
				.andExpect(model().attribute("userid", user.getId()))
				.andExpect(model().attribute("username", user.getUsername()))
				.andExpect(model().attribute("email", "shopper@example.com"))
				.andExpect(model().attribute("password", ""))
				.andExpect(model().attribute("address", "Home"));
	}

	@Test
	@WithMockUser(username = "ghost", roles = "USER")
	void profileDisplayReportsUnknownUser() throws Exception {
		mockMvc.perform(get("/profileDisplay"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("msg", "User not found"));
	}

	@Test
	void updateUserPersistsChangesAndRedirectsHome() throws Exception {
		User user = persistUser();
		String newName = user.getUsername() + "-new";

		mockMvc.perform(post("/updateuser").with(csrf()).with(user(user.getUsername()).roles("USER"))
				.param("userid", String.valueOf(user.getId()))
				.param("username", newName)
				.param("email", "moved@example.com")
				.param("password", "newpw")
				.param("address", "New Home"))
				.andExpect(redirectedUrl("/"));

		User reloaded = userService.getUserById(user.getId());
		assertEquals(newName, reloaded.getUsername());
		assertEquals("moved@example.com", reloaded.getEmail());
		assertEquals("New Home", reloaded.getAddress());
		assertTrue(reloaded.getPassword().startsWith("$2a$"));
	}

	@Test
	@WithMockUser(roles = "USER")
	void updateUserForUnknownIdStillRedirects() throws Exception {
		mockMvc.perform(post("/updateuser").with(csrf())
				.param("userid", String.valueOf(Integer.MAX_VALUE))
				.param("username", "nobody")
				.param("email", "n@example.com")
				.param("password", "")
				.param("address", "nowhere"))
				.andExpect(redirectedUrl("/"));
	}
}
