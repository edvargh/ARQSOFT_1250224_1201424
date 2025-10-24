package pt.psoft.g1.psoftg1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import pt.psoft.g1.psoftg1.shared.id.Base65IdGenerator;
import pt.psoft.g1.psoftg1.shared.id.IdGenerator;
import pt.psoft.g1.psoftg1.shared.id.UlidGenerator;

@Configuration
public class IdGeneratorConfig {

  @Bean
  @Profile("id-base65")
  public IdGenerator base65IdGenerator() {
    return new Base65IdGenerator();
  }

  @Bean
  @Profile("id-ts")
  public IdGenerator timestampIdGenerator() {
    return new UlidGenerator();
  }
}
