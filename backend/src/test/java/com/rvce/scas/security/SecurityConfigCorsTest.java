package com.rvce.scas.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rvce.scas.rbac.CustomAccessDeniedHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigCorsTest {

    @Test
    void allowsGithubCodespacesFrontendOrigin() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(UserDetailsServiceImpl.class),
                mock(JwtAuthFilter.class),
                new ObjectMapper(),
                mock(CustomAccessDeniedHandler.class)
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("OPTIONS");
        request.setRequestURI("/api/auth/login");
        request.addHeader("Origin", "https://demo-5173.app.github.dev");

        CorsConfiguration config = securityConfig.corsConfigurationSource().getCorsConfiguration(request);

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOriginPatterns()).contains("https://*.app.github.dev");
    }
}
