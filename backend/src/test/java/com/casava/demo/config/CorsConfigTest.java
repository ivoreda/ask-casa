package com.casava.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CorsConfigTest {

  @Test
  void parseOriginsTrimsAndSplits() {
    assertThat(CorsConfig.parseOrigins(" https://a.example/ ,http://localhost:5173"))
        .containsExactly("https://a.example", "http://localhost:5173");
  }

  @Test
  void parseOriginsDefaultsWhenBlank() {
    assertThat(CorsConfig.parseOrigins("  ")).containsExactly("http://localhost:5173");
  }
}
