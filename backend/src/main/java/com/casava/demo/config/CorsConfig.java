package com.casava.demo.config;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

  private final AiProperties aiProperties;

  public CorsConfig(AiProperties aiProperties) {
    this.aiProperties = aiProperties;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    String[] origins = parseOrigins(aiProperties.getFrontendOrigin());
    log.info("CORS allowed origins: {}", Arrays.toString(origins));
    registry
        .addMapping("/**")
        .allowedOrigins(origins)
        .allowedMethods("GET", "POST", "OPTIONS")
        .allowedHeaders("*")
        .exposedHeaders("*");
  }

  static String[] parseOrigins(String raw) {
    if (raw == null || raw.isBlank()) {
      return new String[] {"http://localhost:5173"};
    }
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
        .toArray(String[]::new);
  }
}
