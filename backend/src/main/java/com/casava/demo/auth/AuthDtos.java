package com.casava.demo.auth;

import java.util.UUID;

public final class AuthDtos {

  private AuthDtos() {}

  public record RegisterRequest(String email, String password, String name) {}

  public record LoginRequest(String email, String password) {}

  public record UserResponse(UUID id, String email, String name) {}

  public record AuthResponse(String token, UserResponse user) {}
}
