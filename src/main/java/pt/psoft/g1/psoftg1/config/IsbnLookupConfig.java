package pt.psoft.g1.psoftg1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class IsbnLookupConfig {
  @Bean
  public Executor isbnLookupExecutor() {
    return Executors.newFixedThreadPool(4);
  }
}
