package com.casava.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

  private final AiProperties aiProperties;

  public CorsConfig(AiProperties aiProperties) {
    this.aiProperties = aiProperties;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedOrigins(aiProperties.getFrontendOrigin())
        .allowedMethods("GET", "POST", "OPTIONS")
        .allowedHeaders("*");
  }
}
