package com.mse.edu.forum.api;

import com.mse.edu.forum.api.generated.RepliesApi;
import com.mse.edu.forum.api.generated.model.CreateReplyRequest;
import com.mse.edu.forum.api.generated.model.ReplyResponse;
import com.mse.edu.forum.service.ReplyService;
import jakarta.validation.Valid;
import java.util.List;
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
	public ResponseEntity<List<ReplyResponse>> listRepliesForPost(Long postId) {
		log.debug("listRepliesForPost postId={}", postId);
		return ResponseEntity.ok(replyService.findByPostId(postId));
	}

	@Override
	public ResponseEntity<ReplyResponse> getReplyById(Long id) {
		log.debug("getReplyById id={}", id);
		return replyService
				.findById(id)
				.map(ResponseEntity::ok)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reply not found"));
	}

	@Override
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ReplyResponse> createReply(Long postId, @Valid CreateReplyRequest createReplyRequest) {
		log.debug("createReply postId={}", postId);
		ReplyResponse created = replyService.create(postId, createReplyRequest);
		return ResponseEntity.status(HttpStatus.CREATED).body(created);
	}
}
