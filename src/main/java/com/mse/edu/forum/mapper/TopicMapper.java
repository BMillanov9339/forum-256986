package com.mse.edu.forum.mapper;

import com.mse.edu.forum.api.generated.model.CreateTopicRequest;
import com.mse.edu.forum.api.generated.model.TopicResponse;
import com.mse.edu.forum.domain.TopicEntity;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.MappingTarget;
import com.mse.edu.forum.api.generated.model.UpdateTopicRequest;

@Mapper(componentModel = "spring")
public interface TopicMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "viewCount", ignore = true)
	@Mapping(target = "deletedAt", ignore = true)
	@Mapping(target = "deletedBy", ignore = true)
	@Mapping(target = "authorId", ignore = true)
	@Mapping(target = "title", source = "title", qualifiedByName = "trimmed")
	@Mapping(target = "content", source = "content", qualifiedByName = "trimmed")
	TopicEntity toEntity(CreateTopicRequest request);

	@Mapping(target = "createdAt", source = "entity.createdAt", qualifiedByName = "instantToOffset")
	@Mapping(target = "updatedAt", source = "entity.updatedAt", qualifiedByName = "instantToOffset")
	@Mapping(target = "deletedAt", source = "entity.deletedAt", qualifiedByName = "instantToOffset")
	@Mapping(target = "deleted", expression = "java(entity.getDeletedAt() != null)")
	@Mapping(target = "authorUsername", source = "authorUsername")
	@Mapping(target = "authorRole", ignore = true)
	TopicResponse toResponse(TopicEntity entity, String authorUsername);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "viewCount", ignore = true)
	@Mapping(target = "deletedAt", ignore = true)
	@Mapping(target = "deletedBy", ignore = true)
	@Mapping(target = "authorId", ignore = true)
	@Mapping(target = "title", source = "title", qualifiedByName = "trimmed")
	@Mapping(target = "content", source = "content", qualifiedByName = "trimmed")
	void applyUpdate(UpdateTopicRequest request, @MappingTarget TopicEntity entity);

	@Named("trimmed")
	default String trimmed(String value) {
		return value == null ? null : value.trim();
	}

	@Named("instantToOffset")
	default OffsetDateTime instantToOffset(Instant instant) {
		return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
	}
}
