package com.casava.demo;

import com.casava.demo.config.AiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AiProperties.class)
public class CasavaDemoApplication {

  public static void main(String[] args) {
    SpringApplication.run(CasavaDemoApplication.class, args);
  }
}
