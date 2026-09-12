package com.jtspringproject.JtSpringProject.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = PasswordEncoderConfig.class)
@ActiveProfiles("test")
class PasswordEncoderConfigTest {

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void passwordEncoderIsBCrypt() {
		assertInstanceOf(BCryptPasswordEncoder.class, passwordEncoder);
	}

	@Test
	void encodedPasswordMatchesRawAndRejectsWrong() {
		String encoded = passwordEncoder.encode("secret");
		assertNotEquals("secret", encoded);
		assertTrue(encoded.startsWith("$2"));
		assertTrue(passwordEncoder.matches("secret", encoded));
		assertFalse(passwordEncoder.matches("wrong", encoded));
	}
}
