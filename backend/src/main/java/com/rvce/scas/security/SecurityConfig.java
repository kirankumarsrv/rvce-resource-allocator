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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        /*
         * Beginner-level explanation of SecurityConfig choices:
         *
         * - CSRF disabled: CSRF protection is necessary when authentication is carried in
         *   browser cookies (which are auto-sent by browsers). With stateless JWT bearer
         *   tokens in `Authorization` headers, CSRF is not applicable and would cause
         *   unnecessary failures for legitimate API clients.
         *
         * - SessionCreationPolicy.STATELESS: prevents Spring from creating `HttpSession`
         *   (no JSESSIONID cookies). This ensures any pod can handle requests without
         *   sticky sessions, enabling horizontal scaling in Kubernetes.
         *
         * - AuthenticationEntryPoint: returns JSON 401 responses for unauthorized requests
         *   (instead of HTML redirect), which is important for single-page apps and API clients.
         *
         * - CORS configuration: explicitly lists allowed origins, methods, and headers so
         *   browsers permit cross-origin requests from the frontend. `setAllowCredentials(true)`
         *   is safe here because the app uses Authorization headers instead of cookies.
         *
         * - Note on permitAll: prefer explicit permits for login and refresh endpoints only.
         *   Avoid permitting the entire `/api/auth/**` path because it may include logout
         *   endpoints that expect an authenticated principal.
         */
        http
                // T-005 DECISION [11]: disable CSRF for stateless bearer-token API.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        // T-005 DECISION [12]: no HttpSession/JSESSIONID in JWT architecture.
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // REVIEW-RISK (critical): "/api/auth/**" also permits logout endpoints.
                        // That allows unauthenticated access to /logout and /logout-all controller methods,
                        // which expect a non-null principal and can lead to runtime errors if token missing.
                        // Safer approach: permit only login/refresh explicitly and require auth for logout routes.
                        .requestMatchers(
                                "/api/auth/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .requestMatchers("/api/exam/*/seating/my-seat").hasRole("STUDENT")
                        .requestMatchers("/api/exam/**").hasAnyRole("DEPT_COORD", "ADMIN", "TTO", "EXAM_CONTROLLER")
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

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
                // T-005 DECISION [13]: BCrypt strength 12 to balance brute-force resistance and UX latency.
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

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
        // Using Authorization header for bearer tokens; credentials flag mainly impacts cookie mode,
        // but keeping explicit origins avoids wildcard + credentials misconfiguration.
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
