package com.mse.edu.forum.support;

import com.mse.edu.forum.maintenance.RestoreMaintenanceState;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

	static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("forum")
			.withUsername("forum_app")
			.withPassword("test123");

	static {
		POSTGRES.start();
	}

	@DynamicPropertySource
	static void configureDataSource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
		registry.add("app.jwt.secret", () -> "test-jwt-secret-at-least-32-characters-long");
		registry.add("app.bootstrap-admin.password", () -> "test-admin-password");
	}

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected RestoreMaintenanceState restoreMaintenanceState;

	@BeforeEach
	void resetRestoreState() {
		restoreMaintenanceState.finishRestore();
	}
}
