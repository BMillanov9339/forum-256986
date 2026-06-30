package com.mse.edu.forum.api;

import com.mse.edu.forum.api.generated.RepliesApi;
import com.mse.edu.forum.api.generated.model.CreateReplyRequest;
import com.mse.edu.forum.api.generated.model.ReplyResponse;
import com.mse.edu.forum.api.generated.model.ReplyPage;
import com.mse.edu.forum.api.generated.model.UpdateReplyRequest;
import com.mse.edu.forum.service.ReplyService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class RepliesApiController implements RepliesApi {

	private static final Logger log = LogManager.getLogger(RepliesApiController.class);

	private final ReplyService replyService;

	public RepliesApiController(ReplyService replyService) {
		this.replyService = replyService;
	}

	@Override
	public ResponseEntity<ReplyPage> listRepliesForTopic(Long topicId, Integer page, Integer size) {
		return ResponseEntity.ok(replyService.findByTopicId(topicId, page, size));
	}

	@Override
	public ResponseEntity<ReplyResponse> getReplyById(Long id) {
		log.debug("getReplyById id={}", id);
		return ResponseEntity.ok(replyService.findById(id));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ReplyResponse> createReply(Long topicId, @Valid CreateReplyRequest createReplyRequest) {
		ReplyResponse created = replyService.create(topicId, createReplyRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ReplyResponse> updateReply(Long id, @Valid UpdateReplyRequest request) {
		return ResponseEntity.ok(replyService.update(id, request));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Void> deleteReply(
			Long id, com.mse.edu.forum.api.generated.model.DeleteContentRequest request) {
		replyService.delete(id, request);
		return ResponseEntity.noContent().build();
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<Void> purgeReply(
			Long id, @Valid com.mse.edu.forum.api.generated.model.DeleteContentRequest request) {
		replyService.purge(id, request);
		return ResponseEntity.noContent().build();
	}
}
