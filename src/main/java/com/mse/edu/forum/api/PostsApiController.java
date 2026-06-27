package com.mse.edu.forum.api;

import com.mse.edu.forum.api.generated.PostsApi;
import com.mse.edu.forum.api.generated.model.CreatePostRequest;
import com.mse.edu.forum.api.generated.model.PostResponse;
import com.mse.edu.forum.service.PostService;
import jakarta.validation.Valid;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PostsApiController implements PostsApi {

	private static final Logger log = LogManager.getLogger(PostsApiController.class);

	private final PostService postService;

	public PostsApiController(PostService postService) {
		this.postService = postService;
	}

	@Override
	public ResponseEntity<List<PostResponse>> listPosts() {
		log.debug("listPosts invoked");
		return ResponseEntity.ok(postService.findAll());
	}

	@Override
	public ResponseEntity<PostResponse> getPostById(Long id) {
		log.debug("getPostById invoked id={}", id);
		return postService
				.findById(id)
				.map(ResponseEntity::ok)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<PostResponse> createPost(@Valid CreatePostRequest createPostRequest) {
		log.debug("createPost invoked title={}", createPostRequest.getTitle());
		PostResponse created = postService.create(createPostRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}
}
