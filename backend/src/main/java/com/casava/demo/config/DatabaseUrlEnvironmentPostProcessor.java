package com.casava.demo.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.StringUtils;

/**
 * Maps Railway-style DATABASE_URL (postgres://user:pass@host:port/db) to Spring datasource
 * properties. When unset, application.yml H2 defaults remain. When set, DATABASE_URL overrides
 * yml H2 unless SPRING_DATASOURCE_URL is explicitly present in the process environment.
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

  @Override
  public void postProcessEnvironment(
      ConfigurableEnvironment environment, SpringApplication application) {
    String databaseUrl = environment.getProperty("DATABASE_URL");
    if (!StringUtils.hasText(databaseUrl)) {
      return;
    }
    // Prefer an intentional env override; do not treat application.yml H2 as a blocker.
    if (hasSystemEnvironmentProperty(environment, "SPRING_DATASOURCE_URL")) {
      return;
    }

    URI uri = URI.create(databaseUrl);
    String scheme = uri.getScheme();
    if (scheme == null) {
      return;
    }
    boolean postgres =
        scheme.equals("postgres") || scheme.equals("postgresql");
    if (!postgres) {
      return;
    }

    String userInfo = uri.getUserInfo();
    String username = null;
    String password = null;
    if (userInfo != null) {
      String[] parts = userInfo.split(":", 2);
      username = decode(parts[0]);
      if (parts.length > 1) {
        password = decode(parts[1]);
      }
    }

    String path = uri.getPath() == null ? "" : uri.getPath();
    String jdbcUrl =
        "jdbc:postgresql://"
            + uri.getHost()
            + (uri.getPort() > 0 ? ":" + uri.getPort() : "")
            + path
            + (uri.getQuery() != null ? "?" + uri.getQuery() : "");

    Map<String, Object> props = new HashMap<>();
    props.put("spring.datasource.url", jdbcUrl);
    props.put("spring.datasource.driver-class-name", "org.postgresql.Driver");
    if (username != null) {
      props.put("spring.datasource.username", username);
    }
    if (password != null) {
      props.put("spring.datasource.password", password);
    }
    environment
        .getPropertySources()
        .addFirst(new MapPropertySource("databaseUrlProcessor", props));
  }

  private static boolean hasSystemEnvironmentProperty(
      ConfigurableEnvironment environment, String name) {
    PropertySource<?> systemEnvironment = environment.getPropertySources().get("systemEnvironment");
    return systemEnvironment != null && systemEnvironment.containsProperty(name);
  }

  private static String decode(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }
}
