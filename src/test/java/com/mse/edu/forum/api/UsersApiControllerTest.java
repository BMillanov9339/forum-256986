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
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");

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
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
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
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
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
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		createUser(adminToken, "privateuser", "USER", "password1");
		var privateUser = userRepository.findByUsernameIgnoreCase("privateuser").orElseThrow();
		privateUser.setEmail("private@example.com");
		userRepository.saveAndFlush(privateUser);
		createUser(adminToken, "mod1", "MODERATOR", "password1");
		String modToken = loginAndGetToken(mockMvc, "mod1", "password1");

		mockMvc.perform(get("/users").header("Authorization", "Bearer " + modToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[?(@.username == 'admin')]").exists())
				.andExpect(jsonPath("$.items[?(@.username == 'privateuser')].email")
						.value(org.hamcrest.Matchers.contains(org.hamcrest.Matchers.nullValue())));
	}

	@Test
	void regularUserCannotListUsers() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		createUser(adminToken, "erin", "USER", "password1");
		String userToken = loginAndGetToken(mockMvc, "erin", "password1");

		mockMvc.perform(get("/users").header("Authorization", "Bearer " + userToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@Test
	void legacyUserUpdateEndpointIsAdminOnly() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
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
				.andExpect(jsonPath("$.message").value("Access denied"));

		mockMvc.perform(put("/users/{id}", userId)
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"frank","role":"USER","password":"attacker-password"}
								"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void userCannotReadOtherUserProfile() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		createUser(adminToken, "grace", "USER", "password1");
		long otherId = createUser(adminToken, "heidi", "USER", "password1");
		String graceToken = loginAndGetToken(mockMvc, "grace", "password1");

		mockMvc.perform(get("/users/{id}", otherId).header("Authorization", "Bearer " + graceToken))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("FORBIDDEN"));
	}

	@Test
	void adminCanDeleteUser() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		long userId = createUser(adminToken, "ivan", "USER", "password1");

		mockMvc.perform(delete("/users/{id}", userId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/users/{id}", userId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("deleted-user-" + userId))
				.andExpect(jsonPath("$.email").doesNotExist());
	}

	@Test
	void passwordChangeInvalidatesExistingToken() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		long userId = createUser(adminToken, "jane", "USER", "password1");
		String oldToken = loginAndGetToken(mockMvc, "jane", "password1");

		mockMvc.perform(put("/users/me/password")
						.header("Authorization", "Bearer " + oldToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"currentPassword":"password1","newPassword":"new-password1"}
								"""))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/users/{id}", userId).header("Authorization", "Bearer " + oldToken))
				.andExpect(status().isUnauthorized());
		loginAndGetToken(mockMvc, "jane", "new-password1");
	}

	@Test
	void deletedUserTokenIsRejected() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		long userId = createUser(adminToken, "kate", "USER", "password1");
		String userToken = loginAndGetToken(mockMvc, "kate", "password1");

		mockMvc.perform(delete("/users/{id}", userId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + userToken))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void demotedModeratorLosesPrivilegesImmediately() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		long moderatorId = createUser(adminToken, "leo", "MODERATOR", "password1");
		String moderatorToken = loginAndGetToken(mockMvc, "leo", "password1");

		mockMvc.perform(put("/users/{id}/role", moderatorId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"role\":\"USER\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/users").header("Authorization", "Bearer " + moderatorToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminCannotDeleteOrDemoteSelf() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		long adminId = userRepository.findByUsernameIgnoreCase("admin").orElseThrow().getId();

		mockMvc.perform(delete("/users/{id}", adminId).header("Authorization", "Bearer " + token))
				.andExpect(status().isConflict());
		mockMvc.perform(put("/users/{id}/role", adminId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"role\":\"USER\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void userCanUpdateOwnProfileWithoutSubmittingRole() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		createUser(adminToken, "profileuser", "USER", "password1");
		String token = loginAndGetToken(mockMvc, "profileuser", "password1");

		mockMvc.perform(put("/users/me")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"renameduser","email":"Renamed@example.com"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("renameduser"))
				.andExpect(jsonPath("$.email").value("renamed@example.com"))
				.andExpect(jsonPath("$.role").value("USER"));

		loginAndGetToken(mockMvc, "renamed@example.com", "password1");
	}

	@Test
	void regularUserCannotPromoteModerator() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		long targetId = createUser(adminToken, "targetuser", "USER", "password1");
		createUser(adminToken, "ordinaryuser", "USER", "password1");
		String token = loginAndGetToken(mockMvc, "ordinaryuser", "password1");

		mockMvc.perform(put("/users/{id}/role", targetId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"role\":\"MODERATOR\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void usernamesAreUniqueIgnoringCase() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		createUser(token, "mixedcase", "USER", "password1");

		mockMvc.perform(post("/users")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"MixedCase\",\"role\":\"USER\",\"password\":\"password1\"}"))
				.andExpect(status().isConflict());
	}

	@Test
	void passwordLimitUsesUtf8Bytes() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		String unicodePassword = "😀".repeat(20);

		mockMvc.perform(post("/users")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"unicode","role":"USER","password":"%s"}
								""".formatted(unicodePassword)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Password must not exceed 72 UTF-8 bytes"));
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
