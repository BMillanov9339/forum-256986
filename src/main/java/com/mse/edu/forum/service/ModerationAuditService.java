package com.mse.edu.forum.service;

import com.mse.edu.forum.domain.ModerationActionEntity;
import com.mse.edu.forum.repo.ModerationActionRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class ModerationAuditService {
	private final ModerationActionRepository actions;

	public ModerationAuditService(ModerationActionRepository actions) {
		this.actions = actions;
	}

	public void record(
			long actorId,
			String targetType,
			long targetId,
			String action,
			String reasonCode,
			String note,
			boolean userNotified) {
		ModerationActionEntity entry = new ModerationActionEntity();
		entry.setActorId(actorId);
		entry.setTargetType(targetType);
		entry.setTargetId(targetId);
		entry.setAction(action);
		entry.setReasonCode(reasonCode);
		entry.setNote(note);
		entry.setUserNotified(userNotified);
		entry.setCreatedAt(Instant.now());
		actions.save(entry);
	}
}
