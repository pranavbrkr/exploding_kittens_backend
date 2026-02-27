package com.kitten.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication(scanBasePackages = "com.kitten")
@EntityScan(basePackages = "com.kitten")
@EnableJpaRepositories(basePackages = "com.kitten")
public class SessionServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(SessionServiceApplication.class, args);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
