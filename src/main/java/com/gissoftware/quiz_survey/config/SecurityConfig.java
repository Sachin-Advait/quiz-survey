package com.gissoftware.quiz_survey.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableScheduling
public class SecurityConfig {

    @Autowired
    private KongAuthFilter kongAuthFilter;

    // Register filters as Spring Beans
    @Bean
    public SecurityHeadersFilter securityHeadersFilter() {
        return new SecurityHeadersFilter();
    }

    @Bean
    public RefererValidationFilter refererValidationFilter() {
        return new RefererValidationFilter();
    }

    @Bean
    public HostValidationFilter hostValidationFilter() {
        return new HostValidationFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                                        .permitAll()
                                        .requestMatchers("/api/user/**")
                                        .authenticated()
                                        .requestMatchers("/api/admin/**")
                                        .authenticated()
                                        .requestMatchers("/api/user/ws/**")
                                        .authenticated()
                                        .anyRequest()
                                        .denyAll())

                // Apply filters in correct order with BEAN INSTANCES
                .addFilterBefore(securityHeadersFilter(), BasicAuthenticationFilter.class)
                .addFilterBefore(refererValidationFilter(), securityHeadersFilter().getClass())
                .addFilterBefore(hostValidationFilter(), refererValidationFilter().getClass())
                .addFilterBefore(kongAuthFilter, hostValidationFilter().getClass())
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("https://omantelsip.omantel.om"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
