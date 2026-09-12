package com.jtspringproject.JtSpringProject.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.jtspringproject.JtSpringProject.models.User;

@SpringBootTest
@ActiveProfiles("test")
class UserDaoTest {

	@Autowired
	private userDao userDao;

	private User newUser(String username) {
		User user = new User();
		user.setUsername(username);
		user.setEmail(username + "@example.com");
		user.setPassword("secret");
		user.setRole("ROLE_USER");
		user.setAddress("1 Test Street");
		return user;
	}

	private String uniqueName() {
		return "user-" + UUID.randomUUID();
	}

	@Test
	void saveUserAssignsIdentityId() {
		User saved = userDao.saveUser(newUser(uniqueName()));

		assertTrue(saved.getId() > 0);
		assertNotNull(userDao.getUserById(saved.getId()));
	}

	@Test
	void userExistsReflectsPersistedUsers() {
		String username = uniqueName();
		assertFalse(userDao.userExists(username));

		userDao.saveUser(newUser(username));

		assertTrue(userDao.userExists(username));
	}

	@Test
	void getUserByUsernameReturnsMatchOrNull() {
		String username = uniqueName();
		User saved = userDao.saveUser(newUser(username));

		User found = userDao.getUserByUsername(username);
		assertNotNull(found);
		assertEquals(saved.getId(), found.getId());
		assertEquals("ROLE_USER", found.getRole());

		assertNull(userDao.getUserByUsername("missing-" + UUID.randomUUID()));
	}

	@Test
	void getAllUserIncludesSavedUsers() {
		User saved = userDao.saveUser(newUser(uniqueName()));

		assertTrue(userDao.getAllUser().stream().anyMatch(u -> u.getId() == saved.getId()));
	}

	@Test
	void saveUserUpdatesExistingUser() {
		User saved = userDao.saveUser(newUser(uniqueName()));
		saved.setAddress("2 Updated Avenue");

		userDao.saveUser(saved);

		assertEquals("2 Updated Avenue", userDao.getUserById(saved.getId()).getAddress());
	}
}
