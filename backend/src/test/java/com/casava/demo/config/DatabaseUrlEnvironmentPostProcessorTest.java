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
import org.springframework.core.env.SystemEnvironmentPropertySource;

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

  @Test
  void databaseUrlOverridesApplicationYmlWhenSet() {
    ConfigurableEnvironment env = new StandardEnvironment();
    Map<String, Object> yml = new HashMap<>();
    yml.put("spring.datasource.url", "jdbc:h2:file:./data/casava;DB_CLOSE_DELAY=-1");
    yml.put("spring.datasource.username", "sa");
    yml.put("spring.datasource.password", "");
    env.getPropertySources().addLast(new MapPropertySource("applicationConfig", yml));

    Map<String, Object> map = new HashMap<>();
    map.put("DATABASE_URL", "postgres://myuser:s3cret@hostname:5432/railway");
    env.getPropertySources().addFirst(new MapPropertySource("systemEnvironment", map));

    new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(env, new SpringApplication());

    assertThat(env.getProperty("spring.datasource.url"))
        .isEqualTo("jdbc:postgresql://hostname:5432/railway");
  }

  @Test
  void skipsWhenSpringDatasourceUrlEnvIsSet() {
    ConfigurableEnvironment env = new StandardEnvironment();
    Map<String, Object> yml = new HashMap<>();
    yml.put("spring.datasource.url", "jdbc:h2:file:./data/casava;DB_CLOSE_DELAY=-1");
    env.getPropertySources().addLast(new MapPropertySource("applicationConfig", yml));

    Map<String, Object> systemEnv = new HashMap<>();
    systemEnv.put("DATABASE_URL", "postgres://myuser:s3cret@hostname:5432/railway");
    systemEnv.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://explicit:5432/db");
    env.getPropertySources()
        .replace(
            "systemEnvironment",
            new SystemEnvironmentPropertySource("systemEnvironment", systemEnv));

    new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(env, new SpringApplication());

    assertThat(env.getPropertySources().contains("databaseUrlProcessor")).isFalse();
    assertThat(env.getProperty("spring.datasource.url"))
        .isEqualTo("jdbc:postgresql://explicit:5432/db");
  }

  @Test
  void urlDecodesUsernameAndPassword() {
    ConfigurableEnvironment env = new StandardEnvironment();
    Map<String, Object> map = new HashMap<>();
    map.put(
        "DATABASE_URL",
        "postgres://my%40user:p%40ss%3Aword@hostname:5432/railway");
    env.getPropertySources().addFirst(new MapPropertySource("systemEnvironment", map));

    new DatabaseUrlEnvironmentPostProcessor().postProcessEnvironment(env, new SpringApplication());

    assertThat(env.getProperty("spring.datasource.username")).isEqualTo("my@user");
    assertThat(env.getProperty("spring.datasource.password")).isEqualTo("p@ss:word");
  }
}
