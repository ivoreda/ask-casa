package com.casava.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class DatabaseUrlEnvironmentPostProcessorTest {

  @Test
  void convertsPostgresDatabaseUrlToSpringDatasourceProperties() {
    ConfigurableEnvironment env = new StandardEnvironment();
    Map<String, Object> map = new HashMap<>();
    map.put(
        "DATABASE_URL",
        "postgres://myuser:s3cret@hostname:5432/railway");
    env.getPropertySources().addFirst(new MapPropertySource("test", map));

    EnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
    processor.postProcessEnvironment(env, new SpringApplication());

    assertThat(env.getProperty("spring.datasource.url"))
        .isEqualTo("jdbc:postgresql://hostname:5432/railway");
    assertThat(env.getProperty("spring.datasource.username")).isEqualTo("myuser");
    assertThat(env.getProperty("spring.datasource.password")).isEqualTo("s3cret");
    assertThat(env.getProperty("spring.datasource.driver-class-name"))
        .isEqualTo("org.postgresql.Driver");
  }

  @Test
  void leavesH2DefaultsWhenDatabaseUrlMissing() {
    ConfigurableEnvironment env = new StandardEnvironment();
    EnvironmentPostProcessor processor = new DatabaseUrlEnvironmentPostProcessor();
    processor.postProcessEnvironment(env, new SpringApplication());

    assertThat(env.getProperty("spring.datasource.url")).isNull();
  }
}
