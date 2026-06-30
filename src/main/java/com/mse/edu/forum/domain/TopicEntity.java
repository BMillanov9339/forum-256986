package com.mse.edu.forum.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "topics")
public class TopicEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 500)
	private String title;

	@Column(nullable = false, length = 10_000)
	private String content;

	@Column(name = "author_id", nullable = false)
	private Long authorId;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	@Column(nullable = false)
	private long viewCount;

	private Instant deletedAt;

	private Long deletedBy;

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		if (updatedAt == null) {
			updatedAt = createdAt;
		}
	}

	@jakarta.persistence.PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}
}
