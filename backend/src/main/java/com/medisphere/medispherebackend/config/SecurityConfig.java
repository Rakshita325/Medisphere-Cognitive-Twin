package com.medisphere.medispherebackend.config;

import jakarta.servlet.http.HttpServletResponse;
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

                        // ML prediction APIs
                        .requestMatchers("/api/ml/**")
                        .hasAnyRole("ADMIN", "DOCTOR")

                        // Patient Twin APIs
                        .requestMatchers("/api/patient-twins/**")
                        .hasAnyRole("ADMIN", "DOCTOR","NURSE")

                        // FHIR APIs
                        .requestMatchers("/api/fhir/**")
                        .hasAnyRole("ADMIN", "DOCTOR","NURSE")

                        // Vitals APIs
                        .requestMatchers("/api/vitals/**")
                        .hasAnyRole("ADMIN", "DOCTOR", "NURSE")

                        // Alert APIs (Milestone 3)
                        .requestMatchers("/api/alerts/**")
                        .hasAnyRole("ADMIN", "DOCTOR", "NURSE")

                        // Vital Record APIs (Milestone 3)
                        .requestMatchers("/api/vital-records/**")
                        .hasAnyRole("ADMIN", "DOCTOR", "NURSE")

                        // Care Plan APIs (Milestone 4 Part 1)
                        .requestMatchers("/api/care-plans/**", "/api/careplans/**")
                        .hasAnyRole("ADMIN", "DOCTOR", "NURSE")

                        // Treatment Tracking APIs (Milestone 4 Part 2)
                        .requestMatchers("/api/treatment-tracking/**")
                        .hasAnyRole("ADMIN", "DOCTOR", "NURSE")

                        // Health Monitoring APIs (Milestone 4 Part 3)
                        .requestMatchers("/api/health-monitoring/**")
                        .hasAnyRole("ADMIN", "DOCTOR", "NURSE")

                        // Progress / Improvement APIs (Milestone 4 Part 4)
                        .requestMatchers("/api/progress/**")
                        .hasAnyRole("ADMIN", "DOCTOR", "NURSE")

                        // Patient Data APIs (Rich health records from patient-data.json)
                        .requestMatchers("/api/patient-data/**")
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
                .httpBasic(httpBasic -> httpBasic.authenticationEntryPoint((request, response, authException) -> {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, authException.getMessage());
                }));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}