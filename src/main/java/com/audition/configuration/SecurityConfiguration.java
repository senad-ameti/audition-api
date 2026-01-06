package com.audition.configuration;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.info.InfoEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private static final String ACTUATOR_ROLE = "ACTUATOR";

    @Bean
    SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(
                auth -> auth.requestMatchers(
                        // only expose health and info endpoints without authentication
                        EndpointRequest.to(HealthEndpoint.class, InfoEndpoint.class))
                    .permitAll()
                    // all other actuator endpoints require ACTUATOR role
                    .requestMatchers(EndpointRequest.toAnyEndpoint()).hasRole(ACTUATOR_ROLE)
                    // all other requests are permitted without authentication (we can update this later as needed)
                    .anyRequest().permitAll()
            )
            .httpBasic()
            .and()
            .csrf(csrf -> csrf.ignoringRequestMatchers(EndpointRequest.toAnyEndpoint()));

        return http.build();
    }

    // for testing purposes ONLY, we create an in-memory user with ACTUATOR role
    // this should be replaced with a more secure user management in production, like OAuth2, LDAP, etc.
    @Bean
    UserDetailsService authenticatedActuatorUser() {
        return new InMemoryUserDetailsManager(
            User.withDefaultPasswordEncoder()
                .username("actuator")
                .password("actuator")
                .roles(ACTUATOR_ROLE)
                .build()
        );
    }
}
