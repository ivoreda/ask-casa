package com.casava.demo.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;

  private JwtService jwtService;
  private PasswordEncoder passwordEncoder;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService("test-secret-must-be-long-enough-123456", 3_600_000L);
    passwordEncoder = new BCryptPasswordEncoder();
    authService = new AuthService(userRepository, jwtService, passwordEncoder);
  }

  @Test
  void registerHashesPasswordAndReturnsToken() {
    when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User user = invocation.getArgument(0);
              user.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
              return user;
            });

    AuthDtos.AuthResponse response =
        authService.register(
            new AuthDtos.RegisterRequest("  Ada@Example.COM  ", "secret123", "Ada Lovelace"));

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.getEmail()).isEqualTo("ada@example.com");
    assertThat(saved.getName()).isEqualTo("Ada Lovelace");
    assertThat(saved.getPasswordHash()).isNotEqualTo("secret123");
    assertThat(passwordEncoder.matches("secret123", saved.getPasswordHash())).isTrue();
    assertThat(saved.getCreatedAt()).isNotNull();

    assertThat(response.token()).isNotBlank();
    assertThat(jwtService.parseUserId(response.token()))
        .isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    assertThat(jwtService.parseEmail(response.token())).isEqualTo("ada@example.com");
    assertThat(response.user().email()).isEqualTo("ada@example.com");
    assertThat(response.user().name()).isEqualTo("Ada Lovelace");
  }

  @Test
  void registerDuplicateEmailThrowsConflict() {
    when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

    assertThatThrownBy(
            () ->
                authService.register(
                    new AuthDtos.RegisterRequest("  ADA@EXAMPLE.COM ", "secret123", "Ada")))
        .isInstanceOf(AuthException.class)
        .satisfies(
            ex -> {
              AuthException authEx = (AuthException) ex;
              assertThat(authEx.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            });

    verify(userRepository, never()).save(any());
  }

  @Test
  void loginBadPasswordThrowsUnauthorized() {
    User user = new User();
    user.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    user.setEmail("ada@example.com");
    user.setName("Ada");
    user.setPasswordHash(passwordEncoder.encode("correct-password"));

    when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

    assertThatThrownBy(
            () ->
                authService.login(
                    new AuthDtos.LoginRequest("  Ada@Example.COM ", "wrong-password")))
        .isInstanceOf(AuthException.class)
        .satisfies(
            ex -> {
              AuthException authEx = (AuthException) ex;
              assertThat(authEx.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
            });
  }

  @Test
  void loginNormalizesEmailBeforeLookup() {
    User user = new User();
    user.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
    user.setEmail("ada@example.com");
    user.setName("Ada");
    user.setPasswordHash(passwordEncoder.encode("secret123"));

    when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

    AuthDtos.AuthResponse response =
        authService.login(new AuthDtos.LoginRequest("  ADA@example.COM ", "secret123"));

    assertThat(response.user().email()).isEqualTo("ada@example.com");
    assertThat(jwtService.parseEmail(response.token())).isEqualTo("ada@example.com");
  }
}
