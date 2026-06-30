package com.mse.edu.forum.api;

import static com.mse.edu.forum.support.ApiTestSupport.loginAndGetToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mse.edu.forum.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuthApiControllerTest extends AbstractIntegrationTest {

	@Test
	void login_returnsTokenForValidCredentials() throws Exception {
		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "identifier": "admin",
								  "password": "test-admin-password"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isString())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresInSeconds").isNumber());
	}

	@Test
	void login_returns401ForInvalidCredentials() throws Exception {
		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "identifier": "admin",
								  "password": "wrong-password"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	@Test
	void repeatedFailedLoginsAreRateLimited() throws Exception {
		for (int attempt = 0; attempt < 10; attempt++) {
			mockMvc.perform(post("/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"identifier":"rate-limit-target","password":"wrong-password"}
									"""))
					.andExpect(status().isUnauthorized());
		}

		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"identifier":"rate-limit-target","password":"wrong-password"}
								"""))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.error").value("RATE_LIMITED"));
	}

	@Test
	void login_returns400ForBlankUsername() throws Exception {
		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "identifier": "",
								  "password": "admin"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
	}

	@Test
	void loginAndGetToken_helperWorks() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/topics")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void currentUser_returnsDatabaseIdentity() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");

		mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("admin"))
				.andExpect(jsonPath("$.role").value("ADMIN"));
	}

	@Test
	void malformedJson_usesStableErrorEnvelopeAndRequestId() throws Exception {
		mockMvc.perform(post("/auth/login")
						.header("X-Request-ID", "test-request-123")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("MALFORMED_REQUEST"))
				.andExpect(jsonPath("$.requestId").value("test-request-123"))
				.andExpect(jsonPath("$.fieldErrors").isMap());
	}

	@Test
	void registrationCreatesRegularUserWhoCanLoginByUsernameOrEmail() throws Exception {
		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "newmember",
								  "email": "NewMember@example.com",
								  "password": "password1"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("newmember"))
				.andExpect(jsonPath("$.email").value("newmember@example.com"))
				.andExpect(jsonPath("$.role").value("USER"));

		loginAndGetToken(mockMvc, "newmember", "password1");
		loginAndGetToken(mockMvc, "NEWMEMBER@EXAMPLE.COM", "password1");
	}

	@Test
	void registrationRejectsDuplicateEmailIgnoringCase() throws Exception {
		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"firstmember","email":"member@example.com","password":"password1"}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"secondmember","email":"MEMBER@example.com","password":"password1"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Email already in use"));
	}
}
