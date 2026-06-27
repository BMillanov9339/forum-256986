package com.mse.edu.forum.api;

import static com.mse.edu.forum.support.ApiTestSupport.extractLongField;
import static com.mse.edu.forum.support.ApiTestSupport.loginAndGetToken;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mse.edu.forum.repo.PostRepository;
import com.mse.edu.forum.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class PostsApiControllerTest extends AbstractIntegrationTest {

	@Autowired
	private PostRepository postRepository;

	@BeforeEach
	void setUp() {
		postRepository.deleteAll();
	}

	@Test
	void createPostAndGetPosts() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "admin");

		mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "My first post",
								  "content": "Hello from MockMvc + Testcontainers"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.title").value("My first post"))
				.andExpect(jsonPath("$.content").value("Hello from MockMvc + Testcontainers"))
				.andExpect(jsonPath("$.createdAt").exists());

		mockMvc.perform(get("/posts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].title", hasItem("My first post")))
				.andExpect(jsonPath("$[*].content", hasItem("Hello from MockMvc + Testcontainers")));
	}

	@Test
	void createPost_requiresAuthentication() throws Exception {
		mockMvc.perform(post("/posts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "No token",
								  "content": "Should fail"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void getPostById_returnsCreatedPost() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "admin");

		MvcResult createResult = mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Single post",
								  "content": "For get by id"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();

		long id = extractLongField(createResult.getResponse().getContentAsString(), "id");

		mockMvc.perform(get("/posts/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id))
				.andExpect(jsonPath("$.title").value("Single post"))
				.andExpect(jsonPath("$.content").value("For get by id"));
	}

	@Test
	void getPostById_returns404WhenMissing() throws Exception {
		mockMvc.perform(get("/posts/{id}", 999999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Post not found"));
	}

	@Test
	void listPosts_isPublic() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "admin");
		mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Visible post",
								  "content": "Public read"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/posts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].title", hasItem("Visible post")))
				.andExpect(jsonPath("$[*].title", not(hasItem("No token"))));
	}

	@Test
	void listPosts_returnsInsertionOrder() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "admin");

		mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Post A",
								  "content": "A"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/posts")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Post B",
								  "content": "B"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/posts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Post A"))
				.andExpect(jsonPath("$[1].title").value("Post B"));
	}

	@Test
	void restoreInProgress_blocksReadWith503AndRetryAfter() throws Exception {
		restoreMaintenanceState.startRestore();

		mockMvc.perform(get("/posts"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(header().string("Retry-After", "120"))
				.andExpect(jsonPath("$.error").value("RESTORE_IN_PROGRESS"));
	}

	@Test
	void restoreInProgress_blocksWriteWith503AndRetryAfter() throws Exception {
		restoreMaintenanceState.startRestore();

		mockMvc.perform(post("/posts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Blocked during restore",
								  "content": "Should return 503"
								}
								"""))
				.andExpect(status().isServiceUnavailable())
				.andExpect(header().string("Retry-After", "120"))
				.andExpect(jsonPath("$.error").value("RESTORE_IN_PROGRESS"));
	}

	@Test
	void restoreInProgress_allowsReadinessEndpoint() throws Exception {
		restoreMaintenanceState.startRestore();

		mockMvc.perform(get("/readyz"))
				.andExpect(status().isOk());
	}
}
