package com.ecommerce.controller;

import com.ecommerce.config.JwtService;
import com.ecommerce.dto.*;
import com.ecommerce.entity.User;
import com.ecommerce.exception.DuplicateResourceException;
import com.ecommerce.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Register and login")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new DuplicateResourceException("Email already registered: " + req.getEmail());
        User user = User.builder()
                .firstName(req.getFirstName()).lastName(req.getLastName())
                .email(req.getEmail()).password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone()).roles(Set.of("USER")).active(true).build();
        User saved = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok("Registration successful", buildAuth(saved)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        User user = userRepository.findByEmailAndActiveTrue(req.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword()))
            throw new BadCredentialsException("Invalid credentials");
        return ResponseEntity.ok(ApiResponse.ok("Login successful", buildAuth(user)));
    }

    private AuthResponse buildAuth(User user) {
        return AuthResponse.builder()
                .token(jwtService.generateToken(user)).tokenType("Bearer")
                .userId(user.getId()).email(user.getEmail()).fullName(user.getFullName())
                .roles(List.copyOf(user.getRoles())).build();
    }
}
