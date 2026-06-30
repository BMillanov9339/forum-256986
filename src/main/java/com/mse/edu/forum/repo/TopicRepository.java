package com.mse.edu.forum.repo;

import com.mse.edu.forum.domain.TopicEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TopicRepository extends JpaRepository<TopicEntity, Long> {

	boolean existsByTitleIgnoreCaseAndDeletedAtIsNull(String title);

	boolean existsByTitleIgnoreCaseAndIdNotAndDeletedAtIsNull(String title, Long id);

	@Modifying(clearAutomatically = true)
	@Query("update TopicEntity t set t.viewCount = t.viewCount + 1 where t.id = :id")
	int incrementViewCount(Long id);

	@Modifying
	@Query(
			value = """
					INSERT INTO topic_views (topic_id, user_id)
					VALUES (:topicId, :userId)
					ON CONFLICT DO NOTHING
					""",
			nativeQuery = true)
	int recordUniqueView(Long topicId, Long userId);
}
