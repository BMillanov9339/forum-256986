package com.mse.edu.forum.repo;

import com.mse.edu.forum.domain.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

	Optional<UserEntity> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsByUsernameAndIdNot(String username, Long id);

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, Long id);

	void deleteByUsernameNot(String username);
}
