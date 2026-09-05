package com.medisphere.medispherebackend.service;

import com.medisphere.medispherebackend.dto.AuthResponse;
import com.medisphere.medispherebackend.dto.LoginRequest;
import com.medisphere.medispherebackend.dto.SignupRequest;
import com.medisphere.medispherebackend.model.Role;
import com.medisphere.medispherebackend.model.User;
import com.medisphere.medispherebackend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class AuthService implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse signup(SignupRequest request) {
        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Full name is required");
        }

        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }

        String username = request.getUsername().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username '" + username + "' is already taken");
        }

        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters long");
        }

        // Do NOT allow unauthenticated users to create an ADMIN account
        if (request.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Creating an ADMIN account via public registration is not permitted");
        }

        Role role = request.getRole() != null ? request.getRole() : Role.DOCTOR;

        User user = new User();
        user.setFullName(request.getFullName().trim());
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getUsername(),
                savedUser.getRole(),
                savedUser.getCreatedAt()
        );
    }

    public AuthResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password are required");
        }

        String username = request.getUsername().trim().toLowerCase();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        return new AuthResponse(
                user.getId(),
                user.getFullName() != null ? user.getFullName() : user.getUsername(),
                user.getUsername(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    @Override
    public void run(String... args) {
        // Seed/migrate default accounts with BCrypt passwords if not present
        seedUserIfNotExists("doctor1", "Dr. Sarah Mitchell", "doctor123", Role.DOCTOR);
        seedUserIfNotExists("admin1", "System Administrator", "admin123", Role.ADMIN);
        seedUserIfNotExists("nurse1", "Nurse James Wilson", "nurse123", Role.NURSE);
    }

    private void seedUserIfNotExists(String username, String fullName, String rawPassword, Role role) {
        if (!userRepository.existsByUsername(username)) {
            User user = new User();
            user.setFullName(fullName);
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(rawPassword));
            user.setRole(role);
            user.setCreatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }
}
