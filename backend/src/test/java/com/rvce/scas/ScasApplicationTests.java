package com.rvce.scas;

import com.rvce.scas.security.JwtTokenProvider;
import com.rvce.scas.security.UserDetailsServiceImpl;
import com.rvce.scas.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
class ScasApplicationTests {

	@MockBean
	private UserDetailsServiceImpl userDetailsService;

	@MockBean
	private JwtTokenProvider jwtTokenProvider;

	@MockBean
	private AuthService authService;

	@Test
	void contextLoads() {
	}

}
