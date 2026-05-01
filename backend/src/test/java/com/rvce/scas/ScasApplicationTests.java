package com.rvce.scas;

import com.rvce.scas.security.JwtTokenProvider;
import com.rvce.scas.security.UserDetailsServiceImpl;
import com.rvce.scas.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Smoke test that verifies the Spring context starts successfully.
 */
@SpringBootTest(properties = {
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
class ScasApplicationTests {

	@MockitoBean
	private UserDetailsServiceImpl userDetailsService;

	@MockitoBean
	private JwtTokenProvider jwtTokenProvider;

	@MockitoBean
	private AuthService authService;

	/**
	 * Verifies that the application context can load without startup errors.
	 */
	@Test
	void contextLoads() {
	}

}
