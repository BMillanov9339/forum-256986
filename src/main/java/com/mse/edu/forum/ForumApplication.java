package com.mse.edu.forum;

import com.mse.edu.forum.config.CorsProperties;
import com.mse.edu.forum.maintenance.RestoreProperties;
import com.mse.edu.forum.security.JwtProperties;
import com.mse.edu.forum.security.BootstrapAdminProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableMethodSecurity
@EnableConfigurationProperties({
	JwtProperties.class,
	BootstrapAdminProperties.class,
	RestoreProperties.class,
	CorsProperties.class
})
public class ForumApplication {

	public static void main(String[] args) {
		SpringApplication.run(ForumApplication.class, args);
	}

}
