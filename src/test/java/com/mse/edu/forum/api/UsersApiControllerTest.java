package com.mse.edu.forum.api;

import static com.mse.edu.forum.support.ApiTestSupport.extractLongField;
import static com.mse.edu.forum.support.ApiTestSupport.loginAndGetToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mse.edu.forum.repo.UserRepository;
import com.mse.edu.forum.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class UsersApiControllerTest extends AbstractIntegrationTest {

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		userRepository.deleteByUsernameNot("admin");
	}

	@Test
	void adminCanCreateUser() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "admin");

		mockMvc.perform(post("/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "alice",
								  "email": "alice@forum.local",
								  "role": "USER",
								  "password": "password1"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("alice"))
				.andExpect(jsonPath("$.role").value("USER"));
	}

	@Test
	void createUser_returns409ForDuplicateUsername() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "admin");
		createUser(adminToken, "bob", "USER", "password1");

		mockMvc.perform(post("/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "bob",
								  "role": "USER",
								  "password": "password2"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("CONFLICT"))
				.andExpect(jsonPath("$.message").value("Username already taken"));
	}

	@Test
	void createUser_requiresAdminRole() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "admin");
		long userId = createUser(adminToken, "carol", "USER", "password1");
		String userToken = loginAndGetToken(mockMvc, "carol", "password1");

		mockMvc.perform(post("/users")
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "dave",
								  "role": "USER",
								  "password": "password1"
								}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));

		mockMvc.perform(get("/users/{id}", userId)
						.header("Authorization", "Bearer " + userToken))
				.andExpect(status().isOk());
	}

	@Test
	void moderatorCanListUsers() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "admin");
		createUser(adminToken, "mod1", "MODERATOR", "password1");
		String modToken = loginAndGetToken(mockMvc, "mod1", "password1");

		mockMvc.perform(get("/users").header("Authorization", "Bearer " + modToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.username == 'admin')]").exists());
	}

	@Test
	void regularUserCannotListUsers() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "admin");
		createUser(adminToken, "erin", "USER", "password1");
		String userToken = loginAndGetToken(mockMvc, "erin", "password1");

		mockMvc.perform(get("/users").header("Authorization", "Bearer " + userToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@Test
	void userCanUpdateSelfButNotChangeRole() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "admin");
		long userId = createUser(adminToken, "frank", "USER", "password1");
		String userToken = loginAndGetToken(mockMvc, "frank", "password1");

		mockMvc.perform(put("/users/{id}", userId)
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "frank",
								  "role": "ADMIN",
								  "email": "frank@forum.local"
								}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"))
				.andExpect(jsonPath("$.message").value("Only admins can change roles"));
	}

	@Test
	void userCannotReadOtherUserProfile() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "admin");
		createUser(adminToken, "grace", "USER", "password1");
		long otherId = createUser(adminToken, "heidi", "USER", "password1");
		String graceToken = loginAndGetToken(mockMvc, "grace", "password1");

		mockMvc.perform(get("/users/{id}", otherId).header("Authorization", "Bearer " + graceToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@Test
	void adminCanDeleteUser() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "admin");
		long userId = createUser(adminToken, "ivan", "USER", "password1");

		mockMvc.perform(delete("/users/{id}", userId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/users/{id}", userId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("NOT_FOUND"));
	}

	private long createUser(String adminToken, String username, String role, String password) throws Exception {
		MvcResult result = mockMvc.perform(post("/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "%s",
								  "role": "%s",
								  "password": "%s"
								}
								""".formatted(username, role, password)))
				.andExpect(status().isCreated())
				.andReturn();
		return extractLongField(result.getResponse().getContentAsString(), "id");
	}
}
