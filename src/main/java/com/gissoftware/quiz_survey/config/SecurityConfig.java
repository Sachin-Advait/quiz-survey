package com.gissoftware.quiz_survey.config;

import com.gissoftware.quiz_survey.logging.RequestResponseLoggingFilter;
import java.util.List;
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

@Configuration
@EnableWebSecurity
@EnableScheduling
public class SecurityConfig {

  //  @Autowired
  //  private KongAuthFilter kongAuthFilter;

  //   Register filters as Spring Beans
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

  //  @Bean
  //  public FilterRegistrationBean<KongAuthFilter> kongAuthFilterRegistration(KongAuthFilter
  // filter) {
  //    FilterRegistrationBean<KongAuthFilter> registration = new FilterRegistrationBean<>(filter);
  //    registration.setEnabled(false);
  //    return registration;
  //  }

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity http, RequestResponseLoggingFilter loggingFilter) throws Exception {

    return http.addFilterBefore(
            loggingFilter,
            org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
                .class)
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/api/user/sse/**")
                    .permitAll()
                    .requestMatchers("/api/user/ws/**")
                    .permitAll()
                    .requestMatchers("/api/user/**")
                    .permitAll()
                    .requestMatchers("/api/admin/**")
                    .permitAll()
                    .anyRequest()
                    .denyAll())
        .addFilterBefore(securityHeadersFilter(), BasicAuthenticationFilter.class)
        .addFilterBefore(refererValidationFilter(), securityHeadersFilter().getClass())
        .addFilterBefore(hostValidationFilter(), refererValidationFilter().getClass())
        //        .addFilterBefore(kongAuthFilter, hostValidationFilter().getClass())
        .build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(
        List.of(
            "https://omantelsip.omantel.om",
            "http://localhost:3000",
            "https://185.177.116.176",
            "https://omandigitalservices.online"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
