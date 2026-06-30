package com.mse.edu.forum.api;

import com.mse.edu.forum.api.generated.TopicsApi;
import com.mse.edu.forum.api.generated.model.CreateTopicRequest;
import com.mse.edu.forum.api.generated.model.TopicDetails;
import com.mse.edu.forum.api.generated.model.TopicResponse;
import com.mse.edu.forum.api.generated.model.TopicPage;
import com.mse.edu.forum.api.generated.model.UpdateTopicRequest;
import com.mse.edu.forum.service.TopicService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TopicsApiController implements TopicsApi {

	private static final Logger log = LogManager.getLogger(TopicsApiController.class);

	private final TopicService topicService;

	public TopicsApiController(TopicService topicService) {
		this.topicService = topicService;
	}

	@Override
	public ResponseEntity<TopicPage> listTopics(Integer page, Integer size) {
		return ResponseEntity.ok(topicService.findAll(page, size));
	}

	@Override
	public ResponseEntity<TopicDetails> getTopicById(Long id, Integer replyPage) {
		return ResponseEntity.ok(topicService.open(id, replyPage));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<TopicResponse> createTopic(@Valid CreateTopicRequest request) {
		TopicResponse created = topicService.create(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<TopicResponse> updateTopic(Long id, @Valid UpdateTopicRequest request) {
		return ResponseEntity.ok(topicService.update(id, request));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> deleteTopic(
			Long id, com.mse.edu.forum.api.generated.model.DeleteContentRequest request) {
		topicService.delete(id, request);
		return ResponseEntity.noContent().build();
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> purgeTopic(
			Long id, @Valid com.mse.edu.forum.api.generated.model.DeleteContentRequest request) {
		topicService.purge(id, request);
		return ResponseEntity.noContent().build();
	}
}
