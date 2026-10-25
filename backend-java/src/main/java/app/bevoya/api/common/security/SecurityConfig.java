package app.bevoya.api.common.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
class SecurityConfig {

  @Bean
  SecurityFilterChain api(HttpSecurity http) throws Exception {
    ProblemJsonSecurityHandlers problems = new ProblemJsonSecurityHandlers();
    return http
        .csrf(csrf -> csrf.disable()) // stateless bearer API, no cookies
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a
            .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
            .requestMatchers("/health").permitAll()
            .anyRequest().authenticated())
        .oauth2ResourceServer(o -> o
            .jwt(Customizer.withDefaults())
            .authenticationEntryPoint(problems)
            .accessDeniedHandler(problems))
        .exceptionHandling(e -> e
            .authenticationEntryPoint(problems)
            .accessDeniedHandler(problems))
        .build();
  }
}