package com.mse.edu.forum.repo;

import com.mse.edu.forum.domain.UserEntity;
import java.util.Optional;
import java.util.List;
import com.mse.edu.forum.domain.UserRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

	Optional<UserEntity> findByUsername(String username);

	Optional<UserEntity> findByUsernameIgnoreCase(String username);

	Optional<UserEntity> findByEmailIgnoreCase(String email);

	boolean existsByUsernameIgnoreCase(String username);

	boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

	boolean existsByEmailIgnoreCase(String email);

	boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from UserEntity u where u.role = :role order by u.id")
	List<UserEntity> lockAllByRole(UserRole role);

	@Transactional
	void deleteByUsernameNot(String username);
}
