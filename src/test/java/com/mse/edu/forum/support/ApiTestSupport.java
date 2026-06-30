package com.mse.edu.forum.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

public final class ApiTestSupport {

	private ApiTestSupport() {}

	public static String loginAndGetToken(MockMvc mockMvc, String username, String password) throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "identifier": "%s",
								  "password": "%s"
								}
								""".formatted(username, password)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isString())
				.andReturn();

		String body = loginResult.getResponse().getContentAsString();
		String marker = "\"accessToken\":\"";
		int start = body.indexOf(marker);
		if (start < 0) {
			throw new IllegalStateException("accessToken not found in login response: " + body);
		}
		start += marker.length();
		int end = body.indexOf('"', start);
		if (end < 0) {
			throw new IllegalStateException("Invalid login response: " + body);
		}
		return body.substring(start, end);
	}

	public static long extractLongField(String json, String fieldName) {
		Pattern pattern = Pattern.compile("\"" + fieldName + "\":(\\d+)");
		Matcher matcher = pattern.matcher(json);
		if (!matcher.find()) {
			throw new IllegalStateException("Field not found: " + fieldName + " in " + json);
		}
		return Long.parseLong(matcher.group(1));
	}
}
