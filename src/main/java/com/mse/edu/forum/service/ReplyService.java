package com.mse.edu.forum.service;

import com.mse.edu.forum.api.generated.model.CreateReplyRequest;
import com.mse.edu.forum.api.generated.model.ReplyPage;
import com.mse.edu.forum.api.generated.model.ReplyResponse;
import com.mse.edu.forum.api.generated.model.UpdateReplyRequest;
import com.mse.edu.forum.api.generated.model.DeleteContentRequest;
import com.mse.edu.forum.domain.ReplyEntity;
import com.mse.edu.forum.mapper.ReplyMapper;
import com.mse.edu.forum.repo.TopicRepository;
import com.mse.edu.forum.repo.ReplyRepository;
import com.mse.edu.forum.repo.UserRepository;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReplyService {

	private final ReplyRepository replies;
	private final TopicRepository topics;
	private final UserRepository users;
	private final ReplyMapper mapper;
	private final ModerationAuditService audit;

	public ReplyService(
			ReplyRepository replies,
			TopicRepository topics,
			UserRepository users,
			ReplyMapper mapper,
			ModerationAuditService audit) {
		this.replies = replies;
		this.topics = topics;
		this.users = users;
		this.mapper = mapper;
		this.audit = audit;
	}

	@Transactional(readOnly = true)
	public ReplyPage findByTopicId(Long topicId, int page, int size) {
		requireTopic(topicId);
		var result = replies.findByTopicId(topicId, PageRequest.of(page, size, Sort.by(
				Sort.Order.asc("createdAt"), Sort.Order.asc("id"))));
		var ids = result.stream().map(ReplyEntity::getAuthorId).collect(Collectors.toSet());
		Map<Long, String> names = users.findAllById(ids).stream()
				.collect(Collectors.toMap(u -> u.getId(), u -> u.getUsername()));
		var items = result.stream().map(r -> response(r, names.get(r.getAuthorId()))).toList();
		return new ReplyPage(page, size, result.getTotalElements(), result.getTotalPages(), items);
	}

	@Transactional(readOnly = true)
	public ReplyResponse findById(Long id) {
		ReplyEntity reply = requireReply(id);
		return response(reply, username(reply.getAuthorId()));
	}

	@Transactional
	public ReplyResponse create(Long topicId, CreateReplyRequest request) {
		requireActiveTopic(topicId);
		var actor = TopicService.currentUser();
		ReplyEntity reply = mapper.toEntity(request, topicId);
		reply.setAuthorId(actor.getId());
		return response(replies.save(reply), actor.getUsername());
	}

	@Transactional
	public ReplyResponse update(Long id, UpdateReplyRequest request) {
		ReplyEntity reply = requireReply(id);
		requireActive(reply);
		TopicService.requireOwnerOrModerator(reply.getAuthorId(), users);
		mapper.applyUpdate(request, reply);
		return response(replies.save(reply), username(reply.getAuthorId()));
	}

	@Transactional
	public void delete(Long id, DeleteContentRequest request) {
		ReplyEntity reply = requireReply(id);
		if (reply.getDeletedAt() != null) {
			return;
		}
		var actor = TopicService.currentUser();
		boolean moderator = actor.getDomainRole() != com.mse.edu.forum.domain.UserRole.USER;
		TopicService.requireOwnerOrModerator(reply.getAuthorId(), users);
		if (moderator && (request == null || request.getReasonCode() == null || request.getReasonCode().isBlank())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Moderator deletion reason is required");
		}
		reply.setDeletedAt(java.time.Instant.now());
		reply.setDeletedBy(actor.getId());
		replies.save(reply);
		if (moderator) {
			audit.record(
					actor.getId(),
					"REPLY",
					id,
					"DELETE",
					request.getReasonCode(),
					request.getNote(),
					Boolean.TRUE.equals(request.getUserNotified()));
		}
	}

	@Transactional
	public void purge(Long id, DeleteContentRequest request) {
		if (request == null || request.getReasonCode() == null || request.getReasonCode().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Purge reason is required");
		}
		ReplyEntity reply = requireReply(id);
		long actorId = TopicService.currentUser().getId();
		audit.record(
				actorId,
				"REPLY",
				id,
				"PURGE",
				request.getReasonCode(),
				request.getNote(),
				Boolean.TRUE.equals(request.getUserNotified()));
		replies.delete(reply);
	}

	private void requireTopic(Long id) {
		if (!topics.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found");
		}
	}

	private void requireActiveTopic(Long id) {
		var topic = topics.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
		if (topic.getDeletedAt() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot reply to a deleted topic");
		}
	}

	private static void requireActive(ReplyEntity reply) {
		if (reply.getDeletedAt() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Deleted replies are immutable");
		}
	}

	private ReplyEntity requireReply(Long id) {
		return replies.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reply not found"));
	}

	private String username(Long id) {
		return users.findById(id).map(u -> u.getUsername()).orElse("deleted");
	}

	private ReplyResponse response(ReplyEntity reply, String username) {
		ReplyResponse response = mapper.toResponse(reply, username);
		response.setAuthorRole(com.mse.edu.forum.api.generated.model.UserRole.fromValue(
				users.findById(reply.getAuthorId())
						.map(u -> u.getRole().name())
						.orElse(com.mse.edu.forum.domain.UserRole.USER.name())));
		var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
		boolean moderator = authentication != null
				&& authentication.getPrincipal() instanceof com.mse.edu.forum.security.ForumUserDetails details
				&& details.getDomainRole() != com.mse.edu.forum.domain.UserRole.USER;
		if (reply.getDeletedAt() != null && !moderator) {
			response.setContent("[deleted]");
		}
		return response;
	}
}
