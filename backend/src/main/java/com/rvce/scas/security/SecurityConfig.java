package com.rvce.scas.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rvce.scas.dto.ErrorResponseDto;
import com.rvce.scas.rbac.CustomAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Instant;
import java.util.List;

/**
 * Central Spring Security configuration for the stateless SCAS JWT API.
 *
 * <p>Fields:</p>
 * <ul>
 *   <li>{@code userDetailsService}: user lookup service used by DaoAuthenticationProvider.</li>
 *   <li>{@code jwtAuthFilter}: Bearer-token filter that authenticates each request.</li>
 *   <li>{@code objectMapper}: serializes JSON error responses for API clients.</li>
 *   <li>{@code customAccessDeniedHandler}: turns authorization failures into structured responses.</li>
 * </ul>
 *
 * <p>Critical steps in this configuration:</p>
 * <ol>
 *   <li>Disable CSRF because authentication uses headers instead of session cookies.</li>
 *   <li>Force stateless session handling so any pod can serve any request.</li>
 *   <li>Permit only login/refresh and infrastructure endpoints anonymously.</li>
 *   <li>Require authentication for logout routes and protected business endpoints.</li>
 *   <li>Return JSON errors instead of HTML redirects.</li>
 *   <li>Register the JWT filter before username/password authentication.</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

        /**
         * Builds the security filter chain.
         *
         * @param http the security builder
         * @return the configured security filter chain
         * @throws Exception if Spring Security cannot build the chain
         */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // T-005 DECISION [11]: disable CSRF for stateless bearer-token API.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        // T-005 DECISION [12]: no HttpSession/JSESSIONID in JWT architecture.
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/actuator/health",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/api/auth/logout", "/api/auth/logout-all").authenticated()
                        .requestMatchers("/api/exam/*/seating/my-seat").hasRole("STUDENT")
                        .requestMatchers("/api/exam/**").hasAnyRole("DEPT_COORD", "ADMIN", "TTO", "EXAM_CONTROLLER")
                        .requestMatchers("/api/timetable/available").hasAnyRole("TEACHER", "TTO", "DEPT_COORD", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/timetable/generate", "/api/timetable/upload", "/api/timetable/**")
                        .hasAnyRole("TTO", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        // T-005 DECISION [14]: return JSON 401 for API clients instead of HTML redirect.
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                            String errorCode = response.getHeader("X-Auth-Error") != null
                                    ? response.getHeader("X-Auth-Error")
                                    : "UNAUTHORIZED";

                            ErrorResponseDto error = ErrorResponseDto.builder()
                                    .timestamp(Instant.now())
                                    .status(401)
                                    .error("Unauthorized")
                                    .code(errorCode)
                                    .message("Authentication required. Provide a valid Bearer token.")
                                    .path(request.getRequestURI())
                                    .build();

                            response.getWriter().write(objectMapper.writeValueAsString(error));
                        })
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .authenticationProvider(authenticationProvider());

        return http.build();
    }

        /**
         * Creates the authentication provider backed by the custom user details service.
         *
         * @return DAO authentication provider
         */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
                DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

        /**
         * Uses BCrypt for password hashing.
         *
         * @return BCrypt password encoder with strength 12
         */
    @Bean
    public PasswordEncoder passwordEncoder() {
                // T-005 DECISION [13]: BCrypt strength 12 to balance brute-force resistance and UX latency.
        return new BCryptPasswordEncoder(12);
    }

        /**
         * Exposes the application authentication manager.
         *
         * @param config authentication configuration
         * @return authentication manager
         * @throws Exception if the manager cannot be obtained
         */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

        /**
         * Defines CORS rules for the frontend origins.
         *
         * @return CORS configuration source
         */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://scas.rvce.edu.in"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "Accept", "X-Requested-With", "X-Auth-Error"
        ));
        config.setExposedHeaders(List.of("X-Auth-Error"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
