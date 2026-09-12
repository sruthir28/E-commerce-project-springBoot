package com.jtspringproject.JtSpringProject.configuration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.DispatcherType;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigurationTest {

	@Autowired
	private MockMvc mockMvc;

	// ---- public endpoints ----

	@Test
	void adminLoginPageIsPublic() throws Exception {
		mockMvc.perform(get("/admin/login"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/views/adminlogin.jsp"));
	}

	@Test
	void userLoginPageIsPublic() throws Exception {
		mockMvc.perform(get("/login"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/views/userLogin.jsp"));
	}

	@Test
	void registerPageIsPublic() throws Exception {
		mockMvc.perform(get("/register"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/views/register.jsp"));
	}

	@Test
	void newUserRegisterEndpointIsPublic() throws Exception {
		mockMvc.perform(post("/newuserregister").with(csrf())
				.param("username", "newuser")
				.param("email", "new@example.com")
				.param("password", "pw")
				.param("address", "somewhere"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/views/userLogin.jsp"));
	}

	@Test
	void forwardedJspDispatchIsNotBlockedForAnonymous() throws Exception {
		mockMvc.perform(get("/views/userLogin.jsp")
				.with(request -> {
					request.setDispatcherType(DispatcherType.FORWARD);
					return request;
				}))
				.andExpect(status().isOk());
	}

	@Test
	void directJspRequestStillRequiresLogin() throws Exception {
		mockMvc.perform(get("/views/userLogin.jsp"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("http://localhost/login"));
	}

	// ---- admin chain ----

	@Test
	void adminHomeRedirectsAnonymousToAdminLogin() throws Exception {
		mockMvc.perform(get("/admin/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("http://localhost/admin/login"));
	}

	@Test
	void adminCategoriesRedirectsAnonymousToAdminLogin() throws Exception {
		mockMvc.perform(get("/admin/categories"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("http://localhost/admin/login"));
	}

	@Test
	@WithMockUser(roles = "USER")
	void adminRoutesForbiddenForUserRole() throws Exception {
		mockMvc.perform(get("/admin/categories"))
				.andExpect(status().isForbidden())
				.andExpect(forwardedUrl("/403"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adminRoutesAllowedForAdminRole() throws Exception {
		mockMvc.perform(get("/admin/categories"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/views/categories.jsp"));
	}

	@Test
	void adminLogoutViaGetRedirectsToAdminLogin() throws Exception {
		mockMvc.perform(get("/admin/logout").with(user("admin").roles("ADMIN")))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/admin/login"));
	}

	// ---- user chain ----

	@Test
	void userHomeRedirectsAnonymousToLogin() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("http://localhost/login"));
	}

	@Test
	@WithMockUser(roles = "USER")
	void userHomeAllowedForUserRole() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andExpect(forwardedUrl("/views/index.jsp"));
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void userRoutesForbiddenForAdminOnlyRole() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().isForbidden())
				.andExpect(forwardedUrl("/403"));
	}

	@Test
	void userLogoutViaGetRedirectsToLogin() throws Exception {
		mockMvc.perform(get("/logout").with(user("bob").roles("USER")))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/login"));
	}
}
