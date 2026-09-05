package com.casava.demo.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  @Test
  void roundTripsUserIdAndEmail() {
    UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    JwtService jwt = new JwtService("test-secret-must-be-long-enough-123456", 3_600_000L);
    String token = jwt.createToken(userId, "ada@example.com");
    assertThat(jwt.parseUserId(token)).isEqualTo(userId);
    assertThat(jwt.parseEmail(token)).isEqualTo("ada@example.com");
  }
}
