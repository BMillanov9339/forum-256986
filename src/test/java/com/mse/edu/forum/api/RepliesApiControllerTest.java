package com.mse.edu.forum.api;

import static com.mse.edu.forum.support.ApiTestSupport.extractLongField;
import static com.mse.edu.forum.support.ApiTestSupport.loginAndGetToken;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mse.edu.forum.repo.PostRepository;
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
	private PostRepository postRepository;

	@Autowired
	private ReplyRepository replyRepository;

	@Autowired
	private UserRepository userRepository;

	@BeforeEach
	void setUp() {
		replyRepository.deleteAll();
		postRepository.deleteAll();
		userRepository.deleteByUsernameNot("admin");
	}

	@Test
	void createAndListRepliesForPost() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "admin");
		long postId = createPost(token, "Thread", "Original post");

		mockMvc.perform(post("/posts/{postId}/replies", postId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "content": "First reply"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.postId").value(postId))
				.andExpect(jsonPath("$.content").value("First reply"));

		mockMvc.perform(post("/posts/{postId}/replies", postId)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "content": "Second reply"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/posts/{postId}/replies", postId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].content").value("First reply"))
				.andExpect(jsonPath("$[1].content").value("Second reply"));
	}

	@Test
	void createReply_requiresAuthentication() throws Exception {
		long postId = createPost(loginAndGetToken(mockMvc, "admin", "admin"), "Thread", "Body");

		mockMvc.perform(post("/posts/{postId}/replies", postId)
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
		mockMvc.perform(get("/posts/{postId}/replies", 999999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("Post not found"));
	}

	@Test
	void createReply_returns404WhenPostMissing() throws Exception {
		String token = loginAndGetToken(mockMvc, "admin", "admin");

		mockMvc.perform(post("/posts/{postId}/replies", 999999L)
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
		String token = loginAndGetToken(mockMvc, "admin", "admin");
		long postId = createPost(token, "Thread", "Body");

		MvcResult createResult = mockMvc.perform(post("/posts/{postId}/replies", postId)
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

	private long createPost(String token, String title, String content) throws Exception {
		MvcResult result = mockMvc.perform(post("/posts")
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
