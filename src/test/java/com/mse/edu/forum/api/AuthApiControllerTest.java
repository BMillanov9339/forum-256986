package com.mse.edu.forum.api;

import static com.mse.edu.forum.support.ApiTestSupport.loginAndGetToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
								  "username": "admin",
								  "password": "admin"
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
								  "username": "admin",
								  "password": "wrong-password"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	@Test
	void login_returns400ForBlankUsername() throws Exception {
		mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "username": "",
								  "password": "admin"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
	}

	@Test
	void loginAndGetToken_helperWorks() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "admin");
		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/posts")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}
}
