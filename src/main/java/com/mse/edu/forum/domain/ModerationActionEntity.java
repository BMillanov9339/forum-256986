package com.mse.edu.forum.domain;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "moderation_actions")
public class ModerationActionEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Long actorId;
	private String targetType;
	private Long targetId;
	private String action;
	private String reasonCode;
	private String note;
	private boolean userNotified;
	private Instant createdAt;
}
