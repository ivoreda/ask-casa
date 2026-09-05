package com.casava.demo.auth;

import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

  private CurrentUser() {}

  public static UUID requireUserId() {
    return findUserId()
        .orElseThrow(() -> new AuthException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
  }

  public static boolean isPresent() {
    return findUserId().isPresent();
  }

  public static Optional<UUID> findUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof UUID userId) {
      return Optional.of(userId);
    }
    return Optional.empty();
  }
}
