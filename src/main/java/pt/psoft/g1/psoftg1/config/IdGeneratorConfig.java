package pt.psoft.g1.psoftg1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import pt.psoft.g1.psoftg1.shared.id.Base64UrlIdGenerator;
import pt.psoft.g1.psoftg1.shared.id.IdGenerator;
import pt.psoft.g1.psoftg1.shared.id.UlidGenerator;

@Configuration
public class IdGeneratorConfig {

  @Bean
  @Profile("id-base64")
  public IdGenerator base64IdGenerator() {
    return new Base64UrlIdGenerator();
  }

  @Bean
  @Profile("id-ts")
  public IdGenerator timestampIdGenerator() {
    return new UlidGenerator();
  }
}
