package com.jtspringproject.JtSpringProject.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import com.jtspringproject.JtSpringProject.dao.userDao;
import com.jtspringproject.JtSpringProject.models.User;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceTest {

	@Autowired
	private userService userService;

	@Autowired
	private userDao userDao;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private UserDetailsService userDetailsService;

	private User newUser(String role) {
		User user = new User();
		user.setUsername("svc-" + UUID.randomUUID());
		user.setEmail("svc@example.com");
		user.setPassword("plain-pw");
		user.setRole(role);
		user.setAddress("addr");
		return user;
	}

	@Test
	void addUserEncodesPlainPasswordOnce() {
		User saved = userService.addUser(newUser("ROLE_NORMAL"));

		assertTrue(saved.getPassword().startsWith("$2a$"));
		assertTrue(passwordEncoder.matches("plain-pw", saved.getPassword()));

		String encoded = saved.getPassword();
		userService.addUser(saved);
		assertEquals(encoded, saved.getPassword());
	}

	@Test
	void addUserRejectsDuplicateUsername() {
		User first = userService.addUser(newUser("ROLE_NORMAL"));
		User duplicate = newUser("ROLE_NORMAL");
		duplicate.setUsername(first.getUsername());

		assertThrows(IllegalStateException.class, () -> userService.addUser(duplicate));
	}

	@Test
	void checkUserExistsAndGetUsers() {
		User saved = userService.addUser(newUser("ROLE_NORMAL"));

		assertTrue(userService.checkUserExists(saved.getUsername()));
		assertFalse(userService.checkUserExists("missing-" + UUID.randomUUID()));
		assertTrue(userService.getUsers().stream().anyMatch(u -> u.getId() == saved.getId()));
		assertEquals(saved.getUsername(), userService.getUserById(saved.getId()).getUsername());
	}

	@Test
	void getUserByUsernameMigratesLegacyPlainTextPassword() {
		User legacy = userDao.saveUser(newUser("ROLE_NORMAL"));
		assertEquals("plain-pw", legacy.getPassword());

		User loaded = userService.getUserByUsername(legacy.getUsername());

		assertTrue(loaded.getPassword().startsWith("$2a$"));
		assertTrue(passwordEncoder.matches("plain-pw", userDao.getUserById(legacy.getId()).getPassword()));
		assertNull(userService.getUserByUsername("missing-" + UUID.randomUUID()));
	}

	@Test
	void updateUserProfileKeepsPasswordWhenBlankAndEncodesWhenProvided() {
		User saved = userService.addUser(newUser("ROLE_NORMAL"));
		String originalHash = saved.getPassword();

		User unchangedPw = userService.updateUserProfile(saved.getId(), saved.getUsername() + "-x",
				"x@example.com", "   ", "new addr");
		assertEquals(originalHash, unchangedPw.getPassword());
		assertEquals("new addr", unchangedPw.getAddress());

		User changedPw = userService.updateUserProfile(saved.getId(), saved.getUsername() + "-x",
				"x@example.com", "another", "new addr");
		assertNotEquals(originalHash, changedPw.getPassword());
		assertTrue(passwordEncoder.matches("another", changedPw.getPassword()));

		User preEncoded = userService.updateUserProfile(saved.getId(), saved.getUsername() + "-x",
				"x@example.com", originalHash, "new addr");
		assertEquals(originalHash, preEncoded.getPassword());

		assertNull(userService.updateUserProfile(Integer.MAX_VALUE, "n", "n@example.com", "p", "a"));
	}

	@Test
	void userDetailsServiceMapsRolesAndRejectsUnknownUsers() {
		User admin = userService.addUser(newUser("ROLE_ADMIN"));
		User normal = userService.addUser(newUser("ROLE_NORMAL"));

		UserDetails adminDetails = userDetailsService.loadUserByUsername(admin.getUsername());
		assertNotNull(adminDetails);
		assertTrue(adminDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
		assertEquals(admin.getPassword(), adminDetails.getPassword());

		UserDetails normalDetails = userDetailsService.loadUserByUsername(normal.getUsername());
		assertTrue(normalDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));

		assertThrows(UsernameNotFoundException.class,
				() -> userDetailsService.loadUserByUsername("missing-" + UUID.randomUUID()));
	}
}
