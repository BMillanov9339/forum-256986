package com.mse.edu.forum.mapper;

import com.mse.edu.forum.api.generated.model.CreateUserRequest;
import com.mse.edu.forum.api.generated.model.UpdateUserRequest;
import com.mse.edu.forum.api.generated.model.UserResponse;
import com.mse.edu.forum.api.generated.model.UserRole;
import com.mse.edu.forum.domain.UserEntity;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface UserMapper {

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "passwordHash", ignore = true)
	@Mapping(target = "authVersion", ignore = true)
	@Mapping(target = "anonymizedAt", ignore = true)
	@Mapping(target = "username", source = "username", qualifiedByName = "trimmed")
	@Mapping(target = "email", source = "email", qualifiedByName = "normalizeEmail")
	@Mapping(target = "role", source = "role")
	UserEntity toEntity(CreateUserRequest request);

	@Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "instantToOffset")
	@Mapping(target = "role", source = "role")
	UserResponse toResponse(UserEntity entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "passwordHash", ignore = true)
	@Mapping(target = "authVersion", ignore = true)
	@Mapping(target = "anonymizedAt", ignore = true)
	@Mapping(target = "username", source = "username", qualifiedByName = "trimmed")
	@Mapping(target = "email", source = "email", qualifiedByName = "normalizeEmail")
	@Mapping(target = "role", source = "role")
	void applyUpdate(UpdateUserRequest request, @MappingTarget UserEntity entity);

	com.mse.edu.forum.domain.UserRole toDomainRole(UserRole role);

	UserRole toApiRole(com.mse.edu.forum.domain.UserRole role);

	@Named("trimmed")
	default String trimmed(String value) {
		return value == null ? null : value.trim();
	}

	@Named("normalizeEmail")
	default String normalizeEmail(String value) {
		if (value == null) {
			return null;
		}
		String t = value.trim();
		return t.isEmpty() ? null : t;
	}

	@Named("instantToOffset")
	default OffsetDateTime instantToOffset(Instant instant) {
		return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
	}
}
