package com.casava.demo.auth;

import java.time.Instant;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  public AuthService(
      UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.jwtService = jwtService;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional
  public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (userRepository.existsByEmail(email)) {
      throw new AuthException(HttpStatus.CONFLICT, "Email already registered");
    }

    User user = new User();
    user.setEmail(email);
    user.setName(request.name());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setCreatedAt(Instant.now());

    User saved = userRepository.save(user);
    return toAuthResponse(saved);
  }

  @Transactional(readOnly = true)
  public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
    String email = normalizeEmail(request.email());
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(
                () -> new AuthException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    return toAuthResponse(user);
  }

  private static String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private AuthDtos.AuthResponse toAuthResponse(User user) {
    String token = jwtService.createToken(user.getId(), user.getEmail());
    AuthDtos.UserResponse userResponse =
        new AuthDtos.UserResponse(user.getId(), user.getEmail(), user.getName());
    return new AuthDtos.AuthResponse(token, userResponse);
  }
}
