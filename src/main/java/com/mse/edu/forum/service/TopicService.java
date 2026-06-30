package com.mse.edu.forum.service;

import com.mse.edu.forum.api.generated.model.CreateTopicRequest;
import com.mse.edu.forum.api.generated.model.TopicDetails;
import com.mse.edu.forum.api.generated.model.TopicPage;
import com.mse.edu.forum.api.generated.model.TopicResponse;
import com.mse.edu.forum.api.generated.model.UpdateTopicRequest;
import com.mse.edu.forum.api.generated.model.DeleteContentRequest;
import com.mse.edu.forum.domain.TopicEntity;
import com.mse.edu.forum.domain.UserRole;
import com.mse.edu.forum.mapper.TopicMapper;
import com.mse.edu.forum.repo.TopicRepository;
import com.mse.edu.forum.repo.UserRepository;
import com.mse.edu.forum.security.ForumUserDetails;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TopicService {

	private final TopicRepository topics;
	private final UserRepository users;
	private final TopicMapper mapper;
	private final ReplyService replies;
	private final com.mse.edu.forum.repo.ReplyRepository replyRepository;
	private final ModerationAuditService audit;

	public TopicService(
			TopicRepository topics,
			UserRepository users,
			TopicMapper mapper,
			ReplyService replies,
			com.mse.edu.forum.repo.ReplyRepository replyRepository,
			ModerationAuditService audit) {
		this.topics = topics;
		this.users = users;
		this.mapper = mapper;
		this.replies = replies;
		this.replyRepository = replyRepository;
		this.audit = audit;
	}

	@Transactional(readOnly = true)
	public TopicPage findAll(int page, int size) {
		var result = topics.findAll(PageRequest.of(page, size, Sort.by(
				Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
		Map<Long, String> names = authorNames(result.getContent());
		var items = result.stream().map(t -> response(t, names.get(t.getAuthorId()))).toList();
		return new TopicPage(page, size, result.getTotalElements(), result.getTotalPages(), items);
	}

	@Transactional
	public TopicDetails open(Long id, int replyPage) {
		TopicEntity topic = requireTopic(id);
		ForumUserDetails viewer = currentUserOrNull();
		if (viewer != null && topics.recordUniqueView(id, viewer.getId()) == 1) {
			topics.incrementViewCount(id);
			topic = requireTopic(id);
		}
		return new TopicDetails(
				response(topic, requireUsername(topic.getAuthorId())),
				replies.findByTopicId(id, replyPage, 10));
	}

	@Transactional
	public TopicResponse create(CreateTopicRequest request) {
		String title = request.getTitle().trim();
		if (topics.existsByTitleIgnoreCaseAndDeletedAtIsNull(title)) {
			throw conflict("Topic title already exists");
		}
		ForumUserDetails actor = currentUser();
		TopicEntity topic = mapper.toEntity(request);
		topic.setAuthorId(actor.getId());
		return response(topics.saveAndFlush(topic), actor.getUsername());
	}

	@Transactional
	public TopicResponse update(Long id, UpdateTopicRequest request) {
		TopicEntity topic = requireTopic(id);
		requireActive(topic);
		requireOwnerOrModerator(topic.getAuthorId(), users);
		String title = request.getTitle().trim();
		if (topics.existsByTitleIgnoreCaseAndIdNotAndDeletedAtIsNull(title, id)) {
			throw conflict("Topic title already exists");
		}
		mapper.applyUpdate(request, topic);
		return response(topics.saveAndFlush(topic), requireUsername(topic.getAuthorId()));
	}

	@Transactional
	public void delete(Long id, DeleteContentRequest request) {
		TopicEntity topic = requireTopic(id);
		if (topic.getDeletedAt() != null) {
			return;
		}
		ForumUserDetails actor = currentUser();
		boolean moderator = actor.getDomainRole() != com.mse.edu.forum.domain.UserRole.USER;
		requireOwnerOrModerator(topic.getAuthorId(), users);
		if (!moderator) {
			boolean inGracePeriod = topic.getCreatedAt().isAfter(Instant.now().minus(15, ChronoUnit.MINUTES));
			if (!inGracePeriod && replyRepository.countByTopicIdAndDeletedAtIsNull(id) > 0) {
				throw conflict("Topics with replies can no longer be deleted by their author");
			}
		} else if (request == null || request.getReasonCode() == null || request.getReasonCode().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Moderator deletion reason is required");
		}
		topic.setDeletedAt(Instant.now());
		topic.setDeletedBy(actor.getId());
		topics.save(topic);
		if (moderator) {
			audit.record(
					actor.getId(),
					"TOPIC",
					id,
					"DELETE",
					request.getReasonCode(),
					request.getNote(),
					Boolean.TRUE.equals(request.getUserNotified()));
		}
	}

	@Transactional
	public void purge(Long id, DeleteContentRequest request) {
		requireReason(request);
		TopicEntity topic = requireTopic(id);
		long actorId = currentUser().getId();
		audit.record(
				actorId,
				"TOPIC",
				id,
				"PURGE",
				request.getReasonCode(),
				request.getNote(),
				Boolean.TRUE.equals(request.getUserNotified()));
		topics.delete(topic);
	}

	private static void requireReason(DeleteContentRequest request) {
		if (request == null || request.getReasonCode() == null || request.getReasonCode().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Purge reason is required");
		}
	}

	private TopicEntity requireTopic(Long id) {
		return topics.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
	}

	private static void requireActive(TopicEntity topic) {
		if (topic.getDeletedAt() != null) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Deleted topics are immutable");
		}
	}

	private TopicResponse response(TopicEntity topic, String username) {
		TopicResponse response = mapper.toResponse(topic, username);
		response.setAuthorRole(com.mse.edu.forum.api.generated.model.UserRole.fromValue(
				users.findById(topic.getAuthorId()).map(u -> u.getRole().name()).orElse(UserRole.USER.name())));
		if (topic.getDeletedAt() != null && !isModerator()) {
			response.setTitle("[deleted]");
			response.setContent("This topic was deleted.");
		}
		return response;
	}

	private static boolean isModerator() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null
				&& authentication.getPrincipal() instanceof ForumUserDetails details
				&& details.getDomainRole() != com.mse.edu.forum.domain.UserRole.USER;
	}

	private Map<Long, String> authorNames(java.util.Collection<TopicEntity> values) {
		var ids = values.stream().map(TopicEntity::getAuthorId).collect(Collectors.toSet());
		return users.findAllById(ids).stream()
				.collect(Collectors.toMap(u -> u.getId(), u -> u.getUsername()));
	}

	private String requireUsername(Long id) {
		return users.findById(id).map(u -> u.getUsername()).orElse("deleted");
	}

	public static ForumUserDetails currentUser() {
		Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if (principal instanceof ForumUserDetails details) {
			return details;
		}
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
	}

	private static ForumUserDetails currentUserOrNull() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.getPrincipal() instanceof ForumUserDetails details
				? details
				: null;
	}

	static void requireOwnerOrModerator(Long ownerId, UserRepository users) {
		ForumUserDetails actor = currentUser();
		if (actor.getId() == ownerId) {
			return;
		}
		if (actor.getDomainRole() == UserRole.USER) {
			throw new ResponseStatusException(
					HttpStatus.FORBIDDEN, "Only the author or a moderator may modify this content");
		}
		UserRole ownerRole = users.findById(ownerId).map(u -> u.getRole()).orElse(UserRole.USER);
		if (actor.getDomainRole() == UserRole.MODERATOR && ownerRole == UserRole.ADMIN) {
			throw new ResponseStatusException(
					HttpStatus.FORBIDDEN, "Moderators cannot modify administrator content");
		}
	}

	private static ResponseStatusException conflict(String message) {
		return new ResponseStatusException(HttpStatus.CONFLICT, message);
	}
}
