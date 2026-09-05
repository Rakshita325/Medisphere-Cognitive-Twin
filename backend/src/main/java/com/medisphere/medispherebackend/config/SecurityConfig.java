package com.medisphere.medispherebackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        // Authentication APIs (Public)
                        .requestMatchers("/api/auth/**")
                        .permitAll()

                        // Patient Twin APIs
                        .requestMatchers("/api/patient-twins/**")
                        .hasAnyRole("ADMIN", "DOCTOR","NURSE")

                        // FHIR APIs
                        .requestMatchers("/api/fhir/**")
                        .hasAnyRole("ADMIN", "DOCTOR","NURSE")

                        // Vitals APIs
                        .requestMatchers("/api/vitals/**")
                        .hasAnyRole("ADMIN", "DOCTOR", "NURSE")

                        // Admin APIs
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        // Doctor APIs
                        .requestMatchers("/api/doctor/**")
                        .hasAnyRole("ADMIN", "DOCTOR")

                        // Nurse APIs
                        .requestMatchers("/api/nurse/**")
                        .hasAnyRole("ADMIN", "DOCTOR", "NURSE")

                        // All remaining API requests
                        .requestMatchers("/api/**")
                        .authenticated()

                        // Everything else
                        .anyRequest()
                        .permitAll()
                )
                .httpBasic(httpBasic -> {});

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}