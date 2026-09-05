package com.casava.demo.auth;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final UserRepository userRepository;

  public AuthController(AuthService authService, UserRepository userRepository) {
    this.authService = authService;
    this.userRepository = userRepository;
  }

  @PostMapping("/register")
  public AuthDtos.AuthResponse register(@RequestBody AuthDtos.RegisterRequest request) {
    return authService.register(request);
  }

  @PostMapping("/login")
  public AuthDtos.AuthResponse login(@RequestBody AuthDtos.LoginRequest request) {
    return authService.login(request);
  }

  @GetMapping("/me")
  public AuthDtos.UserResponse me() {
    UUID userId = CurrentUser.requireUserId();
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "User not found"));
    return new AuthDtos.UserResponse(user.getId(), user.getEmail(), user.getName());
  }
}
