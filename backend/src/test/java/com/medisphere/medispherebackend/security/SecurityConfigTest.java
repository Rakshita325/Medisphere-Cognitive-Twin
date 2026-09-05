package com.medisphere.medispherebackend.security;

import com.medisphere.medispherebackend.config.SecurityConfig;
import com.medisphere.medispherebackend.controller.FhirPatientController;
import com.medisphere.medispherebackend.fhir.FhirPatientService;
import com.medisphere.medispherebackend.service.ConsentAuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SecurityConfigTest.Users.class)
class SecurityConfigTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean FhirPatientService fhirPatientService;
    @MockitoBean ConsentAuthorizationService consentAuthorizationService;

    @Test
    void unauthenticatedFhirRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/fhir/patients"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedDoctorCanReachFhirRoute() throws Exception {
        mockMvc.perform(get("/api/fhir/patients")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("admin1").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class Users {
        @Bean
        InMemoryUserDetailsManager users() {
            return new InMemoryUserDetailsManager(User.withUsername("doctor")
                    .password(new BCryptPasswordEncoder().encode("password"))
                    .roles("DOCTOR").build());
        }
    }
}
