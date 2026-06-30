package com.mse.edu.forum.api;

import static com.mse.edu.forum.support.ApiTestSupport.extractLongField;
import static com.mse.edu.forum.support.ApiTestSupport.loginAndGetToken;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mse.edu.forum.repo.TopicRepository;
import com.mse.edu.forum.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class TopicsApiControllerTest extends AbstractIntegrationTest {

	@Autowired
	private TopicRepository topicRepository;

	@BeforeEach
	void setUp() {
		topicRepository.deleteAll();
	}

	@Test
	void createPostAndGetPosts() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");

		mockMvc.perform(post("/topics")
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
				.andExpect(jsonPath("$.authorUsername").value("admin"))
				.andExpect(jsonPath("$.createdAt").exists());

		mockMvc.perform(get("/topics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].title", hasItem("My first post")))
				.andExpect(jsonPath("$.items[*].content", hasItem("Hello from MockMvc + Testcontainers")));
	}

	@Test
	void createPost_requiresAuthentication() throws Exception {
		mockMvc.perform(post("/topics")
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
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");

		MvcResult createResult = mockMvc.perform(post("/topics")
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

		mockMvc.perform(get("/topics/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topic.id").value(id))
				.andExpect(jsonPath("$.topic.title").value("Single post"))
				.andExpect(jsonPath("$.topic.content").value("For get by id"))
				.andExpect(jsonPath("$.replies.size").value(10));
	}

	@Test
	void getPostById_returns404WhenMissing() throws Exception {
		mockMvc.perform(get("/topics/{id}", 999999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Topic not found"));
	}

	@Test
	void listPosts_isPublic() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		mockMvc.perform(post("/topics")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Visible post",
								  "content": "Public read"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/topics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].title", hasItem("Visible post")))
				.andExpect(jsonPath("$.items[*].title", not(hasItem("No token"))));
	}

	@Test
	void listPosts_returnsNewestFirst() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");

		mockMvc.perform(post("/topics")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Post A",
								  "content": "A"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/topics")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Post B",
								  "content": "B"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/topics"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].title").value("Post B"))
				.andExpect(jsonPath("$.items[1].title").value("Post A"))
				.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void paginationHonorsRequestedLimit() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		createPost(token, "First", "A");
		createPost(token, "Second", "B");

		mockMvc.perform(get("/topics").param("size", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items.length()").value(1))
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void nonOwnerCannotUpdatePost() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		long postId = createPost(adminToken, "Admin post", "Body");
		MvcResult user = mockMvc.perform(post("/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"username\":\"reader\",\"role\":\"USER\",\"password\":\"password1\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String userToken = loginAndGetToken(mockMvc, "reader", "password1");

		mockMvc.perform(put("/topics/{id}", postId)
						.header("Authorization", "Bearer " + userToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"Hijacked\",\"content\":\"Nope\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void topicTitlesAreUniqueIgnoringCaseAndWhitespace() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		createPost(token, "Unique Topic", "First");

		mockMvc.perform(post("/topics")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"  unique topic  ","content":"Second"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Topic title already exists"));
	}

	@Test
	void openingTopicCountsEachAuthenticatedViewerOnce() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		long topicId = createPost(token, "Viewed topic", "Body");

		mockMvc.perform(get("/topics/{id}", topicId).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topic.viewCount").value(1))
				.andExpect(jsonPath("$.topic.updatedAt").exists());
		mockMvc.perform(get("/topics/{id}", topicId).header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topic.viewCount").value(1));
		mockMvc.perform(get("/topics/{id}", topicId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topic.viewCount").value(1));
	}

	@Test
	void moderatorCanEditAnotherUsersTopic() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		mockMvc.perform(post("/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"moderateduser","role":"USER","password":"password1"}
								"""))
				.andExpect(status().isCreated());
		String ownerToken = loginAndGetToken(mockMvc, "moderateduser", "password1");
		long topicId = createPost(ownerToken, "Moderated topic", "Before");
		mockMvc.perform(post("/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"topicmod","role":"MODERATOR","password":"password1"}
								"""))
				.andExpect(status().isCreated());
		String moderatorToken = loginAndGetToken(mockMvc, "topicmod", "password1");

		mockMvc.perform(put("/topics/{id}", topicId)
						.header("Authorization", "Bearer " + moderatorToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Moderated topic","content":"After"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("After"))
				.andExpect(jsonPath("$.updatedAt").exists());
	}

	@Test
	void authorDeletionLeavesTopicTombstone() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		mockMvc.perform(post("/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"topicowner","role":"USER","password":"password1"}
								"""))
				.andExpect(status().isCreated());
		String token = loginAndGetToken(mockMvc, "topicowner", "password1");
		long topicId = createPost(token, "Temporary topic", "Original content");

		mockMvc.perform(delete("/topics/{id}", topicId).header("Authorization", "Bearer " + token))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/topics/{id}", topicId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.topic.deleted").value(true))
				.andExpect(jsonPath("$.topic.title").value("[deleted]"))
				.andExpect(jsonPath("$.topic.content").value("This topic was deleted."));
	}

	@Test
	void moderatorDeletionRequiresReason() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		mockMvc.perform(post("/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"deletemod","role":"MODERATOR","password":"password1"}
								"""))
				.andExpect(status().isCreated());
		String moderatorToken = loginAndGetToken(mockMvc, "deletemod", "password1");
		mockMvc.perform(post("/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"deleteowner","role":"USER","password":"password1"}
								"""))
				.andExpect(status().isCreated());
		String ownerToken = loginAndGetToken(mockMvc, "deleteowner", "password1");
		long topicId = createPost(ownerToken, "Moderated topic for deletion", "Body");

		mockMvc.perform(delete("/topics/{id}", topicId).header("Authorization", "Bearer " + moderatorToken))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Moderator deletion reason is required"));

		mockMvc.perform(delete("/topics/{id}", topicId)
						.header("Authorization", "Bearer " + moderatorToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"reasonCode":"OFF_TOPIC","note":"Moderation test"}
								"""))
				.andExpect(status().isNoContent());
	}

	@Test
	void moderatorCannotModifyAdministratorTopic() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		long topicId = createPost(adminToken, "Administrator topic", "Body");
		mockMvc.perform(post("/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"limitedmod","role":"MODERATOR","password":"password1"}
								"""))
				.andExpect(status().isCreated());
		String moderatorToken = loginAndGetToken(mockMvc, "limitedmod", "password1");

		mockMvc.perform(put("/topics/{id}", topicId)
						.header("Authorization", "Bearer " + moderatorToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Administrator topic","content":"Changed"}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Moderators cannot modify administrator content"));

		mockMvc.perform(delete("/topics/{id}", topicId)
						.header("Authorization", "Bearer " + moderatorToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"reasonCode":"MODERATION"}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Moderators cannot modify administrator content"));
	}

	@Test
	void deletedTopicCannotBeEditedOrReceiveReplies() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		long topicId = createPost(token, "Deleted immutable topic", "Body");
		mockMvc.perform(delete("/topics/{id}", topicId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"reasonCode":"CLEANUP"}
								"""))
				.andExpect(status().isNoContent());

		mockMvc.perform(put("/topics/{id}", topicId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Changed deleted topic","content":"Changed"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Deleted topics are immutable"));

		mockMvc.perform(post("/topics/{id}/replies", topicId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"Hidden reply"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Cannot reply to a deleted topic"));
	}

	@Test
	void restoreInProgress_blocksReadWith503AndRetryAfter() throws Exception {
		restoreMaintenanceState.startRestore();

		mockMvc.perform(get("/topics"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(header().string("Retry-After", "120"))
				.andExpect(jsonPath("$.error").value("RESTORE_IN_PROGRESS"));
	}

	@Test
	void restoreInProgress_blocksWriteWith503AndRetryAfter() throws Exception {
		restoreMaintenanceState.startRestore();

		mockMvc.perform(post("/topics")
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

	private long createPost(String token, String title, String content) throws Exception {
		MvcResult result = mockMvc.perform(post("/topics")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"%s","content":"%s"}
								""".formatted(title, content)))
				.andExpect(status().isCreated())
				.andReturn();
		return extractLongField(result.getResponse().getContentAsString(), "id");
	}
}
