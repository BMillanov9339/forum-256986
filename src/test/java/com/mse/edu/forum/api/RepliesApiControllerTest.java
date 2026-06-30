package com.mse.edu.forum.api;

import static com.mse.edu.forum.support.ApiTestSupport.extractLongField;
import static com.mse.edu.forum.support.ApiTestSupport.loginAndGetToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mse.edu.forum.repo.TopicRepository;
import com.mse.edu.forum.repo.ReplyRepository;
import com.mse.edu.forum.repo.UserRepository;
import com.mse.edu.forum.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class RepliesApiControllerTest extends AbstractIntegrationTest {

	@Autowired
	private TopicRepository topicRepository;

	@Autowired
	private ReplyRepository replyRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		replyRepository.deleteAll();
		topicRepository.deleteAll();
		userRepository.deleteByUsernameNot("admin");
	}

	@Test
	void createAndListRepliesForPost() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		long postId = createPost(token, "Thread", "Original post");

		mockMvc.perform(post("/topics/{topicId}/replies", postId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "content": "First reply"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.topicId").value(postId))
				.andExpect(jsonPath("$.content").value("First reply"))
				.andExpect(jsonPath("$.updatedAt").exists());

		mockMvc.perform(post("/topics/{topicId}/replies", postId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "content": "Second reply"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/topics/{topicId}/replies", postId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].content").value("First reply"))
				.andExpect(jsonPath("$.items[1].content").value("Second reply"));
	}

	@Test
	void createReply_requiresAuthentication() throws Exception {
		long postId = createPost(
				loginAndGetToken(mockMvc, "admin", "test-admin-password"), "Thread", "Body");

		mockMvc.perform(post("/topics/{topicId}/replies", postId)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "content": "Anonymous reply"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
	}

	@Test
	void listRepliesForPost_returns404WhenPostMissing() throws Exception {
		mockMvc.perform(get("/topics/{topicId}/replies", 999999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Topic not found"));
	}

	@Test
	void createReply_returns404WhenPostMissing() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");

		mockMvc.perform(post("/topics/{topicId}/replies", 999999L)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "content": "Orphan reply"
								}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("NOT_FOUND"));
	}

	@Test
	void getReplyById_returnsCreatedReply() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		long postId = createPost(token, "Thread", "Body");

		MvcResult createResult = mockMvc.perform(post("/topics/{topicId}/replies", postId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "content": "Lookup me"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();

		long replyId = extractLongField(createResult.getResponse().getContentAsString(), "id");

		mockMvc.perform(get("/replies/{id}", replyId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(replyId))
				.andExpect(jsonPath("$.content").value("Lookup me"));
	}

	@Test
	void getReplyById_returns404WhenMissing() throws Exception {
		mockMvc.perform(get("/replies/{id}", 999999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Reply not found"));
	}

	@Test
	void topicDetailsLoadsTenRepliesPerPage() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		long topicId = createPost(token, "Paged discussion", "Body");
		for (int i = 1; i <= 11; i++) {
			mockMvc.perform(post("/topics/{topicId}/replies", topicId)
							.header("Authorization", "Bearer " + token)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{"content":"Reply %d"}
									""".formatted(i)))
					.andExpect(status().isCreated());
		}

		mockMvc.perform(get("/topics/{id}", topicId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.replies.size").value(10))
				.andExpect(jsonPath("$.replies.items.length()").value(10))
				.andExpect(jsonPath("$.replies.totalElements").value(11))
				.andExpect(jsonPath("$.replies.totalPages").value(2));
	}

	@Test
	void authorCanEditReplyAndReceivesModificationTimestamp() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		long topicId = createPost(token, "Editable discussion", "Body");
		MvcResult result = mockMvc.perform(post("/topics/{topicId}/replies", topicId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"Before"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		long replyId = extractLongField(result.getResponse().getContentAsString(), "id");

		mockMvc.perform(put("/replies/{id}", replyId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"After"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("After"))
				.andExpect(jsonPath("$.updatedAt").exists());
	}

	@Test
	void moderatorCannotModifyAdministratorReply() throws Exception {
		String adminToken = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		long topicId = createPost(adminToken, "Administrator reply discussion", "Body");
		MvcResult result = mockMvc.perform(post("/topics/{topicId}/replies", topicId)
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"Administrator reply"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.authorRole").value("ADMIN"))
				.andReturn();
		long replyId = extractLongField(result.getResponse().getContentAsString(), "id");

		mockMvc.perform(post("/users")
						.header("Authorization", "Bearer " + adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"replymod","role":"MODERATOR","password":"password1"}
								"""))
				.andExpect(status().isCreated());
		String moderatorToken = loginAndGetToken(mockMvc, "replymod", "password1");

		mockMvc.perform(put("/replies/{id}", replyId)
						.header("Authorization", "Bearer " + moderatorToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"Changed"}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Moderators cannot modify administrator content"));

		mockMvc.perform(delete("/replies/{id}", replyId)
						.header("Authorization", "Bearer " + moderatorToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"reasonCode":"MODERATION"}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message").value("Moderators cannot modify administrator content"));
	}

	@Test
	void deletedReplyCannotBeEdited() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "test-admin-password");
		long topicId = createPost(token, "Deleted reply discussion", "Body");
		MvcResult result = mockMvc.perform(post("/topics/{topicId}/replies", topicId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"Reply to delete"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		long replyId = extractLongField(result.getResponse().getContentAsString(), "id");

		mockMvc.perform(delete("/replies/{id}", replyId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"reasonCode":"CLEANUP"}
								"""))
				.andExpect(status().isNoContent());

		mockMvc.perform(put("/replies/{id}", replyId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"content":"Changed"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Deleted replies are immutable"));
	}

	private long createPost(String token, String title, String content) throws Exception {
		MvcResult result = mockMvc.perform(post("/topics")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "%s",
								  "content": "%s"
								}
								""".formatted(title, content)))
				.andExpect(status().isCreated())
				.andReturn();
		return extractLongField(result.getResponse().getContentAsString(), "id");
	}
}
