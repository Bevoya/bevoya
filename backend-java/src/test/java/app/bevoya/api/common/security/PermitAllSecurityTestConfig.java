package app.bevoya.api.common.security;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Makes only the test-only endpoints public. Everything else uses the real security chain. */
@TestConfiguration
public class PermitAllSecurityTestConfig {

  @Bean
  @Order(Ordered.HIGHEST_PRECEDENCE)
  SecurityFilterChain testOnlyEndpointsArePublic(HttpSecurity http) throws Exception {
    return http
        .securityMatcher("/test-only/**")
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(a -> a.anyRequest().permitAll())
        .build();
  }
}